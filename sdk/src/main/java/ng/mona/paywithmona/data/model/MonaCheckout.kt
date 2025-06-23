package ng.mona.paywithmona.data.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import ng.mona.paywithmona.data.serializer.LocalDateSerializer
import java.time.LocalDate

@Serializable
@Stable
data class MonaCheckout(
    val transactionAmountInKobo: Int,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val bvn: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val dob: LocalDate? = null,
)
