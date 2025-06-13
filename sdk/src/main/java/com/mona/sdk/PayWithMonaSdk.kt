package com.mona.sdk

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mona.sdk.data.model.MerchantBranding
import com.mona.sdk.domain.PaymentMethod
import com.mona.sdk.event.SuccessRateType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface PayWithMonaSdk {
    val merchantApiKey: Flow<String?>

    val merchantBranding: Flow<MerchantBranding?>

    fun initialize(merchantKey: String, context: Context)

    suspend fun saveMerchantApiKey(merchantApiKey: String)

    suspend fun initiatePayment(
        transactionAmountInKobo: Int,
        successRateType: SuccessRateType,
        firstName: String? = null,
        lastName: String? = null,
        phoneNumber: String? = null,
        bvn: String? = null,
        dob: LocalDate? = null,
    )

    @Composable
    fun PayWithMona(modifier: Modifier)

    companion object {
        val instance: PayWithMonaSdk by lazy { PayWithMonaSdkImpl() }

        operator fun invoke(): PayWithMonaSdk = instance

        internal val internalInstance get() = instance as PayWithMonaSdkImpl
    }
}