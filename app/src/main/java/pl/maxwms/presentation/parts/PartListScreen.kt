package pl.maxwms.presentation.parts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import pl.maxwms.presentation.theme.GlossyFab
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.maxwms.R
import pl.maxwms.presentation.components.PartRow
import pl.maxwms.presentation.components.StockAdjustSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartListScreen(
    initialFilter: String,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onAddPart: () -> Unit,
    onEditPart: (String) -> Unit,
    viewModel: PartListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialFilter) {
        viewModel.applyInitialFilter(initialFilter)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.parts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    TextButton(onClick = onAddPart) {
                        Text(stringResource(R.string.part_add))
                    }
                },
            )
        },
        floatingActionButton = {
            GlossyFab(onClick = onScan) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.cd_scan))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.parts_search)) },
                    singleLine = true,
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.lowStockOnly,
                            onClick = viewModel::toggleLowStock,
                            label = { Text(stringResource(R.string.parts_filter_low)) },
                        )
                    }
                    items(state.zones) { zone ->
                        FilterChip(
                            selected = state.zone == zone,
                            onClick = { viewModel.selectZone(zone) },
                            label = { Text(zone) },
                        )
                    }
                }
            }
            if (state.racks.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.racks) { rack ->
                            FilterChip(
                                selected = state.rack == rack,
                                onClick = { viewModel.selectRack(rack) },
                                label = { Text(rack) },
                            )
                        }
                    }
                }
            }
            if (state.parts.isEmpty()) {
                item { Text(stringResource(R.string.parts_empty)) }
            }
            items(state.parts, key = { it.id }) { part ->
                PartRow(part = part, onClick = { viewModel.openPart(part.id) })
            }
        }
    }
    val selected = state.selected
    if (selected != null) {
        StockAdjustSheet(
            part = selected,
            message = state.insufficient?.let { stringResource(R.string.scanner_insufficient, it) },
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
