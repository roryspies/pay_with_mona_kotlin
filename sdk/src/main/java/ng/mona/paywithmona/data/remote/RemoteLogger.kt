package ng.mona.paywithmona.data.remote

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import ng.mona.paywithmona.data.serializer.SdkJson
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * A client for logging API requests, responses, and errors to a remote webhook.
 * Uses Ktor for HTTP communication.
 *
 * @param webhookUrl The URL of the remote logging webhook.
 * @param enabled A boolean indicating whether logging is enabled. Defaults to true.
 */
internal class RemoteLogger(
    private val webhookUrl: String,
    private val enabled: Boolean = true
) {
    private val client = getDefaultHttpClient(10_000, expectSuccess = true)

    /**
     * Logs an API request to the remote webhook.
     *
     * @param method The HTTP method (e.g., "GET", "POST").
     * @param uri The URI of the request.
     * @param headers Optional request headers.
     * @param data Optional request body data.
     */
    suspend fun logRequest(
        method: String,
        uri: String, // Changed to String for simpler URI handling
        headers: Map<String, String>? = null,
        data: Map<String, Any?>? = null
    ) {
        if (!enabled) return

        sendToRemoteLogger(
            mapOf(
                "content" to "🚀 API REQUEST\n" +
                        "```\n" +
                        "Method: $method\n" +
                        "URL: $uri\n" +
                        "Headers: ${formatHeaders(headers ?: emptyMap())}\n" +
                        "Data: ${formatData(data)}\n" +
                        "```\n" +
                        "📆 Time: `${toReadableDateAndTime(LocalDateTime.now())}`"
            )
        )
    }

    /**
     * Logs an API response to the remote webhook.
     *
     * @param statusCode The HTTP status code of the response.
     * @param statusMessage Optional status message from the response.
     * @param uri The URI of the request.
     * @param responseBody The response body as a String.
     */
    suspend fun logResponse(
        statusCode: Int,
        statusMessage: String?,
        uri: String,
        responseBody: String
    ) {
        if (!enabled) return

        sendToRemoteLogger(
            mapOf(
                "content" to "✅ API RESPONSE\n" +
                        "```\n" +
                        "Status: $statusCode ${statusMessage ?: ""}\n" +
                        "URL: $uri\n" +
                        "Response: ${shorten(responseBody)}\n" +
                        "```\n" +
                        "📆 Time: `${toReadableDateAndTime(LocalDateTime.now())}`"
            )
        )
    }

    /**
     * Logs error information to the remote webhook.
     *
     * @param errorType The type of error (e.g., "Network Error", "API Error").
     * @param errorMessage The error message.
     * @param uri The URI where the error occurred.
     * @param statusCode Optional HTTP status code associated with the error.
     * @param responseBody Optional response body associated with the error.
     */
    suspend fun logError(
        errorType: String,
        errorMessage: String,
        uri: String, // Changed to String
        statusCode: Int? = null,
        responseBody: String? = null
    ) {
        if (!enabled) return

        sendToRemoteLogger(
            mapOf(
                "content" to "❌ API ERROR\n" +
                        "```\n" +
                        "Type: $errorType\n" +
                        "Message: $errorMessage\n" +
                        "URL: $uri\n" +
                        "Status Code: ${statusCode ?: "N/A"}\n" +
                        "Response: ${responseBody?.let { shorten(it) } ?: "N/A"}\n" +
                        "```\n" +
                        "📆 Time: `${toReadableDateAndTime(LocalDateTime.now())}`"
            )
        )
    }

    /**
     * Reports crash information to the remote webhook.
     *
     * @param error The error object.
     * @param trace The stack trace associated with the error.
     */
    suspend fun reportCrash(
        error: Throwable, // Changed to Throwable for Kotlin exception handling
        trace: StackTraceElement // Changed to StackTraceElement, or StackTraceElement[] / String for full trace
    ) {
        if (!enabled) return

        sendToRemoteLogger(
            mapOf(
                "content" to "😰 ERROR```${error.toString()}```\n" +
                        "📆⏳ DATE AND TIME:```${toReadableDateAndTime(LocalDateTime.now())}```\n" +
                        "📚STACK TRACE \n${shorten(trace.toString())}" // Pass StackTraceElement.toString()
            )
        )
    }


    /**
     * Sends the given payload to the remote logger webhook.
     *
     * @param payload The map containing the data to send.
     */
    private suspend fun sendToRemoteLogger(payload: Map<String, Any?>) {
        try {
            val response = client.post {
                url(webhookUrl)
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            // Ktor automatically consumes the response body by default,
            // or you can explicitly call response.bodyAsText() if needed.
            // No explicit drain() call needed as Ktor handles it.
            Timber.d("Webhook response status: ${response.status.value}")
        } catch (e: Exception) {
            Timber.e(e, "🔔 ERROR SENDING REMOTE LOGGER MESSAGE ==>> ${e.message}")
        }
    }

    /**
     * Formats headers for display.
     * @param headers The map of headers.
     * @return A formatted string of headers.
     */
    private fun formatHeaders(headers: Map<String, String>): String {
        if (headers.isEmpty()) return "None"
        return headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }
    }

    /**
     * Formats request data for display.
     * @param data The request data.
     * @return A formatted string of the data.
     */
    private fun formatData(data: Any?): String {
        if (data == null) return "None"
        // Use Json.encodeToString to pretty print the data if it's a map or list
        return try {
            shorten(Json.encodeToString(data))
        } catch (_: Exception) {
            // Fallback if data is not serializable as JSON, or if it's already a string
            shorten(data.toString())
        }
    }

    /**
     * Shortens text to prevent message limits.
     * @param text The text to shorten.
     * @param maxLength The maximum length of the text. Defaults to 1000.
     * @return The shortened text.
     */
    private fun shorten(
        text: String,
        maxLength: Int = 1000
    ): String {
        return if (text.length > maxLength) {
            "${text.substring(0, maxLength)}..."
        } else {
            text
        }
    }

    /**
     * Converts a `LocalDateTime` to a readable format.
     * @param dateAndTime The `LocalDateTime` to format.
     * @return A formatted date and time string.
     */
    private fun toReadableDateAndTime(
        dateAndTime: LocalDateTime
    ): String {
        return try {
            dateAndTime.format(DateTimeFormatter.ofPattern("hh:mm a, EEE, dd MMM"))
        } catch (_: DateTimeParseException) {
            dateAndTime.toString() // Fallback to default string representation
        }
    }

    /**
     * Closes the underlying Ktor HttpClient, releasing its resources.
     */
    fun dispose() {
        client.close()
    }
}
