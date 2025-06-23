package ng.mona.paywithmona.util

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

internal fun Map<*, *>.toJsonObject(): JsonObject {
    return JsonObject(
        map { (key, value) ->
            key.toString() to when (value) {
                null -> JsonNull
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is Map<*, *> -> value.toJsonObject()
                is JsonPrimitive -> value
                else -> throw IllegalArgumentException("Unsupported type: ${value::class.java}")
            }
        }.toMap()
    )
}

internal fun String.base64() = Base64.getEncoder().encodeToString(toByteArray())

internal fun String.encodeUrl() = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())