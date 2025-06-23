package ng.mona.paywithmona.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber

internal class RemoteLoggingInterceptor(
    private val logger: RemoteLogger
) : Interceptor {
    val scope = CoroutineScope(Dispatchers.IO)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        try {
            scope.launch {
                // Log the request
                logger.logRequest(
                    method = request.method,
                    uri = request.url.toString(),
                    headers = request.headers.toMultimap()
                        .mapValues { it.value.joinToString(", ") },
                    data = request.body?.toString()?.let { mapOf("body" to it) }
                )
            }

            val response = chain.proceed(request)

            val responseBody = response.body?.string() ?: "No Response Body"
            scope.launch {
                // Log the response
                logger.logResponse(
                    statusCode = response.code,
                    statusMessage = response.message,
                    uri = request.url.toString(),
                    responseBody = responseBody
                )
            }

            // Return the response with the consumed body
            return response.newBuilder()
                .body(responseBody.toResponseBody(response.body?.contentType()))
                .build()
        } catch (e: Exception) {
            scope.launch {
                // Log the error
                logger.logError(
                    errorType = "Network Error",
                    errorMessage = e.message ?: "Unknown error",
                    uri = request.url.toString()
                )
            }
            Timber.e(e, "Error during API call")
            throw e
        }
    }
}