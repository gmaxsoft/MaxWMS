package pl.maxwms.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.maxwms.R
import pl.maxwms.domain.model.Part
import pl.maxwms.domain.model.SyncStatus
import pl.maxwms.presentation.theme.glossyEdge

@Composable
fun PartRow(
    part: Part,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (part.isLowStock) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val content = if (part.isLowStock) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().glossyEdge(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = part.name, style = MaterialTheme.typography.titleMedium)
                Text(text = part.sku, style = MaterialTheme.typography.bodyMedium)
                Text(text = part.location, style = MaterialTheme.typography.bodyMedium)
                if (part.isLowStock || part.syncStatus == SyncStatus.PENDING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (part.isLowStock) {
                            StatusPill(text = stringResource(R.string.dashboard_low_stock))
                        }
                        if (part.syncStatus == SyncStatus.PENDING) {
                            StatusPill(text = stringResource(R.string.parts_pending))
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = part.quantity.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = stringResource(R.string.parts_min, part.minQuantity))
            }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
