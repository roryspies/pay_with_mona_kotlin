package ng.mona.paywithmona.domain

import kotlinx.coroutines.flow.MutableStateFlow
import ng.mona.paywithmona.data.model.MonaCheckout
import ng.mona.paywithmona.data.model.PaymentOptions

internal class MonaSdkState {
    val paymentOptions = MutableStateFlow<PaymentOptions?>(null)
    var transactionId: String? = null
    var friendlyId: String? = null
    var checkout: MonaCheckout? = null
    var method: PaymentMethod? = null
}