package ng.mona.paywithmona.service.sdk

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
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import ng.mona.paywithmona.data.local.SdkStorage
import ng.mona.paywithmona.data.model.Checkout
import ng.mona.paywithmona.data.model.Collection
import ng.mona.paywithmona.data.model.MerchantBranding
import ng.mona.paywithmona.data.model.MonaProduct
import ng.mona.paywithmona.data.repository.AuthRepository
import ng.mona.paywithmona.data.repository.CheckoutRepository
import ng.mona.paywithmona.data.repository.CollectionRepository
import ng.mona.paywithmona.data.serializer.SdkJson
import ng.mona.paywithmona.domain.PayWithMonaSdkState
import ng.mona.paywithmona.domain.PaymentMethod
import ng.mona.paywithmona.domain.PaymentType
import ng.mona.paywithmona.domain.id
import ng.mona.paywithmona.event.AuthState
import ng.mona.paywithmona.event.SdkState
import ng.mona.paywithmona.event.TransactionState
import ng.mona.paywithmona.presentation.PaymentMethods
import ng.mona.paywithmona.presentation.theme.SdkColors
import ng.mona.paywithmona.presentation.theme.SdkTheme
import ng.mona.paywithmona.service.bottomsheet.BottomSheetContent
import ng.mona.paywithmona.service.bottomsheet.BottomSheetHandler
import ng.mona.paywithmona.service.bottomsheet.BottomSheetResponse
import ng.mona.paywithmona.service.sse.FirebaseSseListener
import ng.mona.paywithmona.service.sse.SseListenerType
import timber.log.Timber
import java.security.SecureRandom

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

    private val collection by lazy {
        CollectionRepository.getInstance(context)
    }

    private val sse = FirebaseSseListener()

    private var state = PayWithMonaSdkState()

    private val customTabsConnection by lazy {
        CustomTabsConnection(context)
    }

    private val bottomSheet by lazy {
        BottomSheetHandler(
            scope = scope,
            state = { state },
            transactionState = { transactionState },
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

    val paymentOptions = state.paymentOptions

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
        checkout: Checkout,
        modifier: Modifier,
    ) = SdkTheme(
        content = {
            val currentActivity = LocalActivity.current
            when (currentActivity) {
                is FragmentActivity -> {
                    activity = currentActivity

                    LaunchedEffect(Unit) {
                        sdkState.update { SdkState.Loading }
                        validatePii()
                        sdkState.update { SdkState.Idle }
                    }

                    LaunchedEffect(checkout) {
                        state.let {
                            it.paymentOptions.update { checkout.paymentOptions }
                            it.checkout = checkout
                        }
                    }

                    val sdkState by sdkState.collectAsStateWithLifecycle(SdkState.Idle)
                    val paymentOptions by state.paymentOptions.collectAsStateWithLifecycle()
                    PaymentMethods(
                        paymentOptions,
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

    suspend fun consentCollection(id: String, activity: FragmentActivity): Collection? {
        try {
            this.activity = activity
            sdkState.update { SdkState.Loading }
            bottomSheet.show(BottomSheetContent.Loading, activity)

            val data = collection.fetchCollection(id)

            val merchantName = merchantBranding.first()?.name ?: merchantBranding.first()?.tradingName
            bottomSheet.show(
                BottomSheetContent.CollectionConfirmation(
                    merchantName = merchantName.orEmpty(),
                    collection = data
                ),
                activity
            )

            if (bottomSheet.response.first() != BottomSheetResponse.ToCollectionAccountSelection) {
                Timber.e("Collection consent was not granted")
                return null
            }

            // Initialize SSE listener for real-time events
            sse.initialize()

            val key = storage.keyId.first()
            if (key.isNullOrBlank()) {
                if (!initiateKeyExchange(product = MonaProduct.Collections)) {
                    Timber.e("Key exchange failed or was declined")
                    return null
                }
            }

            validatePii()

            bottomSheet.show(
                BottomSheetContent.CollectionAccountSelection(
                    merchantName = merchantName.orEmpty(),
                    collection = data,
                    paymentOptions = state.paymentOptions.value,
                ),
                activity
            )

            var method: PaymentMethod.SavedInfo
            do {
                when (val response = bottomSheet.response.first()) {
                    is BottomSheetResponse.ApproveCollectionDebiting -> {
                        method = response.method
                        break
                    }

                    BottomSheetResponse.AddBankAccount -> {
                        launchUrl(
                            UrlBuilder.addAccount(
                                MonaProduct.Checkout,
                                id
                            )
                        )
                    }

                    else -> {
                        Timber.e("Collection debiting was not approved")
                        return null
                    }
                }
            } while (true)

            bottomSheet.show(BottomSheetContent.Loading, activity)

            collection.consentCollection(
                method.id ?: return null,
                id,
                activity
            ) ?: return null

            bottomSheet.show(
                BottomSheetContent.CollectionSuccess(merchantName.orEmpty(), data, method),
                activity
            )

            // wait for a response from the bottom sheet first before continuing
            bottomSheet.response.first()

            return data.copy(
                bankId = method.id,
            )
        } catch (e: Exception) {
            handleError(e, SdkState.Idle)
            return null
        } finally {
            resetInternalState(false)
            this.activity = null
        }
    }

    suspend fun validatePii() = runCatching {
        val keyId = storage.keyId.first() ?: return@runCatching

        val response = auth.validatePii(keyId) ?: return@runCatching

        val exists = response["exists"]?.jsonPrimitive?.booleanOrNull ?: false
        // Non Mona User
        if (!exists) {
            return@runCatching authState.update { AuthState.NotAMonaUser }
        }

        // This is a Mona user, update the payment options
        state.paymentOptions.update {
            SdkJson.decodeFromJsonElement(response["savedPaymentOptions"] ?: return@update it)
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

    suspend fun reset() {
        // Reset the SDK state
        resetInternalState()
        authState.update { AuthState.LoggedOut }

        // Clear the stored preferences
        storage.clear()
    }

    private fun makePayment(method: PaymentMethod) = scope.launch {
        var success = false

        try {
            sdkState.update { SdkState.Loading }
            bottomSheet.show(BottomSheetContent.Loading, activity)

            state.method = method

            // Initialize SSE listener for real-time events
            sse.initialize()

            // Concurrently listen for transaction completion.
            try {
                sseListener.subscribeToEvents(SseListenerType.PaymentUpdates)
                sseListener.subscribeToEvents(SseListenerType.TransactionMessages)
                sseListener.subscribeToEvents(SseListenerType.CustomTabs)
            } catch (_: Exception) {

            }

            val hasKey = !storage.keyId.first().isNullOrBlank()
            val isSavedPaymentMethod = method is PaymentMethod.SavedInfo

            // If the user doesn't have a key and they want to use a saved payment method,
            // key exchange needs to be done, so handle first.
            val doKeyExchange = !hasKey && isSavedPaymentMethod
            if (doKeyExchange) {
                val response = initiateKeyExchange()
                if (!response) {
                    Timber.e("Key exchange failed or was declined")
                    return@launch
                }
            }

            when (isSavedPaymentMethod) {
                true -> run {
                    bottomSheet.show(BottomSheetContent.CheckoutConfirmation, activity)
                    if (bottomSheet.response.first() != BottomSheetResponse.Pay) {
                        return@run
                    }
                    bottomSheet.show(BottomSheetContent.Loading, activity)
                    val response = checkout.makePayment(
                        activity,
                        state,
                        bottomSheet
                    ) ?: return@run
                    state.checkout = state.checkout?.copy(
                        friendlyId = response["friendlyID"]?.jsonPrimitive?.content
                    )
                    sdkState.update { SdkState.TransactionInitiated }
                    transactionState.update {
                        TransactionState.Initiated(
                            transactionId = response["transactionRef"]?.jsonPrimitive?.content,
                            friendlyId = state.checkout?.friendlyId,
                            amount = state.checkout?.transactionAmountInKobo
                        )
                    }
                    bottomSheet.show(
                        BottomSheetContent.CheckoutInitiated,
                        activity,
                    )
                    // wait for the transaction to complete
                    val bottomSheetResponse = bottomSheet.response.first {
                        it is BottomSheetResponse.CheckoutComplete || it is BottomSheetResponse.Dismissed
                    }
                    success =
                        bottomSheetResponse is BottomSheetResponse.CheckoutComplete && bottomSheetResponse.success
                }

                else -> {
                    val sessionId = generateSessionId()
                    val url = UrlBuilder(
                        sessionId = sessionId,
                        merchantKey = getMerchantKey(),
                        transactionId = state.checkout?.transactionId.orEmpty(),
                        method = method,
                        type = when (hasKey) {
                            true -> PaymentType.DirectPayment
                            else -> PaymentType.DirectPaymentWithPossibleAuth
                        }
                    )
                    launchUrl(url)
                    sseListener.subscribeToAuthEvents(sessionId)
                }
            }
        } catch (e: Exception) {
            handleError(e, SdkState.Idle)
        } finally {
            resetInternalState(false)
        }

        if (success) {
            transactionState.update {
                val info = it as? TransactionState.WithInfo ?: return@update it
                TransactionState.NavigateToResult(
                    transactionId = info.transactionId
                        ?: state.checkout?.transactionId.orEmpty(),
                    friendlyId = info.friendlyId ?: state.checkout?.friendlyId.orEmpty(),
                    amount = info.amount ?: state.checkout?.transactionAmountInKobo ?: 0L,
                )
            }
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
    ): Boolean {
        val sessionId = generateSessionId()
        val url = UrlBuilder(
            sessionId = sessionId,
            merchantKey = getMerchantKey(),
            transactionId = state.checkout?.transactionId.orEmpty(),
            method = method,
            withRedirect = withRedirect,
            type = when (product) {
                MonaProduct.Collections -> PaymentType.Collections
                else -> null
            }
        )
        launchUrl(url)
        return sseListener.subscribeToAuthEvents(sessionId, product)
    }

    private suspend fun performAuth(
        token: String,
        product: MonaProduct = MonaProduct.Checkout,
    ): Boolean = withContext(Dispatchers.Main) {
        var successful = false

        try {
            authState.update { AuthState.PerformingLogin }

            customTabsConnection.close(activity)

            sdkState.update { SdkState.Loading }

            val loginResponse = auth.login(
                token,
                state.checkout?.phoneNumber.orEmpty()
            ) ?: return@withContext false

            bottomSheet.show(BottomSheetContent.KeyExchange, activity)

            when (bottomSheet.response.first() == BottomSheetResponse.CanEnrol) {
                true -> {
                    bottomSheet.show(BottomSheetContent.Loading, activity)
                    auth.signAndCommitKeys(
                        loginResponse["deviceAuth"]!!.jsonObject,
                        activity ?: return@withContext false,
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

        successful
    }

    private fun generateSessionId() = SecureRandom().nextInt(999_999_999).toString()

    private fun launchUrl(url: String) = scope.launch(Dispatchers.Main) {
        customTabsConnection.launch(
            url,
            activity ?: throw IllegalStateException(
                "Activity is not set. Make sure to call sdk function within a FragmentActivity."
            ),
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
            resetInternalState(false)
            sdkState.update { state }
        }
    }

    private fun resetInternalState(
        resetMonaState: Boolean = true,
    ) {
        if (resetMonaState) {
            state = PayWithMonaSdkState()
        }
        sdkState.update { SdkState.Idle }
        bottomSheet.dismiss()
        customTabsConnection.close(activity)
        sse.stopAllListening()
    }
}