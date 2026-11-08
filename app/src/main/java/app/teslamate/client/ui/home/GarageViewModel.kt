package app.teslamate.client.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.teslamate.client.data.repo.TeslaMateRepository
import app.teslamate.client.data.settings.SettingsRepository
import app.teslamate.client.domain.Car
import app.teslamate.client.ui.UserError
import app.teslamate.client.ui.toUserError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GarageState {
    data object Loading : GarageState
    data class Ready(val cars: List<Car>, val selectedId: Int) : GarageState
    data object Empty : GarageState
    data class Error(val error: UserError) : GarageState
}

@HiltViewModel
class GarageViewModel @Inject constructor(
    private val repository: TeslaMateRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<GarageState>(GarageState.Loading)
    val state: StateFlow<GarageState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private var job: Job? = null

    fun refresh(initial: Boolean = false) {
        job?.cancel()
        job = viewModelScope.launch {
            if (initial || _state.value !is GarageState.Ready) {
                _state.value = GarageState.Loading
            } else {
                _refreshing.value = true
            }
            try {
                val settings = settingsRepository.snapshot()
                if (settings.baseUrl.isBlank()) {
                    _state.value = GarageState.Error(UserError.NotConfigured)
                    return@launch
                }
                val cars = repository.getCars()
                if (cars.isEmpty()) {
                    _state.value = GarageState.Empty
                    return@launch
                }
                val selected = settings.selectedCarId?.takeIf { id -> cars.any { it.id == id } }
                    ?: cars.first().id
                if (selected != settings.selectedCarId) {
                    settingsRepository.setSelectedCar(selected)
                }
                _state.value = GarageState.Ready(cars, selected)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = GarageState.Error(e.toUserError())
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun selectCar(id: Int) {
        val current = _state.value
        if (current is GarageState.Ready && current.selectedId != id) {
            _state.value = current.copy(selectedId = id)
            viewModelScope.launch { settingsRepository.setSelectedCar(id) }
        }
    }
}
