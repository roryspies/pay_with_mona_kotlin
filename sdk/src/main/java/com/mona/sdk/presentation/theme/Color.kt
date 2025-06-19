package com.mona.sdk.presentation.theme

import androidx.compose.ui.graphics.Color

data class SdkColors(
    val primary: Color = Color(0xFF3045FB),
    val text: Color = Color.White,
    val surface: Color = Color(0xFFF7F7F8),
    val neutral300: Color = Color(0xFF6A6C60)
)

internal fun Color.inverted(): Color {
    return Color(
        red = 1f - red,
        green = 1f - green,
        blue = 1f - blue,
        alpha = alpha
    )
}