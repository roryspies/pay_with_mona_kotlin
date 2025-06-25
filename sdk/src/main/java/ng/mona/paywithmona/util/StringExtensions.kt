package ng.mona.paywithmona.util

internal fun String.capitalize(lowercase: Boolean = false): String {
    return if (isNotEmpty()) {
        this[0].uppercaseChar() + when (lowercase) {
            true -> substring(1).lowercase()
            false -> substring(1)
        }
    } else {
        this
    }
}