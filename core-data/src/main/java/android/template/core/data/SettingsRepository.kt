package android.template.core.data

import kotlinx.coroutines.flow.Flow

/**
 * Interface to the application settings.
 */
interface SettingsRepository {

    val locationInterval: Flow<Int>

    val backgroundTracking: Flow<Boolean>

    suspend fun setLocationInterval(
        value: Int
    )

    suspend fun setBackgroundTracking(
        enabled: Boolean
    )

    companion object {
        const val DEFAULT_BACKGROUND_TRACKING = false
        const val DEFAULT_LOCATION_INTERVAL = 5
    }
}
