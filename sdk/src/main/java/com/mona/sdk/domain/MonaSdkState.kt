package com.mona.sdk.domain

import com.mona.sdk.data.model.PaymentOptions
import kotlinx.coroutines.flow.MutableStateFlow

internal class MonaSdkState {
    val paymentOptions = MutableStateFlow<PaymentOptions?>(null)
}