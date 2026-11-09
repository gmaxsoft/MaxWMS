package pl.maxwms.domain.repository

import kotlinx.coroutines.flow.Flow
import pl.maxwms.domain.model.MovementType
import pl.maxwms.domain.model.Part
import pl.maxwms.domain.model.PartDraft
import pl.maxwms.domain.model.PartSaveResult
import pl.maxwms.domain.model.StockChangeResult
import pl.maxwms.domain.model.StockMovement

interface PartRepository {
    fun observeParts(): Flow<List<Part>>

    fun observePart(id: String): Flow<Part?>

    fun observePendingCount(): Flow<Int>

    fun observeRecentMovements(): Flow<List<StockMovement>>

    suspend fun findByCode(code: String): Part?

    suspend fun adjust(
        partId: String,
        delta: Int,
        type: MovementType,
    ): StockChangeResult

    suspend fun ensureSamples()

    suspend fun save(draft: PartDraft): PartSaveResult
}
