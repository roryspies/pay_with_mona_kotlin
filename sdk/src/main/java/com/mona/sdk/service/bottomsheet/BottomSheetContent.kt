package com.mona.sdk.service.bottomsheet

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
    data object CollectionAccountSelection : BottomSheetContent
    data object CollectionSuccess : BottomSheetContent
    data object CollectionConfirmation : BottomSheetContent
    data object Loading : BottomSheetContent
}