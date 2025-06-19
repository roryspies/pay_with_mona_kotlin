package com.mona.sdk.data.service.sse

/**
 * Enum to represent different types of SSE listeners
 */
internal enum class SseListenerType {
    PaymentUpdates,
    TransactionMessages,
    CustomTabs,
    AuthenticationEvents
}