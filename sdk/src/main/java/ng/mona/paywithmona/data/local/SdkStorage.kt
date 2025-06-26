package ng.mona.paywithmona.data.local

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import ng.mona.paywithmona.data.model.MerchantBranding
import ng.mona.paywithmona.data.serializer.SdkJson
import ng.mona.paywithmona.domain.SingletonCompanionWithDependency

internal class SdkStorage(
    context: Context,
) {
    private val store by lazy {
        DataStore.getInstance(context)
    }

    val merchantKey = store.getSecurePreference<String>(MERCHANT_KEY)

    val checkoutId = store.getSecurePreference<String>(MONA_CHECKOUT_ID)

    val merchantBranding = store.getPreference(
        key = MERCHANT_BRANDING,
        defaultValue = null
    ).map { value ->
        value?.let { SdkJson.decodeFromString<MerchantBranding>(it) }
    }

    val hasPasskey = store.getSecurePreference<String>(HAS_PASSKEY).map {
        it?.toBoolean() ?: false
    }

    val keyId = store.getSecurePreference<String>(KEY_ID)

    suspend fun setMerchantKey(merchantKey: String) {
        store.putSecurePreference(MERCHANT_KEY, merchantKey)
    }

    suspend fun setCheckoutId(checkoutId: String) {
        store.putSecurePreference(MONA_CHECKOUT_ID, checkoutId)
    }

    suspend fun setMerchantBranding(branding: MerchantBranding) {
        store.putPreference(MERCHANT_BRANDING, Json.encodeToString(branding))
    }

    suspend fun setHasPasskey(hasPasskey: Boolean) {
        store.putSecurePreference(HAS_PASSKEY, hasPasskey.toString())
    }

    suspend fun setKeyId(keyId: String) {
        store.putSecurePreference(KEY_ID, keyId)
    }

    suspend fun clear() {
        store.removePreference(MONA_CHECKOUT_ID)
        store.removePreference(KEY_ID)
        store.removePreference(HAS_PASSKEY)
    }

    companion object : SingletonCompanionWithDependency<SdkStorage, Context>() {
        private val MERCHANT_KEY = stringPreferencesKey("merchant_key")
        private val MERCHANT_BRANDING = stringPreferencesKey("merchant_branding")
        private val HAS_PASSKEY = stringPreferencesKey("has_passkey")
        private val KEY_ID = stringPreferencesKey("key_id")
        private val MONA_CHECKOUT_ID = stringPreferencesKey("mona_checkout_id")

        override fun createInstance(dependency: Context) = SdkStorage(dependency)
    }
}