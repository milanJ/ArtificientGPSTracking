package android.template.feature.trip_history.ui

import android.icu.text.DateFormat
import android.template.core.data.TripModel
import android.template.core.data.TripsRepository
import android.template.core.util.formatKilometers
import android.text.format.DateUtils
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TripHistoryViewModel @Inject constructor(
    tripsRepository: TripsRepository
) : ViewModel() {

    val uiState: StateFlow<TripHistoryUiState> = tripsRepository
        .trips
        .map<List<TripModel>, TripHistoryUiState> {
            val uiModels = it.map { genre ->
                genre.toTripUiModel(dateFormat)
            }
            TripHistoryUiState.Success(data = uiModels)
        }
        .catch {
            emit(TripHistoryUiState.Error(it))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TripHistoryUiState.Loading)

    private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.getDefault())
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
data class TripUiModel(
    val id: Long,
    val startTimeAndDate: String,
    val duration: String,
    val distance: String
)

private fun TripModel.toTripUiModel(
    dateFormat: DateFormat
) = TripUiModel(
    id = id,
    startTimeAndDate = dateFormat.format(startDate),
    duration = DateUtils.formatElapsedTime(duration / 1000L),
    distance = formatKilometers(distance, Locale.getDefault()),
)
