package pl.maxwms.data.local

import pl.maxwms.data.local.entity.PartEntity
import pl.maxwms.domain.logic.LocationParser
import pl.maxwms.domain.model.SyncStatus

object SampleCatalog {
    fun entities(now: Long = System.currentTimeMillis()): List<PartEntity> = listOf(
        part(
            id = "11111111-1111-4111-8111-111111111111",
            sku = "PW-PUMP-150",
            barcode = "5900000000001",
            name = "Pompa wysokociśnieniowa 150 bar",
            quantity = 4,
            minQuantity = 5,
            location = "Strefa A -> Regał 1 -> Półka 1 -> Pojemnik 1",
            now = now,
        ),
        part(
            id = "22222222-2222-4222-8222-222222222222",
            sku = "GEN-ENG-65",
            barcode = "5900000000002",
            name = "Silnik spalinowy 6,5 KM",
            quantity = 8,
            minQuantity = 2,
            location = "Strefa A -> Regał 1 -> Półka 2 -> Pojemnik 1",
            now = now,
        ),
        part(
            id = "33333333-3333-4333-8333-333333333333",
            sku = "PW-HOSE-10",
            barcode = "5900000000003",
            name = "Wąż ciśnieniowy 10 m",
            quantity = 2,
            minQuantity = 6,
            location = "Strefa A -> Regał 2 -> Półka 1 -> Pojemnik 3",
            now = now,
        ),
        part(
            id = "44444444-4444-4444-8444-444444444444",
            sku = "PW-NOZ-035",
            barcode = "5900000000004",
            name = "Dysza rotacyjna 035",
            quantity = 40,
            minQuantity = 10,
            location = "Strefa B -> Regał 1 -> Półka 1 -> Pojemnik 1",
            now = now,
        ),
        part(
            id = "55555555-5555-4555-8555-555555555555",
            sku = "PW-GUN-01",
            barcode = "5900000000005",
            name = "Pistolet wysokociśnieniowy",
            quantity = 15,
            minQuantity = 4,
            location = "Strefa B -> Regał 1 -> Półka 2 -> Pojemnik 2",
            now = now,
        ),
        part(
            id = "66666666-6666-4666-8666-666666666666",
            sku = "GEN-FRAME-01",
            barcode = "5900000000006",
            name = "Rama agregatu",
            quantity = 3,
            minQuantity = 2,
            location = "Strefa C -> Regał 3 -> Półka 1 -> Pojemnik 1",
            now = now,
        ),
        part(
            id = "77777777-7777-4777-8777-777777777777",
            sku = "GEN-ALT-3K",
            barcode = "5900000000007",
            name = "Alternator 3 kW",
            quantity = 1,
            minQuantity = 2,
            location = "Strefa C -> Regał 3 -> Półka 2 -> Pojemnik 1",
            now = now,
        ),
        part(
            id = "88888888-8888-4888-8888-888888888888",
            sku = "GEN-TANK-15",
            barcode = "5900000000008",
            name = "Zbiornik paliwa 15 l",
            quantity = 12,
            minQuantity = 4,
            location = "Strefa C -> Regał 2 -> Półka 3 -> Pojemnik 4",
            now = now,
        ),
        part(
            id = "99999999-9999-4999-8999-999999999999",
            sku = "PW-SEAL-01",
            barcode = "5900000000009",
            name = "Uszczelka pompy",
            quantity = 80,
            minQuantity = 20,
            location = "Strefa A -> Regał 2 -> Półka 3 -> Pojemnik 1",
            now = now,
        ),
        part(
            id = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
            sku = "GEN-PANEL-01",
            barcode = "5900000000010",
            name = "Panel sterowania agregatu",
            quantity = 6,
            minQuantity = 3,
            location = "Strefa B -> Regał 2 -> Półka 1 -> Pojemnik 1",
            now = now,
        ),
    )

    private fun part(
        id: String,
        sku: String,
        barcode: String,
        name: String,
        quantity: Int,
        minQuantity: Int,
        location: String,
        now: Long,
    ): PartEntity {
        val parsed = LocationParser.parse(location)
        return PartEntity(
            id = id,
            sku = sku,
            barcode = barcode,
            name = name,
            quantity = quantity,
            minQuantity = minQuantity,
            location = location,
            zone = parsed.zone,
            rack = parsed.rack,
            lastUpdated = now,
            syncStatus = SyncStatus.SYNCED.name,
        )
    }
}
