package android.template.feature.tracking.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class DefaultLocationRepository @Inject constructor() : LocationRepository {

    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
    override val trackingState: Flow<TrackingState>
        get() = _trackingState.asStateFlow()

    private val _userLocation = MutableStateFlow<LocationModel?>(null)
    override val userLocation: Flow<LocationModel?>
        get() = _userLocation.asStateFlow()

    override suspend fun setUserLocation(
        value: LocationModel?
    ) {
        _userLocation.value = value
    }

    override suspend fun setTrackingState(
        value: TrackingState
    ) {
        _trackingState.value = value
    }
}
