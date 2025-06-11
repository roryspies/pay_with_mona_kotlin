package com.mona.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MonaCheckout(
    @SerialName("first_name")
    val firstName: String? = null,
    @SerialName("middle_name")
    val middleName: String? = null,
    @SerialName("last_name")
    val lastName: String? = null,
    @SerialName("date_of_birth")
    val dateOfBirth: String? = null,
    @SerialName("bvn")
    val bvn: String? = null,
    @SerialName("transaction_id")
    val transactionId: String? = null,
    @SerialName("merchant_name")
    val merchantName: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    @SerialName("primary_color")
    val primaryColor: Int? = null,
    @SerialName("secondary_color")
    val secondaryColor: Int? = null,
    @SerialName("amount")
    val amount: Double = 0.0,
)