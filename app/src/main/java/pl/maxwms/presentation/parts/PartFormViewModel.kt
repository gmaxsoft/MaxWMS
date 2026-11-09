package pl.maxwms.presentation.parts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.maxwms.domain.model.PartDelivery
import pl.maxwms.domain.model.PartDraft
import pl.maxwms.domain.model.PartSaveResult
import pl.maxwms.domain.repository.PartRepository
import javax.inject.Inject

enum class PartFormMessage {
    NAME,
    SKU,
    BARCODE,
    QUANTITY,
    MIN,
    DUPLICATE_BARCODE,
    DUPLICATE_SKU,
    NOT_FOUND,
}

data class PartFormUiState(
    val isEdit: Boolean = false,
    val loading: Boolean = true,
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val quantity: String = "0",
    val minQuantity: String = "0",
    val location: String = "",
    val saving: Boolean = false,
    val missing: Boolean = false,
    val message: PartFormMessage? = null,
)

@HiltViewModel
class PartFormViewModel @Inject constructor(
    private val partRepository: PartRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val partId: String = savedStateHandle.get<String>("partId").orEmpty()
    private val state = MutableStateFlow(PartFormUiState(isEdit = partId.isNotBlank()))
    val uiState = state

    private val savedEvents = MutableSharedFlow<PartDelivery>(extraBufferCapacity = 1)
    val saved = savedEvents.asSharedFlow()

    init {
        if (partId.isBlank()) {
            state.update { it.copy(loading = false) }
        } else {
            viewModelScope.launch {
                val part = partRepository.observePart(partId).first()
                if (part == null) {
                    state.update { it.copy(loading = false, missing = true) }
                } else {
                    state.update {
                        it.copy(
                            loading = false,
                            name = part.name,
                            sku = part.sku,
                            barcode = part.barcode,
                            quantity = part.quantity.toString(),
                            minQuantity = part.minQuantity.toString(),
                            location = part.location,
                        )
                    }
                }
            }
        }
    }

    fun onName(value: String) = state.update { it.copy(name = value, message = null) }

    fun onSku(value: String) = state.update { it.copy(sku = value, message = null) }

    fun onBarcode(value: String) = state.update { it.copy(barcode = value, message = null) }

    fun onQuantity(value: String) = state.update { it.copy(quantity = digits(value), message = null) }

    fun onMinQuantity(value: String) = state.update { it.copy(minQuantity = digits(value), message = null) }

    fun onLocation(value: String) = state.update { it.copy(location = value, message = null) }

    fun save() {
        val current = state.value
        if (current.saving || current.missing) return
        val quantity = current.quantity.toIntOrNull()
        val minQuantity = current.minQuantity.toIntOrNull()
        val message = when {
            current.name.isBlank() -> PartFormMessage.NAME
            current.sku.isBlank() -> PartFormMessage.SKU
            current.barcode.isBlank() -> PartFormMessage.BARCODE
            quantity == null || quantity < 0 -> PartFormMessage.QUANTITY
            minQuantity == null || minQuantity < 0 -> PartFormMessage.MIN
            else -> null
        }
        if (message != null || quantity == null || minQuantity == null) {
            state.update { it.copy(message = message) }
            return
        }
        viewModelScope.launch {
            state.update { it.copy(saving = true, message = null) }
            when (
                val result = partRepository.save(
                    PartDraft(
                        id = partId.ifBlank { null },
                        name = current.name,
                        sku = current.sku,
                        barcode = current.barcode,
                        quantity = quantity,
                        minQuantity = minQuantity,
                        location = current.location,
                    ),
                )
            ) {
                is PartSaveResult.Saved -> savedEvents.emit(result.delivery)
                PartSaveResult.DuplicateBarcode -> state.update { it.copy(message = PartFormMessage.DUPLICATE_BARCODE) }
                PartSaveResult.DuplicateSku -> state.update { it.copy(message = PartFormMessage.DUPLICATE_SKU) }
                PartSaveResult.NotFound -> state.update { it.copy(missing = true) }
                PartSaveResult.Invalid -> state.update { it.copy(message = PartFormMessage.NAME) }
            }
            state.update { it.copy(saving = false) }
        }
    }

    private fun digits(value: String): String = value.filter { it.isDigit() }.take(7)
}
