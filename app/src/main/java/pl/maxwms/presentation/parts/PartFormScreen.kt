package pl.maxwms.presentation.parts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import pl.maxwms.presentation.theme.GlossyButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.maxwms.R
import pl.maxwms.domain.model.PartDelivery

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartFormScreen(
    onBack: () -> Unit,
    viewModel: PartFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val sent = stringResource(R.string.part_saved_sent)
    val localOnly = stringResource(R.string.part_saved_local)
    val queued = stringResource(R.string.part_saved_queued)
    val rejected = stringResource(R.string.part_saved_rejected)
    LaunchedEffect(viewModel) {
        viewModel.saved.collect { delivery ->
            val text = when (delivery) {
                PartDelivery.SENT -> sent
                PartDelivery.LOCAL_ONLY -> localOnly
                PartDelivery.QUEUED -> queued
                PartDelivery.REJECTED -> rejected
            }
            snackbar.showSnackbar(text)
            onBack()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (state.isEdit) R.string.part_form_edit else R.string.part_form_new),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.loading -> CircularProgressIndicator(modifier = Modifier.padding(padding).padding(24.dp))
            state.missing -> Text(
                text = stringResource(R.string.part_error_missing),
                modifier = Modifier.padding(padding).padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PartField(state.name, viewModel::onName, stringResource(R.string.part_name))
                PartField(state.sku, viewModel::onSku, stringResource(R.string.part_sku))
                PartField(state.barcode, viewModel::onBarcode, stringResource(R.string.part_barcode))
                PartField(
                    value = state.quantity,
                    onValue = viewModel::onQuantity,
                    label = stringResource(R.string.part_quantity),
                    number = true,
                )
                PartField(
                    value = state.minQuantity,
                    onValue = viewModel::onMinQuantity,
                    label = stringResource(R.string.part_min),
                    number = true,
                )
                PartField(
                    value = state.location,
                    onValue = viewModel::onLocation,
                    label = stringResource(R.string.part_location),
                    hint = stringResource(R.string.part_location_hint),
                )
                state.message?.let { message ->
                    Text(
                        text = stringResource(message.toText()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                GlossyButton(
                    onClick = viewModel::save,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    Text(stringResource(R.string.part_save))
                }
            }
        }
    }
}

@Composable
private fun PartField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    hint: String? = null,
    number: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = hint?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (number) KeyboardType.Number else KeyboardType.Text,
        ),
    )
}

private fun PartFormMessage.toText(): Int = when (this) {
    PartFormMessage.NAME -> R.string.part_error_name
    PartFormMessage.SKU -> R.string.part_error_sku
    PartFormMessage.BARCODE -> R.string.part_error_barcode
    PartFormMessage.QUANTITY -> R.string.part_error_quantity
    PartFormMessage.MIN -> R.string.part_error_min
    PartFormMessage.DUPLICATE_BARCODE -> R.string.part_error_duplicate_barcode
    PartFormMessage.DUPLICATE_SKU -> R.string.part_error_duplicate_sku
    PartFormMessage.NOT_FOUND -> R.string.part_error_missing
}
