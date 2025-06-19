package com.mona.sdk.domain

import com.mona.sdk.data.model.Bank
import com.mona.sdk.data.model.Card

internal sealed interface PaymentMethod {
    data class SavedInfo(val card: Card? = null, val bank: Bank? = null) : PaymentMethod

    data object PayByTransfer : PaymentMethod

    data object PayWithCard : PaymentMethod
}

internal enum class PaymentMethodType(val key: String) {
    Card("card"),
    Bank("bank"),
    Transfer("transfer")
}

internal val PaymentMethod.type: PaymentMethodType
    get() = when (this) {
        is PaymentMethod.SavedInfo -> if (card != null) PaymentMethodType.Card else PaymentMethodType.Bank
        PaymentMethod.PayByTransfer -> PaymentMethodType.Transfer
        PaymentMethod.PayWithCard -> PaymentMethodType.Card
    }