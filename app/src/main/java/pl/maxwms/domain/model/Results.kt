package pl.maxwms.domain.model

sealed interface UnlockResult {
    data class Success(
        val offline: Boolean,
        val companyName: String?,
    ) : UnlockResult

    data object Invalid : UnlockResult

    data object Empty : UnlockResult

    data object Network : UnlockResult
}

sealed interface StockChangeResult {
    data class Success(val part: Part) : StockChangeResult

    data object NotFound : StockChangeResult

    data class Insufficient(val available: Int) : StockChangeResult
}

sealed interface SyncOutcome {
    data object Success : SyncOutcome

    data object NotConfigured : SyncOutcome

    data class Failed(val reason: SyncFailure) : SyncOutcome
}
