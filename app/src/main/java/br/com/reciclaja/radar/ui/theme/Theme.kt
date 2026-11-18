package br.com.reciclaja.radar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.reciclaja.radar.domain.model.AlertLevel

// ---- Tokens de cor (fonte única de verdade visual; nenhuma cor solta nas telas) ----
object Palette {
    // Marca
    val Green = Color(0xFF2E9E5B)
    val GreenLight = Color(0xFF7FD8A0)
    val Teal = Color(0xFF00897B)
    val TealLight = Color(0xFF80CBC4)
    // Superfícies
    val SurfaceLight = Color(0xFFF7FAF6)
    val SurfaceDark = Color(0xFF101411)
    val CardLight = Color(0xFFFFFFFF)
    val CardDark = Color(0xFF1B211C)
    val InkLight = Color(0xFF17201A)
    val InkDark = Color(0xFFE3EAE4)
    val MutedLight = Color(0xFF5C6B60)
    val MutedDark = Color(0xFFA9B7AC)
    // Semânticas (texto/ícone sobre o card)
    val NormalLight = Color(0xFF43A047)
    val AttentionLight = Color(0xFFF2A900)
    val CriticalLight = Color(0xFFE53935)
    val InfoLight = Color(0xFF1E88E5)
    val NormalDark = Color(0xFF81C784)
    val AttentionDark = Color(0xFFFFCA28)
    val CriticalDark = Color(0xFFEF9A9A)
    val InfoDark = Color(0xFF90CAF9)
}

@Immutable
data class StatusColors(val normal: Color, val info: Color, val attention: Color, val critical: Color, val muted: Color) {
    fun of(level: AlertLevel?) = when (level) {
        null -> normal
        AlertLevel.INFORMATIVO -> info
        AlertLevel.ATENCAO -> attention
        AlertLevel.CRITICO -> critical
    }
}

val LocalStatusColors = staticCompositionLocalOf {
    StatusColors(Palette.NormalLight, Palette.InfoLight, Palette.AttentionLight, Palette.CriticalLight, Palette.MutedLight)
}

// ---- Espaçamento e forma (grade de 4 dp) ----
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
}

private val RadarShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
)

// ---- Tipografia enxuta: 6 níveis ----
private val RadarTypography = Typography(
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

private val LightColors = lightColorScheme(
    primary = Palette.Green, onPrimary = Color.White,
    secondary = Palette.Teal, onSecondary = Color.White,
    background = Palette.SurfaceLight, onBackground = Palette.InkLight,
    surface = Palette.CardLight, onSurface = Palette.InkLight,
    surfaceVariant = Color(0xFFE6EEE7), onSurfaceVariant = Palette.MutedLight,
    error = Palette.CriticalLight, onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Palette.GreenLight, onPrimary = Color(0xFF00391A),
    secondary = Palette.TealLight, onSecondary = Color(0xFF003731),
    background = Palette.SurfaceDark, onBackground = Palette.InkDark,
    surface = Palette.CardDark, onSurface = Palette.InkDark,
    surfaceVariant = Color(0xFF2A322C), onSurfaceVariant = Palette.MutedDark,
    error = Palette.CriticalDark, onError = Color(0xFF601410),
)

@Composable
fun RadarTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val status = if (dark)
        StatusColors(Palette.NormalDark, Palette.InfoDark, Palette.AttentionDark, Palette.CriticalDark, Palette.MutedDark)
    else
        StatusColors(Palette.NormalLight, Palette.InfoLight, Palette.AttentionLight, Palette.CriticalLight, Palette.MutedLight)
    CompositionLocalProvider(LocalStatusColors provides status) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = RadarTypography,
            shapes = RadarShapes,
            content = content,
        )
    }
}
