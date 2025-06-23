package ng.mona.paywithmona.service.bottomsheet

internal sealed interface BottomSheetResponse {
    data object CanEnrol : BottomSheetResponse

    data object Pay : BottomSheetResponse

    data class Otp(val otp: String) : BottomSheetResponse

    data object Dismissed : BottomSheetResponse
}