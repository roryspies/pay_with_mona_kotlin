package ng.mona.paywithmona.data.repository

import android.content.Context
import androidx.fragment.app.FragmentActivity
import io.ktor.client.call.body
import io.ktor.client.request.cookie
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ng.mona.paywithmona.data.local.SdkStorage
import ng.mona.paywithmona.data.remote.httpClient
import ng.mona.paywithmona.domain.MonaSdkState
import ng.mona.paywithmona.domain.PaymentMethod
import ng.mona.paywithmona.domain.PaymentMethodType
import ng.mona.paywithmona.domain.SingletonCompanionWithDependency
import ng.mona.paywithmona.domain.type
import ng.mona.paywithmona.service.biometric.BiometricPromptConfig
import ng.mona.paywithmona.service.biometric.BiometricService
import ng.mona.paywithmona.service.bottomsheet.BottomSheetContent
import ng.mona.paywithmona.service.bottomsheet.BottomSheetHandler
import ng.mona.paywithmona.service.bottomsheet.BottomSheetResponse
import ng.mona.paywithmona.util.CryptoUtil
import ng.mona.paywithmona.util.base64
import ng.mona.paywithmona.util.toJsonObject
import timber.log.Timber
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

internal class CheckoutRepository private constructor(
    context: Context,
) {
    private val storage by lazy {
        SdkStorage.getInstance(context)
    }

//    suspend fun getTransaction(
//        transactionId: String,
//    ): JsonObject? = try {
//        httpClient.get("pay?transactionId=${transactionId.encodeUrl()}").body()
//    } catch (e: Exception) {
//        Timber.e(e, "Failed to get transaction details")
//        null
//    }

    suspend fun makePayment(
        activity: FragmentActivity?,
        state: MonaSdkState,
        bottomSheetHandler: BottomSheetHandler,
        sign: Boolean = false,
        extras: Map<String, String> = emptyMap(),
    ): JsonObject? {
        val method = state.method as? PaymentMethod.SavedInfo ?: return null
        val checkoutId = storage.checkoutId.first()
        val payload = buildMap {
            put("hasDeviceKey", !checkoutId.isNullOrBlank())
            put("transactionId", state.transactionId)
            when (method.type) {
                PaymentMethodType.Card -> {
                    put("bankId", method.card?.bankId)
                }

                else -> {
                    put("origin", method.bank?.id)
                    putAll(extras)
                }
            }
        }

        val keyId = storage.keyId.first().orEmpty()
        val (signature, nonce, timestamp) = when (sign) {
            true -> {
                Timber.i("REQUESTING TO SIGN PAYMENT ==>> PAY LOAD TO BE SIGNED ==>> $payload")

                val nonce = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis().toString()
                val data = mapOf(
                    "method" to "POST".base64(),
                    "uri" to "/pay".base64(),
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
                                title = "Authorize payment",
                                subtitle = "Use your biometric to authorize this transaction",
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to create signature for payment")
                    null
                }
                if (signature == null) {
                    Timber.e("signature is null, cannot proceed with payment")
                    return null
                }

                Triple(signature, nonce, timestamp)
            }

            else -> {
                Triple(null, null, null)
            }
        }

        val response: JsonObject = try {
            val checkoutId = storage.checkoutId.first()
            httpClient.post("pay") {
                header("x-client-type", "bioApp") // TODO: make this dynamic
                if (!checkoutId.isNullOrBlank()) {
                    cookie("mona_checkoutId", checkoutId)
                }
                if (keyId.isNotBlank()) {
                    header("x-mona-key-id", keyId)
                }
                if (signature != null) {
                    header("x-mona-pay-auth", signature)
                }
                if (nonce != null) {
                    header("x-mona-nonce", nonce)
                }
                if (timestamp != null) {
                    header("x-mona-timestamp", timestamp)
                }
                val checkoutType = if (method.type == PaymentMethodType.Card) {
                    method.type.key
                } else null
                if (checkoutType != null) {
                    header("x-mona-checkout-type", checkoutType)
                }
                setBody(payload.toJsonObject())
            }.body()
        } catch (e: Exception) {
            Timber.e(e, "Failed to initiate payment")
            return null
        }

        // Payment is completed successfully
        if (response["success"]?.jsonPrimitive?.booleanOrNull == true) {
            return response
        }

        val task = when (response["task"]) {
            is JsonObject -> response["task"]?.jsonObject
            else -> null
        }
        if (task == null || task.isEmpty()) {
            Timber.e("Payment failed with no task information: $response")
            return null
        }

        val taskType = task["taskType"]?.jsonPrimitive?.contentOrNull?.lowercase()
        val fieldType = task["fieldType"]?.jsonPrimitive?.contentOrNull?.lowercase()
        return when {
            taskType == "sign" -> makePayment(
                activity = activity,
                state = state,
                bottomSheetHandler = bottomSheetHandler,
                sign = true
            )

            fieldType == "pin" || fieldType == "otp" -> {
                bottomSheetHandler.show(
                    BottomSheetContent.OtpInput(
                        title = task["taskDescription"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        length = task["fieldLength"]?.jsonPrimitive?.intOrNull ?: 0,
                        isPassword = fieldType == "pin"
                    ),
                    activity
                )
                val response = bottomSheetHandler.response.first()
                if (response !is BottomSheetResponse.Otp) {
                    Timber.e("Payment task was cancelled or failed: $response")
                    return null
                }
                bottomSheetHandler.show(
                    BottomSheetContent.Loading,
                    activity
                )
                makePayment(
                    activity = activity,
                    state = state,
                    bottomSheetHandler = bottomSheetHandler,
                    extras = mapOf(
                        fieldType to when (task["encrypted"]?.jsonPrimitive?.booleanOrNull) {
                            true -> CryptoUtil.encrypt(response.otp)
                            else -> response.otp
                        }
                    )
                )
            }

            else -> {
                Timber.e("Payment task type is not supported: $taskType")
                null
            }
        }
    }

    fun generateSessionId() = SecureRandom().nextInt(999_999_999).toString()

    companion object : SingletonCompanionWithDependency<CheckoutRepository, Context>() {
        override fun createInstance(dependency: Context) = CheckoutRepository(dependency)
    }
}