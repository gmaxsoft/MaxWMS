package pl.maxwms.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.maxwms.domain.model.Part
import pl.maxwms.domain.model.SyncStatus

class StockAdjusterTest {
    @Test
    fun issueStopsAtZero() {
        val result = StockAdjuster.apply(current = 3, delta = -5)
        assertTrue(result is AdjustmentResult.Insufficient)
        assertEquals(3, (result as AdjustmentResult.Insufficient).available)
    }

    @Test
    fun receiptAddsStock() {
        val result = StockAdjuster.apply(current = 4, delta = 10)
        assertEquals(14, (result as AdjustmentResult.Applied).quantity)
    }
}

class LocationParserTest {
    @Test
    fun splitsZoneAndRack() {
        val parsed = LocationParser.parse("Strefa A -> Regał 2 -> Półka 3 -> Pojemnik 1")
        assertEquals("Strefa A", parsed.zone)
        assertEquals("Regał 2", parsed.rack)
    }
}

class ServiceCodeHasherTest {
    @Test
    fun matchesKnownOfflineCode() {
        val hash = "512c7b10aa572e9c7c43133a38e7d05546d8de64f3370382608b84363cac8246"
        assertEquals(hash, ServiceCodeHasher.sha256("MAXWMS-SERWIS-2026"))
        assertTrue(ServiceCodeHasher.matches("  MAXWMS-SERWIS-2026  ", hash))
        assertFalse(ServiceCodeHasher.matches("inny-kod", hash))
    }
}

class BaseUrlNormalizerTest {
    @Test
    fun addsSchemeAndSlash() {
        assertEquals("https://crm.firma.pl/", BaseUrlNormalizer.normalize("crm.firma.pl"))
        assertEquals("http://192.168.0.8:8080/", BaseUrlNormalizer.normalize("http://192.168.0.8:8080"))
        assertEquals("", BaseUrlNormalizer.normalize("  "))
        assertEquals(null, BaseUrlNormalizer.normalize("not a url"))
    }
}

class CatalogMergerTest {
    @Test
    fun keepsLocalQuantityWhileMovementsArePending() {
        val merged = CatalogMerger.merge(
            local = sample(quantity = 9, updated = 200),
            remote = sample(quantity = 20, updated = 300),
            hasPendingMovements = true,
        )
        assertEquals(9, merged.quantity)
        assertEquals(SyncStatus.PENDING, merged.syncStatus)
        assertEquals("local-id", merged.id)
    }

    @Test
    fun takesNewerRemoteQuantityWhenQueueIsEmpty() {
        val merged = CatalogMerger.merge(
            local = sample(quantity = 9, updated = 100),
            remote = sample(quantity = 12, updated = 200, name = "Pompa CRM", id = "remote-id"),
            hasPendingMovements = false,
        )
        assertEquals(12, merged.quantity)
        assertEquals("Pompa CRM", merged.name)
        assertEquals("local-id", merged.id)
        assertEquals(SyncStatus.SYNCED, merged.syncStatus)
    }

    @Test
    fun keepsNewerLocalQuantity() {
        val merged = CatalogMerger.merge(
            local = sample(quantity = 7, updated = 500),
            remote = sample(quantity = 4, updated = 100),
            hasPendingMovements = false,
        )
        assertEquals(7, merged.quantity)
        assertEquals("Strefa A -> Regał 1", merged.location)
    }

    @Test
    fun keepsLocationWhileEditIsWaitingForCrm() {
        val merged = CatalogMerger.merge(
            local = sample(quantity = 4, updated = 100, pendingChange = pl.maxwms.domain.model.PendingChange.UPDATE)
                .copy(location = "Strefa B -> Regał 9", zone = "Strefa B", rack = "Regał 9"),
            remote = sample(quantity = 20, updated = 900, name = "Z CRM"),
            hasPendingMovements = false,
        )
        assertEquals("Strefa B -> Regał 9", merged.location)
        assertEquals(4, merged.quantity)
        assertEquals("Pompa", merged.name)
    }

    private fun sample(
        quantity: Int,
        updated: Long,
        name: String = "Pompa",
        id: String = "local-id",
        pendingChange: pl.maxwms.domain.model.PendingChange = pl.maxwms.domain.model.PendingChange.NONE,
    ) = Part(
        id = id,
        sku = "PW-PUMP-150",
        barcode = "5900000000001",
        name = name,
        quantity = quantity,
        minQuantity = 2,
        location = "Strefa A -> Regał 1",
        zone = "Strefa A",
        rack = "Regał 1",
        lastUpdated = updated,
        syncStatus = SyncStatus.SYNCED,
        pendingChange = pendingChange,
    )
}
