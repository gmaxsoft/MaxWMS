package pl.maxwms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import pl.maxwms.data.billing.BillingManager
import pl.maxwms.presentation.MaxWmsRoot
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaxWmsRoot() }
    }

    override fun onResume() {
        super.onResume()
        billingManager.refresh()
    }
}
