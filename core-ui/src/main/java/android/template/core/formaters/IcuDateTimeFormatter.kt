package android.template.core.formaters

import android.icu.text.DateFormat
import android.text.format.DateUtils
import java.util.Date
import java.util.Locale

/**
 * Concrete implementation of [DateTimeFormatter].
 */
class IcuDateTimeFormatter : DateTimeFormatter {

    private val formatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.getDefault())

    override fun formatDateTime(
        date: Date
    ): String = formatter.format(date)

    override fun formatElapsedTime(
        milliseconds: Long
    ): String = DateUtils.formatElapsedTime(milliseconds / 1000L)
}
