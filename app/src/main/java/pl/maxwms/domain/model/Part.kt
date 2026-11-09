package pl.maxwms.domain.model

data class Part(
    val id: String,
    val sku: String,
    val barcode: String,
    val name: String,
    val quantity: Int,
    val minQuantity: Int,
    val location: String,
    val zone: String,
    val rack: String,
    val lastUpdated: Long,
    val syncStatus: SyncStatus,
    val pendingChange: PendingChange = PendingChange.NONE,
) {
    val isLowStock: Boolean get() = quantity <= minQuantity
}
