package pl.maxwms.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import pl.maxwms.data.local.MaxWmsDatabase
import pl.maxwms.data.local.SampleCatalog
import pl.maxwms.data.local.dao.PartDao
import pl.maxwms.data.local.dao.StockMovementDao
import pl.maxwms.data.local.entity.PartEntity
import pl.maxwms.data.local.entity.StockMovementEntity
import pl.maxwms.data.local.toDomain
import pl.maxwms.data.sync.SyncScheduler
import pl.maxwms.domain.logic.AdjustmentResult
import pl.maxwms.domain.logic.LocationParser
import pl.maxwms.domain.logic.StockAdjuster
import pl.maxwms.domain.model.MovementType
import pl.maxwms.domain.model.Part
import pl.maxwms.domain.model.PartDelivery
import pl.maxwms.domain.model.PartDraft
import pl.maxwms.domain.model.PartSaveResult
import pl.maxwms.domain.model.PendingChange
import pl.maxwms.domain.model.StockChangeResult
import pl.maxwms.domain.model.StockMovement
import pl.maxwms.domain.model.SyncFailure
import pl.maxwms.domain.model.SyncOutcome
import pl.maxwms.domain.model.SyncStatus
import pl.maxwms.domain.repository.CatalogSync
import pl.maxwms.domain.repository.PartRepository
import pl.maxwms.domain.repository.SettingsRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartRepositoryImpl @Inject constructor(
    private val database: MaxWmsDatabase,
    private val partDao: PartDao,
    private val movementDao: StockMovementDao,
    private val syncScheduler: SyncScheduler,
    private val settingsRepository: SettingsRepository,
    private val catalogSync: CatalogSync,
) : PartRepository {
    override fun observeParts(): Flow<List<Part>> = partDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observePart(id: String): Flow<Part?> = partDao.observeById(id).map { it?.toDomain() }

    override fun observePendingCount(): Flow<Int> = combine(
        movementDao.observePendingCount(),
        partDao.observePendingUpsertCount(),
    ) { movements, parts -> movements + parts }

    override fun observeRecentMovements(): Flow<List<StockMovement>> =
        movementDao.observeRecent().map { rows -> rows.map { it.toDomain() } }

    override suspend fun findByCode(code: String): Part? {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return null
        return (partDao.getByBarcode(trimmed) ?: partDao.getBySku(trimmed))?.toDomain()
    }

    override suspend fun adjust(
        partId: String,
        delta: Int,
        type: MovementType,
    ): StockChangeResult {
        val current = partDao.getById(partId) ?: return StockChangeResult.NotFound
        val newQuantity = when (val result = StockAdjuster.apply(current.quantity, delta)) {
            is AdjustmentResult.Applied -> result.quantity
            is AdjustmentResult.Insufficient -> return StockChangeResult.Insufficient(result.available)
        }
        val now = System.currentTimeMillis()
        val movement = StockMovementEntity(
            id = UUID.randomUUID().toString(),
            partId = current.id,
            sku = current.sku,
            barcode = current.barcode,
            delta = delta,
            quantityAfter = newQuantity,
            type = type.name,
            occurredAt = now,
            syncStatus = SyncStatus.PENDING.name,
            lastError = null,
        )
        database.withTransaction {
            partDao.updateQuantity(current.id, newQuantity, now, SyncStatus.PENDING.name)
            movementDao.insert(movement)
        }
        syncScheduler.enqueueSoon()
        return StockChangeResult.Success(
            current.toDomain().copy(
                quantity = newQuantity,
                lastUpdated = now,
                syncStatus = SyncStatus.PENDING,
            ),
        )
    }

    override suspend fun ensureSamples() {
        val existing = partDao.allSkus().toSet()
        val missing = SampleCatalog.entities().filter { it.sku !in existing }
        if (missing.isNotEmpty()) {
            partDao.upsertAll(missing)
        }
    }

    override suspend fun save(draft: PartDraft): PartSaveResult {
        val name = draft.name.trim()
        val sku = draft.sku.trim()
        val barcode = draft.barcode.trim()
        val location = draft.location.trim()
        if (name.isEmpty() || sku.isEmpty() || barcode.isEmpty() || draft.quantity < 0 || draft.minQuantity < 0) {
            return PartSaveResult.Invalid
        }
        val existing = draft.id?.let { partDao.getById(it) }
        if (draft.id != null && existing == null) return PartSaveResult.NotFound
        val barcodeOwner = partDao.getByBarcode(barcode)
        if (barcodeOwner != null && barcodeOwner.id != existing?.id) return PartSaveResult.DuplicateBarcode
        val skuOwner = partDao.getBySku(sku)
        if (skuOwner != null && skuOwner.id != existing?.id) return PartSaveResult.DuplicateSku

        val now = System.currentTimeMillis()
        val parsed = LocationParser.parse(location)
        val pendingChange = if (existing == null || existing.pendingChange == PendingChange.CREATE.name) {
            PendingChange.CREATE
        } else {
            PendingChange.UPDATE
        }
        val entity = PartEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            sku = sku,
            barcode = barcode,
            name = name,
            quantity = draft.quantity,
            minQuantity = draft.minQuantity,
            location = location,
            zone = parsed.zone,
            rack = parsed.rack,
            lastUpdated = now,
            syncStatus = SyncStatus.PENDING.name,
            pendingChange = pendingChange.name,
        )
        if (existing == null) partDao.insert(entity) else partDao.update(entity)

        if (settingsRepository.snapshot().baseUrl.isBlank()) {
            return PartSaveResult.Saved(PartDelivery.LOCAL_ONLY)
        }
        return when (val outcome = catalogSync.pushPart(entity.id)) {
            SyncOutcome.Success -> PartSaveResult.Saved(PartDelivery.SENT)
            SyncOutcome.NotConfigured -> PartSaveResult.Saved(PartDelivery.LOCAL_ONLY)
            is SyncOutcome.Failed -> {
                syncScheduler.enqueueSoon()
                val delivery = if (outcome.reason == SyncFailure.NETWORK || outcome.reason == SyncFailure.UNKNOWN) {
                    PartDelivery.QUEUED
                } else {
                    PartDelivery.REJECTED
                }
                PartSaveResult.Saved(delivery)
            }
        }
    }
}
