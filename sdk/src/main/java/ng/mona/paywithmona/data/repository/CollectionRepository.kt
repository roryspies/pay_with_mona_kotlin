package ng.mona.paywithmona.data.repository

import android.content.Context
import kotlinx.serialization.json.JsonObject
import ng.mona.paywithmona.data.local.SdkStorage
import ng.mona.paywithmona.domain.SingletonCompanionWithDependency

internal class CollectionRepository private constructor(
    context: Context,
) {
    private val storage by lazy {
        SdkStorage.getInstance(context)
    }

    suspend fun consentCollection(): JsonObject? {}

    companion object : SingletonCompanionWithDependency<CollectionRepository, Context>() {
        override fun createInstance(dependency: Context) = CollectionRepository(dependency)
    }
}