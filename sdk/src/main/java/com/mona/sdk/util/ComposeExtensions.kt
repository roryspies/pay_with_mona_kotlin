package com.mona.sdk.util

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

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