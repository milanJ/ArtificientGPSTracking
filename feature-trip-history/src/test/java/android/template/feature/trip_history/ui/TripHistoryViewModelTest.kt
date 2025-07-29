package android.template.feature.trip_history.ui

import android.template.core.data.TripModel
import android.template.core.data.TripsRepository
import android.template.core.data.WaypointModel
import android.template.core.formaters.DateTimeFormatter
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit test for the [TripHistoryViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TripHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState starts with Loading`() = runTest {
        val repository = object : TripsRepository {

            override val trips: Flow<List<TripModel>>
                get() = flow { emit(emptyList()) }

            override suspend fun createTrip(
                timestamp: Long
            ): TripModel = throw NotImplementedError()

            override suspend fun updateTrip(
                trip: TripModel
            ) = throw NotImplementedError()

            override suspend fun addWaypoint(
                waypoint: WaypointModel
            ) = throw NotImplementedError()

            override suspend fun getWaypoints(
                tripId: Long
            ): List<WaypointModel> = throw NotImplementedError()
        }

        val viewModel = TripHistoryViewModel(
            repository,
            object : DateTimeFormatter {

                override fun formatDateTime(
                    date: Date
                ): String = "mock-date-time"

                override fun formatElapsedTime(
                    milliseconds: Long
                ): String = "mock-elapsed-time"
            },
            testDispatcher
        )
        assertEquals(viewModel.uiState.first(), TripHistoryUiState.Loading)
    }

    @Test
    fun `uiState emits Error when repository throws`() = runTest {
        val repository = object : TripsRepository {

            override val trips: Flow<List<TripModel>>
                get() = flow { throw Exception("Something went wrong.") }

            override suspend fun createTrip(
                timestamp: Long
            ): TripModel = throw NotImplementedError()

            override suspend fun updateTrip(
                trip: TripModel
            ) = throw NotImplementedError()

            override suspend fun addWaypoint(
                waypoint: WaypointModel
            ) = throw NotImplementedError()

            override suspend fun getWaypoints(
                tripId: Long
            ): List<WaypointModel> = throw NotImplementedError()
        }

        val viewModel = TripHistoryViewModel(
            repository,
            object : DateTimeFormatter {

                override fun formatDateTime(
                    date: Date
                ): String = "mock-date-time"

                override fun formatElapsedTime(
                    milliseconds: Long
                ): String = "mock-elapsed-time"
            },
            testDispatcher
        )

        val state = viewModel.uiState.first { it is TripHistoryUiState.Error }
        state as TripHistoryUiState.Error

        assertEquals(state.throwable.message, "Something went wrong.")
    }

    @Test
    fun `uiState emits Success when trips are returned`() = runTest {
        val repository = object : TripsRepository {

            override val trips: Flow<List<TripModel>>
                get() = flow {
                    emit(
                        listOf(
                            TripModel(id = 1, startDate = Date(0), duration = 1000, distance = 1),
                            TripModel(id = 2, startDate = Date(10000), duration = 5000, distance = 10),
                            TripModel(id = 3, startDate = Date(100000), duration = 15000, distance = 100),
                            TripModel(id = 4, startDate = Date(1000000), duration = 77000, distance = 25300)
                        )
                    )
                }

            override suspend fun createTrip(
                timestamp: Long
            ): TripModel = throw NotImplementedError()

            override suspend fun updateTrip(
                trip: TripModel
            ) = throw NotImplementedError()

            override suspend fun addWaypoint(
                waypoint: WaypointModel
            ) = throw NotImplementedError()

            override suspend fun getWaypoints(
                tripId: Long
            ): List<WaypointModel> = throw NotImplementedError()
        }

        val viewModel = TripHistoryViewModel(
            repository,
            object : DateTimeFormatter {

                override fun formatDateTime(
                    date: Date
                ): String = "mock-date-time"

                override fun formatElapsedTime(
                    milliseconds: Long
                ): String = (milliseconds / 1000L).toString()
            },
            testDispatcher
        )

        val state = viewModel.uiState.first { it is TripHistoryUiState.Success }
        state as TripHistoryUiState.Success

        assertEquals(4, state.data.size)
        assertEquals(1, state.data[0].id)
        assertEquals("mock-date-time", state.data[0].startTimeAndDate)
        assertEquals("1", state.data[0].duration)
        assertEquals("1", state.data[0].distance)
    }
}
