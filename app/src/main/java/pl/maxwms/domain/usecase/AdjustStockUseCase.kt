package pl.maxwms.domain.usecase

import pl.maxwms.domain.model.MovementType
import pl.maxwms.domain.model.StockChangeResult
import pl.maxwms.domain.repository.PartRepository
import javax.inject.Inject

class AdjustStockUseCase @Inject constructor(
    private val repository: PartRepository,
) {
    suspend operator fun invoke(
        partId: String,
        delta: Int,
        type: MovementType? = null,
    ): StockChangeResult {
        val resolved = type ?: if (delta < 0) MovementType.ISSUE else MovementType.RECEIPT
        return repository.adjust(partId, delta, resolved)
    }
}
