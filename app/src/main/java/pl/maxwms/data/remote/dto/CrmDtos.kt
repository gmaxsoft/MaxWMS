package pl.maxwms.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PartsResponse(
    val parts: List<PartDto> = emptyList(),
)

@Serializable
data class PartDto(
    val id: String = "",
    val sku: String = "",
    val barcode: String = "",
    val name: String = "",
    val quantity: Int = 0,
    val minQuantity: Int = 0,
    val location: String = "",
    val updatedAt: String? = null,
)

@Serializable
data class MovementsRequest(
    val movements: List<MovementDto>,
)

@Serializable
data class MovementDto(
    val clientId: String,
    val partId: String,
    val sku: String,
    val barcode: String,
    val delta: Int,
    val quantityAfter: Int,
    val reason: String,
    val occurredAt: String,
)

@Serializable
data class MovementsResponse(
    val accepted: List<String> = emptyList(),
    val rejected: List<RejectedMovement> = emptyList(),
)

@Serializable
data class RejectedMovement(
    val clientId: String = "",
    val message: String = "",
)

@Serializable
data class VerifyLicenseRequest(
    val serviceCode: String,
    val deviceId: String,
)

@Serializable
data class VerifyLicenseResponse(
    val valid: Boolean = false,
    val companyName: String? = null,
    val expiresAt: String? = null,
    val message: String? = null,
)
