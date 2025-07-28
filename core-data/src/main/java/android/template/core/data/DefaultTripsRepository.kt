package android.template.core.data

import android.template.core.database.Trip
import android.template.core.database.TripDao
import android.template.core.database.Waypoint
import android.template.core.database.WaypointDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

/**
 * Concrete implementation of [TripsRepository].
 */
class DefaultTripsRepository @Inject constructor(
    private val tripDao: TripDao,
    private val waypointDao: WaypointDao,
    private val ioDispatcher: CoroutineDispatcher
) : TripsRepository {

    override val trips: Flow<List<TripModel>> = tripDao.getTrips()
        .map { items ->
            items.map {
                it.toTripModel()
            }
        }
        .flowOn(ioDispatcher)

    override suspend fun createTrip(
        timestamp: Long
    ): TripModel = withContext(ioDispatcher) {
        val trip = Trip(
            startTimestamp = timestamp,
            duration = 0,
            distance = 0
        )
        val tripId = tripDao.insertTrip(trip)
        return@withContext TripModel(
            id = tripId,
            startDate = Date(timestamp),
            duration = 0,
            distance = 0
        )
    }

    override suspend fun updateTrip(
        trip: TripModel
    ) = withContext(ioDispatcher) {
        tripDao.updateTrip(trip.toTrip())
    }

    override suspend fun addWaypoint(
        waypoint: WaypointModel
    ) = withContext(ioDispatcher) {
        waypointDao.insertWaypoint(
            Waypoint(
                timestamp = waypoint.timestamp,
                speed = waypoint.speed,
                latitude = waypoint.latitude,
                longitude = waypoint.longitude,
                tripId = waypoint.tripId,
            )
        )
    }
}

private fun Trip.toTripModel() = TripModel(
    id = id,
    startDate = Date(startTimestamp),
    duration = duration,
    distance = distance
)

private fun TripModel.toTrip() = Trip(
    id = id,
    startTimestamp = startDate.time,
    duration = duration,
    distance = distance
)
