package pl.maxwms.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import pl.maxwms.data.local.secure.SecureStore
import pl.maxwms.data.local.settings.SettingsDataSource
import pl.maxwms.domain.logic.BaseUrlNormalizer
import pl.maxwms.domain.model.AppSettings
import pl.maxwms.domain.model.SyncFailure
import pl.maxwms.domain.model.ThemeMode
import pl.maxwms.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataSource: SettingsDataSource,
    private val secureStore: SecureStore,
) : SettingsRepository {
    override val settings: Flow<AppSettings> = combine(
        dataSource.settings,
        secureStore.apiKey,
        secureStore.companyName,
    ) { stored, apiKey, company ->
        AppSettings(
            baseUrl = stored.baseUrl,
            apiKey = apiKey,
            themeMode = stored.themeMode,
            lastSyncAt = stored.lastSyncAt,
            syncFailure = stored.syncFailure,
            companyName = company,
            seeded = stored.seeded,
        )
    }

    override suspend fun snapshot(): AppSettings = settings.first()

    override suspend fun saveConnection(baseUrl: String, apiKey: String): Boolean {
        val normalized = BaseUrlNormalizer.normalize(baseUrl) ?: return false
        secureStore.setApiKey(apiKey)
        dataSource.saveBaseUrl(normalized)
        return true
    }

    override suspend fun setTheme(mode: ThemeMode) {
        dataSource.setTheme(mode)
    }

    override suspend fun markSyncSuccess() {
        dataSource.markSyncSuccess(System.currentTimeMillis())
    }

    override suspend fun markSyncFailure(failure: SyncFailure) {
        dataSource.markSyncFailure(failure)
    }

    override suspend fun isSeeded(): Boolean = snapshot().seeded

    override suspend fun setSeeded() {
        dataSource.setSeeded()
    }
}
