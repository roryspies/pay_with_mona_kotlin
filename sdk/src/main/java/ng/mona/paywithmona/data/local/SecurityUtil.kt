package ng.mona.paywithmona.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProperties.BLOCK_MODE_GCM
import android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
import java.security.InvalidAlgorithmParameterException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.UnrecoverableKeyException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Utility class for performing AES/GCM encryption and decryption using the Android Keystore.
 * Keys are stored securely in the hardware-backed keystore.
 */
internal class SecurityUtil {
    // Lazy initialization for thread-safe access to cryptographic objects
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }

    private val keyGenerator: KeyGenerator by lazy {
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    }

    // Using ThreadLocal for Cipher to ensure thread safety without explicit synchronization
    // if multiple threads need to perform crypto ops concurrently.
    // Each thread gets its own Cipher instance.
    private val cipher: ThreadLocal<Cipher> = ThreadLocal.withInitial {
        try {
            Cipher.getInstance(AES_GCM_TRANSFORMATION)
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException("AES algorithm not found", e)
        } catch (e: NoSuchPaddingException) {
            throw IllegalStateException("NoPadding padding not found", e)
        }
    }

    /**
     * Encrypts the given plaintext string using a key identified by [keyAlias] from Android Keystore.
     *
     * @param keyAlias The alias for the key in Android Keystore.
     * @param plaintext The string to encrypt.
     * @return A Pair where first is the Initialization Vector (IV) and second is the encrypted data (ciphertext).
     * @throws SecurityException if cryptographic operations fail or key cannot be generated/retrieved.
     */
    @Throws(SecurityException::class)
    fun encryptData(keyAlias: String, plaintext: String): Pair<ByteArray, ByteArray> {
        return try {
            val secretKey = getOrCreateSecretKey(keyAlias)
            cipher.get()?.run {
                init(Cipher.ENCRYPT_MODE, secretKey)
                val encryptedData = doFinal(plaintext.toByteArray(Charsets.UTF_8))
                Pair(iv, encryptedData) // IV is generated internally by GCM during encryption
            } ?: throw IllegalStateException("Cipher instance is null")
        } catch (e: Exception) {
            throw SecurityException("Encryption failed for alias $keyAlias", e)
        }
    }

    /**
     * Decrypts the given encrypted data using a key identified by [keyAlias] from Android Keystore.
     *
     * @param keyAlias The alias for the key in Android Keystore.
     * @param iv The Initialization Vector (IV) used during encryption.
     * @param encryptedData The encrypted data (ciphertext).
     * @return The decrypted plaintext string.
     * @throws SecurityException if cryptographic operations fail or key cannot be retrieved.
     */
    @Throws(SecurityException::class)
    fun decryptData(keyAlias: String, iv: ByteArray, encryptedData: ByteArray): String {
        return try {
            val secretKey = getSecretKey(keyAlias) // Key must exist for decryption
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.get()?.run {
                init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)
                doFinal(encryptedData).toString(Charsets.UTF_8)
            } ?: throw IllegalStateException("Cipher instance is null")
        } catch (e: UnrecoverableKeyException) {
            throw SecurityException("Key '$keyAlias' not found or unrecoverable for decryption.", e)
        } catch (e: NoSuchAlgorithmException) {
            throw SecurityException("Algorithm not found.", e)
        } catch (e: KeyStoreException) {
            throw SecurityException("Keystore error.", e)
        } catch (e: NoSuchProviderException) {
            throw SecurityException("Security provider not found.", e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw SecurityException("Invalid algorithm parameter.", e)
        } catch (e: BadPaddingException) {
            // This can indicate tampering or wrong key/IV
            throw SecurityException(
                "Decryption failed due to bad padding or authentication tag.",
                e
            )
        } catch (e: IllegalBlockSizeException) {
            throw SecurityException("Decryption failed due to illegal block size.", e)
        } catch (e: NoSuchPaddingException) {
            throw SecurityException("NoPadding padding not found.", e)
        } catch (e: Exception) {
            throw SecurityException(
                "An unexpected error occurred during decryption for alias $keyAlias.",
                e
            )
        }
    }

    /**
     * Retrieves an existing SecretKey or generates a new one if it doesn't exist for the given alias.
     * Keys are generated with AES/GCM properties.
     *
     * @param keyAlias The alias for the key.
     * @return The SecretKey.
     * @throws SecurityException if key generation or retrieval fails.
     */
    @Throws(SecurityException::class)
    private fun getOrCreateSecretKey(keyAlias: String): SecretKey {
        return try {
            (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey ?: run {
                keyGenerator.apply {
                    init(
                        KeyGenParameterSpec.Builder(
                            keyAlias,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                        )
                            .setBlockModes(BLOCK_MODE_GCM)
                            .setEncryptionPaddings(ENCRYPTION_PADDING_NONE)
                            .setKeySize(256)
                            // Crucial for GCM: ensures a unique IV per encryption
                            .setRandomizedEncryptionRequired(true)
                            // Optional: require user authentication (e.g., biometrics) to use the key
                            // .setUserAuthenticationRequired(true)
                            // Optional: invalidate key if new biometrics are enrolled
                            // .setInvalidatedByBiometricEnrollment(true)
                            // Optional: stronger security with StrongBox Keymaster (if available)
                            // .setIsStrongBoxBacked(true)
                            .build()
                    )
                }.generateKey()
            }
        } catch (e: Exception) {
            throw SecurityException("Failed to get or create secret key for alias $keyAlias", e)
        }
    }

    /**
     * Retrieves an existing SecretKey for decryption. Assumes the key already exists.
     * Used internally by decryptData.
     *
     * @param keyAlias The alias for the key.
     * @return The SecretKey.
     * @throws UnrecoverableKeyException if the key cannot be found or retrieved.
     */
    @Throws(
        UnrecoverableKeyException::class,
        KeyStoreException::class,
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class
    )
    private fun getSecretKey(keyAlias: String): SecretKey {
        return keyStore.getKey(keyAlias, null) as? SecretKey
            ?: throw UnrecoverableKeyException("Key '$keyAlias' not found in Keystore.")
    }

    companion object {
        // Constants for cryptographic operations
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128 // Bits
    }
}
