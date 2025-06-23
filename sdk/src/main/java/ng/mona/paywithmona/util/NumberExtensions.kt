package ng.mona.paywithmona.util

import java.text.NumberFormat
import java.util.Locale

internal fun Number.format(): String {
    val corrected = this.toFloat() / 100
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "NG"))

    // Set minimum fraction digits to 0 for whole numbers, 2 for decimal values
    formatter.minimumFractionDigits = if (corrected % 1 == 0f) 0 else 2

    return formatter.format(corrected)
}