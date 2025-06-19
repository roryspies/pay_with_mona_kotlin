package com.mona.sdk

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mona.sdk.data.model.MerchantBranding
import com.mona.sdk.data.model.MonaCheckout
import com.mona.sdk.data.remote.dto.InitiatePaymentResponse
import com.mona.sdk.data.service.sdk.PayWithMonaSdkImpl
import com.mona.sdk.event.AuthState
import com.mona.sdk.event.SdkState
import com.mona.sdk.event.TransactionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PayWithMonaSdk {

    val keyId: Flow<String?>

    val merchantKey: Flow<String?>

    val merchantBranding: Flow<MerchantBranding?>

    val authState: StateFlow<AuthState>

    val sdkState: StateFlow<SdkState>

    val transactionState: StateFlow<TransactionState>

    fun initialize(merchantKey: String, context: Context)

    @Composable
    fun PayWithMona(
        payment: InitiatePaymentResponse,
        checkout: MonaCheckout,
        modifier: Modifier = Modifier,
    )

    companion object {
        val instance: PayWithMonaSdk by lazy { PayWithMonaSdkImpl() }

        operator fun invoke(): PayWithMonaSdk = instance
    }
}