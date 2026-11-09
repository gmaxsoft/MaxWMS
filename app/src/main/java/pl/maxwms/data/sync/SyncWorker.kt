package pl.maxwms.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import pl.maxwms.domain.model.SyncFailure
import pl.maxwms.domain.model.SyncOutcome
import pl.maxwms.domain.repository.CatalogSync

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val catalogSync: CatalogSync,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return when (val outcome = catalogSync.sync()) {
            SyncOutcome.Success, SyncOutcome.NotConfigured -> Result.success()
            is SyncOutcome.Failed -> when (outcome.reason) {
                SyncFailure.NETWORK, SyncFailure.UNKNOWN -> Result.retry()
                else -> Result.failure()
            }
        }
    }
}
