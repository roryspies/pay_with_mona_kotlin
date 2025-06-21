package com.mona.sdk.data.service.sdk

import android.app.Activity
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.android.material.snackbar.Snackbar
import com.mona.sdk.data.local.SdkStorage
import com.mona.sdk.data.model.MonaCheckout
import com.mona.sdk.data.model.MonaProduct
import com.mona.sdk.data.remote.dto.InitiatePaymentResponse
import com.mona.sdk.data.repository.AuthRepository
import com.mona.sdk.data.repository.CheckoutRepository
import com.mona.sdk.data.service.sse.FirebaseSseListener
import com.mona.sdk.data.service.sse.SseListenerType
import com.mona.sdk.domain.MonaSdkState
import com.mona.sdk.domain.PaymentMethod
import com.mona.sdk.domain.PaymentType
import com.mona.sdk.domain.UrlBuilder
import com.mona.sdk.event.AuthState
import com.mona.sdk.event.SdkState
import com.mona.sdk.event.TransactionState
import com.mona.sdk.presentation.ConfirmKeyExchangeModal
import com.mona.sdk.presentation.PaymentMethods
import com.mona.sdk.presentation.shared.bottomsheet.showBottomSheet
import com.mona.sdk.presentation.theme.SdkColors
import com.mona.sdk.presentation.theme.SdkTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

internal class PayWithMonaSdkImpl(merchantKey: String, context: Context) {
    private var activity: Activity? = null

    private val scope = MainScope()

    private val storage by lazy {
        SdkStorage.getInstance(context)
    }

    private val auth by lazy {
        AuthRepository.getInstance(context)
    }

    private val checkout by lazy {
        CheckoutRepository.getInstance(context)
    }

    private val sse = FirebaseSseListener()

    private val state = MonaSdkState()

    private val customTabsConnection by lazy {
        CustomTabsConnection(context)
    }

    private val sseListener by lazy {
        SseListener(
            sse = { sse },
            state = { state },
            performAuth = ::performAuth,
            closeCustomTabs = {
                customTabsConnection.close(activity)
            },
            updateTransactionState = { transactionState ->
                this.transactionState.update { transactionState }
            },
            updateSdkState = { sdkState ->
                this.sdkState.update { sdkState }
            },
        )
    }

    val keyId = storage.keyId

    val merchantKey = storage.merchantKey

    val merchantBranding = storage.merchantBranding

    val authState = MutableStateFlow(AuthState.LoggedOut)

    val sdkState = MutableStateFlow(SdkState.Idle)

    val transactionState = MutableStateFlow<TransactionState>(TransactionState.Idle)

    init {
        scope.launch {
            if (Timber.forest().isEmpty()) {
                Timber.plant(Timber.DebugTree())
            }

            // update the auth state based on the current user session
            val keyId = storage.keyId.first()
            authState.update {
                when (keyId.isNullOrBlank()) {
                    true -> AuthState.LoggedOut
                    false -> AuthState.LoggedIn
                }
            }

            // Initialize the SDK with the provided merchant key
            val existingKey = storage.merchantKey.first()
            val branding = storage.merchantBranding.first()
            if (existingKey != merchantKey || branding == null) {
                auth.fetchMerchantBranding(merchantKey) ?: throw IllegalStateException(
                    "Failed to fetch merchant branding for key: $merchantKey"
                )
            }
        }
    }

    @Composable
    fun PayWithMona(
        payment: InitiatePaymentResponse,
        checkout: MonaCheckout,
        modifier: Modifier,
    ) {
        SdkTheme(
            content = {
                activity = LocalActivity.current

                LaunchedEffect(payment, checkout) {
                    state.let {
                        it.paymentOptions.update { payment.savedPaymentOptions }
                        it.transactionId = payment.transactionId
                        it.friendlyId = payment.friendlyId
                        it.checkout = checkout
                    }
                }

                PaymentMethods(payment.savedPaymentOptions, modifier, ::makePayment)

                DisposableEffect(Unit) {
                    // clean up the activity reference when the composable is disposed
                    onDispose {
                        activity = null
                    }
                }
            }
        )
    }

    internal fun makePayment(method: PaymentMethod) = scope.launch {
        try {
            sdkState.update { SdkState.Loading }

            // Initialize SSE listener for real-time events
            sse.initialize()

            // Concurrently listen for transaction completion.
            try {
                sseListener.subscribeToEvents(SseListenerType.PaymentUpdates)
                sseListener.subscribeToEvents(SseListenerType.TransactionMessages)
                sseListener.subscribeToEvents(SseListenerType.CustomTabs)
            } catch (_: Exception) {

            }

            val keyId = storage.keyId.first()

            // If the user doesn't have a keyID and they want to use a saved payment method,
            // Key exchange needs to be done, so handle first.
            val doKeyExchange = keyId.isNullOrBlank() && method is PaymentMethod.SavedInfo
            if (doKeyExchange) {
                initiateKeyExchange(method)
            }

            when (method) {
                is PaymentMethod.SavedInfo -> {
                    val response = checkout.makePayment(method, activity, state) ?: return@launch
                    state.friendlyId = response["friendlyID"]?.jsonPrimitive?.content
                    sdkState.update { SdkState.TransactionInitiated }
                    transactionState.update {
                        TransactionState.Initiated(
                            transactionId = response["transactionRef"]?.jsonPrimitive?.content,
                            friendlyId = state.friendlyId,
                            amount = state.checkout?.transactionAmountInKobo
                        )
                    }
                }

                else -> {
                    val sessionId = checkout.generateSessionId()
                    val url = UrlBuilder(
                        sessionId = sessionId,
                        merchantKey = getMerchantKey(),
                        transactionId = state.transactionId.orEmpty(),
                        method = method,
                        type = when {
                            keyId.isNullOrBlank() -> PaymentType.DirectPaymentWithPossibleAuth
                            else -> PaymentType.DirectPayment
                        }
                    )
                    async { sseListener.subscribeToAuthEvents(sessionId) }
                    launchUrl(url)
                }
            }
        } finally {
            sdkState.update { SdkState.Idle }
        }
    }

    private suspend fun getMerchantKey(): String {
        return storage.merchantKey.first() ?: throw IllegalStateException(
            "Merchant key is not set. Please initialize the SDK with a valid merchant key."
        )
    }

    private suspend fun initiateKeyExchange(
        method: PaymentMethod? = null,
        withRedirect: Boolean = true,
        product: MonaProduct = MonaProduct.Checkout,
    ) {
        val sessionId = checkout.generateSessionId()
        val url = UrlBuilder(
            sessionId = sessionId,
            merchantKey = getMerchantKey(),
            transactionId = state.transactionId.orEmpty(),
            method = method,
            withRedirect = withRedirect,
            type = when (product) {
                MonaProduct.Collections -> PaymentType.Collections
                else -> null
            }
        )
        launchUrl(url)

        val result = sseListener.subscribeToAuthEvents(sessionId)
        Timber.e("Key exchange result: $result")
    }

    private suspend fun validatePii() {
        val keyId = storage.keyId.first() ?: return

        val response = auth.validatePii(keyId) ?: return

        val exists = response["exists"]?.jsonPrimitive?.booleanOrNull ?: false
        // Non Mona User
        if (!exists) {
            return authState.update { AuthState.NotAMonaUser }
        }

        // This is a Mona user, update the payment options
        state.paymentOptions.update {
            Json.decodeFromJsonElement(response["savedPaymentOptions"] ?: return@update it)
        }

        if (storage.keyId.first().isNullOrBlank()) {
            authState.update { AuthState.LoggedOut }
        }

        authState.update {
            when (storage.keyId.first().isNullOrBlank()) {
                // User has not done key exchange
                true -> AuthState.LoggedOut
                // User has done key exchange
                false -> AuthState.LoggedIn
            }
        }
    }

    private fun performAuth(
        token: String,
        product: MonaProduct = MonaProduct.Checkout,
    ) {
        scope.launch {
            authState.update { AuthState.PerformingLogin }

            customTabsConnection.close(activity)

            sdkState.update { SdkState.Loading }

            val loginResponse = auth.login(
                token,
                state.checkout?.phoneNumber.orEmpty()
            ) ?: return@launch

            val canEnrol = CompletableDeferred<Boolean>()

            val dialog = showBottomSheet(
                activity = activity,
                onDismiss = {
                    canEnrol.complete(false)
                },
                content = {
                    ConfirmKeyExchangeModal(
                        onUserDecision = { canEnrol.complete(it) }
                    )
                }
            )

            when (canEnrol.await()) {
                false -> {
                    activity?.run {
                        Snackbar.make(
                            window.decorView,
                            "Enrollment Declined",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        authState.update { AuthState.LoggedOut }
                        sdkState.update { SdkState.Idle }
                    }
                }

                true -> {
                    auth.signAndCommitKeys(
                        loginResponse["deviceAuth"]!!.jsonObject,
                        activity ?: return@launch,
                    )

                    val checkoutId = storage.checkoutId.first()
                    if (!checkoutId.isNullOrBlank()) {
                        authState.update { AuthState.LoggedIn }
                        sdkState.update { SdkState.Success }
                    }
                }
            }

            dialog?.dismiss()
        }
    }

    private fun launchUrl(url: String) = scope.launch {
        customTabsConnection.launch(
            url,
            activity ?: return@launch,
            color = merchantBranding.first()?.colors?.primary ?: SdkColors().primary
        )
    }
}