package com.mona.sdk.domain

internal abstract class SingletonCompanion<T> {

    @Volatile
    private var instance: T? = null

    operator fun invoke(): T {
        return instance ?: synchronized(this) {
            instance ?: createInstance().also { instance = it }
        }
    }

    abstract fun createInstance(): T
}