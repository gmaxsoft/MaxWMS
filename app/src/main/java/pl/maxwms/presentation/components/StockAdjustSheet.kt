package pl.maxwms.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import pl.maxwms.presentation.theme.GlossyButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.maxwms.R
import pl.maxwms.domain.model.Part

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustSheet(
    part: Part,
    message: String?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onAdjust: (Int) -> Unit,
    onEdit: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        StockAdjustContent(
            part = part,
            message = message,
            busy = busy,
            onAdjust = onAdjust,
            onEdit = onEdit,
        )
    }
}

@Composable
private fun StockAdjustContent(
    part: Part,
    message: String?,
    busy: Boolean,
    onAdjust: (Int) -> Unit,
    onEdit: () -> Unit,
) {
    var showCustom by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = part.name, style = MaterialTheme.typography.headlineMedium)
        Text(text = part.sku, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.scanner_location),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = part.location, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.scanner_stock),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = part.quantity.toString(),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = if (part.isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        if (part.isLowStock) {
            Text(
                text = stringResource(R.string.dashboard_low_stock),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (message != null) {
            Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        }
        GlossyButton(
            onClick = onEdit,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Text(stringResource(R.string.part_edit))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            AdjustButton("-1", busy, Modifier.weight(1f)) { onAdjust(-1) }
            AdjustButton("-5", busy, Modifier.weight(1f)) { onAdjust(-5) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            AdjustButton("+1", busy, Modifier.weight(1f)) { onAdjust(1) }
            AdjustButton("+10", busy, Modifier.weight(1f)) { onAdjust(10) }
        }
        GlossyButton(
            onClick = { showCustom = true },
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            Text(stringResource(R.string.scanner_custom))
        }
    }
    if (showCustom) {
        CustomQuantityDialog(
            onDismiss = { showCustom = false },
            onConfirm = { delta ->
                showCustom = false
                onAdjust(delta)
            },
        )
    }
}

@Composable
private fun AdjustButton(
    label: String,
    busy: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlossyButton(
        onClick = onClick,
        enabled = !busy,
        modifier = modifier.height(64.dp),
    ) {
        Text(text = label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CustomQuantityDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var issue by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.scanner_custom)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = issue,
                        onClick = { issue = true },
                        label = { Text(stringResource(R.string.scanner_issue)) },
                    )
                    FilterChip(
                        selected = !issue,
                        onClick = { issue = false },
                        label = { Text(stringResource(R.string.scanner_receipt)) },
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { char -> char.isDigit() }.take(6)
                        error = false
                    },
                    label = { Text(stringResource(R.string.scanner_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error) {
                    Text(
                        text = stringResource(R.string.scanner_invalid_amount),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            GlossyButton(
                onClick = {
                    val value = amount.toIntOrNull()
                    if (value == null || value <= 0) {
                        error = true
                    } else {
                        onConfirm(if (issue) -value else value)
                    }
                },
                modifier = Modifier.height(56.dp),
            ) {
                Text(stringResource(R.string.scanner_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(56.dp)) {
                Text(stringResource(R.string.paywall_cancel))
            }
        },
    )
}
