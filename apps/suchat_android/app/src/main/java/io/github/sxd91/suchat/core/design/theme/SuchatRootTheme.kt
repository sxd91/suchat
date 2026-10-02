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
import io.github.sxd91.suchat.ui.theme.ColorSource
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

    // ★★ 2026-10-02 重要修正（用户反馈「莫奈取色怎么没了，改得太猎奇」）★★
    //
    // ## 我上一版把默认配色改坏了
    //
    // 原版（e489614 起一直如此）是：
    // ```
    // colorSchemeMode = ColorSchemeMode.MonetSystem,  // ← 读系统壁纸取色 = 真·莫奈
    // keyColor = 品牌绿,                                // ← Monet 模式下它只是 fallback
    // ```
    // 我上一版改成「默认走 `ColorSchemeMode.System` + 品牌绿当种子」——
    // `System` 模式**不读壁纸**，直接用 keyColor 生成配色，
    // 于是整套配色从「跟随壁纸」退化成「固定绿」= **莫奈取色消失**。
    //
    // ## 现在的正确逻辑
    //
    // | 颜色来源 | miuix 模式 | 种子色来源 |
    // |---|---|---|
    // | **Wallpaper（背景取色，默认）** | `MonetSystem` | **系统壁纸**（真·莫奈） |
    // | Monet（莫奈取色） | `MonetSystem` | 系统壁纸（同上一档，保持兼容） |
    // | Custom（自定义颜色） | `System` | 用户选定的 `seedColor` |
    //
    // 即：**默认必须回到 `MonetSystem`**，只有用户显式选「自定义颜色」时才用自选种子。
    val colorSource = appearance.colorSource
    val useSystemSeed = colorSource != ColorSource.Custom.key
    val paletteStyle = SuchatThemeDefaults.paletteStyleOf(appearance.paletteStyleName)
    val seed = Color(appearance.seedColor)

    val scheme: ColorScheme = rememberDynamicColorScheme(
        seedColor = seed,
        isDark = darkTheme,
        style = paletteStyle,
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
            paletteStyle = paletteStyle,
            colorSpec = SuchatThemeDefaults.colorSpec,
            seedColor = seed,
            // true = 用系统壁纸取色（真·莫奈）；false = 用用户自选种子色。
            monetFromSystem = useSystemSeed,
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
    monetFromSystem: Boolean,
    content: @Composable () -> Unit,
) {
    val miuixPalette = remember(paletteStyle) { paletteStyle.toMiuixPaletteStyle() }
    val miuixSpec = remember(colorSpec) { colorSpec.toMiuixColorSpec() }
    // darkTheme 参与 key：ThemeController 的 isDark 构造后不可写，
    // 深浅色切换必须重建 controller，否则颜色不跟随。
    //
    // colorSchemeMode 也参与 key：「背景取色 ↔ 莫奈取色」切换时要重建。
    //
    // ★ 2026-10-02：模式不再硬编码。
    //  · monetFromSystem = true  → `MonetSystem`（**用系统壁纸提取的种子色**）
    //  · 否则                    → `System`（用我们传入的 keyColor = 自选/品牌色）
    //
    // 注：miuix 的 `Monet*` 三档（MonetSystem/MonetLight/MonetDark）语义是
    // 「种子色由系统取色服务提供」；`System/Light/Dark` 则是「用 keyColor」。
    val mode = if (monetFromSystem) {
        ColorSchemeMode.MonetSystem
    } else {
        ColorSchemeMode.System
    }
    val controller = remember(seedColor, miuixPalette, miuixSpec, darkTheme, mode) {
        ThemeController(
            colorSchemeMode = mode,
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
    /** 品牌绿。 */
    val seedColor: Color = Color(0xFF07C160)

    /** 品牌绿的 ARGB Int（用于 SharedPreferences 落盘与 [SuchatAppearance] 默认值）。 */
    const val SEED_ARGB: Int = 0xFF07C160.toInt()

    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot
    val colorSpec: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2025

    /** 9 种调色风格（与 material-kolor / miuix 的枚举一一对应）。 */
    val paletteStyles: List<PaletteStyle> = listOf(
        PaletteStyle.TonalSpot,
        PaletteStyle.Neutral,
        PaletteStyle.Vibrant,
        PaletteStyle.Expressive,
        PaletteStyle.Rainbow,
        PaletteStyle.FruitSalad,
        PaletteStyle.Monochrome,
        PaletteStyle.Fidelity,
        PaletteStyle.Content,
    )

    /** 按名字取调色风格（未知名字回落 TonalSpot）。 */
    fun paletteStyleOf(name: String): PaletteStyle =
        paletteStyles.firstOrNull { it.name == name } ?: PaletteStyle.TonalSpot

    /** 调色风格的中文标签（设置页展示用）。 */
    fun paletteLabel(style: PaletteStyle): String = when (style) {
        PaletteStyle.TonalSpot -> "沉稳（默认）"
        PaletteStyle.Neutral -> "中性"
        PaletteStyle.Vibrant -> "鲜艳"
        PaletteStyle.Expressive -> "张扬"
        PaletteStyle.Rainbow -> "彩虹"
        PaletteStyle.FruitSalad -> "果盘"
        PaletteStyle.Monochrome -> "黑白"
        PaletteStyle.Fidelity -> "忠实原色"
        PaletteStyle.Content -> "内容取色"
    }
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