package com.mona.sdk.util

import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume

internal fun <T> CancellableContinuation<T>.resumeSafely(value: T) {
    if (isActive) {
        resume(value)
    } else {
        // Continuation is already completed, do nothing
    }
}