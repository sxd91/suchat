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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class SuchatThemeMode { System, Light, Dark }

/**
 * 外观配置 —— **可观察**，改动立刻全应用生效。
 *
 * ## 为什么是 class 而不是 data class
 *
 * 旧实现是 `data class SuchatAppearance(...)`，且只在一处构造
 * （`androidx.compose.runtime.remember { SuchatAppearance() }`）——
 * 设置页想改它就再也改不动（值不可变、且没有跨页面共享）。
 *
 * 现在改成用 `mutableStateOf` 持有各字段的普通类：
 *  - 设置页写 `appearance.themeMode = SuchatThemeMode.Dark`
 *    → 触发重组 → [io.github.sxd91.suchat.core.design.theme.SuchatRootTheme]
 *    重算配色 → **全应用实时变色**，不需要重进页面、更不需要重启；
 *  - 因为是同一个实例在树里共享（CompositionLocal），各处读到的永远一致。
 *
 * ## 三个字段都对应契约里的显式设置（`docs/android-experience.md`）
 *
 * 契约明确要求这些设置**不由设备能力启发式推断**，必须是用户显式选择 ——
 * 所以它们既是持久化项（见 [io.github.sxd91.suchat.ui.page.settings.BoolKey] /
 * `ChoiceKey`），也是这里的热切换项。
 *
 * @param themeMode 深浅色（System / Light / Dark）。
 * @param glassMode 玻璃渲染档（LiquidGlass / Blur / None）。
 * @param performance 性能档（Full / Balanced / Battery）。
 * @param transition 页面转场风格（Shared Element / Miuix / AOSP / Fade）。
 * @param reduceMotion 减少动态效果（替换为短淡化）。
 */
class SuchatAppearance(
    themeMode: SuchatThemeMode = SuchatThemeMode.System,
    glassMode: String = "LiquidGlass",
    performance: String = "Full",
    transition: String = "Shared Element",
    reduceMotion: Boolean = false,
) {
    var themeMode: SuchatThemeMode by mutableStateOf(themeMode)
    var glassMode: String by mutableStateOf(glassMode)
    var performance: String by mutableStateOf(performance)
    var transition: String by mutableStateOf(transition)
    var reduceMotion: Boolean by mutableStateOf(reduceMotion)
}

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
