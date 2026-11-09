package pl.maxwms.data.seed

import pl.maxwms.data.local.SampleCatalog
import pl.maxwms.data.local.dao.PartDao
import pl.maxwms.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogSeeder @Inject constructor(
    private val partDao: PartDao,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun seedIfNeeded() {
        if (settingsRepository.isSeeded()) return
        if (partDao.count() == 0) {
            partDao.upsertAll(SampleCatalog.entities())
        }
        settingsRepository.setSeeded()
    }
}
