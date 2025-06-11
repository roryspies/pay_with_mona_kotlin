package com.mona.sdk.data.model

import androidx.compose.ui.graphics.Color
import com.mona.sdk.util.ColorSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BrandingColors(
    @SerialName("primaryColour")
    @Serializable(with = ColorSerializer::class)
    val primary: Color,
    @SerialName("primaryText")
    @Serializable(with = ColorSerializer::class)
    val text: Color,
)