package com.mona.sdk.domain

import com.mona.sdk.data.remote.ApiConfig.PAY_HOST
import com.mona.sdk.util.encodeUrl

internal object UrlBuilder {
    operator fun invoke(
        sessionId: String,
        merchantKey: String,
        transactionId: String,
        method: PaymentMethod? = null,
        type: PaymentType? = null,
        withRedirect: Boolean = true,
    ): String {
        val loginScope = merchantKey.encodeUrl()
        val encodedSessionId = sessionId.encodeUrl()
        val encodedTransactionId = transactionId.encodeUrl()
        val encodedMethod = method?.type?.key?.encodeUrl().orEmpty()

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
                                    (method.bank?.id ?: method.card?.bankId)?.encodeUrl()
                                }"
                            }
                        }
                        "&redirect=${"$PAY_HOST/$encodedTransactionId?embedding=true&sdk=true&method=${encodedMethod}&$extra".encodeUrl()}"
                    }

                    else -> ""
                }
                "$PAY_HOST/login?loginScope=$loginScope$redirect&sessionId=$encodedSessionId&transactionId=$encodedTransactionId"
            }
        }
    }
}