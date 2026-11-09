package pl.maxwms.presentation.parts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.maxwms.domain.model.Part
import pl.maxwms.domain.model.StockChangeResult
import pl.maxwms.domain.repository.PartRepository
import pl.maxwms.domain.usecase.AdjustStockUseCase
import javax.inject.Inject

data class PartListUiState(
    val query: String = "",
    val zone: String = "",
    val rack: String = "",
    val lowStockOnly: Boolean = false,
    val zones: List<String> = emptyList(),
    val racks: List<String> = emptyList(),
    val parts: List<Part> = emptyList(),
    val selected: Part? = null,
    val insufficient: Int? = null,
    val adjusting: Boolean = false,
)

@HiltViewModel
class PartListViewModel @Inject constructor(
    partRepository: PartRepository,
    private val adjustStock: AdjustStockUseCase,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val zone = MutableStateFlow("")
    private val rack = MutableStateFlow("")
    private val lowStockOnly = MutableStateFlow(false)
    private val selectedId = MutableStateFlow<String?>(null)
    private val insufficient = MutableStateFlow<Int?>(null)
    private val adjusting = MutableStateFlow(false)
    private var appliedInitial = false

    private val filters = combine(query, zone, rack, lowStockOnly) { search, selectedZone, selectedRack, lowOnly ->
        Filter(search, selectedZone, selectedRack, lowOnly)
    }

    val uiState = combine(
        partRepository.observeParts(),
        filters,
        selectedId,
        insufficient,
        adjusting,
    ) { parts, filter, selected, shortage, busy ->
        val zones = parts.map { it.zone }.filter { it.isNotBlank() }.distinct().sorted()
        val racks = parts
            .filter { filter.zone.isEmpty() || it.zone == filter.zone }
            .map { it.rack }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val visible = parts.filter { part ->
            val needle = filter.query.trim()
            val matchesQuery = needle.isEmpty() ||
                part.name.contains(needle, true) ||
                part.sku.contains(needle, true) ||
                part.barcode.contains(needle, true) ||
                part.location.contains(needle, true)
            val matchesZone = filter.zone.isEmpty() || part.zone == filter.zone
            val matchesRack = filter.rack.isEmpty() || part.rack == filter.rack
            val matchesLow = !filter.lowStockOnly || part.isLowStock
            matchesQuery && matchesZone && matchesRack && matchesLow
        }
        PartListUiState(
            query = filter.query,
            zone = filter.zone,
            rack = filter.rack,
            lowStockOnly = filter.lowStockOnly,
            zones = zones,
            racks = racks,
            parts = visible,
            selected = parts.firstOrNull { it.id == selected },
            insufficient = shortage,
            adjusting = busy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PartListUiState())

    fun applyInitialFilter(filter: String) {
        if (appliedInitial) return
        appliedInitial = true
        if (filter == "low") lowStockOnly.value = true
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun selectZone(value: String) {
        zone.value = if (zone.value == value) "" else value
        rack.value = ""
    }

    fun selectRack(value: String) {
        rack.value = if (rack.value == value) "" else value
    }

    fun toggleLowStock() {
        lowStockOnly.value = !lowStockOnly.value
    }

    fun openPart(id: String) {
        insufficient.value = null
        selectedId.value = id
    }

    fun dismissPart() {
        selectedId.value = null
        insufficient.value = null
    }

    fun adjust(delta: Int) {
        val id = selectedId.value ?: return
        if (adjusting.value) return
        viewModelScope.launch {
            adjusting.value = true
            when (val result = adjustStock(id, delta)) {
                is StockChangeResult.Success -> insufficient.value = null
                is StockChangeResult.Insufficient -> insufficient.value = result.available
                StockChangeResult.NotFound -> selectedId.value = null
            }
            adjusting.value = false
        }
    }

    private data class Filter(
        val query: String,
        val zone: String,
        val rack: String,
        val lowStockOnly: Boolean,
    )
}
