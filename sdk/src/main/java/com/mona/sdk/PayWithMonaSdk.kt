package com.mona.sdk

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mona.sdk.data.model.MerchantBranding
import com.mona.sdk.data.model.MonaCheckout
import com.mona.sdk.data.remote.dto.InitiatePaymentResponse
import com.mona.sdk.event.AuthState
import com.mona.sdk.event.SdkState
import com.mona.sdk.event.TransactionState
import com.mona.sdk.service.sdk.PayWithMonaSdkImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

object PayWithMonaSdk {
    @SuppressLint("StaticFieldLeak")
    internal lateinit var instance: PayWithMonaSdkImpl

    val keyId: Flow<String?>
        get() = instance.keyId

    val merchantKey: Flow<String?>
        get() = instance.merchantKey

    val merchantBranding: StateFlow<MerchantBranding?>
        get() = instance.merchantBranding

    val authState: StateFlow<AuthState>
        get() = instance.authState

    val sdkState: StateFlow<SdkState>
        get() = instance.sdkState

    val transactionState: StateFlow<TransactionState>
        get() = instance.transactionState

    fun initialize(merchantKey: String, context: Context) {
        instance = PayWithMonaSdkImpl(merchantKey, context)
    }

    @Composable
    fun PayWithMona(
        payment: InitiatePaymentResponse,
        checkout: MonaCheckout,
        modifier: Modifier = Modifier,
    ) {
        instance.PayWithMona(
            payment,
            checkout,
            modifier,
        )
    }

    suspend fun reset() {
        instance.reset()
    }
}