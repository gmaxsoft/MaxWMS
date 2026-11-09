package pl.maxwms.domain.repository

import kotlinx.coroutines.flow.Flow
import pl.maxwms.domain.model.AccessState
import pl.maxwms.domain.model.UnlockResult

interface LicenseRepository {
    fun observeAccess(): Flow<AccessState>

    suspend fun unlockWithServiceCode(
        code: String,
        saveAsApiKey: Boolean,
    ): UnlockResult

    suspend fun clearCrmLicense()
}
