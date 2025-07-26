package android.template.feature.mymodel.ui

import android.template.core.data.TripsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val tripsRepository: TripsRepository
) : ViewModel() {

//    val uiState: StateFlow<TrackingUiState> = tripsRepository
//        .myModels.map<List<String>, TrackingUiState> { Success(data = it) }
//        .catch { emit(Error(it)) }
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)
//
//    fun addMyModel(name: String) {
//        viewModelScope.launch {
//            myModelRepository.add(name)
//        }
//    }
}

sealed interface TrackingUiState {
    object Loading : TrackingUiState

    data class Error(
        val throwable: Throwable
    ) : TrackingUiState

    data class Success(
        val data: List<String>
    ) : TrackingUiState
}
