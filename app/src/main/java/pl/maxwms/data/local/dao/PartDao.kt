package pl.maxwms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import pl.maxwms.data.local.entity.PartEntity

@Dao
interface PartDao {
    @Query("SELECT * FROM parts ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<PartEntity>>

    @Query("SELECT * FROM parts WHERE id = :id")
    fun observeById(id: String): Flow<PartEntity?>

    @Query("SELECT * FROM parts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PartEntity?

    @Query("SELECT * FROM parts WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): PartEntity?

    @Query("SELECT * FROM parts WHERE sku = :sku LIMIT 1")
    suspend fun getBySku(sku: String): PartEntity?

    @Query("SELECT sku FROM parts")
    suspend fun allSkus(): List<String>

    @Query("SELECT COUNT(*) FROM parts")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(parts: List<PartEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(part: PartEntity)

    @Query(
        "UPDATE parts SET quantity = :quantity, lastUpdated = :lastUpdated, syncStatus = :syncStatus WHERE id = :id",
    )
    suspend fun updateQuantity(
        id: String,
        quantity: Int,
        lastUpdated: Long,
        syncStatus: String,
    )

    @Query("UPDATE parts SET syncStatus = :syncStatus WHERE id = :id AND pendingChange = 'NONE'")
    suspend fun updateSyncStatus(id: String, syncStatus: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(part: PartEntity)

    @Update
    suspend fun update(part: PartEntity)

    @Query("SELECT * FROM parts WHERE pendingChange != 'NONE' ORDER BY lastUpdated ASC")
    suspend fun pendingUpserts(): List<PartEntity>

    @Query("SELECT COUNT(*) FROM parts WHERE pendingChange != 'NONE'")
    fun observePendingUpsertCount(): Flow<Int>

    @Query(
        "UPDATE parts SET pendingChange = 'NONE', syncStatus = :syncStatus WHERE id = :id",
    )
    suspend fun markUpsertSynced(id: String, syncStatus: String)
}
