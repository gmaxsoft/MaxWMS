package pl.maxwms.data.local

import pl.maxwms.data.local.entity.PartEntity
import pl.maxwms.data.local.entity.StockMovementEntity
import pl.maxwms.data.remote.dto.MovementDto
import pl.maxwms.data.remote.dto.PartDto
import pl.maxwms.domain.logic.LocationParser
import pl.maxwms.domain.model.MovementType
import pl.maxwms.domain.model.Part
import pl.maxwms.domain.model.PendingChange
import pl.maxwms.domain.model.StockMovement
import pl.maxwms.domain.model.SyncStatus
import java.time.Instant

fun PartEntity.toDomain(): Part = Part(
    id = id,
    sku = sku,
    barcode = barcode,
    name = name,
    quantity = quantity,
    minQuantity = minQuantity,
    location = location,
    zone = zone,
    rack = rack,
    lastUpdated = lastUpdated,
    syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.PENDING),
    pendingChange = runCatching { PendingChange.valueOf(pendingChange) }.getOrDefault(PendingChange.NONE),
)

fun Part.toEntity(): PartEntity {
    val parsed = LocationParser.parse(location)
    return PartEntity(
        id = id,
        sku = sku,
        barcode = barcode,
        name = name,
        quantity = quantity,
        minQuantity = minQuantity,
        location = location,
        zone = zone.ifBlank { parsed.zone },
        rack = rack.ifBlank { parsed.rack },
        lastUpdated = lastUpdated,
        syncStatus = syncStatus.name,
        pendingChange = pendingChange.name,
    )
}

fun PartEntity.toPartDto(): PartDto = PartDto(
    id = id,
    sku = sku,
    barcode = barcode,
    name = name,
    quantity = quantity,
    minQuantity = minQuantity,
    location = location,
    updatedAt = Instant.ofEpochMilli(lastUpdated).toString(),
)

fun StockMovementEntity.toDomain(): StockMovement = StockMovement(
    id = id,
    partId = partId,
    sku = sku,
    barcode = barcode,
    delta = delta,
    quantityAfter = quantityAfter,
    type = runCatching { MovementType.valueOf(type) }.getOrDefault(MovementType.ADJUSTMENT),
    occurredAt = occurredAt,
    syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.PENDING),
    lastError = lastError,
)

fun StockMovementEntity.toDto(): MovementDto = MovementDto(
    clientId = id,
    partId = partId,
    sku = sku,
    barcode = barcode,
    delta = delta,
    quantityAfter = quantityAfter,
    reason = type,
    occurredAt = Instant.ofEpochMilli(occurredAt).toString(),
)
