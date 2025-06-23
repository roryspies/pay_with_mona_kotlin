package ng.mona.paywithmona.service.sse

/**
 * Enum to represent different states of the SSE connection
 */
internal enum class SseConnectionState {
    /** Not connected to any Firebase event source */
    Disconnected,

    /** Currently establishing a connection */
    Connecting,

    /** Successfully connected and receiving events */
    Connected,

    /** Connection encountered an error */
    Error,

    /** Connection is in background mode (maintained but optimized) */
    BackgroundMaintained
}