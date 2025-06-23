package ng.mona.paywithmona.service.sdk

import ng.mona.paywithmona.data.remote.ApiConfig
import ng.mona.paywithmona.domain.PaymentMethod
import ng.mona.paywithmona.domain.PaymentType
import ng.mona.paywithmona.domain.type
import ng.mona.paywithmona.util.encodeUrl

internal object UrlBuilder {
    operator fun invoke(
        sessionId: String,
        merchantKey: String,
        transactionId: String,
        method: PaymentMethod? = null,
        type: PaymentType? = null,
        withRedirect: Boolean = true,
    ): String {
        val encodedTransactionId = transactionId.encodeUrl()
        val embedding = "embedding=true&sdk=true&method=${method?.type?.key?.encodeUrl().orEmpty()}"
        val scope = "loginScope=${merchantKey.encodeUrl()}&sessionId=${sessionId.encodeUrl()}"

        return when (type) {
            PaymentType.DirectPayment -> {
                "${ApiConfig.PAY_HOST}/$encodedTransactionId?$embedding"
            }

            PaymentType.DirectPaymentWithPossibleAuth -> {
                "${ApiConfig.PAY_HOST}/$encodedTransactionId?$embedding&$scope"
            }

            PaymentType.Collections -> {
                "${ApiConfig.PAY_HOST}/collections?$scope"
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
                        "&redirect=${"${ApiConfig.PAY_HOST}/$encodedTransactionId?$embedding&$scope&$extra".encodeUrl()}"
                    }

                    else -> ""
                }
                "${ApiConfig.PAY_HOST}/login?$scope&transactionId=$encodedTransactionId$redirect"
            }
        }
    }
}