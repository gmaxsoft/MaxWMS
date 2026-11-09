package pl.maxwms.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import pl.maxwms.data.local.entity.StockMovementEntity

@Dao
interface StockMovementDao {
    @Insert
    suspend fun insert(movement: StockMovementEntity)

    @Query(
        "SELECT * FROM stock_movements WHERE syncStatus = 'PENDING' ORDER BY occurredAt ASC",
    )
    suspend fun pending(): List<StockMovementEntity>

    @Query("SELECT COUNT(*) FROM stock_movements WHERE syncStatus = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM stock_movements WHERE partId = :partId AND syncStatus = 'PENDING'",
    )
    suspend fun pendingCountForPart(partId: String): Int

    @Query("SELECT * FROM stock_movements ORDER BY occurredAt DESC LIMIT 30")
    fun observeRecent(): Flow<List<StockMovementEntity>>

    @Query(
        "UPDATE stock_movements SET syncStatus = 'SYNCED', lastError = NULL WHERE id IN (:ids)",
    )
    suspend fun markSynced(ids: List<String>)

    @Query("UPDATE stock_movements SET lastError = :error WHERE id = :id")
    suspend fun markError(id: String, error: String)

    @Query(
        "UPDATE stock_movements SET syncStatus = 'SYNCED', lastError = NULL WHERE partId = :partId AND syncStatus = 'PENDING'",
    )
    suspend fun markPendingSyncedForPart(partId: String)
}
