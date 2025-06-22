package com.mona.sdk.data.repository

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.mona.sdk.data.local.SdkStorage
import com.mona.sdk.data.remote.httpClient
import com.mona.sdk.domain.MonaSdkState
import com.mona.sdk.domain.PaymentMethod
import com.mona.sdk.domain.PaymentMethodType
import com.mona.sdk.domain.SingletonCompanionWithDependency
import com.mona.sdk.domain.type
import com.mona.sdk.service.biometric.BiometricService
import com.mona.sdk.util.base64
import com.mona.sdk.util.encodeUrl
import com.mona.sdk.util.toJsonObject
import io.ktor.client.call.body
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    suspend fun getTransaction(
        transactionId: String,
    ): JsonObject? = try {
        httpClient.get("pay?transactionId=${transactionId.encodeUrl()}").body()
    } catch (e: Exception) {
        Timber.e(e, "Failed to get transaction details")
        null
    }

    suspend fun makePayment(
        activity: FragmentActivity?,
        state: MonaSdkState,
        sign: Boolean = false,
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
// TODO:                    if (_transactionOTP != null) "otp": _transactionOTP,
// TODO:                   if (_transactionPIN != null) "pin": _transactionPIN,
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
                val signature = activity?.let {
                    BiometricService.signTransaction(it, hash)
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
//            MonaSDKNotifier().resetPinAndOTP()
            return null
        }

        // Payment is completed successfully
        if (response["success"]?.jsonPrimitive?.booleanOrNull == true) {
            return response
        }

        val task = response["task"]
        if (task == null || task !is JsonObject || task.jsonObject.isEmpty()) {
            Timber.e("Payment failed with no task information: $response")
            return null
        }

        val taskType = task.jsonObject["taskType"]?.jsonPrimitive?.contentOrNull?.lowercase()
        val fieldType = task.jsonObject["fieldType"]?.jsonPrimitive?.contentOrNull?.lowercase()
        return when {
            taskType == "sign" -> {
                Timber.i("Payment requires signing, proceeding with signing")
                makePayment(
                    activity = activity,
                    state = state,
                    sign = true
                )
            }

            fieldType == "pin" -> {
//                val pin = monaSDK.triggerPinOrOTPFlow(
//                    pinOrOtpType = PaymentTaskType.PIN,
//                    taskModel = TransactionTaskModel.fromJSON(task)
//                )
//                if (!pin.isNullOrEmpty()) {
//                    monaSDK.setTransactionPIN(pin)
//                    makePayment(
//                        paymentType = paymentType,
//                        onPayComplete = onPayComplete
//                    )
//                } else {
//                    log("User cancelled PIN entry")
//                }
                null
            }


            fieldType == "otp" -> {
//                val otp = monaSDK.triggerPinOrOTPFlow(
//                    pinOrOtpType = PaymentTaskType.OTP,
//                    taskModel = TransactionTaskModel.fromJSON(task)
//                )
//                log("🥰 PaymentService OTP WAS ENTERED ::: $otp")
//                if (!otp.isNullOrEmpty()) {
//                    monaSDK.setTransactionOTP(otp)
//                    makePayment(
//                        paymentType = paymentType,
//                        onPayComplete = onPayComplete
//                    )
//                } else {
//                    log("User cancelled OTP entry")
//                }
                null
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