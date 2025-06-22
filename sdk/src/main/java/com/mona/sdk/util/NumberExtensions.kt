package com.mona.sdk.util

import java.text.NumberFormat
import java.util.Locale

internal fun Number.format(): String {
    return NumberFormat.getCurrencyInstance(Locale("en", "NG")).format(this.toFloat() / 100)
}