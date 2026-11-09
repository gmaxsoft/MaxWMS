package pl.maxwms.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import pl.maxwms.presentation.theme.glossyEdge
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import pl.maxwms.presentation.theme.GlossyButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import pl.maxwms.presentation.theme.GlossyFab
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.maxwms.R
import pl.maxwms.domain.model.AccessSource
import pl.maxwms.domain.model.AccessState
import pl.maxwms.presentation.components.PartRow
import pl.maxwms.presentation.components.StockAdjustSheet
import pl.maxwms.presentation.util.formatDateTime
import pl.maxwms.presentation.util.syncFailureMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onScan: () -> Unit,
    onParts: () -> Unit,
    onLowStock: () -> Unit,
    onAddPart: () -> Unit,
    onEditPart: (String) -> Unit,
    onSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val saved = stringResource(R.string.scanner_saved)
    val syncSuccess = stringResource(R.string.sync_success)
    val syncMissing = stringResource(R.string.sync_not_configured)
    val syncFailed = (state.notice as? DashboardNotice.SyncFailed)?.reason?.let { syncFailureMessage(it) }
    LaunchedEffect(state.notice, saved, syncSuccess, syncMissing, syncFailed) {
        val text = when (state.notice) {
            DashboardNotice.Saved -> saved
            DashboardNotice.SyncSuccess -> syncSuccess
            DashboardNotice.SyncNotConfigured -> syncMissing
            is DashboardNotice.SyncFailed -> syncFailed
            is DashboardNotice.Insufficient, null -> null
        }
        if (text != null) {
            snackbar.showSnackbar(text)
            viewModel.consumeNotice()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = viewModel::sync, enabled = !state.syncing) {
                        Icon(Icons.Filled.Sync, contentDescription = stringResource(R.string.cd_sync))
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.cd_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            GlossyFab(onClick = onScan, modifier = Modifier.height(72.dp)) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.cd_scan))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (state.syncing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = if (state.online) stringResource(R.string.dashboard_online) else stringResource(R.string.dashboard_offline),
                    color = if (state.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = state.lastSyncAt?.let { stringResource(R.string.dashboard_last_sync, formatDateTime(it)) }
                        ?: stringResource(R.string.dashboard_never_synced),
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.syncFailure?.let { failure ->
                    syncFailureMessage(failure)?.let { message ->
                        Text(text = message, color = MaterialTheme.colorScheme.error)
                    }
                }
                val license = when (val access = state.access) {
                    is AccessState.Unlocked -> when (access.source) {
                        AccessSource.CRM_LICENSE -> stringResource(R.string.access_crm)
                        AccessSource.PLAY_SUBSCRIPTION -> stringResource(R.string.access_play)
                    }
                    else -> null
                }
                if (license != null) {
                    Text(text = license, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = stringResource(R.string.dashboard_pending, state.pendingCount),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (state.lowStockCount > 0) {
                item {
                    Card(
                        onClick = onLowStock,
                        modifier = Modifier.glossyEdge(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.dashboard_low_stock), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.dashboard_low_stock_count, state.lowStockCount))
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.dashboard_search)) },
                    singleLine = true,
                )
            }
            item {
                GlossyButton(onClick = onAddPart, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    Text(stringResource(R.string.part_add))
                }
            }
            item {
                GlossyButton(onClick = onParts, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(stringResource(R.string.dashboard_parts))
                }
            }
            if (state.parts.isEmpty()) {
                item {
                    Text(
                        text = if (state.query.isBlank()) {
                            stringResource(R.string.dashboard_empty)
                        } else {
                            stringResource(R.string.dashboard_no_results)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(state.parts, key = { it.id }) { part ->
                PartRow(part = part, onClick = { viewModel.openPart(part.id) })
            }
        }
        }
    }
    val selected = state.selected
    if (selected != null) {
        val sheetMessage = (state.notice as? DashboardNotice.Insufficient)?.let {
            stringResource(R.string.scanner_insufficient, it.available)
        }
        StockAdjustSheet(
            part = selected,
            message = sheetMessage,
            busy = state.adjusting,
            onDismiss = viewModel::dismissPart,
            onAdjust = viewModel::adjust,
            onEdit = {
                viewModel.dismissPart()
                onEditPart(selected.id)
            },
        )
    }
}
