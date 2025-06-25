package ng.mona.paywithmona.service.bottomsheet

import ng.mona.paywithmona.domain.PaymentMethod

internal sealed interface BottomSheetResponse {
    data object CanEnrol : BottomSheetResponse

    data object Pay : BottomSheetResponse

    data class Otp(val otp: String) : BottomSheetResponse

    data class CheckoutComplete(val success: Boolean) : BottomSheetResponse

    data object ToCollectionAccountSelection : BottomSheetResponse

    data class ApproveCollectionDebiting(val method: PaymentMethod.SavedInfo) : BottomSheetResponse

    data object AddBankAccount : BottomSheetResponse

    data object Dismissed : BottomSheetResponse
}