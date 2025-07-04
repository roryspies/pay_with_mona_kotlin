package ng.mona.paywithmona.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MerchantBranding(
    val name: String,
    val image: String,
    val tradingName: String,
    val colors: BrandingColors,
)