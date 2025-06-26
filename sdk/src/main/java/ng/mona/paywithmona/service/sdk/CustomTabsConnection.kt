package ng.mona.paywithmona.service.sdk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.DisplayMetrics
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.roundToInt

/**
 * Manages Chrome Custom Tabs connections and lifecycle
 *
 * This class provides a suspendable custom tab launch mechanism that waits for the user
 * to close the custom tab before resuming execution. It uses Android's ProcessLifecycleOwner
 * to monitor app lifecycle transitions and intelligently detect when the custom tab is closed.
 *
 * Key Features:
 * - Suspendable launch function that waits for custom tab closure
 * - Intelligent lifecycle monitoring to distinguish between tab opening and closing
 * - Automatic cleanup of observers and resources
 * - Support for programmatic custom tab closure
 *
 * Usage:
 * ```kotlin
 * val connection = CustomTabsConnection()
 *
 * // This will suspend until the user closes the custom tab
 * connection.launch(url, activity, color)
 * println("Custom tab was closed!")
 * ```
 */
internal class CustomTabsConnection {

    // region Properties

    /** Chrome Custom Tabs client for managing the connection */
    private var tabsClient: CustomTabsClient? = null

    /** Current custom tabs session */
    private var session: CustomTabsSession? = null

    /** Deferred for waiting on service connection to be ready */
    private var ready: CompletableDeferred<Unit?>? = null

    /** Deferred for waiting on custom tab to be closed */
    private var closed: CompletableDeferred<Unit>? = null

    /** Reference to the current activity using the custom tab */
    private var currentActivity: Activity? = null

    /** Flag indicating if a custom tab is currently active */
    private var isActive = false

    /** Coroutine scope for managing lifecycle-related coroutines */
    private val lifecycleScope = CoroutineScope(Dispatchers.Main)

    /** Job for the delay mechanism used in lifecycle detection */
    private var delayJob: Job? = null

    // endregion

    // region Custom Tabs Service Connection

    /**
     * Connection callback for Chrome Custom Tabs service.
     * Handles service connection and disconnection events.
     */
    private val connection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(
            name: ComponentName,
            client: CustomTabsClient
        ) {
            tabsClient = client
            ready?.complete(Unit)

            // Warm up the browser process for better performance
            tabsClient?.warmup(0)
            session = tabsClient?.newSession(null)

            Timber.d("Custom Tabs service connected: $name")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            tabsClient = null
            ready?.complete(null)
            session = null

            Timber.d("Custom Tabs service disconnected: $name")
        }
    }

    // endregion

    // region Lifecycle Observer

    /**
     * Lifecycle observer that monitors app transitions to detect when custom tabs are closed.
     *
     * The detection mechanism works as follows:
     * 1. onResume: Start a 500ms delay timer
     * 2. If onPause occurs within 500ms, cancel the timer (user is transitioning to custom tab)
     * 3. If timer completes without cancellation, check if custom tab is still active
     * 4. If custom tab is no longer active, complete the launch function
     */
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            Timber.d("App resumed - starting custom tab closure detection")

            if (isActive) {
                // Cancel any existing delay job to avoid multiple checks
                delayJob?.cancel()

                // Start delay timer - if this completes without being cancelled by onPause,
                // it means we're staying in the app (not transitioning to custom tab)
                delayJob = lifecycleScope.launch {
                    runCatching {
                        delay(LIFECYCLE_DETECTION_DELAY_MS)

                        // If we reach here, onPause didn't cancel us within the delay period
                        if (isActive && !isCustomTabStillActive()) {
                            Timber.d("Custom tab closed - completing launch function")
                            closed?.complete(Unit)
                        } else {
                            Timber.d("Custom tab still active after delay")
                        }
                    }.onFailure {
                        Timber.d("Lifecycle detection cancelled (app transitioning to background)")
                    }
                }
            }
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            Timber.d("App paused - cancelling lifecycle detection")

            // Cancel the delay job since we're transitioning to background
            // This prevents false positives when opening the custom tab
            delayJob?.cancel()
        }
    }

    // endregion

    // region Public API

    /**
     * Launches a Chrome Custom Tab with the specified URL and suspends until it's closed.
     *
     * This function will:
     * 1. Bind to the Chrome Custom Tabs service
     * 2. Launch the custom tab with the provided configuration
     * 3. Start monitoring for tab closure using lifecycle events
     * 4. Suspend execution until the user closes the tab
     * 5. Clean up resources and resume execution
     *
     * @param url The URL to open in the custom tab
     * @param activity The activity context for launching the tab
     * @param color The primary color for the custom tab toolbar
     *
     * @throws IllegalStateException if the activity becomes null during execution
     */
    @SuppressLint("WrongConstant")
    suspend fun launch(url: String, activity: Activity, color: Color) = withContext(
        Dispatchers.Main
    ) {
        try {
            currentActivity = activity
            Timber.d("Launching custom tab for URL: $url")

            // Establish connection to Custom Tabs service
            bind()

            // Configure and launch the custom tab
            val customTabsIntent = CustomTabsIntent.Builder(session)
                .setShowTitle(true)
                .setSendToExternalDefaultHandlerEnabled(true)
                .setInitialActivityHeightPx((getActivityHeight(activity) * HEIGHT_RATIO).roundToInt())
                .setActivitySideSheetMaximizationEnabled(true)
                .setActivitySideSheetDecorationType(CustomTabsIntent.ACTIVITY_SIDE_SHEET_DECORATION_TYPE_SHADOW)
                .setActivitySideSheetRoundedCornersPosition(CustomTabsIntent.ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_TOP)
                .setToolbarCornerRadiusDp(TOOLBAR_CORNER_RADIUS_DP)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(color.toArgb())
                        .build()
                )
                .build()

            customTabsIntent.launchUrl(activity, url.toUri())

            // Set up monitoring for tab closure
            isActive = true
            closed = CompletableDeferred()
            ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)

            // Suspend until the custom tab is closed
            Timber.d("Waiting for custom tab to be closed...")
            closed?.await()

            Timber.d("Custom tab launch completed")
        } finally {
            // Clean up resources
            cleanupAfterLaunch()
        }
    }

    /**
     * Programmatically closes the custom tab by bringing the app back to the foreground.
     *
     * This method checks if a custom tab is currently active and, if so, starts an intent
     * to bring the calling activity back to the foreground, effectively closing the custom tab.
     *
     * @param activity The activity to bring back to the foreground
     */
    fun close(activity: Activity?) {
        if (activity == null) {
            Timber.w("Cannot close custom tab: activity is null")
            return
        }

        try {
            currentActivity = activity

            if (isCustomTabStillActive()) {
                Timber.d("Programmatically closing custom tab")
                activity.startActivity(
                    Intent(activity, activity.javaClass).apply {
                        flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
                    }
                )
            } else {
                Timber.d("No active custom tab to close")
            }
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Failed to close custom tab")
        } finally {
            currentActivity = null
        }
    }

    // endregion

    // region Private Methods

    /**
     * Establishes a connection to the Chrome Custom Tabs service.
     *
     * @throws IllegalStateException if no current activity is set
     */
    private suspend fun bind() {
        val activity = currentActivity ?: throw IllegalStateException("Activity is null")

        // Return early if already connected
        if (tabsClient != null) {
            Timber.d("Custom Tabs service already connected")
            return
        }

        // Get the package name of the default browser that supports Custom Tabs
        val packageName = CustomTabsClient.getPackageName(activity, null)
        if (packageName == null) {
            Timber.w("No Custom Tabs service available")
            return
        }

        Timber.d("Binding to Custom Tabs service: $packageName")
        ready = CompletableDeferred()

        val bound = CustomTabsClient.bindCustomTabsService(activity, packageName, connection)
        if (!bound) {
            Timber.w("Failed to bind to Custom Tabs service")
            return
        }

        // Wait for the service connection to be established
        ready?.await()
        Timber.d("Custom Tabs service bound successfully")
    }

    /**
     * Gets the height of the current activity's window.
     *
     * @param activity The activity to measure
     * @return The height in pixels
     */
    private fun getActivityHeight(activity: Activity): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.windowManager.currentWindowMetrics.bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getMetrics(metrics)
            metrics.heightPixels
        }
    }

    /**
     * Checks if a custom tab is currently active by looking for Custom Tabs services
     * in the current app task stack.
     *
     * @return true if a custom tab is active, false otherwise
     */
    private fun isCustomTabStillActive(): Boolean = getCustomTabResolveInfo() != null

    /**
     * Attempts to find a Custom Tabs service resolve info for the current top activity.
     * This is used to determine if a custom tab is currently active.
     *
     * @return ResolveInfo if a custom tab is active, null otherwise
     */
    private fun getCustomTabResolveInfo(): ResolveInfo? {
        val activity = currentActivity ?: return null
        val activityManager = activity.getSystemService<ActivityManager>() ?: return null
        val activityName = ComponentName(activity, activity.javaClass)

        for (appTask in activityManager.appTasks) {
            val taskInfo = appTask.taskInfo

            // Skip if this isn't our app's task or there's no top activity
            if (activityName != taskInfo.baseActivity || taskInfo.topActivity == null) {
                continue
            }

            // Create intent to check for Custom Tabs service
            val serviceIntent = Intent(ACTION_CUSTOM_TABS_CONNECTION).apply {
                setPackage(taskInfo.topActivity?.packageName)
            }

            // Query for Custom Tabs service
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.packageManager.resolveService(
                    serviceIntent,
                    PackageManager.ResolveInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                activity.packageManager.resolveService(serviceIntent, 0)
            }

            // If we found a Custom Tabs service, a custom tab is active
            if (resolveInfo != null) {
                return resolveInfo
            }
        }

        return null
    }

    /**
     * Cleans up resources after the launch function completes.
     */
    private fun cleanupAfterLaunch() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        delayJob?.cancel()
        isActive = false
        currentActivity = null
        closed = null
        delayJob = null

        Timber.d("Custom tab launch cleanup completed")
    }

    // endregion

    // region Constants

    companion object {
        /** Delay in milliseconds for lifecycle detection mechanism */
        private const val LIFECYCLE_DETECTION_DELAY_MS = 500L

        /** Height ratio for the custom tab (90% of screen height) */
        private const val HEIGHT_RATIO = 0.9

        /** Corner radius for the custom tab toolbar in DP */
        private const val TOOLBAR_CORNER_RADIUS_DP = 16
    }

    // endregion
}