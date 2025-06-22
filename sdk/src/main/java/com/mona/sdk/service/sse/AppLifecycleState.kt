package com.mona.sdk.service.sse

/**
 * Enum for app lifecycle states
 */
internal enum class AppLifecycleState {
    Resumed,
    Inactive,
    Paused,
    Detached,
    Hidden
}