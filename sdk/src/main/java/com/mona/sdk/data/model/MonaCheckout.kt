package com.mona.sdk.data.model

import java.time.LocalDate

data class MonaCheckout(
    val transactionAmountInKobo: Int,
    val successEventType: SuccessEventType,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val bvn: String? = null,
    val dob: LocalDate? = null,
)
