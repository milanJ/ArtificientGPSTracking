package android.template.core.util

import android.icu.text.DecimalFormat
import android.icu.text.NumberFormat
import java.util.Locale

fun formatKilometers(
    distanceInMeters: Int,
    locale: Locale
): String {
    val numberFormat = NumberFormat.getNumberInstance(locale)
    if (numberFormat is DecimalFormat) {
        val decimalFormat: DecimalFormat = numberFormat
        // Adjust pattern based on desired precision for kilometers
        if (distanceInMeters < 1000) { // Less than 1 km, show meters
            return java.lang.String.format(locale, "%d m", distanceInMeters.toInt())
        } else {
            decimalFormat.applyPattern("#.#") // One decimal place for kilometers
            val formattedValue: String? = decimalFormat.format(distanceInMeters / 1000.0)
            return java.lang.String.format(locale, "%s km", formattedValue)
        }
    }
    return distanceInMeters.toString() // Fallback
}
