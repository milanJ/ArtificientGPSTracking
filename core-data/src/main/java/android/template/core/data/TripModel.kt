package android.template.core.data

import java.util.Date

/**
 * A model class representing a single trip the user has taken. Exposes trip data to the upper layers of the app.
 */
data class TripModel(
    val id: Long,
    val startDate: Date,
    val duration: Long,
    val distance: Int
)
