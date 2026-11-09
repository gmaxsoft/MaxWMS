package pl.maxwms.data.repository

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pl.maxwms.BuildConfig
import pl.maxwms.data.billing.BillingManager
import pl.maxwms.data.local.secure.SecureStore
import pl.maxwms.data.remote.CrmClientFactory
import pl.maxwms.data.remote.dto.VerifyLicenseRequest
import pl.maxwms.domain.logic.ServiceCodeHasher
import pl.maxwms.domain.model.AccessSource
import pl.maxwms.domain.model.AccessState
import pl.maxwms.domain.model.UnlockResult
import pl.maxwms.domain.repository.LicenseRepository
import pl.maxwms.domain.repository.SettingsRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStore: SecureStore,
    private val billingManager: BillingManager,
    private val settingsRepository: SettingsRepository,
    private val clientFactory: CrmClientFactory,
) : LicenseRepository {
    override fun observeAccess(): Flow<AccessState> = combine(
        secureStore.crmUnlocked,
        billingManager.state,
    ) { crmUnlocked, billing ->
        when {
            crmUnlocked -> AccessState.Unlocked(AccessSource.CRM_LICENSE)
            !billing.ready -> AccessState.Checking
            billing.subscribed -> AccessState.Unlocked(AccessSource.PLAY_SUBSCRIPTION)
            else -> AccessState.Locked
        }
    }

    override suspend fun unlockWithServiceCode(
        code: String,
        saveAsApiKey: Boolean,
    ): UnlockResult {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return UnlockResult.Empty
        val baseUrl = settingsRepository.snapshot().baseUrl
        if (baseUrl.isBlank()) {
            return unlockOffline(trimmed, saveAsApiKey) ?: UnlockResult.Invalid
        }
        return try {
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
            val response = clientFactory.create(baseUrl).verifyLicense(
                VerifyLicenseRequest(serviceCode = trimmed, deviceId = deviceId),
            )
            if (!response.valid) {
                UnlockResult.Invalid
            } else {
                secureStore.setCrmUnlocked(true, response.companyName)
                if (saveAsApiKey) secureStore.setApiKey(trimmed)
                UnlockResult.Success(offline = false, companyName = response.companyName)
            }
        } catch (_: IOException) {
            unlockOffline(trimmed, saveAsApiKey) ?: UnlockResult.Network
        } catch (error: HttpException) {
            if (error.code() in 500..599) {
                unlockOffline(trimmed, saveAsApiKey) ?: UnlockResult.Network
            } else {
                UnlockResult.Invalid
            }
        } catch (_: Exception) {
            unlockOffline(trimmed, saveAsApiKey) ?: UnlockResult.Network
        }
    }

    override suspend fun clearCrmLicense() {
        secureStore.setCrmUnlocked(false, null)
    }

    private fun unlockOffline(code: String, saveAsApiKey: Boolean): UnlockResult? {
        if (!ServiceCodeHasher.matches(code, BuildConfig.OFFLINE_SERVICE_CODE_SHA256)) return null
        secureStore.setCrmUnlocked(true, null)
        if (saveAsApiKey) secureStore.setApiKey(code)
        return UnlockResult.Success(offline = true, companyName = null)
    }
}
