package ng.mona.paywithmona.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Bank(
    @SerialName("bankName")
    val name: String = "",

    @SerialName("bankId")
    val id: String = "",

    @SerialName("logo")
    val logo: String = "",

    @SerialName("institutionCode")
    val institutionCode: String = "",

    @SerialName("accountNumber")
    val accountNumber: String = "",

    @SerialName("webLinkAndroid")
    val webLinkAndroid: String? = null,

    @SerialName("ussdCode")
    val ussdCode: String? = null,

    @SerialName("isPrimary")
    val isPrimary: Boolean = false,

    @SerialName("isNibssAlt")
    val isNibssAlt: Boolean = false,

    @SerialName("manualPaymentRequired")
    val manualPaymentRequired: Boolean = false,

    @SerialName("hasInstantPay")
    val hasInstantPay: Boolean = false,

    @SerialName("activeIn")
    val activeIn: Int? = null
)