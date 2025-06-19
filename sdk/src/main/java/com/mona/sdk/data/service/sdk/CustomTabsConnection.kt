package com.mona.sdk.data.service.sdk

import android.app.Activity
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.content.pm.PackageManager
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
import kotlinx.coroutines.CompletableDeferred
import timber.log.Timber
import kotlin.math.roundToInt

internal class CustomTabsConnection(context: Context) {
    private var tabsClient: CustomTabsClient? = null
    private var session: CustomTabsSession? = null
    private var ready: CompletableDeferred<Unit?>? = null

    private val connection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(
            name: ComponentName,
            client: CustomTabsClient
        ) {
            tabsClient = client
            ready?.complete(Unit)
            tabsClient?.warmup(0)
            session = tabsClient?.newSession(null)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            tabsClient = null
            ready?.complete(null)
            session = null
        }
    }

    private val customTabsObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)

            // check if the custom tab is still active
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningTasks = activityManager.appTasks

            runningTasks.forEach { task ->
                Timber.d("Running task: ${task.taskInfo.baseActivity?.className}")
            }

            // remove observer when the activity is resumed
//            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            Timber.d("CustomTabsService paused")
        }
    }

    suspend fun launch(url: String, activity: Activity, color: Color) {
        bind(activity)
        CustomTabsIntent
            .Builder(session)
            .setShowTitle(true)
            .setSendToExternalDefaultHandlerEnabled(true)
            .setInitialActivityHeightPx((getActivityHeight(activity) * 0.9).roundToInt())
            .setActivitySideSheetMaximizationEnabled(true)
            .setActivitySideSheetDecorationType(CustomTabsIntent.ACTIVITY_SIDE_SHEET_DECORATION_TYPE_SHADOW)
            .setActivitySideSheetRoundedCornersPosition(
                CustomTabsIntent.ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_TOP
            )
            .setToolbarCornerRadiusDp(16)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(color.toArgb())
                    .build()
            )
            .build()
            .launchUrl(activity, url.toUri())
    }

    fun close(activity: Activity?) {
        val activityManager = activity?.getSystemService<ActivityManager>() ?: return
        val activityName = ComponentName(activity, activity.javaClass)
        for (appTask in activityManager.appTasks) {
            val taskInfo = appTask.taskInfo
            if (activityName != taskInfo.baseActivity || taskInfo.topActivity == null) {
                continue
            }

            val serviceIntent = Intent(ACTION_CUSTOM_TABS_CONNECTION).apply {
                setPackage(taskInfo.topActivity?.packageName)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.packageManager.resolveService(
                    serviceIntent,
                    PackageManager.ResolveInfoFlags.of(0L)
                )
            } else {
                activity.packageManager.resolveService(serviceIntent, 0)
            } ?: continue

            try {
                activity.startActivity(
                    Intent(activity, activity.javaClass).apply {
                        setFlags(FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP)
                    }
                )
            } catch (_: ActivityNotFoundException) {
            } finally {
                break
            }
        }
    }

    private suspend fun bind(activity: Activity) {
        // Check for an existing connection
        if (tabsClient != null) {
            // Do nothing if there is an existing service connection
            return
        }

        // Get the default browser package name, this will be null if
        // the default browser does not provide a CustomTabsService
        val packageName = CustomTabsClient.getPackageName(activity, null)
        if (packageName == null) {
            // Do nothing as service connection is not supported
            return
        }

        ready = CompletableDeferred()
        val bound = CustomTabsClient.bindCustomTabsService(
            activity,
            packageName,
            connection
        )

        // If binding to the service failed, we proceed without it
        if (!bound) {
            return
        }

        // delay to ensure the service is connected
        ready?.await()

        // start observing lifecycle events
//        ProcessLifecycleOwner.get().lifecycle.addObserver(customTabsObserver)
    }

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
}