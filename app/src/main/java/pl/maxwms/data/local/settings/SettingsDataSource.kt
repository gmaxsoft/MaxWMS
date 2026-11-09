package pl.maxwms.data.local.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.maxwms.domain.model.SyncFailure
import pl.maxwms.domain.model.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "maxwms_settings")

data class StoredSettings(
    val baseUrl: String,
    val themeMode: ThemeMode,
    val lastSyncAt: Long?,
    val syncFailure: SyncFailure?,
    val seeded: Boolean,
)

@Singleton
class SettingsDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val settings: Flow<StoredSettings> = context.settingsDataStore.data.map { prefs ->
        val failureName = prefs[Keys.SYNC_FAILURE].orEmpty()
        StoredSettings(
            baseUrl = prefs[Keys.BASE_URL].orEmpty(),
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.THEME] ?: ThemeMode.SYSTEM.name) }
                .getOrDefault(ThemeMode.SYSTEM),
            lastSyncAt = prefs[Keys.LAST_SYNC]?.takeIf { it > 0L },
            syncFailure = failureName.takeIf { it.isNotBlank() }?.let {
                runCatching { SyncFailure.valueOf(it) }.getOrNull()
            },
            seeded = prefs[Keys.SEEDED] == true,
        )
    }

    suspend fun saveBaseUrl(url: String) {
        context.settingsDataStore.edit { it[Keys.BASE_URL] = url }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun markSyncSuccess(at: Long) {
        context.settingsDataStore.edit {
            it[Keys.LAST_SYNC] = at
            it[Keys.SYNC_FAILURE] = ""
        }
    }

    suspend fun markSyncFailure(failure: SyncFailure) {
        context.settingsDataStore.edit { it[Keys.SYNC_FAILURE] = failure.name }
    }

    suspend fun setSeeded() {
        context.settingsDataStore.edit { it[Keys.SEEDED] = true }
    }

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val THEME = stringPreferencesKey("theme_mode")
        val LAST_SYNC = longPreferencesKey("last_sync_at")
        val SYNC_FAILURE = stringPreferencesKey("sync_failure")
        val SEEDED = booleanPreferencesKey("catalog_seeded")
    }
}
