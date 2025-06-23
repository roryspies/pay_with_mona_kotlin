package ng.mona.paywithmona.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ng.mona.paywithmona.PayWithMonaSdk.initialize
import ng.mona.paywithmona.PayWithMonaSdk.merchantBranding

@Composable
internal fun SdkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // if inspection mode is enabled, we just initialize the sdk with a dummy value
    if (LocalInspectionMode.current) {
        initialize("merchant_key", LocalContext.current)
    }
    val merchantBranding by merchantBranding.collectAsStateWithLifecycle()

    val primary = merchantBranding?.colors?.primary ?: SdkColors.primary

    val colorScheme = remember(primary) {
        when {
//            darkTheme -> darkColorScheme(
//                primary = colors.primary,
//                surface = colors.surface.inverted(),
//            )

            else -> lightColorScheme(
                primary = primary,
                surface = SdkColors.surface,
                background = SdkColors.white,
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}