package app.teslamate.client.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.teslamate.client.data.repo.TeslaMateRepository
import app.teslamate.client.data.settings.SettingsRepository
import app.teslamate.client.data.settings.normalizeBaseUrl
import app.teslamate.client.ui.UserError
import app.teslamate.client.ui.toUserError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsForm(
    val baseUrl: String = "",
    val token: String = "",
    val ready: Boolean = false,
    val saving: Boolean = false,
    val invalidUrl: Boolean = false,
    val error: UserError? = null,
    val connectedCount: Int? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val repository: TeslaMateRepository,
) : ViewModel() {
    private val _form = MutableStateFlow(SettingsForm())
    val form: StateFlow<SettingsForm> = _form.asStateFlow()

    init {
        viewModelScope.launch {
            var hadSavedSecrets = false
            settingsRepository.settings.collect { saved ->
                val hasSavedSecrets = saved.baseUrl.isNotEmpty() || saved.apiToken.isNotEmpty()
                _form.update { current ->
                    when {
                        !current.ready -> current.copy(
                            baseUrl = saved.baseUrl,
                            token = saved.apiToken,
                            ready = true,
                        )
                        hadSavedSecrets && !hasSavedSecrets -> SettingsForm(ready = true)
                        else -> current
                    }
                }
                hadSavedSecrets = hasSavedSecrets
            }
        }
    }

    fun onBaseUrlChange(value: String) {
        _form.update { it.copy(baseUrl = value, invalidUrl = false, connectedCount = null) }
    }

    fun onTokenChange(value: String) {
        _form.update { it.copy(token = value, connectedCount = null) }
    }

    fun save() {
        val current = _form.value
        val normalized = normalizeBaseUrl(current.baseUrl)
        if (normalized == null) {
            _form.update { it.copy(invalidUrl = true, error = null, connectedCount = null) }
            return
        }
        viewModelScope.launch {
            _form.update {
                it.copy(
                    saving = true,
                    invalidUrl = false,
                    error = null,
                    connectedCount = null,
                    baseUrl = normalized,
                )
            }
            settingsRepository.save(normalized, current.token.trim())
            try {
                val cars = repository.getCars()
                _form.update { it.copy(saving = false, connectedCount = cars.size, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _form.update { it.copy(saving = false, error = e.toUserError()) }
            }
        }
    }

    fun clear() {
        viewModelScope.launch {
            settingsRepository.clear()
            _form.update {
                SettingsForm(ready = true)
            }
        }
    }
}
