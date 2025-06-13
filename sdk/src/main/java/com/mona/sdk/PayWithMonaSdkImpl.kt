package com.mona.sdk

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import androidx.activity.compose.LocalActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.ACTIVITY_SIDE_SHEET_DECORATION_TYPE_SHADOW
import androidx.browser.customtabs.CustomTabsIntent.ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_TOP
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import com.mona.sdk.data.repository.AuthRepository
import com.mona.sdk.data.repository.PaymentRepository
import com.mona.sdk.domain.MonaSdkState
import com.mona.sdk.domain.PaymentMethod
import com.mona.sdk.domain.PaymentType
import com.mona.sdk.event.SuccessRateType
import com.mona.sdk.presentation.PaymentMethods
import com.mona.sdk.presentation.theme.SdkColors
import com.mona.sdk.presentation.theme.SdkTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import kotlin.math.roundToInt


internal class PayWithMonaSdkImpl() : PayWithMonaSdk {
    private var _context: Context? = null
    private val context: Context
        get() = _context
            ?: throw IllegalStateException("PayWithMonaSdk not initialized. Call initialize() first.")

    private val scope = MainScope()

    private val auth: AuthRepository
        get() = AuthRepository.getInstance(context)

    private val payment: PaymentRepository
        get() = PaymentRepository.getInstance(context)

    private var customTabsClient: CustomTabsClient? = null
    private var customTabsSession: CustomTabsSession? = null
    private var customTabsReady: CompletableDeferred<Unit?>? = null
    private val customTabsConnection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(
            name: ComponentName,
            client: CustomTabsClient
        ) {
            customTabsClient = client
            customTabsReady?.complete(Unit)
            customTabsClient?.warmup(0)
            customTabsSession = customTabsClient?.newSession(null)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            customTabsClient = null
            customTabsReady?.complete(null)
            customTabsSession = null
        }
    }

    internal val state = MonaSdkState()

    override val merchantApiKey by lazy {
        auth.merchantApiKey
    }

    override val merchantBranding by lazy {
        auth.merchantBranding
    }

    override fun initialize(merchantKey: String, context: Context) {
        _context = context.applicationContext
        Timber.plant(Timber.DebugTree())
        scope.launch {
            val existingKey = auth.merchantKey.first()
            val branding = auth.merchantBranding.first()
            if (existingKey != merchantKey || branding == null) {
                auth.fetchMerchantBranding(merchantKey) ?: throw IllegalStateException(
                    "Failed to fetch merchant branding for key: $merchantKey"
                )
            }
        }
    }

    override suspend fun saveMerchantApiKey(merchantApiKey: String) = auth.saveMerchantApiKey(
        merchantApiKey
    )

    override suspend fun initiatePayment(
        transactionAmountInKobo: Int,
        successRateType: SuccessRateType,
        firstName: String?,
        lastName: String?,
        phoneNumber: String?,
        bvn: String?,
        dob: LocalDate?
    ) {
        val merchantKey = getMerchantKey()
        val merchantApiKey = auth.merchantApiKey.first()
        val paymentOptions = payment.initiatePayment(
            merchantApiKey = merchantApiKey,
            merchantKey = merchantKey,
            transactionAmountInKobo = transactionAmountInKobo,
            successRateType = successRateType,
            phoneNumber = phoneNumber,
            bvn = bvn,
            dob = dob,
            firstName = firstName,
            lastName = lastName
        )
        state.paymentOptions.update { paymentOptions }
    }

    internal fun makePayment(method: PaymentMethod, activity: Activity) = scope.launch {
        val url = payment.buildPaymentUrl(
            merchantKey = getMerchantKey(),
            transactionId = "",
            method = method,
            type = PaymentType.DirectPaymentWithPossibleAuth
        )
        launchUrl(url, activity)
    }

    @Composable
    override fun PayWithMona(modifier: Modifier) {
        SdkTheme(
            content = {
                val options by state.paymentOptions.collectAsState()
                val activity = LocalActivity.current
                PaymentMethods(options, modifier) {
                    makePayment(it, activity!!)
                }
            }
        )
    }

    private suspend fun getMerchantKey(): String {
        return auth.merchantKey.first() ?: throw IllegalStateException(
            "Merchant key is not set. Please initialize the SDK with a valid merchant key."
        )
    }

    private fun launchUrl(url: String, activity: Activity) = scope.launch {
        val color = merchantBranding.first()?.colors?.primary ?: SdkColors().primary
        bindCustomTabService(activity)
        val intent = CustomTabsIntent
            .Builder(customTabsSession)
            .setShowTitle(true)
            .setSendToExternalDefaultHandlerEnabled(true)
            .setInitialActivityHeightPx((getActivityHeight(activity) * 0.9).roundToInt())
            .setActivitySideSheetMaximizationEnabled(true)
            .setActivitySideSheetDecorationType(ACTIVITY_SIDE_SHEET_DECORATION_TYPE_SHADOW)
            .setActivitySideSheetRoundedCornersPosition(
                ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_TOP
            )
            .setToolbarCornerRadiusDp(16)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(color.toArgb())
                    .build()
            )
            .build()

        intent.launchUrl(activity, url.toUri())
    }

    private fun getActivityHeight(activity: Activity): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.windowManager.currentWindowMetrics.bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.getMetrics(metrics)
            metrics.heightPixels
        }
    }


    private suspend fun bindCustomTabService(context: Context) {
        // Check for an existing connection
        if (customTabsClient != null) {
            // Do nothing if there is an existing service connection
            return
        }

        // Get the default browser package name, this will be null if
        // the default browser does not provide a CustomTabsService
        val packageName = CustomTabsClient.getPackageName(context, null)
        if (packageName == null) {
            // Do nothing as service connection is not supported
            return
        }

        customTabsReady = CompletableDeferred()
        val bound = CustomTabsClient.bindCustomTabsService(
            context,
            packageName,
            customTabsConnection
        )

        // If binding to the service failed, we proceed without it
        if (!bound) {
            return
        }

        // delay to ensure the service is connected
        customTabsReady?.await()
    }
}