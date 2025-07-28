package android.template.feature.tracking.data

import kotlinx.coroutines.flow.Flow

interface LocationRepository {

    val trackingState: Flow<TrackingState>

    val userLocation: Flow<LocationModel?>

    suspend fun setUserLocation(
        value: LocationModel?
    )

    suspend fun setTrackingState(
        value: TrackingState
    )
}
