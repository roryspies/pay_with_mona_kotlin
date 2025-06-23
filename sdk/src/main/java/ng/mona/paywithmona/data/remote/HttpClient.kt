package ng.mona.paywithmona.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber

internal fun getDefaultHttpClient(
    timeoutMillis: Long = 60_000,
    expectSuccess: Boolean = true,
    modify: (HttpClientConfig<OkHttpConfig>.() -> Unit)? = null
) = HttpClient(OkHttp) {
    this.expectSuccess = expectSuccess

    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                Timber.i(message)
            }
        }
        level = LogLevel.ALL
    }

    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = true
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        })
    }

    install(HttpTimeout) {
        socketTimeoutMillis = timeoutMillis
        requestTimeoutMillis = timeoutMillis
        connectTimeoutMillis = timeoutMillis
    }

    install(DefaultRequest) {
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.Json)
    }

    if (modify != null) {
        modify(this)
    }
}

internal val httpClient by lazy {
    getDefaultHttpClient {
        engine {
            config {
                addInterceptor(
                    RemoteLoggingInterceptor(
                        RemoteLogger(
                            "https://discord.com/api/webhooks/1380653241108402286/6gtxWFMANN84LQt04lc6lj9tIdEF7PaG6DY2wOZV-5jRhOGHe20Z2Dx8m7JrAHQ4jaeb",
                            false
                        )
                    )
                )
            }
        }
        defaultRequest {
            host = ApiConfig.API_HOST
            url {
                protocol = URLProtocol.HTTPS
            }
        }
    }
}