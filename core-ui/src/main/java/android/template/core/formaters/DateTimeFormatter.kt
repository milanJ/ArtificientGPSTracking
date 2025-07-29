package android.template.core.formaters

import java.util.Date

/**
 * We need this interface to inject date and time formats into ViewModels, so that we can mock them during testing.
 */
interface DateTimeFormatter {

    fun formatDateTime(
        date: Date
    ): String

    fun formatElapsedTime(
        milliseconds: Long
    ): String
}
