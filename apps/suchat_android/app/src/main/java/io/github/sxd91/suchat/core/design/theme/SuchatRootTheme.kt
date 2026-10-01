package io.github.sxd91.suchat.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import io.github.sxd91.suchat.ui.theme.SuchatAppearance
import io.github.sxd91.suchat.ui.theme.SuchatThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/**
 * Suchat 统一主题入口。
 *
 * ## 组装关系
 *
 * ```
 * SuchatRootTheme(appearance = 契约的三档设置)
 *   ├── MaterialExpressiveTheme  → MaterialTheme.colorScheme（莫奈动态色）
 *   ├── MiuixTheme               → MiuixTheme.colorScheme（miuix 组件用，同源色）
 *   └── LocalSuchatTokens        → SuchatTokens（静态锚点色：气泡绿/顶栏灰…）
 * ```
 *
 * ## 为什么三套并存
 *
 * - **莫奈动态色**：契约要求「跟随系统」时走动态取色，负责系统级组件与设置页；
 * - **miuix 色**：底栏/组件库的默认色来源，必须与莫奈同源，否则观感割裂；
 * - **静态令牌**：聊天页的气泡、顶栏、底栏是产品辨识度锚点，不能被动态色漂移。
 *
 * 三者共用同一个种子色（品牌绿），因此即使分工不同也不会色彩打架。
 *
 * ## 契约对齐（docs/android-experience.md）
 *
 * - 外观三档 `LiquidGlass` / `Blur` / `None`：不在此处判断 —— 由
 *   `SuchatAppearance.glassMode` 传给底栏组件，**不做设备能力启发式**；
 * - 深浅色由 [SuchatAppearance.themeMode] 决定（System / Light / Dark），
 *   与系统动态取色的开关解耦。
 *
 * @param appearance 契约外观配置（来自设置页 / 服务端 `me/appearance`）。
 */
@Composable
fun SuchatRootTheme(
    appearance: SuchatAppearance,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (appearance.themeMode) {
        SuchatThemeMode.System -> systemDark
        SuchatThemeMode.Light -> false
        SuchatThemeMode.Dark -> true
    }

    // 莫奈取色：种子色固定为 Suchat 品牌绿，风格/规范取默认值。
    // 说明：契约未规定种子色来源，此处用品牌绿而非系统壁纸 ——
    // 保证「System 模式下动态取色也不偏离产品色相」。
    val scheme: ColorScheme = rememberDynamicColorScheme(
        seedColor = SuchatThemeDefaults.seedColor,
        isDark = darkTheme,
        style = SuchatThemeDefaults.paletteStyle,
        specVersion = SuchatThemeDefaults.colorSpec,
    )

    val tokens = remember(darkTheme) {
        if (darkTheme) SuchatTokens.dark() else SuchatTokens.light()
    }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
    ) {
        SuchatMiuixThemeRoot(
            darkTheme = darkTheme,
            paletteStyle = SuchatThemeDefaults.paletteStyle,
            colorSpec = SuchatThemeDefaults.colorSpec,
            seedColor = SuchatThemeDefaults.seedColor,
        ) {
            CompositionLocalProvider(
                LocalSuchatTokens provides tokens,
                content = content,
            )
        }
    }
}

/**
 * miuix 主题根 —— 与 Material3 侧共用同一组种子色 / 风格 / 规范版本。
 *
 * miuix 组件（SmallTopAppBar / Card / Switch / ArrowPreference 等）读的是
 * `MiuixTheme.colorScheme`，与 `MaterialTheme.colorScheme` 是两套独立对象，
 * 必须各自包装。
 */
@Composable
private fun SuchatMiuixThemeRoot(
    darkTheme: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
    seedColor: Color,
    content: @Composable () -> Unit,
) {
    val miuixPalette = remember(paletteStyle) { paletteStyle.toMiuixPaletteStyle() }
    val miuixSpec = remember(colorSpec) { colorSpec.toMiuixColorSpec() }
    // darkTheme 参与 key：ThemeController 的 isDark 构造后不可写，
    // 深浅色切换必须重建 controller，否则颜色不跟随。
    val controller = remember(seedColor, miuixPalette, miuixSpec, darkTheme) {
        ThemeController(
            colorSchemeMode = ColorSchemeMode.MonetSystem,
            keyColor = seedColor,
            colorSpec = miuixSpec,
            paletteStyle = miuixPalette,
            isDark = darkTheme,
        )
    }
    MiuixTheme(controller = controller, content = content)
}

/** Suchat 主题默认参数（种子色 = 品牌绿）。 */
object SuchatThemeDefaults {
    val seedColor: Color = Color(0xFF07C160)
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot
    val colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025
}

// --- 枚举映射（material-kolor ↔ miuix） ---

/** material-kolor 的 [PaletteStyle] 与 miuix 的 [ThemePaletteStyle] 一一对应（均为 9 项）。 */
internal fun PaletteStyle.toMiuixPaletteStyle(): ThemePaletteStyle = when (this) {
    PaletteStyle.TonalSpot -> ThemePaletteStyle.TonalSpot
    PaletteStyle.Neutral -> ThemePaletteStyle.Neutral
    PaletteStyle.Vibrant -> ThemePaletteStyle.Vibrant
    PaletteStyle.Expressive -> ThemePaletteStyle.Expressive
    PaletteStyle.Rainbow -> ThemePaletteStyle.Rainbow
    PaletteStyle.FruitSalad -> ThemePaletteStyle.FruitSalad
    PaletteStyle.Monochrome -> ThemePaletteStyle.Monochrome
    PaletteStyle.Fidelity -> ThemePaletteStyle.Fidelity
    PaletteStyle.Content -> ThemePaletteStyle.Content
}

/** material-kolor 的规范版本与 miuix 的 [ThemeColorSpec] 对应。 */
internal fun ColorSpec.SpecVersion.toMiuixColorSpec(): ThemeColorSpec = when (this) {
    ColorSpec.SpecVersion.SPEC_2021 -> ThemeColorSpec.Spec2021
    ColorSpec.SpecVersion.SPEC_2025 -> ThemeColorSpec.Spec2025
}