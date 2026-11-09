package pl.maxwms.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun Modifier.glossyEdge(): Modifier = border(
    width = 1.dp,
    brush = Brush.verticalGradient(
        0.0f to Color.White.copy(alpha = 0.34f),
        1.0f to Color.White.copy(alpha = 0.05f),
    ),
    shape = RoundedCornerShape(16.dp),
)

private val GlossyRed = Brush.verticalGradient(
    0.0f to Color(0xFFFF7A7A),
    0.42f to Color(0xFFE10600),
    1.0f to Color(0xFF7A0000),
)

private val GlossyDisabled = Brush.verticalGradient(
    0.0f to Color(0xFF4A4A4A),
    1.0f to Color(0xFF222222),
)

private val GlossStroke = Brush.verticalGradient(
    0.0f to Color.White.copy(alpha = 0.7f),
    0.35f to Color.White.copy(alpha = 0.12f),
    1.0f to Color.Black.copy(alpha = 0.55f),
)

@Composable
fun GlossyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = Color.White.copy(alpha = 0.55f),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp,
        ),
        modifier = modifier
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = shape,
                ambientColor = Color(0x88FF1A1A),
                spotColor = Color(0xFFFF2D2D),
            )
            .background(if (enabled) GlossyRed else GlossyDisabled, shape)
            .border(1.dp, GlossStroke, shape),
        content = content,
    )
}

@Composable
fun GlossyFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    FloatingActionButton(
        onClick = onClick,
        shape = shape,
        containerColor = Color.Transparent,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
        ),
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = Color(0xAAFF1A1A),
                spotColor = Color(0xFFFF2D2D),
            )
            .background(GlossyRed, shape)
            .border(1.dp, GlossStroke, shape),
        content = content,
    )
}
