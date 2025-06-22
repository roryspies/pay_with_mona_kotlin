package com.mona.sdk.service.sse

import com.mona.sdk.data.remote.ApiConfig.FIREBASE_DB_URL
import com.mona.sdk.domain.SingletonCompanion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Enhanced Firebase Server-Sent Events (SSE) Listener with Background Support
 *
 * Provides robust real-time event listening for Firebase Realtime Database with
 * automatic reconnection, proper resource management, state tracking, and
 * background mode support to maintain connections when custom tabs are opened.
 */
internal class FirebaseSseListener private constructor() {

    // MARK: - Properties

    /** HTTP client for making network requests */
    private lateinit var okHttpClient: OkHttpClient

    /** Firebase Realtime Database URL */
    private var databaseUrl = FIREBASE_DB_URL

    /** Map of active SSE connections */
    private val activeConnections = ConcurrentHashMap<String, SseConnection>()

    /** Flow for broadcasting connection state changes */
    private val stateFlow = MutableStateFlow<Map<String, SseConnectionState>>(emptyMap())

    /** Flag to track initialization state */
    private var isInitialized: Boolean = false

    /** Background manager instance */
    private val backgroundManager = SseBackgroundManager()

    /** Coroutine scope for managing background tasks */
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Job for periodic connection health checks */
    private var healthCheckJob: Job? = null

    /** Flag to track if custom tab is currently open */
    private var isCustomTabOpen: Boolean = false

    // MARK: - Public Getters

    /** Get all connection states */
    val connectionStates: Map<String, SseConnectionState>
        get() = activeConnections.mapValues { it.value.state }

    /** Flow of connection state changes for all listeners */
    val connectionStateFlow: StateFlow<Map<String, SseConnectionState>>
        get() {
            ensureInitialized()
            return stateFlow.asStateFlow()
        }

    /** Check if any listeners are active */
    val hasActiveListeners: Boolean
        get() = activeConnections.isNotEmpty()

    /** Get count of active listeners */
    val activeListenerCount: Int
        get() = activeConnections.size

    /** Check if app is in background mode */
    val isInBackground: Boolean
        get() = backgroundManager.isInBackground

    /** Check if custom tab is open */
    val customTabOpen: Boolean
        get() = isCustomTabOpen

    // MARK: - Initialization

    init {
        backgroundManager.initialize()
    }

    /** Ensures the listener is properly initialized */
    private fun ensureInitialized() {
        if (!isInitialized) {
            okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // No timeout for SSE
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            // Start health check timer
            startHealthCheckTimer()

            isInitialized = true
            logMessage("Initialized base components with background support")
        }
    }

    /** Initialize the SSE listener with a Firebase Realtime Database URL */
    fun initialize(databaseUrl: String? = null) {
        ensureInitialized()

        if (!databaseUrl.isNullOrBlank()) {
            this.databaseUrl = databaseUrl.trim()
        }

        logMessage("Initialized with database URL: ${this.databaseUrl}")
    }

    /** Start health check timer for background maintenance */
    private fun startHealthCheckTimer() {
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch {
            while (isActive) {
                delay(30_000) // 30 seconds
                performHealthCheck()
            }
        }
    }

    /** Perform health check on all connections */
    private fun performHealthCheck() {
        if (activeConnections.isEmpty()) return

        logMessage("Performing health check on ${activeConnections.size} connections")

        val now = System.currentTimeMillis()
        val unhealthyConnections = mutableListOf<String>()

        for ((key, connection) in activeConnections) {
            // Check if connection is unhealthy
            if (!connection.isHealthy) {
                unhealthyConnections.add(key)
                continue
            }

            // Check if connection is stale (no events for 5 minutes)
            connection.lastEventReceived?.let { lastEvent ->
                val timeSinceLastEvent = now - lastEvent
                if (timeSinceLastEvent > 300_000) { // 5 minutes
                    logMessage("Connection $key appears stale, scheduling reconnect")
                    scheduleReconnect(key, connection.config)
                }
            }
        }

        // Reconnect unhealthy connections
        for (key in unhealthyConnections) {
            activeConnections[key]?.let { connection ->
                logMessage("Reconnecting unhealthy connection: $key")
                scheduleReconnect(key, connection.config)
            }
        }
    }

    // MARK: - Background Mode Management

    /** Handle custom tab opening */
    fun onCustomTabOpening() {
        isCustomTabOpen = true
        logMessage("Custom tab opening - entering background mode")
        enterBackgroundMode()
    }

    /** Handle custom tab closing */
    fun onCustomTabClosing() {
        isCustomTabOpen = false
        logMessage("Custom tab closed - checking if should exit background mode")

        // Only exit background mode if app is not actually backgrounded
        if (!backgroundManager.isInBackground) {
            exitBackgroundMode()
        }
    }

    /** Enter background mode for all connections */
    private fun enterBackgroundMode() {
        logMessage("Entering background mode for all connections")

        for (connection in activeConnections.values) {
            connection.enterBackgroundMode()
            if (connection.state == SseConnectionState.Connected) {
                connection.state = SseConnectionState.BackgroundMaintained
                updateConnectionState(
                    connection.config.key,
                    SseConnectionState.BackgroundMaintained
                )
            }
        }
    }

    /** Exit background mode for all connections */
    private fun exitBackgroundMode() {
        logMessage("Exiting background mode for all connections")

        for (connection in activeConnections.values) {
            connection.exitBackgroundMode()
            if (connection.state == SseConnectionState.BackgroundMaintained) {
                connection.state = SseConnectionState.Connected
                updateConnectionState(connection.config.key, SseConnectionState.Connected)
            }
        }
    }

    /** Handle app lifecycle changes */
    fun handleAppLifecycleChange(newState: AppLifecycleState) {
        backgroundManager.handleAppLifecycleChange(newState)

        // Update our connections based on new state
        when {
            backgroundManager.isInBackground && !isCustomTabOpen -> enterBackgroundMode()
            !backgroundManager.isInBackground && !isCustomTabOpen -> exitBackgroundMode()
        }
    }

    // MARK: - Public Listening Methods

    /**
     * @param identifier - Optional identifier for the listener, transactionId for payment updates and transaction messages, sessionId for authentication events.
     */
    fun listenForEvents(
        type: SseListenerType,
        identifier: String? = null,
        onDataChange: ((String) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
        autoReconnect: Boolean = true
    ) = startListening(
        SseListenerConfig(
            type = type,
            identifier = identifier,
            onDataChange = onDataChange,
            onError = onError,
            autoReconnect = autoReconnect
        )
    )

    // MARK: - Core Listening Logic

    /** Start listening with the given configuration */
    private fun startListening(config: SseListenerConfig) {
        ensureInitialized()

        try {
            // Validate initialization
            if (databaseUrl.isEmpty()) {
                throw IllegalStateException("Firebase SSE not initialized with valid database URL.")
            }

            val key = config.key
            val existingConnection = activeConnections[key]

            // If already listening to the same configuration, just log and return
            if (existingConnection != null && existingConnection.isActive) {
                logMessage("Already listening to ${config.displayName}: ${config.identifier}")
                return
            }

            // Clean up any existing connection for this key
            existingConnection?.let {
                stopConnection(key)
            }

            val url = "$databaseUrl${config.path}"

            logMessage("Starting ${config.displayName} listener for: ${config.identifier ?: "global"}")

            establishConnection(config, url)
        } catch (e: Exception) {
            logMessage("Failed to start ${config.displayName} listener: $e")
            handleConnectionError(config.key, e, config.onError)
        }
    }

    /** Establish a new SSE connection with background support */
    private fun establishConnection(config: SseListenerConfig, url: String) {
        val connection = SseConnection(config, url, scope)
        val key = config.key

        activeConnections[key] = connection

        // Set background mode if currently in background
        if (isInBackground || isCustomTabOpen) {
            connection.enterBackgroundMode()
        }

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "text/event-stream")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Connection", "keep-alive")
                .build()

            updateConnectionState(key, SseConnectionState.Connecting)

            val eventSource = EventSources
                .createFactory(okHttpClient)
                .newEventSource(request, object : EventSourceListener() {
                    override fun onOpen(eventSource: EventSource, response: Response) {
                        connection.lastEventReceived = System.currentTimeMillis()
                        connection.resetErrorCount()
                        logMessage("${config.displayName} connection established at: $url")

                        val newState = if (isInBackground || isCustomTabOpen) {
                            SseConnectionState.BackgroundMaintained
                        } else {
                            SseConnectionState.Connected
                        }

                        updateConnectionState(key, newState)
                    }

                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String
                    ) {
                        connection.lastEventReceived = System.currentTimeMillis()
                        connection.resetErrorCount()

                        logMessage(
                            "Event received for ${config.displayName}: ${
                                if (data.length > 100) "${
                                    data.substring(
                                        0,
                                        100
                                    )
                                }..." else data
                            }"
                        )
                        processEvent(data, config.onDataChange, config.onError)
                    }

                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: Response?
                    ) {
                        connection.incrementErrorCount()
                        logMessage("Connection error for ${config.displayName}: $t")
                        handleConnectionError(key, t ?: Exception("Unknown error"), config.onError)

                        if (config.autoReconnect) {
                            scheduleReconnect(key, config)
                        }
                    }

                    override fun onClosed(eventSource: EventSource) {
                        logMessage("Connection closed for ${config.displayName}")
                        updateConnectionState(key, SseConnectionState.Disconnected)

                        if (config.autoReconnect) {
                            scheduleReconnect(key, config)
                        }
                    }
                })

            connection.setEventSource(eventSource)

        } catch (e: Exception) {
            connection.incrementErrorCount()
            logMessage("Connection establishment failed for ${config.displayName}: $e")
            handleConnectionError(key, e, config.onError)

            if (config.autoReconnect) {
                scheduleReconnect(key, config)
            }
        }
    }

    /** Schedule a reconnection attempt with exponential backoff */
    private fun scheduleReconnect(key: String, config: SseListenerConfig) {
        val connection = activeConnections[key] ?: return

        // Calculate delay with exponential backoff
        val baseDelay = if (connection.state == SseConnectionState.Error) 5L else 3L
        val backoffMultiplier = (connection.consecutiveErrors * 2L).coerceAtMost(8L)
        val delay = baseDelay * backoffMultiplier

        logMessage("Scheduling reconnect for ${config.displayName} in $delay seconds... (attempt ${connection.consecutiveErrors + 1})")

        connection.scheduleReconnect(delay) {
            if (activeConnections.containsKey(key)) {
                val url = "$databaseUrl${config.path}"
                establishConnection(config, url)
            }
        }
    }

    // MARK: - Event Processing

    /** Process individual SSE events */
    private fun processEvent(
        event: String,
        onDataChange: ((String) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        try {
            processDataLine(event, onDataChange, onError)
        } catch (e: Exception) {
            logMessage("Event processing error: $e")
            onError?.invoke(e)
        }
    }

    /** Parse and process data lines from SSE events */
    private fun processDataLine(
        jsonData: String,
        onDataChange: ((String) -> Unit)?,
        onError: ((Throwable) -> Unit)?
    ) {
        try {
            if (jsonData.isEmpty() || jsonData == "null") {
                logMessage("Skipping null or empty data")
                return
            }

            val data = JSONObject(jsonData)
            val eventData = data.opt("data")

            when (eventData) {
                is String -> {
                    logMessage(
                        "Processing string event: ${
                            if (eventData.length > 50) "${
                                eventData.substring(
                                    0,
                                    50
                                )
                            }..." else eventData
                        }"
                    )
                    onDataChange?.invoke(eventData)
                }

                is JSONObject -> {
                    logMessage(
                        "Processing object event with keys: ${
                            eventData.keys().asSequence().joinToString(", ")
                        }"
                    )
                    onDataChange?.invoke(eventData.toString())
                }

                null -> {
                    logMessage("Received null event data")
                }

                else -> {
                    logMessage("Unhandled event data type: ${eventData.javaClass.simpleName}")
                    onDataChange?.invoke(JSONObject().put("data", eventData).toString())
                }
            }
        } catch (e: Exception) {
            logMessage("Data processing error: $e")
            onError?.invoke(e)
        }
    }

    // MARK: - Connection Management

    /** Stop a specific connection */
    private fun stopConnection(key: String) {
        val connection = activeConnections[key]
        if (connection != null) {
            logMessage("Stopping connection: $key")
            connection.dispose()
            activeConnections.remove(key)
            updateConnectionState(key, SseConnectionState.Disconnected)
        }
    }

    /** Stop listening for a specific type and identifier */
    fun stopListening(type: SseListenerType, identifier: String? = null) {
        val key = "${type.name}_${identifier ?: "global"}"
        stopConnection(key)
    }

    /** Stop all active listeners */
    fun stopAllListening() {
        logMessage("Stopping all active listeners (${activeConnections.size})")

        val keys = activeConnections.keys.toList()
        for (key in keys) {
            stopConnection(key)
        }

        logMessage("All listeners stopped")
    }

    // MARK: - Error Handling & State Management

    /** Handle connection errors */
    private fun handleConnectionError(
        key: String,
        error: Throwable,
        onError: ((Throwable) -> Unit)?
    ) {
        updateConnectionState(key, SseConnectionState.Error)
        onError?.invoke(error)
    }

    /** Update the connection state for a specific listener */
    private fun updateConnectionState(key: String, newState: SseConnectionState) {
        val connection = activeConnections[key]
        if (connection != null && connection.state != newState) {
            connection.state = newState
            logMessage("Connection state for $key changed to: $newState")

            // Broadcast the updated state map
            if (isInitialized) {
                stateFlow.value = connectionStates
            }
        }
    }

    // MARK: - Cleanup

    /** Dispose all resources */
    fun dispose() {
        logMessage("Disposing all resources")

        healthCheckJob?.cancel()
        healthCheckJob = null

        stopAllListening()

        scope.cancel()
        backgroundManager.dispose()
        isInitialized = false
        logMessage("Successfully disposed all resources")
    }

    // MARK: - Logging

    /** Logging utility with automatic emoji categorization */
    private fun logMessage(message: String) {
        val emoji = when {
            message.contains("error", ignoreCase = true) -> "❌"
            message.contains("failed", ignoreCase = true) -> "🚨"
            message.contains("connect", ignoreCase = true) -> "🔌"
            message.contains("listen", ignoreCase = true) -> "👂"
            message.contains("init") -> "🚀"
            message.contains("dispose") || message.contains("stop") -> "🛑"
            message.contains("success", ignoreCase = true) -> "✅"
            message.contains("event") -> "📬"
            message.contains("process") -> "⚙️"
            message.contains("scheduling") || message.contains("reconnect") -> "🔄"
            else -> "📝"
        }
        val content = "$emoji [SSEListener] $message"
        when {
            message.contains("error", ignoreCase = true) || message.contains(
                "failed",
                true
            ) -> Timber.e(content)

            else -> Timber.i(content)
        }
    }

    companion object : SingletonCompanion<FirebaseSseListener>() {
        override fun createInstance() = FirebaseSseListener()
    }
}