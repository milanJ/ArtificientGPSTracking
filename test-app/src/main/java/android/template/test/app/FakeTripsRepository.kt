package android.template.test.app

import android.template.core.data.TripModel
import android.template.core.data.TripsRepository
import android.template.core.data.WaypointModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Date
import javax.inject.Inject

class FakeTripsRepository @Inject constructor() : TripsRepository {

    private val fakeWaypoints = mutableListOf<WaypointModel>()
    private val fakeTrips = mutableListOf<TripModel>()

    override val trips: Flow<List<TripModel>> = flowOf(fakeTrips)

    override suspend fun createTrip(
        timestamp: Long
    ): TripModel {
        val trip = TripModel(
            id = fakeTrips.size.toLong(),
            startDate = Date(timestamp),
            duration = 0,
            distance = 0
        )
        fakeTrips.add(trip)
        return trip
    }

    override suspend fun updateTrip(
        trip: TripModel
    ) {
        val index = fakeTrips.indexOfFirst { it.id == trip.id }
        if (index != -1) {
            fakeTrips[index] = trip
        }
    }

    override suspend fun addWaypoint(
        waypoint: WaypointModel
    ) {
        fakeWaypoints.add(waypoint)
    }

    override suspend fun getWaypoints(
        tripId: Long
    ): List<WaypointModel> = fakeWaypoints.filter { it.tripId == tripId }
}
