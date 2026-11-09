package pl.maxwms.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.maxwms.domain.model.AccessState
import pl.maxwms.domain.model.AppSettings
import pl.maxwms.domain.model.StockMovement
import pl.maxwms.domain.model.SyncFailure
import pl.maxwms.domain.model.SyncOutcome
import pl.maxwms.domain.model.ThemeMode
import pl.maxwms.domain.repository.LicenseRepository
import pl.maxwms.domain.repository.PartRepository
import pl.maxwms.domain.repository.SettingsRepository
import pl.maxwms.domain.usecase.SyncCatalogUseCase
import javax.inject.Inject

sealed interface SettingsNotice {
    data object Saved : SettingsNotice
    data object InvalidUrl : SettingsNotice
    data object SamplesAdded : SettingsNotice
    data object SyncSuccess : SettingsNotice
    data object SyncNotConfigured : SettingsNotice
    data class SyncFailed(val reason: SyncFailure) : SettingsNotice
}

data class SettingsUiState(
    val draftUrl: String = "",
    val draftKey: String = "",
    val settings: AppSettings = AppSettings(),
    val access: AccessState = AccessState.Checking,
    val movements: List<StockMovement> = emptyList(),
    val pendingCount: Int = 0,
    val syncing: Boolean = false,
    val notice: SettingsNotice? = null,
    val confirmClear: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val licenseRepository: LicenseRepository,
    private val partRepository: PartRepository,
    private val syncCatalog: SyncCatalogUseCase,
) : ViewModel() {
    private val draftUrl = MutableStateFlow<String?>(null)
    private val draftKey = MutableStateFlow<String?>(null)
    private val syncing = MutableStateFlow(false)
    private val notice = MutableStateFlow<SettingsNotice?>(null)
    private val confirmClear = MutableStateFlow(false)
    init {
        viewModelScope.launch {
            val current = settingsRepository.snapshot()
            if (draftUrl.value == null) draftUrl.value = current.baseUrl
            if (draftKey.value == null) draftKey.value = current.apiKey
        }
    }

    val uiState = combine(
        settingsRepository.settings,
        licenseRepository.observeAccess(),
        partRepository.observeRecentMovements(),
        partRepository.observePendingCount(),
        combine(draftUrl, draftKey, syncing, notice, confirmClear) { url, key, busy, message, confirm ->
            Draft(url, key, busy, message, confirm)
        },
    ) { settings, access, movements, pending, draft ->
        SettingsUiState(
            draftUrl = draft.url ?: settings.baseUrl,
            draftKey = draft.key ?: settings.apiKey,
            settings = settings,
            access = access,
            movements = movements,
            pendingCount = pending,
            syncing = draft.syncing,
            notice = draft.notice,
            confirmClear = draft.confirm,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onUrlChange(value: String) {
        draftUrl.value = value
    }

    fun onKeyChange(value: String) {
        draftKey.value = value
    }

    fun save() {
        viewModelScope.launch {
            val ok = settingsRepository.saveConnection(draftUrl.value.orEmpty(), draftKey.value.orEmpty())
            notice.value = if (ok) SettingsNotice.Saved else SettingsNotice.InvalidUrl
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setTheme(mode) }
    }

    fun sync() {
        if (syncing.value) return
        viewModelScope.launch {
            syncing.value = true
            notice.value = when (val outcome = syncCatalog()) {
                SyncOutcome.Success -> SettingsNotice.SyncSuccess
                SyncOutcome.NotConfigured -> SettingsNotice.SyncNotConfigured
                is SyncOutcome.Failed -> SettingsNotice.SyncFailed(outcome.reason)
            }
            syncing.value = false
        }
    }

    fun loadSamples() {
        viewModelScope.launch {
            partRepository.ensureSamples()
            notice.value = SettingsNotice.SamplesAdded
        }
    }

    fun askClearLicense() {
        confirmClear.value = true
    }

    fun dismissClear() {
        confirmClear.value = false
    }

    fun clearLicense() {
        viewModelScope.launch {
            licenseRepository.clearCrmLicense()
            confirmClear.value = false
        }
    }

    fun consumeNotice() {
        notice.value = null
    }

    private data class Draft(
        val url: String?,
        val key: String?,
        val syncing: Boolean,
        val notice: SettingsNotice?,
        val confirm: Boolean,
    )
}
