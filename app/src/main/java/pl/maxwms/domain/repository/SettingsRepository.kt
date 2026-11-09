package pl.maxwms.domain.repository

import kotlinx.coroutines.flow.Flow
import pl.maxwms.domain.model.AppSettings
import pl.maxwms.domain.model.SyncFailure
import pl.maxwms.domain.model.SyncOutcome
import pl.maxwms.domain.model.ThemeMode

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun snapshot(): AppSettings

    suspend fun saveConnection(baseUrl: String, apiKey: String): Boolean

    suspend fun setTheme(mode: ThemeMode)

    suspend fun markSyncSuccess()

    suspend fun markSyncFailure(failure: SyncFailure)

    suspend fun isSeeded(): Boolean

    suspend fun setSeeded()
}

interface CatalogSync {
    suspend fun sync(): SyncOutcome

    suspend fun pushPart(partId: String): SyncOutcome
}
