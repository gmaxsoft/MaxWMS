package pl.maxwms.domain.usecase

import pl.maxwms.domain.model.SyncOutcome
import pl.maxwms.domain.repository.CatalogSync
import javax.inject.Inject

class SyncCatalogUseCase @Inject constructor(
    private val catalogSync: CatalogSync,
) {
    suspend operator fun invoke(): SyncOutcome = catalogSync.sync()
}
