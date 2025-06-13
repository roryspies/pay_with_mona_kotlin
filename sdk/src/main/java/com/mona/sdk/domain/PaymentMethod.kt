package com.mona.sdk.domain

import com.mona.sdk.data.model.Bank
import com.mona.sdk.data.model.Card

internal sealed interface PaymentMethod {
    data class SavedInfo(val card: Card? = null, val bank: Bank? = null) : PaymentMethod

    data object PayByTransfer : PaymentMethod

    data object PayWithCard : PaymentMethod
}

internal val PaymentMethod.type: String
    get() = when (this) {
        is PaymentMethod.SavedInfo -> if (card != null) "card" else "bank"
        PaymentMethod.PayByTransfer -> "transfer"
        PaymentMethod.PayWithCard -> "card"
    }