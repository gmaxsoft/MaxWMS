package pl.maxwms

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pl.maxwms.data.billing.BillingManager
import pl.maxwms.data.seed.CatalogSeeder
import pl.maxwms.data.sync.SyncScheduler
import pl.maxwms.di.ApplicationScope
import javax.inject.Inject

@HiltAndroidApp
class MaxWmsApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var billingManager: BillingManager

    @Inject lateinit var syncScheduler: SyncScheduler

    @Inject lateinit var catalogSeeder: CatalogSeeder

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        billingManager.start()
        syncScheduler.ensurePeriodic()
        appScope.launch {
            runCatching { catalogSeeder.seedIfNeeded() }
        }
    }
}
