package pl.maxwms.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GlossyBlack = darkColorScheme(
    primary = Color(0xFFE10600),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A0000),
    onPrimaryContainer = Color(0xFFFFD4D4),
    secondary = Color(0xFFFF3B3B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A0000),
    onSecondaryContainer = Color.White,
    background = Color(0xFF000000),
    onBackground = Color(0xFFF4F4F4),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFF7F7F7),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFD2D2D2),
    error = Color(0xFFFF4D4D),
    onError = Color.White,
    errorContainer = Color(0xFF2C0000),
    onErrorContainer = Color(0xFFFFD6D6),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF242424),
)

private val WarehouseType = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp),
    bodyMedium = TextStyle(fontSize = 16.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun MaxWmsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GlossyBlack,
        typography = WarehouseType,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(22.dp),
        ),
        content = content,
    )
}
