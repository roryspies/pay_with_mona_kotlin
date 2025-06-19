package com.mona.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentOptions(
    @SerialName("card")
    val cards: List<Card> = emptyList(),
    @SerialName("bank")
    val banks: List<Bank> = emptyList(),
)
