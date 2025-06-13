package com.mona.sdk.util

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal fun Map<String, Any>.toJsonObject(): JsonObject {
    return JsonObject(
        mapValues { (_, value) ->
            when (value) {
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                else -> throw IllegalArgumentException("Unsupported type: ${value::class.java}")
            }
        }
    )
}