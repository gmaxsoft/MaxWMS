package pl.maxwms.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.maxwms.data.network.NetworkMonitor
import pl.maxwms.domain.model.AccessState
import pl.maxwms.domain.model.Part
import pl.maxwms.domain.model.StockChangeResult
import pl.maxwms.domain.model.SyncFailure
import pl.maxwms.domain.model.SyncOutcome
import pl.maxwms.domain.repository.LicenseRepository
import pl.maxwms.domain.repository.PartRepository
import pl.maxwms.domain.repository.SettingsRepository
import pl.maxwms.domain.usecase.AdjustStockUseCase
import pl.maxwms.domain.usecase.SyncCatalogUseCase
import javax.inject.Inject

sealed interface DashboardNotice {
    data object SyncSuccess : DashboardNotice
    data object SyncNotConfigured : DashboardNotice
    data class SyncFailed(val reason: SyncFailure) : DashboardNotice
    data class Insufficient(val available: Int) : DashboardNotice
    data object Saved : DashboardNotice
}

data class DashboardUiState(
    val query: String = "",
    val parts: List<Part> = emptyList(),
    val lowStockCount: Int = 0,
    val pendingCount: Int = 0,
    val online: Boolean = true,
    val lastSyncAt: Long? = null,
    val syncFailure: SyncFailure? = null,
    val access: AccessState = AccessState.Checking,
    val selected: Part? = null,
    val syncing: Boolean = false,
    val adjusting: Boolean = false,
    val notice: DashboardNotice? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val partRepository: PartRepository,
    private val settingsRepository: SettingsRepository,
    private val licenseRepository: LicenseRepository,
    networkMonitor: NetworkMonitor,
    private val adjustStock: AdjustStockUseCase,
    private val syncCatalog: SyncCatalogUseCase,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedId = MutableStateFlow<String?>(null)
    private val syncing = MutableStateFlow(false)
    private val adjusting = MutableStateFlow(false)
    private val notice = MutableStateFlow<DashboardNotice?>(null)

    private val overview = combine(
        partRepository.observeParts(),
        partRepository.observePendingCount(),
        settingsRepository.settings,
        networkMonitor.isOnline,
    ) { parts, pending, settings, online ->
        Overview(parts, pending, settings, online)
    }

    private val selection = combine(licenseRepository.observeAccess(), query, selectedId) { access, search, selected ->
        Triple(access, search, selected)
    }

    private val flags = combine(syncing, adjusting, notice) { isSyncing, isAdjusting, currentNotice ->
        Triple(isSyncing, isAdjusting, currentNotice)
    }

    val uiState = combine(overview, selection, flags) { data, picked, status ->
        val (access, search, selected) = picked
        val (isSyncing, isAdjusting, currentNotice) = status
        DashboardUiState(
            query = search,
            parts = data.parts.filter { it.matches(search) }.take(40),
            lowStockCount = data.parts.count { it.isLowStock },
            pendingCount = data.pending,
            online = data.online,
            lastSyncAt = data.settings.lastSyncAt,
            syncFailure = data.settings.syncFailure,
            access = access,
            selected = data.parts.firstOrNull { it.id == selected },
            syncing = isSyncing,
            adjusting = isAdjusting,
            notice = currentNotice,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun openPart(id: String) {
        notice.value = null
        selectedId.value = id
    }

    fun dismissPart() {
        selectedId.value = null
        notice.value = null
    }

    fun consumeNotice() {
        notice.value = null
    }

    fun adjust(delta: Int) {
        val id = selectedId.value ?: return
        if (adjusting.value) return
        viewModelScope.launch {
            adjusting.value = true
            when (val result = adjustStock(id, delta)) {
                is StockChangeResult.Success -> notice.value = DashboardNotice.Saved
                is StockChangeResult.Insufficient -> notice.value = DashboardNotice.Insufficient(result.available)
                StockChangeResult.NotFound -> selectedId.value = null
            }
            adjusting.value = false
        }
    }

    fun sync() {
        if (syncing.value) return
        viewModelScope.launch {
            syncing.value = true
            notice.value = when (val outcome = syncCatalog()) {
                SyncOutcome.Success -> DashboardNotice.SyncSuccess
                SyncOutcome.NotConfigured -> DashboardNotice.SyncNotConfigured
                is SyncOutcome.Failed -> DashboardNotice.SyncFailed(outcome.reason)
            }
            syncing.value = false
        }
    }

    private data class Overview(
        val parts: List<Part>,
        val pending: Int,
        val settings: pl.maxwms.domain.model.AppSettings,
        val online: Boolean,
    )
}

private fun Part.matches(query: String): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return true
    return name.contains(needle, ignoreCase = true) ||
        sku.contains(needle, ignoreCase = true) ||
        barcode.contains(needle, ignoreCase = true) ||
        location.contains(needle, ignoreCase = true)
}
