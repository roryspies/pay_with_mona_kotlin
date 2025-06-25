package ng.mona.paywithmona.service.bottomsheet

import ng.mona.paywithmona.data.model.Collection

internal sealed interface BottomSheetContent {
    data class OtpInput(
        val title: String,
        val length: Int = 4,
        val isPassword: Boolean = false
    ) : BottomSheetContent

    data object KeyExchange : BottomSheetContent
    data object CheckoutInitiated : BottomSheetContent
    data object CheckoutSuccess : BottomSheetContent
    data object CheckoutFailure : BottomSheetContent
    data object CheckoutConfirmation : BottomSheetContent
    data class CollectionConfirmation(
        val merchantName: String,
        val collection: Collection
    ) : BottomSheetContent
    data object CollectionAccountSelection : BottomSheetContent
    data object CollectionSuccess : BottomSheetContent
    data object Loading : BottomSheetContent
}