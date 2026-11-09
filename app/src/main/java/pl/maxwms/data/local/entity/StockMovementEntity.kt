package pl.maxwms.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["partId"]),
        Index(value = ["syncStatus"]),
    ],
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val partId: String,
    val sku: String,
    val barcode: String,
    val delta: Int,
    val quantityAfter: Int,
    val type: String,
    val occurredAt: Long,
    val syncStatus: String,
    val lastError: String?,
)
