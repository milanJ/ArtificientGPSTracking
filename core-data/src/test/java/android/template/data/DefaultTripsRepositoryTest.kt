package android.template.data

import android.template.core.data.DefaultTripsRepository
import android.template.core.database.Trip
import android.template.core.database.TripDao
import android.template.core.database.Waypoint
import android.template.core.database.WaypointDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [DefaultTripsRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultTripsRepositoryTest {

    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
    }

    @Test
    fun waypoints_newItemSaved_itemIsReturned() = runTest {
        val repository = DefaultTripsRepository(
            FakeTripDao(),
            FakeWaypointDao(),
            testDispatcher
        )

        val createdTrip = repository.createTrip(100L)

        val trips = repository.trips.first()
        assertEquals(trips.size, 1)
        assertEquals(trips.first().id, createdTrip.id)
        assertEquals(trips.first().startDate.time, 100L)
        assertEquals(trips.first().duration, 0L)
        assertEquals(trips.first().distance, 0.0, 0.1)
    }
}

private class FakeTripDao : TripDao {

    private val data = mutableListOf<Trip>()

    override fun getTrips(): Flow<List<Trip>> = flow {
        emit(data)
    }

    override suspend fun updateTrip(
        item: Trip
    ) {
        data.remove(item)
        data.add(item)
    }

    override suspend fun insertTrip(
        item: Trip
    ): Int {
        val trip = item.copy(id = data.size)
        data.add(trip)
        return trip.id
    }
}

private class FakeWaypointDao : WaypointDao {

    private val data = mutableListOf<Waypoint>()

    override fun getWaypoints(
        tripId: Int
    ): Flow<List<Waypoint>> = flow {
        emit(data)
    }

    override suspend fun insertWaypoint(item: Waypoint) {
        data.add(0, item)
    }
}
