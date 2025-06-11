package com.mona.sdk.data.repository

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mona.sdk.data.local.DataStore
import com.mona.sdk.data.model.MerchantBranding
import com.mona.sdk.data.remote.httpClient
import com.mona.sdk.domain.SingletonCompanion
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import timber.log.Timber

internal class AuthRepository(
    private val context: Context,
) {
    private val dataStore: DataStore
        get() = DataStore.getInstance(context)

    val merchantKey = dataStore.getSecurePreference<String>(MERCHANT_KEY)

    val merchantApiKey = dataStore.getSecurePreference<String>(MERCHANT_API_KEY)

    val merchantBranding = dataStore.getPreference(
        key = MERCHANT_BRANDING,
        defaultValue = null
    ).map { value ->
        value?.let { Json.decodeFromString<MerchantBranding>(it) }
    }

    suspend fun fetchMerchantBranding(merchantKey: String): MerchantBranding? {
        return try {
            val response: JsonObject = httpClient.get("merchant/sdk") {
                header("x-public-key", merchantKey)
            }.body()
            Timber.d("Fetched merchant branding: $response")
            val branding = response["data"] ?: return null

            dataStore.putSecurePreference(MERCHANT_KEY, merchantKey)
            dataStore.putPreference(MERCHANT_BRANDING, branding.toString())
            Json.decodeFromJsonElement<MerchantBranding>(branding)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch merchant branding")
            null
        }
    }

    suspend fun saveMerchantApiKey(merchantApiKey: String) {
        dataStore.putSecurePreference(MERCHANT_API_KEY, merchantApiKey)
    }

    companion object : SingletonCompanion<AuthRepository, Context>() {
        private val MERCHANT_KEY = stringPreferencesKey("merchant_key")
        private val MERCHANT_API_KEY = stringPreferencesKey("merchant_api_key")
        private val MERCHANT_BRANDING = stringPreferencesKey("merchant_branding")

        override fun createInstance(dependency: Context) = AuthRepository(dependency)
    }
}