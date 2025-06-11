package com.mona.sdk

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mona.sdk.data.model.MerchantBranding
import kotlinx.coroutines.flow.Flow

interface PayWithMonaSdk {
    val merchantApiKey: Flow<String?>

    val merchantBranding: Flow<MerchantBranding?>

    fun initialize(merchantKey: String, context: Context)

    @Composable
    fun PayWithMona(
        modifier: Modifier,
        darkTheme: Boolean = isSystemInDarkTheme()
    )

    suspend fun saveMerchantApiKey(merchantApiKey: String)

    companion object {
        val instance: PayWithMonaSdk by lazy { PayWithMonaSdkImpl() }

        operator fun invoke(): PayWithMonaSdk = instance
    }
}