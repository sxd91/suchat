package io.github.sxd91.suchat.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class SuchatThemeMode { System, Light, Dark }

data class SuchatAppearance(
    val themeMode: SuchatThemeMode = SuchatThemeMode.System,
    val glassMode: String = "LiquidGlass",
    val performance: String = "Full",
    val transition: String = "Shared Element",
)

private val AuroraLight = lightColorScheme(
    primary = Color(0xFF2D6EAF), primaryContainer = Color(0xFFD2E4FF), onPrimary = Color.White,
    surface = Color(0xFFF8F9FF), surfaceBright = Color(0xFFF9F9FF), onSurface = Color(0xFF171C22),
    onSurfaceVariant = Color(0xFF454C56), outlineVariant = Color(0xFFC5C7D0), secondaryContainer = Color(0xFFD6E3F6)
)
private val AuroraDark = darkColorScheme(
    primary = Color(0xFFA2C9FF), primaryContainer = Color(0xFF174B7A), onPrimary = Color(0xFF00345A),
    surface = Color(0xFF0E141C), surfaceBright = Color(0xFF242B35), onSurface = Color(0xFFE0E8F4),
    onSurfaceVariant = Color(0xFFC4C7D0), outlineVariant = Color(0xFF454B55), secondaryContainer = Color(0xFF2A394D)
)

@Composable
fun SuchatTheme(appearance: SuchatAppearance, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (appearance.themeMode) { SuchatThemeMode.System -> systemDark; SuchatThemeMode.Light -> false; SuchatThemeMode.Dark -> true }
    val colors: ColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && appearance.themeMode == SuchatThemeMode.System) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) AuroraDark else AuroraLight
    MaterialTheme(colorScheme = colors, content = content)
}
