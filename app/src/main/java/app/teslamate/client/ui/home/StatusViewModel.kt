package app.teslamate.client.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.teslamate.client.data.repo.TeslaMateRepository
import app.teslamate.client.data.settings.SettingsRepository
import app.teslamate.client.domain.CarStatus
import app.teslamate.client.ui.UserError
import app.teslamate.client.ui.toUserError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StatusUi(
    val status: CarStatus? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: UserError? = null,
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val repository: TeslaMateRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(StatusUi())
    val ui: StateFlow<StatusUi> = _ui

    private val _toasts = MutableSharedFlow<UserError>(extraBufferCapacity = 1)
    val toasts: SharedFlow<UserError> = _toasts.asSharedFlow()

    val selectedCarId: StateFlow<Int?> = settingsRepository.settings
        .map { it.selectedCarId }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            selectedCarId.collect { id ->
                if (id == null) {
                    _ui.value = StatusUi(loading = true)
                } else {
                    load(id, fromUser = false)
                }
            }
        }
    }

    fun refresh() {
        val id = selectedCarId.value ?: return
        load(id, fromUser = true)
    }

    private fun load(id: Int, fromUser: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val hadContent = _ui.value.status?.carId == id
            _ui.value = _ui.value.copy(
                loading = !hadContent,
                refreshing = hadContent || fromUser,
                error = if (hadContent) null else _ui.value.error,
                status = if (hadContent) _ui.value.status else null,
            )
            try {
                val status = repository.getStatus(id)
                _ui.value = StatusUi(status = status, loading = false, refreshing = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = e.toUserError()
                val current = _ui.value.status
                if (current != null && current.carId == id) {
                    _ui.value = _ui.value.copy(loading = false, refreshing = false)
                    _toasts.tryEmit(error)
                } else {
                    _ui.value = StatusUi(loading = false, error = error)
                }
            }
        }
    }
}
