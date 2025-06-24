package ng.mona.paywithmona.domain

import kotlinx.coroutines.flow.MutableStateFlow
import ng.mona.paywithmona.data.model.Checkout
import ng.mona.paywithmona.data.model.PaymentOptions

internal class PayWithMonaSdkState {
    val paymentOptions = MutableStateFlow<PaymentOptions?>(null)
    var checkout: Checkout? = null
    var method: PaymentMethod? = null
}