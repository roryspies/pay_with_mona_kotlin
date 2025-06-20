package com.mona.sdk.data.model

import com.mona.sdk.data.serializer.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class MonaCheckout(
    val transactionAmountInKobo: Int,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val bvn: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val dob: LocalDate? = null,
)
