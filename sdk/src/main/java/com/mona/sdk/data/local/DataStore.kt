package com.mona.sdk.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.mona.sdk.domain.SingletonCompanion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.IOException
import java.util.Base64

/**
 * Manages preferences storage using AndroidX DataStore, with optional encryption.
 * Encrypted values are serialized to JSON, encrypted using Android Keystore,
 * and then Base64 encoded before storing as strings in DataStore.
 *
 * @param context The application context.
 * @param securityUtil An instance of [SecurityUtil] for encryption/decryption.
 */
internal class DataStore(
    context: Context,
    private val securityUtil: SecurityUtil,
) {
    private val dataStore = context.dataStore

    // Base64 encoder/decoder for converting byte arrays to/from string for storage
    private val base64Encoder = Base64.getEncoder()
    private val base64Decoder = Base64.getDecoder()

    /**
     * Retrieves a non-encrypted preference value.
     *
     * @param key The [Preferences.Key] for the preference.
     * @param defaultValue The default value if the preference is not found.
     * @return A [Flow] emitting the preference value.
     */
    fun <T> getPreference(key: Preferences.Key<T>, defaultValue: T? = null): Flow<T?> {
        return dataStore.data.catch { exception ->
            // DataStore corruption can manifest as IOException
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception // Re-throw other exceptions
            }
        }.map { preferences ->
            preferences[key] ?: defaultValue
        }
    }

    /**
     * Puts a non-encrypted preference value.
     *
     * @param key The [Preferences.Key] for the preference.
     * @param value The value to put.
     */
    suspend fun <T> putPreference(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    /**
     * Puts a sensitive value, encrypting it before storage.
     * The value is first serialized to JSON, then encrypted, and finally Base64 encoded.
     *
     * @param key The [Preferences.Key] (of type String) for the secure preference.
     * @param value The value to encrypt and store.
     * @throws SecurityException if encryption fails.
     */
    suspend inline fun <reified T> putSecurePreference(key: Preferences.Key<String>, value: T) {
        dataStore.edit { preferences ->
            val serializedInput = Json.encodeToString(value)

            val (iv, encryptedData) = try {
                securityUtil.encryptData(MASTER_KEY_ALIAS, serializedInput)
            } catch (e: Exception) {
                throw SecurityException("Failed to encrypt data for key: ${key.name}", e)
            }

            val ivBase64 = base64Encoder.encodeToString(iv)
            val encryptedDataBase64 = base64Encoder.encodeToString(encryptedData)

            val secureString = "$ivBase64$IV_ENCRYPTED_DATA_SEPARATOR$encryptedDataBase64"
            preferences[key] = secureString
        }
    }

    /**
     * Retrieves and decrypts a sensitive value.
     * The stored string is Base64 decoded, then decrypted, and finally deserialized from JSON.
     *
     * @param key The [Preferences.Key] (of type String) for the secure preference.
     * @param defaultValue The default value if the preference is not found or decryption fails.
     * @return A [Flow] emitting the decrypted preference value.
     * @throws SecurityException if decryption fails (e.g., tampering, wrong key).
     */
    inline fun <reified T> getSecurePreference(
        key: Preferences.Key<String>,
        defaultValue: T? = null
    ): Flow<T?> {
        return dataStore.data.catch { exception ->
            // Handle DataStore file corruption
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception // Re-throw other exceptions
            }
        }.map { preferences ->
            val secureString = preferences[key]

            if (secureString.isNullOrEmpty()) {
                return@map defaultValue
            }

            // Split IV and encrypted data
            val parts = secureString.split(IV_ENCRYPTED_DATA_SEPARATOR, limit = 2)
            if (parts.size != 2) {
                // Log or handle error: malformed stored string
                Timber.e("Error: Malformed secure string for key ${key.name}")
                return@map defaultValue
            }
            val (ivBase64, encryptedDataBase64) = parts

            // Base64 decode IV and encrypted data
            val iv = try {
                base64Decoder.decode(ivBase64)
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Malformed IV Base64 for key ${key.name}")
                return@map defaultValue
            }
            val encryptedData = try {
                base64Decoder.decode(encryptedDataBase64)
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Malformed encrypted data Base64 for key ${key.name}")
                return@map defaultValue
            }

            // Decrypt data using SecurityUtil
            val decryptedValue = try {
                securityUtil.decryptData(MASTER_KEY_ALIAS, iv, encryptedData)
            } catch (e: SecurityException) {
                Timber.e(e, "Decryption failed for key ${key.name}: ${e.message}")
                return@map defaultValue
            }

            // Deserialize from JSON using Kotlinx.serialization
            try {
                Json.decodeFromString<T>(decryptedValue)
            } catch (e: Exception) {
                Timber.e(e, "Deserialization failed for key ${key.name}: ${e.message}")
                defaultValue // Return default value on deserialization failure
            }
        }
    }

    /**
     * Removes a preference.
     *
     * @param key The [Preferences.Key] for the preference to remove.
     */
    suspend fun <T> removePreference(key: Preferences.Key<T>) {
        dataStore.edit { it.remove(key) }
    }

    /**
     * Clears all preferences from the DataStore.
     * **Use with caution**, as this will remove all data, including encrypted ones.
     */
    suspend fun clearAllPreference() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    companion object : SingletonCompanion<DataStore, Context>() {
        private val Context.dataStore by preferencesDataStore(
            name = "pay_with_mona_preferences",
        )

        // Constants for key alias and separators (if needed, but Base64 simplifies this)
        // The keyAlias refers to the alias of the master key stored in Android Keystore
        // which is used to encrypt/decrypt individual preference values.
        private const val MASTER_KEY_ALIAS = "finance_sdk_secure_pref_key"

        // Separator for storing IV and encrypted data as a single string
        private const val IV_ENCRYPTED_DATA_SEPARATOR = ":secure_iv_enc:"

        override fun createInstance(dependency: Context) = DataStore(dependency, SecurityUtil())
    }
}
