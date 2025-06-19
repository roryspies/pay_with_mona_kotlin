package com.mona.sdk.data.service.sse

/**
 * Configuration for an SSE listener
 */
internal data class SseListenerConfig(
    val type: SseListenerType,
    val identifier: String? = null, // transactionId or sessionId
    val onDataChange: ((String) -> Unit)? = null,
    val onError: ((Throwable) -> Unit)? = null,
    val autoReconnect: Boolean = true
) {
    /** Generate a unique key for this listener configuration */
    val key: String
        get() = "${type.name}_${identifier ?: "global"}"

    /** Get the Firebase path for this listener type */
    val path: String
        get() = when (type) {
            SseListenerType.PaymentUpdates -> "/public/paymentUpdate/$identifier.json"
            SseListenerType.TransactionMessages -> "/public/transaction-messages/$identifier.json"
            SseListenerType.CustomTabs -> "/public/close_tab.json"
            SseListenerType.AuthenticationEvents -> "/public/login_success/authn_$identifier.json"
        }

    /** Get display name for logging */
    val displayName: String
        get() = when (type) {
            SseListenerType.PaymentUpdates -> "Payment Updates"
            SseListenerType.TransactionMessages -> "Transaction Messages"
            SseListenerType.CustomTabs -> "Custom Tabs"
            SseListenerType.AuthenticationEvents -> "Authentication Events"
        }
}