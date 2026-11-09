package pl.maxwms.domain.usecase

import pl.maxwms.domain.model.UnlockResult
import pl.maxwms.domain.repository.LicenseRepository
import javax.inject.Inject

class UnlockLicenseUseCase @Inject constructor(
    private val repository: LicenseRepository,
) {
    suspend operator fun invoke(code: String, saveAsApiKey: Boolean): UnlockResult {
        return repository.unlockWithServiceCode(code, saveAsApiKey)
    }
}
