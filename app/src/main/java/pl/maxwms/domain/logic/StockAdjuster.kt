package pl.maxwms.domain.logic

sealed interface AdjustmentResult {
    data class Applied(val quantity: Int) : AdjustmentResult

    data class Insufficient(
        val available: Int,
        val requestedDelta: Int,
    ) : AdjustmentResult
}

object StockAdjuster {
    fun apply(current: Int, delta: Int): AdjustmentResult {
        val next = current + delta
        return if (next < 0) {
            AdjustmentResult.Insufficient(current, delta)
        } else {
            AdjustmentResult.Applied(next)
        }
    }
}
