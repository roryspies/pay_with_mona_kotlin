package com.mona.sdk.data.local

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mona.sdk.data.model.MerchantBranding
import com.mona.sdk.domain.SingletonCompanionWithDependency
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

internal class SdkStorage(
    context: Context,
) {
    private val dataStore by lazy {
        DataStore.getInstance(context)
    }

    val merchantKey = dataStore.getSecurePreference<String>(MERCHANT_KEY)

    val merchantApiKey = dataStore.getSecurePreference<String>(MERCHANT_API_KEY)

    val checkoutId = dataStore.getSecurePreference<String>(MONA_CHECKOUT_ID)

    val merchantBranding = dataStore.getPreference(
        key = MERCHANT_BRANDING,
        defaultValue = null
    ).map { value ->
        value?.let { Json.decodeFromString<MerchantBranding>(it) }
    }

    val hasPasskey = dataStore.getSecurePreference<String>(HAS_PASSKEY).map {
        it?.toBoolean() ?: false
    }

    val keyId = dataStore.getSecurePreference<String>(KEY_ID)

    suspend fun setMerchantKey(merchantKey: String) {
        dataStore.putSecurePreference(MERCHANT_KEY, merchantKey)
    }

    suspend fun setCheckoutId(checkoutId: String) {
        dataStore.putSecurePreference(MONA_CHECKOUT_ID, checkoutId)
    }

    suspend fun setMerchantBranding(branding: MerchantBranding) {
        dataStore.putPreference(MERCHANT_BRANDING, Json.encodeToString(branding))
    }

    suspend fun setHasPasskey(hasPasskey: Boolean) {
        dataStore.putSecurePreference(HAS_PASSKEY, hasPasskey.toString())
    }

    suspend fun setKeyId(keyId: String) {
        dataStore.putSecurePreference(KEY_ID, keyId)
    }

    suspend fun setMerchantApiKey(merchantApiKey: String) {
        dataStore.putSecurePreference(MERCHANT_API_KEY, merchantApiKey)
    }

    companion object : SingletonCompanionWithDependency<SdkStorage, Context>() {
        private val MERCHANT_KEY = stringPreferencesKey("merchant_key")
        private val MERCHANT_API_KEY = stringPreferencesKey("merchant_api_key")
        private val MERCHANT_BRANDING = stringPreferencesKey("merchant_branding")
        private val HAS_PASSKEY = stringPreferencesKey("has_passkey")
        private val KEY_ID = stringPreferencesKey("key_id")
        private val MONA_CHECKOUT_ID = stringPreferencesKey("mona_checkout_id")

        override fun createInstance(dependency: Context) = SdkStorage(dependency)
    }
}