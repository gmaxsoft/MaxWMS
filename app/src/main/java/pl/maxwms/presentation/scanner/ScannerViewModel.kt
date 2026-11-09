@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package pl.maxwms.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.maxwms.domain.model.Part
import pl.maxwms.domain.model.StockChangeResult
import pl.maxwms.domain.repository.PartRepository
import pl.maxwms.domain.usecase.AdjustStockUseCase
import javax.inject.Inject

sealed interface ScanEffect {
    data object Found : ScanEffect
    data object Missing : ScanEffect
    data object Adjusted : ScanEffect
}

data class ScannerUiState(
    val part: Part? = null,
    val missingCode: String? = null,
    val insufficient: Int? = null,
    val adjusting: Boolean = false,
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val partRepository: PartRepository,
    private val adjustStock: AdjustStockUseCase,
) : ViewModel() {
    private val openId = MutableStateFlow<String?>(null)
    private val missingCode = MutableStateFlow<String?>(null)
    private val insufficient = MutableStateFlow<Int?>(null)
    private val adjusting = MutableStateFlow(false)
    private var lastCode: String? = null
    private var lastAt: Long = 0L

    val effects = MutableSharedFlow<ScanEffect>(extraBufferCapacity = 1)

    private val part = openId.flatMapLatest { id ->
        if (id == null) flowOf(null) else partRepository.observePart(id)
    }

    val uiState = combine(part, missingCode, insufficient, adjusting) { current, missing, shortage, busy ->
        ScannerUiState(
            part = current,
            missingCode = missing,
            insufficient = shortage,
            adjusting = busy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScannerUiState())

    fun onBarcode(raw: String) {
        val code = raw.trim()
        if (code.isEmpty()) return
        val now = System.currentTimeMillis()
        if (code == lastCode && now - lastAt < 1_200L) return
        lastCode = code
        lastAt = now
        viewModelScope.launch {
            val found = partRepository.findByCode(code)
            if (found == null) {
                missingCode.value = code
                effects.emit(ScanEffect.Missing)
            } else {
                missingCode.value = null
                insufficient.value = null
                openId.value = found.id
                effects.emit(ScanEffect.Found)
            }
        }
    }

    fun dismiss() {
        openId.value = null
        insufficient.value = null
    }

    fun clearMissing() {
        missingCode.value = null
    }

    fun adjust(delta: Int) {
        val id = openId.value ?: return
        if (adjusting.value) return
        viewModelScope.launch {
            adjusting.value = true
            when (val result = adjustStock(id, delta)) {
                is StockChangeResult.Success -> {
                    insufficient.value = null
                    effects.emit(ScanEffect.Adjusted)
                }
                is StockChangeResult.Insufficient -> insufficient.value = result.available
                StockChangeResult.NotFound -> openId.value = null
            }
            adjusting.value = false
        }
    }
}
