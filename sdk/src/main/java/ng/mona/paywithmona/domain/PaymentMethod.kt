package ng.mona.paywithmona.domain

import ng.mona.paywithmona.data.model.Bank
import ng.mona.paywithmona.data.model.Card

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

internal val PaymentMethod.SavedInfo.logo: String?
    get() = bank?.logo ?: card?.logo

internal val PaymentMethod.SavedInfo.name: String?
    get() = bank?.name ?: card?.bankName


internal val PaymentMethod.SavedInfo.number: String?
    get() = bank?.accountNumber ?: card?.accountNumber

internal val PaymentMethod.SavedInfo.id: String?
    get() = bank?.id ?: card?.bankId

internal val PaymentMethod.SavedInfo.activeIn: Int?
    get() = bank?.activeIn ?: card?.activeIn