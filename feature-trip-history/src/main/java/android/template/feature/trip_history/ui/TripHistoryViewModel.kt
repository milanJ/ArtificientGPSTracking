package android.template.feature.trip_history.ui

import android.template.core.data.TripModel
import android.template.core.data.TripsRepository
import android.template.core.data.WaypointModel
import android.template.core.formaters.DateTimeFormatter
import android.template.core.util.formatKilometers
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TripHistoryViewModel @Inject constructor(
    private val tripsRepository: TripsRepository,
    private val dateFormatter: DateTimeFormatter,
    private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    val uiState: StateFlow<TripHistoryUiState> = tripsRepository
        .trips
        .map<List<TripModel>, TripHistoryUiState> {
            val uiModels = it.map { genre ->
                genre.toTripUiModel(dateFormatter)
            }
            TripHistoryUiState.Success(data = uiModels)
        }
        .catch {
            emit(TripHistoryUiState.Error(it))
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            TripHistoryUiState.Loading
        )

    private val _csvExport = MutableStateFlow<CsvExportModel?>(null)
    val csvExport: StateFlow<CsvExportModel?> = _csvExport.asStateFlow()

    fun tripClicked(
        trip: TripUiModel
    ) {
        // Export the trip data into CSV. And let the user share it via a the Share Intent.
        // This is a naive approach that will work small trips. For larger trips, we would have to move exporting to a Service,
        // write waypoints into an actual file, and then share the file.
        viewModelScope.launch {
            val waypoints = tripsRepository.getWaypoints(trip.id)
            val waypointsCsv = withContext(ioDispatcher) {
                waypoints.toWaypointsCsv()
            }
            val tripName = "Trip ${trip.startTimeAndDate}.csv"
            _csvExport.value = CsvExportModel(tripName, waypointsCsv)
        }
    }
}

sealed interface TripHistoryUiState {

    object Loading : TripHistoryUiState

    data class Error(
        val throwable: Throwable
    ) : TripHistoryUiState

    data class Success(
        val data: List<TripUiModel>
    ) : TripHistoryUiState
}

@Immutable
data class CsvExportModel(
    val tripName: String,
    val waypointsCsv: String
)

@Immutable
data class TripUiModel(
    val id: Long,
    val startTimeAndDate: String,
    val duration: String,
    val distance: String
)

private fun TripModel.toTripUiModel(
    dateFormatter: DateTimeFormatter
) = TripUiModel(
    id = id,
    startTimeAndDate = dateFormatter.formatDateTime(startDate),
    duration = dateFormatter.formatElapsedTime(duration),
    distance = formatKilometers(distance, Locale.getDefault()),
)

private fun List<WaypointModel>.toWaypointsCsv(): String {
    val csvBuilder = StringBuilder()

    // Append headers.
    csvBuilder.append("Latitude,Longitude,Timestamp,Speed\n")

    // Append data:
    for (item in this) {
        csvBuilder.append("${item.latitude},${item.longitude},${item.timestamp},${item.speed}\n")
    }

    return csvBuilder.toString()
}
