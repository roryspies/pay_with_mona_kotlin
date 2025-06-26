package ng.mona.paywithmona.data.serializer

import kotlinx.serialization.json.Json

internal val SdkJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
    explicitNulls = false
}