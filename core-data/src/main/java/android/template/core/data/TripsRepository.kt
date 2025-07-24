package android.template.core.data

import kotlinx.coroutines.flow.Flow

/**
 * Interface to the trips and waypoints data.
 */
interface TripsRepository {

    val trips: Flow<List<TripModel>>

    suspend fun createTrip(
        timestamp: Long
    ): TripModel

    suspend fun addWaypoint(
        waypoint: WaypointModel
    )
}
