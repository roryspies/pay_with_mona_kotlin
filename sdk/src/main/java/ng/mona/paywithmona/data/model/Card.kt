package ng.mona.paywithmona.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val bankId: String? = null,
    val institutionCode: String? = null,
    val accountNumber: String? = null,
    val bankName: String? = null,
    val accountName: String? = null,
    val logo: String? = null,
    val activeIn: Int? = null
)