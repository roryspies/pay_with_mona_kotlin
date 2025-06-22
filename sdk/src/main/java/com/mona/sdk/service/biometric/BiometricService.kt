package com.mona.sdk.service.biometric

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mona.sdk.util.resumeSafely
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.concurrent.Executor
import kotlin.coroutines.resumeWithException

object BiometricService {
    private val unsupportedModels: List<String> = listOf("CP3706AS")
    private const val KEY_ALIAS = "ng.mona.app"

    fun isBiometricAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun generatePublicKey(): String {
        if (unsupportedModels.contains(Build.MODEL)) {
            throw BiometricException("Fallback not implemented for this model")
        }
        val keyPairGenerator = KeyPairGenerator.getInstance("EC", "AndroidKeyStore")
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true)
            .build()
        keyPairGenerator.initialize(parameterSpec)
        val keyPair = keyPairGenerator.generateKeyPair()
        return Base64.getEncoder().encodeToString(keyPair.public.encoded)
    }


    suspend fun createSignature(
        activity: FragmentActivity,
        data: String,
        config: BiometricPromptConfig = BiometricPromptConfig(
            title = "Sign Transaction",
            subtitle = "Use your biometric to authorize this transaction"
        )
    ): String = withContext(Dispatchers.Main) {
        val model = Build.MODEL
        if (unsupportedModels.contains(model)) {
            throw BiometricException("Fallback not implemented for this model")
        }
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as? java.security.PrivateKey
            ?: throw BiometricException("Private key not found. Generate key first.")

        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(privateKey)
        signature.update(data.toByteArray())

        val executor: Executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(config.title)
            .setSubtitle(config.subtitle)
            .setNegativeButtonText(config.cancelButtonText)
            .build()

        suspendCancellableCoroutine { cont ->
            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        try {
                            val crypto = result.cryptoObject?.signature
                            val signed = crypto?.sign()
                                ?: throw BiometricException("Signature is null")
                            val base64 = Base64.getEncoder().encodeToString(signed)
                            cont.resumeSafely(base64)
                        } catch (e: Exception) {
                            cont.resumeWithException(BiometricException("Signing failed", e))
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        cont.resumeWithException(BiometricException("Authentication error: $errString"))
                    }

                    override fun onAuthenticationFailed() {
                        // No-op, user can retry
                    }
                }
            )
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(signature))
        }
    }

    suspend fun signTransaction(activity: FragmentActivity, hashedTransaction: String) = try {
        createSignature(activity, hashedTransaction)
    } catch (_: BiometricException) {
        null
    }
}