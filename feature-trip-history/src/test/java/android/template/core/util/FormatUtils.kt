package android.template.core.util

import java.util.Locale

/**
 * We need this function mocked for [TripHistoryViewModelTest].
 */
fun formatKilometers(
    distanceInMeters: Int,
    locale: Locale
): String = distanceInMeters.toString()
