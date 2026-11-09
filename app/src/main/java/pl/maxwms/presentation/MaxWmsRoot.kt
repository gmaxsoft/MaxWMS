package pl.maxwms.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.maxwms.R
import pl.maxwms.domain.model.AccessState
import pl.maxwms.presentation.navigation.MaxWmsNavHost
import pl.maxwms.presentation.paywall.PaywallScreen
import pl.maxwms.presentation.theme.MaxWmsTheme

@Composable
fun MaxWmsRoot(
    accessViewModel: AccessViewModel = hiltViewModel(),
) {
    val access by accessViewModel.access.collectAsStateWithLifecycle()
    MaxWmsTheme {
        when (access) {
            AccessState.Checking -> CheckingLicense()
            AccessState.Locked -> PaywallScreen()
            is AccessState.Unlocked -> MaxWmsNavHost()
        }
    }
}

@Composable
private fun CheckingLicense() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.checking_license),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
