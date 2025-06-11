package com.mona.sdk

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mona.sdk.data.repository.AuthRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

internal class PayWithMonaSdkImpl() : PayWithMonaSdk {
    private var context: Context? = null

    private val scope = MainScope()

    private val authRepository: AuthRepository
        get() = AuthRepository.getInstance(
            context
                ?: throw IllegalStateException("PayWithMonaSdk not initialized. Call initialize() first.")
        )

    override val merchantApiKey by lazy {
        authRepository.merchantApiKey
    }

    override val merchantBranding by lazy {
        authRepository.merchantBranding
    }

    override fun initialize(merchantKey: String, context: Context) {
        this.context = context.applicationContext
        Timber.plant(Timber.DebugTree())
        scope.launch {
            val existingKey = authRepository.merchantKey.first()
            val branding = authRepository.merchantBranding.first()
            if (existingKey != merchantKey || branding == null) {
                authRepository.fetchMerchantBranding(merchantKey) ?: throw IllegalStateException(
                    "Failed to fetch merchant branding for key: $merchantKey"
                )
            }
        }
    }

    override suspend fun saveMerchantApiKey(merchantApiKey: String) =
        authRepository.saveMerchantApiKey(merchantApiKey)

    @Composable
    override fun PayWithMona(modifier: Modifier, darkTheme: Boolean) {
    }
}