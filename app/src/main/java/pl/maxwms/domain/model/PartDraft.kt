package pl.maxwms.domain.model

enum class PendingChange {
    NONE,
    CREATE,
    UPDATE,
}

enum class PartDelivery {
    LOCAL_ONLY,
    SENT,
    QUEUED,
    REJECTED,
}

data class PartDraft(
    val id: String?,
    val name: String,
    val sku: String,
    val barcode: String,
    val quantity: Int,
    val minQuantity: Int,
    val location: String,
)

sealed interface PartSaveResult {
    data class Saved(val delivery: PartDelivery) : PartSaveResult

    data object NotFound : PartSaveResult

    data object DuplicateBarcode : PartSaveResult

    data object DuplicateSku : PartSaveResult

    data object Invalid : PartSaveResult
}
