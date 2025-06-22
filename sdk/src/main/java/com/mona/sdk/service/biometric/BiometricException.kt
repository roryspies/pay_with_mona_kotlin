package com.mona.sdk.service.biometric

class BiometricException(
    override val message: String,
    cause: Throwable? = null
) : Exception(message, cause)