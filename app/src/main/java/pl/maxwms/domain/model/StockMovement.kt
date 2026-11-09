package pl.maxwms.domain.model

data class StockMovement(
    val id: String,
    val partId: String,
    val sku: String,
    val barcode: String,
    val delta: Int,
    val quantityAfter: Int,
    val type: MovementType,
    val occurredAt: Long,
    val syncStatus: SyncStatus,
    val lastError: String?,
)
