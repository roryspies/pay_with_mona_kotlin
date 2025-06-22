package com.mona.sdk.util

import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

internal object CryptoUtil {
    private const val OTHER_PUBLIC_KEY = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApXwFU8YtCfLGnE/YcgRK
JcL2G8aDM50f5blhgujFeLTrMxhQCLoO9HWOL9zcr+DyjVLxoNWvF2RAfJCWrMkv
6a3u21W19VkuHCKMsT872QHo2F8U+NmXXwzjIAElYqgUal0/2BHuvG9ko+azvMk2
RLGK5sZyJKK7iYZN0kosPtrHfEdUXm2eRy/9MKlTTqRx3UmdD4jTlvVEKjIzkKfM
to26uGrhBC1rGapeSPUHs0EoGXrzFzAn47Ua94Dg7TxlrwfRk2SfsCe7fQLma+mK
JokqEQibKB1XcWFSa6BoSrqQEdDLLHoASXgW0A3btPsK71v6c7F0E2zNlBV6D9Ka
aQIDAQAB
-----END PUBLIC KEY-----"""

    private fun getPublicKey(): PublicKey {
        // Strip headers and newlines to get base64 content only
        val publicKeyContent = OTHER_PUBLIC_KEY
            .replace(Regex("-----(BEGIN|END) PUBLIC KEY-----"), "")
            .replace(Regex("\\s"), "")

        // Decode base64 to get the key bytes
        val keyBytes = Base64.getDecoder().decode(publicKeyContent)

        // Create public key from X.509 format
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }

    fun encrypt(data: String): String {
        return Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding").apply {
            init(Cipher.ENCRYPT_MODE, getPublicKey())
        }.doFinal(data.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
    }
}