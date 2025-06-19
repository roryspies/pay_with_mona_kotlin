package com.mona.sdk.data.service.sdk

import com.mona.sdk.data.service.sse.FirebaseSseListener
import com.mona.sdk.data.service.sse.SseListenerType
import com.mona.sdk.domain.MonaSdkState
import com.mona.sdk.event.SdkState
import com.mona.sdk.event.TransactionState
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import kotlin.coroutines.resume

internal class SseListener(
    private val sse: () -> FirebaseSseListener,
    private val state: () -> MonaSdkState,
    private val performAuth: (String, Boolean) -> Unit,
    private val closeCustomTabs: () -> Unit,
    private val updateSdkState: (SdkState) -> Unit,
    private val updateTransactionState: (TransactionState) -> Unit,
) {
    fun subscribeToEvents(type: SseListenerType) {
        sse().listenForEvents(
            type = type,
            identifier = when (type) {
                SseListenerType.PaymentUpdates, SseListenerType.TransactionMessages -> state().transactionId
                else -> null
            },
            onDataChange = {
                val response = Json.decodeFromString<JsonObject>(it)
                when (type) {
                    SseListenerType.CustomTabs -> {
                        if (response["success"]?.jsonPrimitive?.booleanOrNull == true) {
                            updateSdkState(SdkState.Idle)
                            updateTransactionState(TransactionState.NavToResult())
                            closeCustomTabs()
                        }
                    }

                    SseListenerType.PaymentUpdates, SseListenerType.TransactionMessages -> {
                        val state = state()
                        when (response["event"]?.jsonPrimitive?.content) {
                            "transaction_initiated", "progress_update" -> {
                                updateTransactionState(
                                    TransactionState.Initiated(
                                        friendlyId = state.friendlyId,
                                        transactionId = state.transactionId,
                                        amount = state.checkout?.transactionAmountInKobo
                                    )
                                )
                            }

                            "transaction_failed" -> {
                                Timber.e("Transaction failed received: $response")
                                updateTransactionState(
                                    TransactionState.Failed(
                                        friendlyId = state.friendlyId,
                                        transactionId = state.transactionId,
                                        amount = state.checkout?.transactionAmountInKobo
                                    )
                                )
                            }

                            "transaction_completed" -> {
                                Timber.i("Transaction completed received: $response")
                                updateTransactionState(
                                    TransactionState.Completed(
                                        friendlyId = state.friendlyId,
                                        transactionId = state.transactionId,
                                        amount = state.checkout?.transactionAmountInKobo
                                    )
                                )
                            }
                        }
                        updateSdkState(SdkState.Idle)
                    }

                    else -> {
                        // no-op for other types
                    }
                }
            },
            onError = { error ->
                Timber.e(error, "Error in SSE listener for $type")
//                throw error
            }
        )
    }

    suspend fun subscribeToAuthEvents(
        sessionId: String,
        isFromCollections: Boolean = false,
    ) = suspendCancellableCoroutine { cont ->
        sse().listenForEvents(
            type = SseListenerType.AuthenticationEvents,
            identifier = sessionId,
            onDataChange = { event ->
                Timber.i("SSE Auth Event: $event")
                when {
                    event.contains("strongAuthToken") -> {
                        val data = Json.decodeFromString<JsonObject>(event)
                        val token = data["strongAuthToken"]?.jsonPrimitive?.content
                        Timber.i("Strong Auth Token: $token")
                        performAuth(token.orEmpty(), isFromCollections)
                        cont.resume(token)
//                            strongAuthToken = JSONObject(event).getString("strongAuthToken")
//                            authStream.emit(AuthState.PerformingLogin)
//                            sdkCloseCustomTabs()
//                            sdkStateStream.emit(MonaSDKState.Loading)
//
//                            loginWithStrongAuth(isFromCollections)
//                            authCompleter.complete(Unit)
//                            resetPaymentWithPossibleKeyExchange()
                    }

                    else -> {
                        cont.resume(null)
                    }
                }
            },
            onError = {
                Timber.e(it, "Error in SSE listener for AuthenticationEvents")
                cont.resume(null)
//                cont.resumeWith(Result.failure(it))
            }
        )
    }
}