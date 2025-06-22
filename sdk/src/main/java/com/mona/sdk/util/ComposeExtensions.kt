package com.mona.sdk.util

import android.graphics.drawable.BitmapDrawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.view.WindowCompat

@Composable
internal fun appIconPainter(): Painter? {
    if (LocalInspectionMode.current) {
        // In inspection mode, we don't want to load the app icon as it will not be available in the preview.
        return null
    }

    val context = LocalContext.current
    return remember {
        val appInfo = context.applicationInfo
        val drawable = context.packageManager.getApplicationIcon(appInfo)
        if (drawable is BitmapDrawable) {
            BitmapPainter(drawable.bitmap.asImageBitmap())
        } else {
            null
        }
    }
}

@Composable
@Suppress("DEPRECATION")
internal fun setNavigationBarColor(color: Color) {
    val activity = LocalActivity.current as? ComponentActivity
    if (activity == null || LocalInspectionMode.current) {
        // If the activity is null or in inspection mode, we don't apply the color.
        return
    }
    DisposableEffect(Unit) {
        val previousColor = activity.window.navigationBarColor
        activity.window.navigationBarColor = color.toArgb()

        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
//            isAppearanceLightNavigationBars = !isDarkMod
        }

        onDispose {
            activity.window.navigationBarColor = previousColor
        }
    }
}

internal fun Color.lighten(percent: Float): Color {
    val factor = percent / 100f
    val r = (red + factor).coerceIn(0f, 1f)
    val g = (green + factor).coerceIn(0f, 1f)
    val b = (blue + factor).coerceIn(0f, 1f)
    return Color(r, g, b, alpha)
}


internal fun Color.inverted(): Color {
    return Color(
        red = 1f - red,
        green = 1f - green,
        blue = 1f - blue,
        alpha = alpha
    )
}