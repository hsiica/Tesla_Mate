package app.teslamate.client.ui.drives

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.teslamate.client.data.repo.TeslaMateRepository
import app.teslamate.client.data.settings.SettingsRepository
import app.teslamate.client.domain.DriveDetail
import app.teslamate.client.domain.DriveSummary
import app.teslamate.client.domain.Units
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

data class DriveListUi(
    val items: List<DriveSummary>? = null,
    val units: Units = Units.Metric,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: UserError? = null,
)

@HiltViewModel
class DrivesViewModel @Inject constructor(
    private val repository: TeslaMateRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(DriveListUi())
    val ui: StateFlow<DriveListUi> = _ui

    private val _toasts = MutableSharedFlow<UserError>(extraBufferCapacity = 1)
    val toasts: SharedFlow<UserError> = _toasts.asSharedFlow()

    val selectedCarId: StateFlow<Int?> = settingsRepository.settings
        .map { it.selectedCarId }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var page = 1
    private var loadedCarId: Int? = null
    private var job: Job? = null

    init {
        viewModelScope.launch {
            selectedCarId.collect { id ->
                if (id == null) {
                    _ui.value = DriveListUi()
                } else {
                    reload(id)
                }
            }
        }
    }

    fun refresh() {
        val id = selectedCarId.value ?: return
        reload(id)
    }

    fun loadMore() {
        val id = selectedCarId.value ?: return
        val current = _ui.value
        if (current.loading || current.loadingMore || current.refreshing || current.endReached) return
        job = viewModelScope.launch {
            _ui.value = current.copy(loadingMore = true)
            try {
                val next = page + 1
                val result = repository.getDrives(id, page = next)
                page = next
                _ui.value = current.copy(
                    items = (current.items.orEmpty() + result.drives),
                    units = result.units,
                    loadingMore = false,
                    endReached = result.drives.size < TeslaMateRepository.PAGE_SIZE,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.value = current.copy(loadingMore = false)
                _toasts.tryEmit(e.toUserError())
            }
        }
    }

    private fun reload(carId: Int) {
        job?.cancel()
        job = viewModelScope.launch {
            val sameCar = loadedCarId == carId && _ui.value.items != null
            loadedCarId = carId
            page = 1
            _ui.value = if (sameCar) {
                _ui.value.copy(loading = false, refreshing = true, error = null, endReached = false)
            } else {
                DriveListUi(loading = true)
            }
            try {
                val result = repository.getDrives(carId, page = 1)
                _ui.value = DriveListUi(
                    items = result.drives,
                    units = result.units,
                    loading = false,
                    endReached = result.drives.size < TeslaMateRepository.PAGE_SIZE,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = e.toUserError()
                if (sameCar && _ui.value.items != null) {
                    _ui.value = _ui.value.copy(loading = false, refreshing = false)
                    _toasts.tryEmit(error)
                } else {
                    _ui.value = DriveListUi(loading = false, error = error)
                }
            }
        }
    }
}

@HiltViewModel
class DriveDetailViewModel @Inject constructor(
    private val repository: TeslaMateRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val carId: Int = savedStateHandle.get<Int>("carId") ?: 0
    private val driveId: Int = savedStateHandle.get<Int>("driveId") ?: 0

    private val _detail = MutableStateFlow<DriveDetail?>(null)
    val detail: StateFlow<DriveDetail?> = _detail

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<UserError?>(null)
    val error: StateFlow<UserError?> = _error

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = _detail.value == null
            _error.value = null
            try {
                _detail.value = repository.getDrive(carId, driveId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_detail.value == null) _error.value = e.toUserError()
            } finally {
                _loading.value = false
            }
        }
    }
}
