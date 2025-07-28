package android.template.core.data

/**
 * A model class representing a single waypoint of a trip that the user has taken. Exposes waypoint data to the upper layers of the app.
 */
data class WaypointModel(
    val timestamp: Long,
    val speed: Float,
    val latitude: Double,
    val longitude: Double,
    val tripId: Long
)
