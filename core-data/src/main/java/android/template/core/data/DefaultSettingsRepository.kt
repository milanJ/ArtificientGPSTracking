package android.template.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Concrete implementation of [SettingsRepository].
 */
class DefaultSettingsRepository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val ioDispatcher: CoroutineDispatcher
) : SettingsRepository {

    private val backgroundTrackingPreferenceKey = booleanPreferencesKey("background_tracking")
    private val locationIntervalPreferenceKey = intPreferencesKey("location_interval")

    private val Context.dataStore by preferencesDataStore(name = "settings")

    override val locationInterval: Flow<Int>
        get() = applicationContext.dataStore
            .data
            .map { prefs -> prefs[locationIntervalPreferenceKey] ?: SettingsRepository.DEFAULT_LOCATION_INTERVAL }
            .flowOn(ioDispatcher)

    override val backgroundTracking: Flow<Boolean>
        get() = applicationContext.dataStore
            .data
            .map { prefs -> prefs[backgroundTrackingPreferenceKey] ?: SettingsRepository.DEFAULT_BACKGROUND_TRACKING }
            .flowOn(ioDispatcher)

    override suspend fun setLocationInterval(
        value: Int
    ) = with(ioDispatcher) {
        applicationContext.dataStore.edit { it[locationIntervalPreferenceKey] = value }
        return@with
    }

    override suspend fun setBackgroundTracking(
        enabled: Boolean
    ) = with(ioDispatcher) {
        applicationContext.dataStore.edit { it[backgroundTrackingPreferenceKey] = enabled }
        return@with
    }
}
