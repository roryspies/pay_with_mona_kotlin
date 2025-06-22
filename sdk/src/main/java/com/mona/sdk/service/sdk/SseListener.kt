package com.mona.sdk.service.sdk

import com.mona.sdk.data.model.MonaProduct
import com.mona.sdk.domain.MonaSdkState
import com.mona.sdk.event.SdkState
import com.mona.sdk.event.TransactionState
import com.mona.sdk.service.sse.FirebaseSseListener
import com.mona.sdk.service.sse.SseListenerType
import com.mona.sdk.util.resumeSafely
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

internal class SseListener(
    private val sse: () -> FirebaseSseListener,
    private val state: () -> MonaSdkState,
    private val performAuth: suspend (String, MonaProduct) -> Unit,
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
                            "transaction_initiated" -> {
                                updateTransactionState(
                                    TransactionState.Initiated(
                                        friendlyId = state.friendlyId,
                                        transactionId = state.transactionId,
                                        amount = state.checkout?.transactionAmountInKobo,
                                    )
                                )
                            }

                            "progress_update" -> {
                                updateTransactionState(
                                    TransactionState.ProgressUpdate(
                                        friendlyId = state.friendlyId,
                                        transactionId = state.transactionId,
                                        amount = state.checkout?.transactionAmountInKobo,
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
                                Timber.e("Transaction completed received: $response")
                                updateTransactionState(
                                    TransactionState.Completed(
                                        friendlyId = state.friendlyId,
                                        transactionId = state.transactionId,
                                        amount = state.checkout?.transactionAmountInKobo
                                    )
                                )
                            }
                        }
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
        product: MonaProduct = MonaProduct.Checkout,
    ) = suspendCancellableCoroutine { cont ->
        sse().listenForEvents(
            type = SseListenerType.AuthenticationEvents,
            identifier = sessionId,
            onDataChange = { event ->
                if (event.contains("strongAuthToken")) {
                    val data = Json.decodeFromString<JsonObject>(event)
                    val token = data["strongAuthToken"]?.jsonPrimitive?.content

                    CoroutineScope(cont.context).launch {
                        performAuth(token.orEmpty(), product)
                        cont.resumeSafely(token)
                    }
                }
            },
            onError = {
                Timber.e(it, "Error in SSE listener for AuthenticationEvents")
            }
        )
    }
}