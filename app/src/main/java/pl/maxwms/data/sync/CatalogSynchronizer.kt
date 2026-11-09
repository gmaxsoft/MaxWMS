package pl.maxwms.data.sync

import kotlinx.serialization.SerializationException
import pl.maxwms.data.local.dao.PartDao
import pl.maxwms.data.local.dao.StockMovementDao
import pl.maxwms.data.local.toDomain
import pl.maxwms.data.local.toDto
import pl.maxwms.data.local.toEntity
import pl.maxwms.data.local.toPartDto
import pl.maxwms.data.remote.CrmApiService
import pl.maxwms.data.remote.CrmClientFactory
import pl.maxwms.data.remote.dto.MovementsRequest
import pl.maxwms.data.remote.dto.PartDto
import pl.maxwms.domain.logic.CatalogMerger
import pl.maxwms.domain.logic.LocationParser
import pl.maxwms.domain.model.Part
import pl.maxwms.domain.model.PendingChange
import pl.maxwms.domain.model.SyncFailure
import pl.maxwms.domain.model.SyncOutcome
import pl.maxwms.domain.model.SyncStatus
import pl.maxwms.domain.repository.CatalogSync
import pl.maxwms.domain.repository.SettingsRepository
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogSynchronizer @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val clientFactory: CrmClientFactory,
    private val partDao: PartDao,
    private val movementDao: StockMovementDao,
) : CatalogSync {
    override suspend fun sync(): SyncOutcome {
        val settings = settingsRepository.snapshot()
        if (settings.baseUrl.isBlank()) return SyncOutcome.NotConfigured
        val api = try {
            clientFactory.create(settings.baseUrl)
        } catch (_: IllegalArgumentException) {
            settingsRepository.markSyncFailure(SyncFailure.INVALID_URL)
            return SyncOutcome.Failed(SyncFailure.INVALID_URL)
        }
        return try {
            pushParts(api)
            pushMovements(api)
            pull(api)
            settingsRepository.markSyncSuccess()
            SyncOutcome.Success
        } catch (_: IOException) {
            settingsRepository.markSyncFailure(SyncFailure.NETWORK)
            SyncOutcome.Failed(SyncFailure.NETWORK)
        } catch (error: HttpException) {
            val reason = if (error.code() == 401 || error.code() == 403) {
                SyncFailure.UNAUTHORIZED
            } else {
                SyncFailure.SERVER
            }
            settingsRepository.markSyncFailure(reason)
            SyncOutcome.Failed(reason)
        } catch (_: SerializationException) {
            settingsRepository.markSyncFailure(SyncFailure.FORMAT)
            SyncOutcome.Failed(SyncFailure.FORMAT)
        } catch (_: Exception) {
            settingsRepository.markSyncFailure(SyncFailure.UNKNOWN)
            SyncOutcome.Failed(SyncFailure.UNKNOWN)
        }
    }

    override suspend fun pushPart(partId: String): SyncOutcome {
        val settings = settingsRepository.snapshot()
        if (settings.baseUrl.isBlank()) return SyncOutcome.NotConfigured
        val part = partDao.getById(partId) ?: return SyncOutcome.Failed(SyncFailure.UNKNOWN)
        if (part.pendingChange == PendingChange.NONE.name) return SyncOutcome.Success
        val api = try {
            clientFactory.create(settings.baseUrl)
        } catch (_: IllegalArgumentException) {
            return SyncOutcome.Failed(SyncFailure.INVALID_URL)
        }
        return try {
            sendPart(api, part)
            SyncOutcome.Success
        } catch (_: IOException) {
            SyncOutcome.Failed(SyncFailure.NETWORK)
        } catch (error: HttpException) {
            val reason = if (error.code() == 401 || error.code() == 403) {
                SyncFailure.UNAUTHORIZED
            } else {
                SyncFailure.SERVER
            }
            SyncOutcome.Failed(reason)
        } catch (_: Exception) {
            SyncOutcome.Failed(SyncFailure.UNKNOWN)
        }
    }

    private suspend fun pushParts(api: CrmApiService) {
        partDao.pendingUpserts().forEach { part -> sendPart(api, part) }
    }

    private suspend fun sendPart(api: CrmApiService, part: pl.maxwms.data.local.entity.PartEntity) {
        val response = if (part.pendingChange == PendingChange.CREATE.name) {
            api.createPart(part.toPartDto())
        } else {
            api.updatePart(part.id, part.toPartDto())
        }
        if (!response.isSuccessful) throw HttpException(response)
        response.body()?.close()
        movementDao.markPendingSyncedForPart(part.id)
        partDao.markUpsertSynced(part.id, SyncStatus.SYNCED.name)
    }

    private suspend fun pushMovements(api: CrmApiService) {
        val pending = movementDao.pending()
        if (pending.isEmpty()) return
        val response = api.postMovements(MovementsRequest(pending.map { it.toDto() }))
        val rejected = response.rejected.associateBy { it.clientId }
        val accepted = if (response.accepted.isEmpty() && response.rejected.isEmpty()) {
            pending.map { it.id }
        } else {
            response.accepted
        }
        accepted.chunked(100).forEach { chunk ->
            if (chunk.isNotEmpty()) movementDao.markSynced(chunk)
        }
        rejected.forEach { (id, rejection) ->
            movementDao.markError(id, rejection.message.ifBlank { "rejected" })
        }
        pending.map { it.partId }.distinct().forEach { partId ->
            if (movementDao.pendingCountForPart(partId) == 0) {
                partDao.updateSyncStatus(partId, SyncStatus.SYNCED.name)
            }
        }
    }

    private suspend fun pull(api: CrmApiService) {
        api.getParts().parts.forEach { dto ->
            val remote = dto.toPart() ?: return@forEach
            val local = partDao.getByBarcode(remote.barcode)
                ?: partDao.getBySku(remote.sku)
                ?: if (remote.id.isNotBlank()) partDao.getById(remote.id) else null
            val pending = if (local == null) 0 else movementDao.pendingCountForPart(local.id)
            val merged = CatalogMerger.merge(
                local = local?.toDomain(),
                remote = remote,
                hasPendingMovements = pending > 0,
            )
            partDao.upsert(merged.toEntity())
        }
    }

    private fun PartDto.toPart(): Part? {
        if (sku.isBlank() && barcode.isBlank() && name.isBlank()) return null
        val parsed = LocationParser.parse(location)
        val updated = updatedAt?.let { raw ->
            runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
        } ?: 0L
        val stableBarcode = barcode.ifBlank { sku }
        return Part(
            id = id.ifBlank { UUID.randomUUID().toString() },
            sku = sku.ifBlank { stableBarcode },
            barcode = stableBarcode,
            name = name.ifBlank { sku },
            quantity = quantity,
            minQuantity = minQuantity,
            location = location,
            zone = parsed.zone,
            rack = parsed.rack,
            lastUpdated = updated,
            syncStatus = SyncStatus.SYNCED,
        )
    }
}
