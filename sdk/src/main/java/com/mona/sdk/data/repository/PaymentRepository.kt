package com.mona.sdk.data.repository

import android.content.Context
import com.mona.sdk.data.local.DataStore
import com.mona.sdk.data.remote.ApiConfig.PAY_HOST
import com.mona.sdk.data.remote.dto.InitiatePaymentResponse
import com.mona.sdk.data.remote.httpClient
import com.mona.sdk.domain.PaymentMethod
import com.mona.sdk.domain.PaymentType
import com.mona.sdk.domain.SingletonCompanion
import com.mona.sdk.domain.type
import com.mona.sdk.event.SuccessRateType
import com.mona.sdk.util.toJsonObject
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.time.LocalDate

internal class PaymentRepository(
    private val context: Context,
) {
    private val dataStore: DataStore
        get() = DataStore.getInstance(context)

    suspend fun initiatePayment(
        merchantKey: String,
        transactionAmountInKobo: Int,
        successRateType: SuccessRateType,
        merchantApiKey: String? = null,
        phoneNumber: String? = null,
        bvn: String? = null,
        dob: LocalDate? = null,
        firstName: String? = null,
        lastName: String? = null,
        userId: String? = null,
    ): InitiatePaymentResponse {
        if (merchantApiKey.isNullOrBlank()) {
            throw IllegalArgumentException("To initiate payment, API key cannot be empty")
        }
        if (transactionAmountInKobo < (20 * 100)) {
            throw IllegalArgumentException("Cannot initiate payment for less than 20 Naira")
        }

        val firstAndLastName = if (firstName.isNullOrBlank() || lastName.isNullOrBlank()) {
            null
        } else {
            "$firstName $lastName"
        }
        if (dob != null && firstAndLastName.isNullOrBlank()) {
            throw IllegalArgumentException("`Name Value - First and Last` must not be null or empty when `dob` is provided.")
        }
        if (!firstAndLastName.isNullOrBlank() && dob == null) {
            throw IllegalArgumentException("`DOB` must not be null when `Name Value - First and Last` is provided.")
        }

        val response: InitiatePaymentResponse = httpClient.post("demo/checkout") {
            header("x-public-key", merchantKey)
            header("x-api-key", merchantApiKey)
            if (!userId.isNullOrBlank()) {
                header("x-mona-key-id", userId)
            }
            setBody(
                buildMap {
                    put("amount", transactionAmountInKobo)
                    put("successRateType", successRateType.key)
                    if (!phoneNumber.isNullOrBlank()) {
                        put("phone", phoneNumber)
                    }
                    if (!bvn.isNullOrBlank()) {
                        put("bvn", bvn)
                    }
                    if (dob != null) {
                        put("dob", dob.toString())
                    }
                    if (!firstAndLastName.isNullOrBlank()) {
                        put("name", firstAndLastName)
                    }
                }.toJsonObject()
            )
        }.body()

        if (response.transactionId == null || response.friendlyId == null) {
            throw IllegalStateException("Failed to initiate payment: ${response.message}")
        }

        return response
    }

    fun buildPaymentUrl(
        merchantKey: String,
        transactionId: String,
        method: PaymentMethod,
        type: PaymentType? = null,
        withRedirect: Boolean = true,
    ): String {
        val utf8 = StandardCharsets.UTF_8.toString()
        val loginScope = URLEncoder.encode(merchantKey, utf8)
        val encodedSessionId = URLEncoder.encode(generateSessionID(), utf8)
        val encodedTransactionId = URLEncoder.encode(transactionId, utf8)
        val encodedMethod = URLEncoder.encode(method.type, utf8)

        return when (type) {
            PaymentType.DirectPayment -> {
                "${PAY_HOST}/$encodedTransactionId?embedding=true&sdk=true&method=$encodedMethod"
            }

            PaymentType.DirectPaymentWithPossibleAuth -> {
                "${PAY_HOST}/$encodedTransactionId?embedding=true&sdk=true&method=$encodedMethod&loginScope=$loginScope&sessionId=$encodedSessionId"
            }

            PaymentType.Collections -> {
                "${PAY_HOST}/collections?loginScope=$loginScope&sessionId=$encodedSessionId"
            }

            else -> {
                val redirect = when (withRedirect) {
                    true -> {
                        var extra = ""
                        if (method is PaymentMethod.SavedInfo) {
                            if (method.bank == null && method.card == null) {
                                throw IllegalArgumentException("Payment method must have either a bank or card")
                            } else {
                                extra = "bankId=${
                                    URLEncoder.encode(
                                        method.bank?.id ?: method.card?.bankId,
                                        utf8
                                    )
                                }"
                            }
                        }
                        "&redirect=${
                            URLEncoder.encode(
                                "$PAY_HOST/$encodedTransactionId?embedding=true&sdk=true&method=${encodedMethod}&$extra",
                                utf8
                            )
                        }"
                    }

                    else -> ""
                }
                "$PAY_HOST/login?loginScope=$loginScope$redirect&sessionId=$encodedSessionId&transactionId=$encodedTransactionId"
            }
        }
    }

    private fun generateSessionID() = SecureRandom().nextInt(999_999_999).toString()

    companion object : SingletonCompanion<PaymentRepository, Context>() {
        override fun createInstance(dependency: Context) = PaymentRepository(dependency)
    }
}