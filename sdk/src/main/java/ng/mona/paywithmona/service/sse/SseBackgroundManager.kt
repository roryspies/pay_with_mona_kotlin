package ng.mona.paywithmona.service.sse

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ng.mona.paywithmona.domain.SingletonCompanion
import timber.log.Timber

/**
 * Background manager for SSE connections
 */
internal class SseBackgroundManager private constructor() {

    private var currentState: AppLifecycleState = AppLifecycleState.Resumed
    private var backgroundMaintenanceJob: Job? = null
    private var isInitialized: Boolean = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            handleAppLifecycleChange(AppLifecycleState.Resumed)
        }

        override fun onPause(owner: LifecycleOwner) {
            handleAppLifecycleChange(AppLifecycleState.Paused)
        }

        override fun onStop(owner: LifecycleOwner) {
            handleAppLifecycleChange(AppLifecycleState.Detached)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            handleAppLifecycleChange(AppLifecycleState.Hidden)
        }
    }

    /** Initialize background management */
    fun initialize() {
        if (isInitialized) return

        setupAppLifecycleListener()
        isInitialized = true
        logBackgroundMessage("Background manager initialized")
    }

    /** Setup app lifecycle listener */
    private fun setupAppLifecycleListener() = ProcessLifecycleOwner.get().lifecycle.addObserver(
        lifecycleObserver
    )

    /** Handle app lifecycle changes */
    fun handleAppLifecycleChange(newState: AppLifecycleState) {
        if (currentState == newState) return

        val previousState = currentState
        currentState = newState

        logBackgroundMessage("App state changed: $previousState -> $newState")

        when (newState) {
            AppLifecycleState.Paused,
            AppLifecycleState.Detached -> handleAppInBackground()

            AppLifecycleState.Resumed -> handleAppForegrounded()
            AppLifecycleState.Inactive -> handleAppInactive()
            AppLifecycleState.Hidden -> handleAppHidden()
        }
    }

    /** Handle when app goes to background */
    private fun handleAppInBackground() {
        logBackgroundMessage("App is in background - switching to background mode")
        startBackgroundMaintenance()
    }

    /** Handle when app becomes inactive (custom tab scenario) */
    private fun handleAppInactive() {
        logBackgroundMessage("App inactive - likely custom tab opened")
        startBackgroundMaintenance()
    }

    /** Handle when app is hidden */
    private fun handleAppHidden() {
        logBackgroundMessage("App hidden - maintaining connections")
        startBackgroundMaintenance()
    }

    /** Handle when app comes to foreground */
    private fun handleAppForegrounded() {
        logBackgroundMessage("App foregrounded - resuming normal mode")
        stopBackgroundMaintenance()
    }

    /** Start background maintenance */
    private fun startBackgroundMaintenance() {
        backgroundMaintenanceJob?.cancel()
        backgroundMaintenanceJob = scope.launch {
            while (isActive) {
                delay(60_000) // 1 minute
                performBackgroundMaintenance()
            }
        }
    }

    /** Stop background maintenance */
    private fun stopBackgroundMaintenance() {
        backgroundMaintenanceJob?.cancel()
        backgroundMaintenanceJob = null
    }

    /** Perform background maintenance tasks */
    private fun performBackgroundMaintenance() {
        logBackgroundMessage("Performing background maintenance")
        // This will be called by the main SSE listener to maintain connections
    }

    /** Check if app is in background */
    val isInBackground: Boolean
        get() = currentState in listOf(
            AppLifecycleState.Paused,
            AppLifecycleState.Detached,
            AppLifecycleState.Inactive,
            AppLifecycleState.Hidden
        )

    /** Dispose background manager */
    fun dispose() {
        backgroundMaintenanceJob?.cancel()
        scope.cancel()
        isInitialized = false
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        logBackgroundMessage("Background manager disposed")
    }

    /** Log background-specific messages */
    private fun logBackgroundMessage(message: String) {
        Timber.i("🌙 [SSE-Background] $message")
    }

    companion object : SingletonCompanion<SseBackgroundManager>() {
        override fun createInstance() = SseBackgroundManager()
    }
}