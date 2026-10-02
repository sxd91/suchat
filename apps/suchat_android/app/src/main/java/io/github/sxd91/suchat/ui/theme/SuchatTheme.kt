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
import io.github.sxd91.suchat.core.design.theme.SuchatThemeDefaults

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
    colorSource: String = ColorSource.Monet.key,
    seedColor: Int = SuchatThemeDefaults.SEED_ARGB,
    paletteStyleName: String = "TonalSpot",
) {
    var themeMode: SuchatThemeMode by mutableStateOf(themeMode)
    var glassMode: String by mutableStateOf(glassMode)
    var performance: String by mutableStateOf(performance)
    var transition: String by mutableStateOf(transition)
    var reduceMotion: Boolean by mutableStateOf(reduceMotion)

    /**
     * 颜色来源：`Monet`（按种子色生成）/ `Wallpaper`（背景取色，读系统壁纸）/
     * `Custom`（用户自选颜色）。
     *
     * ★ 2026-10-02 新增（用户反馈「外观选不了莫奈取色的颜色和启用背景取色」）。
     *
     * 映射到 miuix 的 `ColorSchemeMode`：
     *  - `Monet`  → `MonetSystem` / `MonetLight` / `MonetDark`（由 [themeMode] 定明暗）
     *  - `Wallpaper` → 同上一组（区别只在**种子色**取自系统壁纸还是用户选的色）
     *  - `Custom` → 用 [seedColor] 生成
     */
    var colorSource: String by mutableStateOf(colorSource)

    /** 自定义种子色（ARGB Int，便于落盘；用 Int 而不是 Color 是为了 SharedPreferences）。 */
    var seedColor: Int by mutableStateOf(seedColor)

    /** material-kolor 的调色风格名（TonalSpot / Vibrant / Expressive / … 共 9 种）。 */
    var paletteStyleName: String by mutableStateOf(paletteStyleName)
}

/**
 * 颜色来源枚举。
 *
 * ## 为什么要分「莫奈种子色」与「背景取色」
 *
 * 两者在 Compose 里的实现是**同一套**（都走 material-kolor / miuix 的动态取色），
 * 差别只在**种子色的来源**：
 *  - `Monet`：种子色由用户/产品指定（品牌绿或自选），产出稳定可预期；
 *  - `Wallpaper`：种子色交给系统从壁纸里提取 —— 换壁纸即换整套配色，
 *    这是 Android 12+ 原生「莫奈取色」的默认行为，也是用户点名要的那一项。
 *
 * 之前主题层把种子色**硬编码**成品牌绿、模式**硬编码**成 `MonetSystem`，
 * 所以设置页里根本没有可选项 —— 用户说「太敷衍」指的就是这个。
 */
enum class ColorSource(val key: String, val label: String) {
    Monet("Monet", "莫奈取色"),
    Wallpaper("Wallpaper", "背景取色"),
    Custom("Custom", "自定义颜色"),
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
