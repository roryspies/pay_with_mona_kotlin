package ng.mona.paywithmona

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import ng.mona.paywithmona.data.model.Checkout
import ng.mona.paywithmona.data.model.Collection
import ng.mona.paywithmona.data.model.MerchantBranding
import ng.mona.paywithmona.event.AuthState
import ng.mona.paywithmona.event.SdkState
import ng.mona.paywithmona.event.TransactionState
import ng.mona.paywithmona.service.sdk.PayWithMonaSdkImpl

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
    fun PayWithMona(checkout: Checkout, modifier: Modifier = Modifier) {
        instance.PayWithMona(
            checkout,
            modifier,
        )
    }

    suspend fun consentCollection(collection: Collection, activity: FragmentActivity) {
        instance.consentCollection(collection, activity)
    }

    suspend fun reset() {
        instance.reset()
    }
}