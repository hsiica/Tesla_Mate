package app.teslamate.client.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

private val Context.settingsDataStore by preferencesDataStore(name = "teslamate_settings")

data class UserSettings(
    val baseUrl: String = "",
    val apiToken: String = "",
    val selectedCarId: Int? = null,
)

data class ApiEndpoint(
    val baseUrl: String,
    val token: String,
) {
    companion object {
        val Empty = ApiEndpoint(baseUrl = "", token = "")
    }
}

@Singleton
class ApiConfigState @Inject constructor() {
    @Volatile
    var endpoint: ApiEndpoint = ApiEndpoint.Empty
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val apiConfigState: ApiConfigState,
) {
    private val dataStore = context.settingsDataStore

    val settings: Flow<UserSettings> = dataStore.data
        .map { prefs ->
            UserSettings(
                baseUrl = prefs[Keys.BASE_URL].orEmpty(),
                apiToken = prefs[Keys.API_TOKEN].orEmpty(),
                selectedCarId = prefs[Keys.CAR_ID],
            )
        }
        .onEach { apiConfigState.endpoint = ApiEndpoint(it.baseUrl, it.apiToken) }

    suspend fun snapshot(): UserSettings = settings.first()

    suspend fun save(baseUrl: String, apiToken: String) {
        dataStore.edit { prefs ->
            prefs[Keys.BASE_URL] = baseUrl
            prefs[Keys.API_TOKEN] = apiToken
        }
    }

    suspend fun setSelectedCar(id: Int) {
        dataStore.edit { prefs -> prefs[Keys.CAR_ID] = id }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val API_TOKEN = stringPreferencesKey("api_token")
        val CAR_ID = intPreferencesKey("selected_car_id")
    }
}
