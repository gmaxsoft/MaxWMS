package pl.maxwms.presentation.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import pl.maxwms.presentation.theme.GlossyButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.maxwms.R
import pl.maxwms.presentation.util.findActivity

@Composable
fun PaywallScreen(viewModel: PaywallViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.paywall_title), style = MaterialTheme.typography.headlineMedium)
        Text(text = stringResource(R.string.paywall_body), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = state.price?.let { stringResource(R.string.paywall_price, it) }
                ?: stringResource(R.string.paywall_price_fallback),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        state.error?.let { error ->
            Text(
                text = stringResource(error.toMessage()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        GlossyButton(
            onClick = { viewModel.subscribe { viewModel.launchBilling(context.findActivity()) } },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Text(stringResource(R.string.paywall_subscribe))
        }
        TextButton(
            onClick = viewModel::openCodeDialog,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Text(stringResource(R.string.paywall_service_code))
        }
    }
    if (state.showCodeDialog) {
        ServiceCodeDialog(
            loading = state.loading,
            error = state.error,
            onDismiss = viewModel::dismissCodeDialog,
            onSubmit = viewModel::submitCode,
        )
    }
}

@Composable
private fun ServiceCodeDialog(
    loading: Boolean,
    error: PaywallError?,
    onDismiss: () -> Unit,
    onSubmit: (String, Boolean) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var saveAsKey by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(stringResource(R.string.paywall_code_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.paywall_code_body))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.paywall_code_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = saveAsKey, onCheckedChange = { saveAsKey = it })
                    Text(stringResource(R.string.paywall_code_save_as_key))
                }
                if (error != null) {
                    Text(stringResource(error.toMessage()), color = MaterialTheme.colorScheme.error)
                }
                if (loading) {
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            GlossyButton(
                onClick = { onSubmit(code, saveAsKey) },
                enabled = !loading,
                modifier = Modifier.height(56.dp),
            ) {
                Text(stringResource(R.string.paywall_unlock))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading, modifier = Modifier.height(56.dp)) {
                Text(stringResource(R.string.paywall_cancel))
            }
        },
    )
}

private fun PaywallError.toMessage(): Int = when (this) {
    PaywallError.INVALID -> R.string.paywall_error_invalid
    PaywallError.NETWORK -> R.string.paywall_error_network
    PaywallError.EMPTY -> R.string.paywall_error_empty
    PaywallError.BILLING -> R.string.paywall_error_billing
    PaywallError.UNAVAILABLE -> R.string.paywall_billing_unavailable
    PaywallError.OWNED -> R.string.paywall_billing_owned
}
