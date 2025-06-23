package ng.mona.paywithmona.data.remote.dto

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ng.mona.paywithmona.data.model.PaymentOptions

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