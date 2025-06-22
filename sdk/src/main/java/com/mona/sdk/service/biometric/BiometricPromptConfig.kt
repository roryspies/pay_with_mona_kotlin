package com.mona.sdk.service.biometric

data class BiometricPromptConfig(
    val title: String = "Biometric Authentication",
    val subtitle: String = "Authenticate to continue",
    val cancelButtonText: String = "Cancel"
)