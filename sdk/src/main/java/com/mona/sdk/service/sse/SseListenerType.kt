package com.mona.sdk.service.sse

/**
 * Enum to represent different types of SSE listeners
 */
internal enum class SseListenerType {
    PaymentUpdates,
    TransactionMessages,
    CustomTabs,
    AuthenticationEvents
}