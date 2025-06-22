package com.mona.sdk.service.sse

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.sse.EventSource
import timber.log.Timber

/**
 * Individual SSE connection wrapper with background support
 */
internal class SseConnection(
    val config: SseListenerConfig,
    val url: String,
    private val scope: CoroutineScope
) {
    var state: SseConnectionState = SseConnectionState.Disconnected
    private var eventSource: EventSource? = null
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    var isInBackground: Boolean = false
    var lastEventReceived: Long? = null
    var consecutiveErrors: Int = 0

    val isActive: Boolean
        get() = eventSource != null

    val isHealthy: Boolean
        get() = consecutiveErrors < 3

    /** Start heartbeat for background mode */
    fun startBackgroundHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isInBackground) {
                delay(10_000) // 10 seconds
                sendHeartbeat()
            }
        }
    }

    /** Stop heartbeat */
    fun stopBackgroundHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    /** Send heartbeat to keep connection alive */
    private fun sendHeartbeat() {
        logBackgroundMessage("SSEConnection ::: SENDING HEARTBEAT :::")
        // This is a lightweight ping to keep the connection active
        lastEventReceived = System.currentTimeMillis()
    }

    /** Mark connection as background mode */
    fun enterBackgroundMode() {
        isInBackground = true
        startBackgroundHeartbeat()
    }

    /** Mark connection as foreground mode */
    fun exitBackgroundMode() {
        isInBackground = false
        stopBackgroundHeartbeat()
    }

    /** Reset error count on successful event */
    fun resetErrorCount() {
        consecutiveErrors = 0
    }

    /** Increment error count */
    fun incrementErrorCount() {
        consecutiveErrors++
    }

    fun dispose() {
        reconnectJob?.cancel()
        reconnectJob = null

        heartbeatJob?.cancel()
        heartbeatJob = null

        eventSource?.cancel()
        eventSource = null

        state = SseConnectionState.Disconnected
    }

    fun setEventSource(eventSource: EventSource) {
        this.eventSource = eventSource
    }

    fun scheduleReconnect(delay: Long, reconnectAction: () -> Unit) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(delay * 1000) // Convert to milliseconds
            if (isActive || state != SseConnectionState.Disconnected) {
                reconnectAction()
            }
        }
    }

    private fun logBackgroundMessage(message: String) {
        Timber.i("🌙 [SSE-Background] $message")
    }
}