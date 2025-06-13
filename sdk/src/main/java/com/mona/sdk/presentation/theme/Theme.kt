package com.mona.sdk.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mona.sdk.PayWithMonaSdk

@Composable
fun SdkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = when {
        LocalInspectionMode.current -> {
            // In inspection mode, we don't want to collect the merchant branding
            // as it may not be available in the preview.
            SdkColors()
        }

        else -> {
            val merchantBranding by PayWithMonaSdk.instance.merchantBranding.collectAsStateWithLifecycle(
                null
            )
            remember(merchantBranding) {
                merchantBranding?.let {
                    SdkColors(primary = it.colors.primary, text = it.colors.text)
                } ?: SdkColors()
            }
        }
    }

    val colorScheme = remember(colors) {
        when {
            darkTheme -> darkColorScheme(
                primary = colors.primary
            )

            else -> lightColorScheme(
                primary = colors.primary,
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}