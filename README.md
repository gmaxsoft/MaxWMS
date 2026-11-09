# MaxWMS

Offline-first warehouse app for the factory floor. Stock changes are written to Room immediately and queued for the CRM. The interface is Polish. High-contrast Material 3, large touch targets, light and dark theme.

## Stack

| Piece | Version | Why this one |
| --- | --- | --- |
| AGP | 9.3.1 | Newest AGP in the Kotlin 2.4.20 compatibility matrix. API 36 is supported. |
| Gradle | 9.5.0 | Minimum required by AGP 9.3. |
| Kotlin / KSP | 2.4.20 / 2.3.12 | Built-in Kotlin (no `kotlin-android` plugin). |
| Compose BOM | 2026.09.00 | Material 3. |
| Room | 2.8.5 | Local source of truth. |
| WorkManager | 2.12.0 | Sync when a network is available. |
| Play Billing | 9.1.0 | Monthly product `wms_pro_monthly`. |
| CameraX / ML Kit | 1.6.2 / 17.3.0 | Continuous barcode and QR scanning. |

`minSdk` is 26 and `targetSdk` is 36. `compileSdk` is 37 because the current Jetpack libraries require it at compile time; runtime behavior stays on API 36.

## Build

JDK 17 and Android SDK 36 are required.

```bat
gradlew.bat test
gradlew.bat assembleDebug
```

Release builds are minified with R8. Override the offline license hash without committing the plaintext code:

```properties
maxwmsOfflineServiceCodeSha256=<sha256 hex>
```

Put that in `~/.gradle/gradle.properties` or pass `-PmaxwmsOfflineServiceCodeSha256=...`.

## License

On launch the app asks Play Billing whether `wms_pro_monthly` is active. Create that subscription in Play Console (about 25 PLN / month).

The paywall action **Mam klucz licencyjny CRM / Kod serwisowy** calls `POST /api/wms/verify-license`. A CRM rejection is final. If the CRM is unreachable, or no base URL is saved yet, the app accepts one offline code and stores the unlock in `EncryptedSharedPreferences`.

Development fallback code: `MAXWMS-SERWIS-2026`.

Its SHA-256 is `512c7b10aa572e9c7c43133a38e7d05546d8de64f3370382608b84363cac8246`. Replace the hash before a production rollout. A CRM unlock skips Play Billing entirely. Clearing it in Settings returns the user to the paywall; the API key stays.

## CRM contract

Base URL is the server root, for example `https://crm.firma.pl` or `http://192.168.0.10:8080` for a hall LAN. Paths below are appended to that root. The client sends `Authorization: Bearer <apiKey>` and `X-Api-Key`.

Unknown JSON fields are ignored. This is the shape the app speaks today — send the real CRM schema if the field names differ and the DTOs in `data/remote/dto/CrmDtos.kt` can be aligned.

### `GET /api/wms/parts`

```json
{
  "parts": [
    {
      "id": "crm-1",
      "sku": "PW-PUMP-150",
      "barcode": "5900000000001",
      "name": "Pompa wysokociśnieniowa 150 bar",
      "quantity": 12,
      "minQuantity": 5,
      "location": "Strefa A -> Regał 2 -> Półka 3 -> Pojemnik 1",
      "updatedAt": "2026-11-09T12:00:00Z"
    }
  ]
}
```

`location` uses `->` between zone, rack, shelf and bin. The first segment is the zone filter, the second is the rack filter.

### `POST /api/wms/movements`

```json
{
  "movements": [
    {
      "clientId": "uuid",
      "partId": "uuid",
      "sku": "PW-PUMP-150",
      "barcode": "5900000000001",
      "delta": -1,
      "quantityAfter": 11,
      "reason": "ISSUE",
      "occurredAt": "2026-11-09T12:04:00Z"
    }
  ]
}
```

`reason` is `ISSUE` (wydanie), `RECEIPT` (przyjęcie) or `ADJUSTMENT`.

```json
{ "accepted": ["uuid"], "rejected": [{ "clientId": "uuid", "message": "unknown sku" }] }
```

An HTTP 200 with empty `accepted` and `rejected` marks the whole batch as accepted.

### `POST /api/wms/parts` and `PUT /api/wms/parts/{id}`

Creating or editing a part sends the same object as one element of `GET /api/wms/parts`. `POST` creates, `PUT` updates. `quantity` is the absolute stock after the edit, not a delta. A successful empty body is enough. If the CRM is not configured, the part stays on the device. If the call fails, it stays queued and the next sync retries it.

```json
{
  "id": "11111111-1111-4111-8111-111111111111",
  "sku": "PW-PUMP-150",
  "barcode": "5900000000001",
  "name": "Pompa wysokociśnieniowa 150 bar",
  "quantity": 12,
  "minQuantity": 5,
  "location": "Strefa A -> Regał 2 -> Półka 3 -> Pojemnik 1",
  "updatedAt": "2026-11-09T12:00:00Z"
}
```

### `POST /api/wms/verify-license`

```json
{ "serviceCode": "token", "deviceId": "android-id" }
```

```json
{ "valid": true, "companyName": "Zakład", "expiresAt": "2027-01-01T00:00:00Z" }
```

## Sync rules

1. Every stock change is a Room transaction: quantity updates and a `PENDING` movement is inserted together.
2. WorkManager sends the queue when a network is up, and also every 15 minutes. Settings can sync immediately.
3. Parts are matched by barcode, then SKU, then id. The local id is kept so queued movements stay attached.
4. While a movement or a part edit is still pending, the device copy wins. A CRM snapshot replaces the part only when its `updatedAt` is at least as new as the local change and nothing is queued.
5. An empty parts response does not delete the local catalog.
6. `POST /api/wms/parts` and `PUT /api/wms/parts/{id}` send the absolute quantity. Pending scanner deltas for that same part are not sent again in the same sync, so the CRM does not apply the change twice.

Cleartext HTTP is allowed because many hall servers have no TLS. Settings shows a warning when the URL starts with `http://`.

The first launch seeds ten sample parts (pressure washers and generators) so the scanner works before the CRM is connected. Settings can add any missing samples again. They merge by SKU and are not duplicated.
