package com.mona.sdk.service.sdk

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.snackbar.Snackbar
import com.mona.sdk.data.local.SdkStorage
import com.mona.sdk.data.model.MerchantBranding
import com.mona.sdk.data.model.MonaCheckout
import com.mona.sdk.data.model.MonaProduct
import com.mona.sdk.data.remote.dto.InitiatePaymentResponse
import com.mona.sdk.data.repository.AuthRepository
import com.mona.sdk.data.repository.CheckoutRepository
import com.mona.sdk.domain.MonaSdkState
import com.mona.sdk.domain.PaymentMethod
import com.mona.sdk.domain.PaymentType
import com.mona.sdk.event.AuthState
import com.mona.sdk.event.SdkState
import com.mona.sdk.event.TransactionState
import com.mona.sdk.presentation.PaymentMethods
import com.mona.sdk.presentation.theme.SdkColors
import com.mona.sdk.presentation.theme.SdkTheme
import com.mona.sdk.service.bottomsheet.BottomSheetContent
import com.mona.sdk.service.bottomsheet.BottomSheetHandler
import com.mona.sdk.service.bottomsheet.BottomSheetResponse
import com.mona.sdk.service.sse.FirebaseSseListener
import com.mona.sdk.service.sse.SseListenerType
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

internal class PayWithMonaSdkImpl(merchantKey: String, context: Context) {
    private var activity: FragmentActivity? = null

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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

    private var state = MonaSdkState()

    private val customTabsConnection by lazy {
        CustomTabsConnection(context)
    }

    private val bottomSheet by lazy {
        BottomSheetHandler(
            scope = scope,
            state = { state },
            transactionState = { transactionState }
        )
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

    val merchantBranding = MutableStateFlow<MerchantBranding?>(null)

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
            var branding = storage.merchantBranding.first()
            if (existingKey != merchantKey || branding == null) {
                auth.fetchMerchantBranding(merchantKey) ?: throw IllegalStateException(
                    "Failed to fetch merchant branding for key: $merchantKey"
                )
            }

            // Update the branding
            storage.merchantBranding.first()?.let { branding ->
                SdkColors.primary = branding.colors.primary
                SdkColors.text = branding.colors.text
                merchantBranding.update { branding }
            }
        }
    }

    @Composable
    fun PayWithMona(
        payment: InitiatePaymentResponse,
        checkout: MonaCheckout,
        modifier: Modifier,
    ) = SdkTheme(
        content = {
            val currentActivity = LocalActivity.current
            when (currentActivity) {
                is FragmentActivity -> {
                    activity = currentActivity

                    val sdkState by sdkState.collectAsStateWithLifecycle(SdkState.Idle)

                    LaunchedEffect(payment, checkout) {
                        state.let {
                            it.paymentOptions.update { payment.savedPaymentOptions }
                            it.transactionId = payment.transactionId
                            it.friendlyId = payment.friendlyId
                            it.checkout = checkout
                        }
                    }

                    PaymentMethods(
                        payment.savedPaymentOptions,
                        modifier,
                        sdkState == SdkState.Loading,
                        ::makePayment
                    )

                    DisposableEffect(Unit) {
                        onDispose {
                            activity = null
                            resetInternalState()
                        }
                    }
                }

                else -> {
                    throw IllegalStateException("PayWithMona must be used within a FragmentActivity")
                }
            }
        }
    )

    suspend fun reset() {
        // Reset the SDK state
        resetInternalState()
        transactionState.update { TransactionState.Idle }
        authState.update { AuthState.LoggedOut }

        // Clear the stored preferences
        storage.clear()
    }

    private fun makePayment(method: PaymentMethod) = scope.launch {
        try {
            sdkState.update { SdkState.Loading }

            state.method = method

            // Initialize SSE listener for real-time events
            sse.initialize()

            // Concurrently listen for transaction completion.
            try {
                sseListener.subscribeToEvents(SseListenerType.PaymentUpdates)
                sseListener.subscribeToEvents(SseListenerType.TransactionMessages)
            } catch (_: Exception) {

            }

            val hasKey = !storage.keyId.first().isNullOrBlank()
            val isSavedPaymentMethod = method is PaymentMethod.SavedInfo

            val pay = suspend {
                bottomSheet.show(BottomSheetContent.Loading, activity)
                val response = checkout.makePayment(activity, state)
                if (response != null) {
                    state.friendlyId = response["friendlyID"]?.jsonPrimitive?.content
                    sdkState.update { SdkState.TransactionInitiated }
                    transactionState.update {
                        TransactionState.Initiated(
                            transactionId = response["transactionRef"]?.jsonPrimitive?.content,
                            friendlyId = state.friendlyId,
                            amount = state.checkout?.transactionAmountInKobo
                        )
                    }
                    bottomSheet.show(
                        BottomSheetContent.CheckoutInitiated,
                        activity,
                    )
                }
            }

            // If the user has a key and is using a saved payment method, just show confirmation
            if (hasKey && isSavedPaymentMethod) {
                bottomSheet.show(
                    BottomSheetContent.CheckoutConfirmation,
                    activity,
                )
                if (bottomSheet.response.first() == BottomSheetResponse.Pay) {
                    pay()
                }
                return@launch
            }

            // Listen for custom tab close events
            try {
                sseListener.subscribeToEvents(SseListenerType.CustomTabs)
            } catch (_: Exception) {

            }

            // If the user doesn't have a key and they want to use a saved payment method,
            // key exchange needs to be done, so handle first.
            val doKeyExchange = !hasKey && isSavedPaymentMethod
            if (doKeyExchange) {
                initiateKeyExchange()
            }

            when (isSavedPaymentMethod) {
                true -> pay()

                else -> {
                    val sessionId = checkout.generateSessionId()
                    val url = UrlBuilder(
                        sessionId = sessionId,
                        merchantKey = getMerchantKey(),
                        transactionId = state.transactionId.orEmpty(),
                        method = method,
                        type = when (hasKey) {
                            true -> PaymentType.DirectPayment
                            else -> PaymentType.DirectPaymentWithPossibleAuth
                        }
                    )
                    async { sseListener.subscribeToAuthEvents(sessionId) }
                    launchUrl(url)
                }
            }
        } catch (e: Exception) {
            handleError(e, SdkState.Idle)
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
        sseListener.subscribeToAuthEvents(sessionId)
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

    private suspend fun performAuth(
        token: String,
        product: MonaProduct = MonaProduct.Checkout,
    ) = withContext(Dispatchers.Main) {
        var successful = false
        try {
            authState.update { AuthState.PerformingLogin }

            customTabsConnection.close(activity)

            sdkState.update { SdkState.Loading }

            val loginResponse = auth.login(
                token,
                state.checkout?.phoneNumber.orEmpty()
            ) ?: return@withContext

            bottomSheet.show(BottomSheetContent.KeyExchange, activity)

            when (bottomSheet.response.first() == BottomSheetResponse.CanEnrol) {
                true -> {
                    bottomSheet.show(BottomSheetContent.Loading, activity)
                    auth.signAndCommitKeys(
                        loginResponse["deviceAuth"]!!.jsonObject,
                        activity ?: return@withContext,
                    )

                    val checkoutId = storage.checkoutId.first()
                    if (!checkoutId.isNullOrBlank()) {
                        authState.update { AuthState.LoggedIn }
                        sdkState.update { SdkState.Success }
                    }

                    successful = true
                }

                else -> {
                    handleError(Exception("Enrollment Declined"), SdkState.Idle)
                    authState.update { AuthState.LoggedOut }
                }
            }
        } catch (e: Exception) {
            handleError(e, SdkState.Idle)
            authState.update { AuthState.LoggedOut }
        } finally {
            if (!successful) {
                bottomSheet.dismiss()
            }
        }
    }

    private fun launchUrl(url: String) = scope.launch(Dispatchers.Main) {
        customTabsConnection.launch(
            url,
            activity ?: return@launch,
            color = SdkColors.primary
        )
    }

    private fun handleError(error: Throwable, state: SdkState = SdkState.Error) {
        scope.launch {
            val message = when (error) {
                is ClientRequestException -> try {
                    error.response.body<JsonObject>()["message"]?.jsonPrimitive?.content
                } catch (_: Exception) {
                    null
                } ?: "An error occurred while processing your request"

                else -> error.message ?: "An unknown error occurred"
            }
            Timber.e(error)
            activity?.run {
                Snackbar.make(
                    window.decorView,
                    message,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            sdkState.update { state }
        }
    }

    private fun resetInternalState() {
        state = MonaSdkState()
        sdkState.update { SdkState.Idle }
        bottomSheet.dismiss()
        customTabsConnection.close(activity)
        sse.stopAllListening()
    }
}