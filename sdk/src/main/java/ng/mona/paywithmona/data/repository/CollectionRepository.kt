package ng.mona.paywithmona.data.repository

import android.content.Context
import androidx.fragment.app.FragmentActivity
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import ng.mona.paywithmona.data.local.SdkStorage
import ng.mona.paywithmona.data.model.Collection
import ng.mona.paywithmona.data.remote.httpClient
import ng.mona.paywithmona.data.serializer.SdkJson
import ng.mona.paywithmona.domain.SingletonCompanionWithDependency
import ng.mona.paywithmona.service.biometric.BiometricPromptConfig
import ng.mona.paywithmona.service.biometric.BiometricService
import ng.mona.paywithmona.util.base64
import ng.mona.paywithmona.util.toJsonObject
import timber.log.Timber
import java.security.MessageDigest
import java.util.UUID

internal class CollectionRepository private constructor(
    context: Context,
) {
    private val storage by lazy {
        SdkStorage.getInstance(context)
    }

    suspend fun fetchCollection(id: String): Collection {
        val response: JsonObject = httpClient.get("collections/$id") {
            header("x-public-key", storage.merchantKey.first())
        }.body()
        if (response["success"]?.jsonPrimitive?.booleanOrNull == true) {
            return SdkJson.decodeFromJsonElement(response["data"]!!)
        }
        throw Exception(
            response["message"]?.jsonPrimitive?.contentOrNull ?: "Failed to fetch collection"
        )
    }

    suspend fun consentCollection(
        bankId: String,
        accessRequestId: String,
        activity: FragmentActivity? = null,
    ): JsonObject? {
        val keyId = storage.keyId.first().orEmpty()
        val payload = mapOf(
            "bankId" to bankId,
            "accessRequestId" to accessRequestId
        )

        val nonce = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis().toString()
        val data = mapOf(
            "method" to "POST".base64(),
            "uri" to "/collections/consent".base64(),
            "body" to Json.encodeToString(payload.toJsonObject()).base64(),
            "params" to Json.encodeToString(emptyMap<String, String>()).base64(),
            "nonce" to nonce.base64(),
            "timestamp" to timestamp.base64(),
            "keyId" to keyId.base64()
        )
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(Json.encodeToString(data).base64().toByteArray())
            .joinToString("") { "%02x".format(it) }

        val signature = try {
            activity?.let {
                BiometricService.createSignature(
                    it,
                    hash,
                    BiometricPromptConfig(
                        title = "Authorize collection",
                        subtitle = "Use your biometric to authorize this collection",
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create signature for collection consent")
            null
        }
        if (signature == null) {
            Timber.e("signature is null, cannot proceed with collection consent")
            return null
        }

        return try {
            httpClient.post("collections/consent") {
                header("x-public-key", storage.merchantKey.first().orEmpty())
                header("x-client-type", "bioApp")
                if (keyId.isNotBlank()) {
                    header("x-mona-key-id", keyId)
                }
                header("x-mona-pay-auth", signature)
                header("x-mona-nonce", nonce)
                header("x-mona-timestamp", timestamp)
                setBody(payload.toJsonObject())
            }.body()
        } catch (e: Exception) {
            Timber.e(e, "Failed to initiate collection consent")
            null
        }
    }

    companion object : SingletonCompanionWithDependency<CollectionRepository, Context>() {
        override fun createInstance(dependency: Context) = CollectionRepository(dependency)
    }
}