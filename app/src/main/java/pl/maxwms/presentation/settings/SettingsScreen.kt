package pl.maxwms.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import pl.maxwms.presentation.theme.GlossyButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.maxwms.R
import pl.maxwms.domain.model.AccessSource
import pl.maxwms.domain.model.AccessState
import pl.maxwms.domain.model.SyncStatus
import pl.maxwms.presentation.util.formatDateTime
import pl.maxwms.presentation.util.formatDelta
import pl.maxwms.presentation.util.syncFailureMessage
import pl.maxwms.presentation.util.toStringRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val saved = stringResource(R.string.settings_saved)
    val invalid = stringResource(R.string.settings_invalid_url)
    val samples = stringResource(R.string.settings_samples_done)
    val synced = stringResource(R.string.sync_success)
    val missing = stringResource(R.string.sync_not_configured)
    val failed = (state.notice as? SettingsNotice.SyncFailed)?.let { syncFailureMessage(it.reason) }
    LaunchedEffect(state.notice, failed) {
        val text = when (state.notice) {
            SettingsNotice.Saved -> saved
            SettingsNotice.InvalidUrl -> invalid
            SettingsNotice.SamplesAdded -> samples
            SettingsNotice.SyncSuccess -> synced
            SettingsNotice.SyncNotConfigured -> missing
            is SettingsNotice.SyncFailed -> failed
            null -> null
        }
        if (text != null) {
            snackbar.showSnackbar(text)
            viewModel.consumeNotice()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionTitle(stringResource(R.string.settings_crm_section)) }
            item {
                OutlinedTextField(
                    value = state.draftUrl,
                    onValueChange = viewModel::onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_base_url)) },
                    placeholder = { Text(stringResource(R.string.settings_base_url_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }
            if (state.draftUrl.trim().startsWith("http://")) {
                item {
                    Text(
                        text = stringResource(R.string.settings_http_warning),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = state.draftKey,
                    onValueChange = viewModel::onKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_api_key)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
            item {
                GlossyButton(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    Text(stringResource(R.string.settings_save))
                }
            }
            item { SectionTitle(stringResource(R.string.settings_sync_section)) }
            item {
                Text(
                    text = state.settings.lastSyncAt?.let { stringResource(R.string.dashboard_last_sync, formatDateTime(it)) }
                        ?: stringResource(R.string.dashboard_never_synced),
                )
                state.settings.syncFailure?.let { failure ->
                    Text(text = stringResource(failure.toStringRes()), color = MaterialTheme.colorScheme.error)
                }
                Text(text = stringResource(R.string.dashboard_pending, state.pendingCount))
            }
            if (state.syncing) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            item {
                GlossyButton(
                    onClick = viewModel::sync,
                    enabled = !state.syncing,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    Text(stringResource(R.string.settings_sync_now))
                }
            }
            item { SectionTitle(stringResource(R.string.settings_queue)) }
            if (state.movements.isEmpty()) {
                item { Text(stringResource(R.string.settings_queue_empty)) }
            } else {
                items(count = state.movements.size, key = { state.movements[it].id }) { index ->
                    val movement = state.movements[index]
                    val status = if (movement.syncStatus == SyncStatus.PENDING) {
                        stringResource(R.string.parts_pending)
                    } else {
                        movement.lastError.orEmpty()
                    }
                    Text(
                        text = stringResource(
                            R.string.settings_movement_line,
                            formatDelta(movement.delta),
                            movement.sku,
                            movement.quantityAfter,
                        ) + if (status.isNotBlank()) " · $status" else "",
                    )
                    Text(
                        text = "${stringResource(movement.type.toStringRes())} · ${formatDateTime(movement.occurredAt)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { SectionTitle(stringResource(R.string.settings_license_section)) }
            item {
                val label = when (val access = state.access) {
                    is AccessState.Unlocked -> when (access.source) {
                        AccessSource.CRM_LICENSE -> stringResource(R.string.settings_license_crm)
                        AccessSource.PLAY_SUBSCRIPTION -> stringResource(R.string.settings_license_play)
                    }
                    else -> stringResource(R.string.paywall_title)
                }
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                state.settings.companyName?.takeIf { it.isNotBlank() }?.let { company ->
                    Text(stringResource(R.string.settings_license_company, company))
                }
            }
            item {
                TextButton(onClick = viewModel::askClearLicense, modifier = Modifier.height(56.dp)) {
                    Text(stringResource(R.string.settings_clear_license))
                }
            }
            item {
                GlossyButton(onClick = viewModel::loadSamples, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    Text(stringResource(R.string.settings_samples))
                }
            }
        }
    }
    if (state.confirmClear) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClear,
            title = { Text(stringResource(R.string.settings_clear_title)) },
            text = { Text(stringResource(R.string.settings_clear_body)) },
            confirmButton = {
                GlossyButton(onClick = viewModel::clearLicense) { Text(stringResource(R.string.settings_clear_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissClear) { Text(stringResource(R.string.paywall_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleLarge)
}
