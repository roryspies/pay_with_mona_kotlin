package com.mona.sdk.data.remote.dto

import androidx.compose.runtime.Stable
import com.mona.sdk.data.model.PaymentOptions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class InitiatePaymentResponse(
    val success: Boolean = false,
    val message: String = "",
    val transactionId: String? = null,
    @SerialName("friendlyID")
    val friendlyId: String? = null,
    val url: String? = null,
    val savedPaymentOptions: PaymentOptions? = null
)