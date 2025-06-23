package ng.mona.paywithmona.data.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ng.mona.paywithmona.util.ColorSerializer

@Serializable
data class BrandingColors(
    @SerialName("primaryColour")
    @Serializable(with = ColorSerializer::class)
    val primary: Color,
    @SerialName("primaryText")
    @Serializable(with = ColorSerializer::class)
    val text: Color,
)