package pl.maxwms.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parts",
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["sku"]),
        Index(value = ["zone"]),
    ],
)
data class PartEntity(
    @PrimaryKey val id: String,
    val sku: String,
    val barcode: String,
    val name: String,
    val quantity: Int,
    val minQuantity: Int,
    val location: String,
    val zone: String,
    val rack: String,
    val lastUpdated: Long,
    val syncStatus: String,
    val pendingChange: String = "NONE",
)
