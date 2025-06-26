package ng.mona.paywithmona.service.sdk

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import ng.mona.paywithmona.data.model.MonaProduct
import ng.mona.paywithmona.data.serializer.SdkJson
import ng.mona.paywithmona.domain.PayWithMonaSdkState
import ng.mona.paywithmona.event.SdkState
import ng.mona.paywithmona.event.TransactionState
import ng.mona.paywithmona.service.sse.FirebaseSseListener
import ng.mona.paywithmona.service.sse.SseListenerType
import ng.mona.paywithmona.util.resumeSafely
import timber.log.Timber

internal class SseListener(
    private val sse: () -> FirebaseSseListener,
    private val state: () -> PayWithMonaSdkState,
    private val performAuth: suspend (String, MonaProduct) -> Boolean,
    private val closeCustomTabs: () -> Unit,
    private val updateSdkState: (SdkState) -> Unit,
    private val updateTransactionState: (TransactionState) -> Unit,
) {
    fun subscribeToEvents(type: SseListenerType) {
        sse().listenForEvents(
            type = type,
            identifier = when (type) {
                SseListenerType.PaymentUpdates, SseListenerType.TransactionMessages -> state().checkout?.transactionId
                else -> null
            },
            onDataChange = {
                val response = SdkJson.decodeFromString<JsonObject>(it)
                when (type) {
                    SseListenerType.CustomTabs -> {
                        if (response["success"]?.jsonPrimitive?.booleanOrNull == true) {
                            val state = state()
                            updateSdkState(SdkState.Idle)
                            updateTransactionState(
                                TransactionState.NavigateToResult(
                                    friendlyId = state.checkout?.friendlyId,
                                    transactionId = state.checkout?.transactionId,
                                    amount = state.checkout?.transactionAmountInKobo,
                                )
                            )
                            closeCustomTabs()
                        }
                    }

                    SseListenerType.PaymentUpdates, SseListenerType.TransactionMessages -> {
                        val state = state()
                        when (response["event"]?.jsonPrimitive?.content) {
                            "transaction_initiated" -> {
                                updateTransactionState(
                                    TransactionState.Initiated(
                                        friendlyId = state.checkout?.friendlyId,
                                        transactionId = state.checkout?.transactionId,
                                        amount = state.checkout?.transactionAmountInKobo,
                                    )
                                )
                            }

                            "progress_update" -> {
                                updateTransactionState(
                                    TransactionState.ProgressUpdate(
                                        friendlyId = state.checkout?.friendlyId,
                                        transactionId = state.checkout?.transactionId,
                                        amount = state.checkout?.transactionAmountInKobo,
                                    )
                                )
                            }

                            "transaction_failed" -> {
                                Timber.e("Transaction failed received: $response")
                                updateTransactionState(
                                    TransactionState.Failed(
                                        friendlyId = state.checkout?.friendlyId,
                                        transactionId = state.checkout?.transactionId,
                                        amount = state.checkout?.transactionAmountInKobo
                                    )
                                )
                            }

                            "transaction_completed" -> {
                                updateTransactionState(
                                    TransactionState.Completed(
                                        friendlyId = state.checkout?.friendlyId,
                                        transactionId = state.checkout?.transactionId,
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
                    val data = SdkJson.decodeFromString<JsonObject>(event)
                    val token = data["strongAuthToken"]?.jsonPrimitive?.content

                    CoroutineScope(cont.context).launch {
                        val response = performAuth(token.orEmpty(), product)
                        cont.resumeSafely(response)
                    }
                }
            },
            onError = {
                Timber.e(it, "Error in SSE listener for AuthenticationEvents")
            }
        )
    }
}