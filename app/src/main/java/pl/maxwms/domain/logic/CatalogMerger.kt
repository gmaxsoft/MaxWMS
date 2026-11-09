package pl.maxwms.domain.logic

import pl.maxwms.domain.model.Part
import pl.maxwms.domain.model.PendingChange
import pl.maxwms.domain.model.SyncStatus

/**
 * A queued edit or a newer local snapshot keeps the device copy.
 * Otherwise the CRM record replaces the local part and the local id stays.
 */
object CatalogMerger {
    fun merge(
        local: Part?,
        remote: Part,
        hasPendingMovements: Boolean,
    ): Part {
        if (local == null) {
            return remote.copy(syncStatus = SyncStatus.SYNCED, pendingChange = PendingChange.NONE)
        }
        val catalogPending = local.pendingChange != PendingChange.NONE
        val keepLocal = catalogPending || hasPendingMovements || remote.lastUpdated < local.lastUpdated
        if (!keepLocal) {
            return remote.copy(
                id = local.id,
                pendingChange = PendingChange.NONE,
                syncStatus = SyncStatus.SYNCED,
            )
        }
        return local.copy(
            syncStatus = if (catalogPending || hasPendingMovements) SyncStatus.PENDING else SyncStatus.SYNCED,
        )
    }
}
