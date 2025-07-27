package android.template.feature.settings.ui

import android.template.core.data.SettingsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val locationInterval: StateFlow<Int> = settingsRepository
        .locationInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_LOCATION_INTERVAL)

    var isBackgroundTrackingEnabled: StateFlow<Boolean> = settingsRepository
        .backgroundTracking
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_BACKGROUND_TRACKING)

    fun updateInterval(
        newInterval: Int
    ) {
        viewModelScope.launch {
            settingsRepository.setLocationInterval(newInterval)
        }
    }

    fun toggleBackgroundTracking(
        enabled: Boolean
    ) {
        viewModelScope.launch {
            settingsRepository.setBackgroundTracking(enabled)
        }
    }
}
