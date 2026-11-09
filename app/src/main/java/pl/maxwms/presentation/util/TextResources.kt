package pl.maxwms.presentation.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.maxwms.R
import pl.maxwms.domain.model.MovementType
import pl.maxwms.domain.model.SyncFailure

@Composable
fun syncFailureMessage(failure: SyncFailure?): String? {
    failure ?: return null
    return stringResource(failure.toStringRes())
}

@StringRes
fun SyncFailure.toStringRes(): Int = when (this) {
    SyncFailure.INVALID_URL -> R.string.sync_error_invalid_url
    SyncFailure.UNAUTHORIZED -> R.string.sync_error_unauthorized
    SyncFailure.SERVER -> R.string.sync_error_server
    SyncFailure.NETWORK -> R.string.sync_error_network
    SyncFailure.FORMAT -> R.string.sync_error_format
    SyncFailure.UNKNOWN -> R.string.sync_error_unknown
}

@StringRes
fun MovementType.toStringRes(): Int = when (this) {
    MovementType.ISSUE -> R.string.movement_issue
    MovementType.RECEIPT -> R.string.movement_receipt
    MovementType.ADJUSTMENT -> R.string.movement_adjustment
}
