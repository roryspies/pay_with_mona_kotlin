package com.mona.sdk.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mona.sdk.R

internal val MonaFontFamily = FontFamily(
    Font(R.font.general_sans_extralight, FontWeight.ExtraLight),
    Font(R.font.general_sans_light, FontWeight.Light),
    Font(R.font.general_sans_regular, FontWeight.Normal),
    Font(R.font.general_sans_medium, FontWeight.Medium),
    Font(R.font.general_sans_semibold, FontWeight.SemiBold),
    Font(R.font.general_sans_bold, FontWeight.Bold),
)
internal val Typography = Typography().let { type ->
    type.copy(
        displayLarge = type.displayLarge.copy(fontFamily = MonaFontFamily),
        displayMedium = type.displayMedium.copy(fontFamily = MonaFontFamily),
        displaySmall = type.displaySmall.copy(fontFamily = MonaFontFamily),
        headlineLarge = type.headlineLarge.copy(fontFamily = MonaFontFamily),
        headlineMedium = type.headlineMedium.copy(fontFamily = MonaFontFamily),
        headlineSmall = type.headlineSmall.copy(fontFamily = MonaFontFamily),
        titleLarge = type.titleLarge.copy(fontFamily = MonaFontFamily),
        titleMedium = type.titleMedium.copy(fontFamily = MonaFontFamily),
        titleSmall = type.titleSmall.copy(fontFamily = MonaFontFamily),
        bodyLarge = type.bodyLarge.copy(fontFamily = MonaFontFamily),
        bodyMedium = type.bodyMedium.copy(fontFamily = MonaFontFamily),
        bodySmall = type.bodySmall.copy(fontFamily = MonaFontFamily),
        labelLarge = type.labelLarge.copy(fontFamily = MonaFontFamily),
        labelMedium = type.labelMedium.copy(fontFamily = MonaFontFamily),
        labelSmall = type.labelSmall.copy(fontFamily = MonaFontFamily),
    )
}