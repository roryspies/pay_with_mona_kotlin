package ng.mona.paywithmona.data.repository

import android.app.Activity
import android.content.Context
import androidx.fragment.app.FragmentActivity
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ng.mona.paywithmona.data.local.SdkStorage
import ng.mona.paywithmona.data.model.MerchantBranding
import ng.mona.paywithmona.data.remote.disableContentTypeKey
import ng.mona.paywithmona.data.remote.httpClient
import ng.mona.paywithmona.domain.SingletonCompanionWithDependency
import ng.mona.paywithmona.service.biometric.BiometricService
import ng.mona.paywithmona.util.toJsonObject
import timber.log.Timber
import java.util.Base64
import java.util.UUID

internal class AuthRepository private constructor(
    context: Context,
) {
    private val storage by lazy {
        SdkStorage.getInstance(context)
    }

    suspend fun login(token: String, phoneNumber: String): JsonObject? {
        return httpClient.post("login") {
            header("x-strong-auth-token", token)
            header("x-mona-key-exchange", "true")
            setBody(
                mapOf(
//                    "phone" to phoneNumber.ifBlank { null }
                    "phone" to null
                )
            )
        }.body()
    }

    suspend fun signAndCommitKeys(deviceAuth: JsonObject, activity: Activity) {
        val id = UUID.randomUUID().toString()
        val attestationResponse = mutableMapOf(
            "id" to id,
            "rawId" to id
        )
        val payload = mutableMapOf(
            "registrationToken" to deviceAuth["registrationToken"],
            "attestationResponse" to attestationResponse
        )

        val publicKey = BiometricService.generatePublicKey()
        if (publicKey.isBlank()) {
            throw Exception("Failed to generate public key")
        }
        attestationResponse["publicKey"] = publicKey

        // Sign data
        val registrationOptions = Json.encodeToString(deviceAuth["registrationOptions"])
        val data = Base64.getEncoder().encodeToString(
            registrationOptions.toByteArray(Charsets.UTF_8)
        )

        val signature = BiometricService.createSignature(
            activity as FragmentActivity,
            data,
        )
        if (signature.isBlank()) {
            throw Exception("Failed to create signature")
        }
        attestationResponse["signature"] = signature

        val response: JsonObject = httpClient.post("keys/commit") {
            setBody(payload.toJsonObject())
        }.body() ?: throw Exception("Failed to commit keys")

        if (response["success"] is JsonPrimitive && response["success"]!!.jsonPrimitive.boolean) {
            storage.setHasPasskey(true)
            storage.setKeyId(response["keyId"]!!.jsonPrimitive.content)
            storage.setCheckoutId(response["mona_checkoutId"]!!.jsonPrimitive.content)
        } else {
            throw Exception("Failed to commit keys: ${response["error"]}")
        }
    }

    suspend fun fetchMerchantBranding(merchantKey: String): MerchantBranding? {
        return try {
            val response: JsonObject = httpClient.get("merchant/sdk") {
                header("x-public-key", merchantKey)
            }.body()
            val branding = response["data"]?.let {
                Json.decodeFromJsonElement<MerchantBranding>(it)
            }

            storage.setMerchantKey(merchantKey)
            if (branding != null) {
                storage.setMerchantBranding(branding)
            }

            branding
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch merchant branding")
            null
        }
    }


    suspend fun validatePii(keyId: String): JsonObject? {
        return try {
            val response: JsonObject = httpClient.post("login/validate") {
                header("x-mona-key-id", keyId)
                attributes.put(disableContentTypeKey, true)
            }.body()
            response["data"]?.jsonObject
        } catch (e: Exception) {
            Timber.e(e, "Failed to validate PII")
            null
        }
    }

    companion object : SingletonCompanionWithDependency<AuthRepository, Context>() {
        override fun createInstance(dependency: Context) = AuthRepository(dependency)
    }
}