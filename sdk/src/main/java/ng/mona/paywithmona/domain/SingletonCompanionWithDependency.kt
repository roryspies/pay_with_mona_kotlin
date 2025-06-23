package ng.mona.paywithmona.domain

internal abstract class SingletonCompanionWithDependency<T, D> {

    @Volatile
    private var instance: T? = null

    fun getInstance(dependency: D): T {
        return instance ?: synchronized(this) {
            instance ?: createInstance(dependency).also { instance = it }
        }
    }

    abstract fun createInstance(dependency: D): T
}