package android.template.feature.tracking.ui

import android.template.core.util.formatKilometers
import android.template.feature.tracking.data.LocationRepository
import android.template.feature.tracking.data.TrackingState
import android.text.format.DateUtils
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackingUIState.INITIAL)
    val uiState: StateFlow<TrackingUIState> = _uiState.asStateFlow()

    init {
        // Combine the two flows from the repositories
        viewModelScope.launch {
            combine(locationRepository.trackingState, locationRepository.userLocation) { trackingState, userLocation ->
                Log.d(TAG, "uiState() :: trackingState = $trackingState, userLocation = $userLocation")
                val isStartTrackingVisible = trackingState == TrackingState.STOPPED
                val isPauseTrackingVisible = trackingState == TrackingState.TRACKING
                val isResumeTrackingVisible = trackingState == TrackingState.PAUSED
                val isStopTrackingVisible = trackingState != TrackingState.STOPPED
                val isExtraInfoVisible = trackingState != TrackingState.STOPPED
                        && trackingState != TrackingState.PAUSED
                        && userLocation != null

                val formatedSpeed = if (userLocation == null) {
                    "0 m/s"
                } else {
                    "${String.format(Locale.getDefault(), "%.2f", userLocation.speed)} m/s"
                }
                val formatedDistance = if (userLocation == null) {
                    "0 m"
                } else {
                    formatKilometers(userLocation.distanceTraveled, Locale.getDefault())
                }
                val formatedElapsedTime = if (userLocation == null) {
                    "0 s"
                } else {
                    DateUtils.formatElapsedTime(userLocation.elapsedTime / 1000L)
                }

                TrackingUIState(
                    latitude = userLocation?.latitude,
                    longitude = userLocation?.longitude,
                    isStartTrackingVisible = isStartTrackingVisible,
                    isPauseTrackingVisible = isPauseTrackingVisible,
                    isResumeTrackingVisible = isResumeTrackingVisible,
                    isStopTrackingVisible = isStopTrackingVisible,
                    isExtraInfoVisible = isExtraInfoVisible,
                    speed = formatedSpeed,
                    distanceTraveled = formatedDistance,
                    elapsedTime = formatedElapsedTime
                )
            }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    TrackingUIState.INITIAL
                )
                .collect { combinedData ->
                    _uiState.value = combinedData
                }
        }
    }

    fun startTracking() {
        viewModelScope.launch {
            locationRepository.setTrackingState(TrackingState.TRACKING)
        }
    }

    fun resumeTracking() {
        viewModelScope.launch {
            locationRepository.setTrackingState(TrackingState.TRACKING)
        }
    }

    fun pauseTracking() {
        viewModelScope.launch {
            locationRepository.setTrackingState(TrackingState.PAUSED)
        }
    }

    fun stopTracking() {
        viewModelScope.launch {
            locationRepository.setTrackingState(TrackingState.STOPPED)
        }
    }
}

data class TrackingUIState(
    val latitude: Double?,
    val longitude: Double?,
    val isStartTrackingVisible: Boolean,
    val isPauseTrackingVisible: Boolean,
    val isResumeTrackingVisible: Boolean,
    val isStopTrackingVisible: Boolean,
    val isExtraInfoVisible: Boolean,
    val speed: String,
    val distanceTraveled: String,
    val elapsedTime: String
) {

    val hasLocation: Boolean = latitude != null && longitude != null

    companion object {

        val INITIAL = TrackingUIState(
            latitude = null,
            longitude = null,
            isStartTrackingVisible = true,
            isPauseTrackingVisible = false,
            isResumeTrackingVisible = false,
            isStopTrackingVisible = false,
            isExtraInfoVisible = false,
            speed = "",
            distanceTraveled = "",
            elapsedTime = ""
        )
    }
}



private const val TAG = "TrackingViewModel"
