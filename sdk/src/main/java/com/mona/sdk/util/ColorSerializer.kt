package com.mona.sdk.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "Color",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeString("#${value.toArgb().toUInt().toString(16).substring(2)}")
    }

    override fun deserialize(decoder: Decoder): Color {
        val hex = decoder.decodeString()
        return hex.replace("#", "").toIntOrNull(16)?.let {
            Color(0xFF000000.toInt() or it)
        } ?: Color.Black
    }
}