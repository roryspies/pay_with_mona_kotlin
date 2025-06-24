package ng.mona.paywithmona.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Collection(
    val id: String? = null,
    val isConsented: Boolean = false,
    @SerialName("maxAmount") val maxAmountInKobo: String? = null,
    val expiryDate: String? = null,
    val startDate: String? = null,
    val monthlyLimit: String? = null,
    val schedule: CollectionSchedule? = null,
    val reference: String? = null,
    val status: String? = null,
    val lastCollectedAt: String? = null,
    val nextCollectionAt: String? = null,
    val debitType: String? = null,
    val loanLinkToken: String? = null
)

@Serializable
data class CollectionSchedule(
    val type: String,
    val entries: List<CollectionScheduleEntry>,
    val frequency: String? = null,
    @SerialName("amount") val amountInKobo: String? = null
)

@Serializable
data class CollectionScheduleEntry(
    @SerialName("amount") val amountInKobo: String,
    val date: String
)
