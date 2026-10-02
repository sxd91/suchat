package io.github.sxd91.suchat.core.design.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import io.github.sxd91.suchat.core.design.glass.animation.DampedDragAnimation
import io.github.sxd91.suchat.core.design.glass.animation.InteractiveHighlight
import io.github.sxd91.suchat.core.design.icon.SuchatIcons
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import kotlin.math.abs
import kotlin.math.sign

/**
 * Tab 项：标题 + 图标语义键（对齐 `SuchatIcons.forKey`）。
 *
 * ⚠️ 原定义在旧的 `LiquidGlassTabBar.kt`（已删）。Kyant 版继续使用这些共享类型，
 * 因此把它们搬到这里 —— 换底栏实现不需要改动调用方（`MainActivity`/`SuchatNavigator`）。
 */
data class TabItem(val label: String, val iconKey: String)

/** 底栏配色（容器 / 指示器 / 未选中内容 / 选中内容）。 */
@Immutable
class LiquidGlassTabBarColors(
    val containerColor: Color,
    val indicatorColor: Color,
    val contentColor: Color,
    val activeContentColor: Color,
)

/** 底栏配色默认值。 */
object LiquidGlassTabBarDefaults {
    @Composable
    fun colors(
        containerColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
        indicatorColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        contentColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        activeContentColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    ) = LiquidGlassTabBarColors(containerColor, indicatorColor, contentColor, activeContentColor)
}

/**
 * 底栏渲染档位（契约 `docs/android-experience.md` 的显式设置）。
 *
 * - [LiquidGlass]：完整液态玻璃（vibrancy + blur + lens 折射 + 色散）
 * - [Blur]：半透明毛玻璃（只 blur）
 * - [None]：纯色（最省电）
 */
enum class TabBarMode { LiquidGlass, Blur, None }

/**
 * 液态玻璃悬浮 Tab 栏 —— **基于 Kyant backdrop 库**（用户指定）。
 *
 * 源：`Kyant0/AndroidLiquidGlass` 的 `catalog/components/LiquidBottomTabs.kt` +
 * `LiquidBottomTab.kt`（库官方示例，Apache-2.0）。
 *
 * ## 与旧版（miuix-blur 自绘）的差异
 *
 * 旧版是「用 miuix-blur 的 `drawBackdrop` 手工拼 effects」，本版改为库官方范式：
 *
 * | 项 | 旧（miuix-blur） | 新（Kyant backdrop） |
 * |---|---|---|
 * | 形状 | `CircleShape` | **`Capsule()`**（com.kyant.shapes，连续曲率） |
 * | 模糊 | `blur(4.dp)` | `blur(8.dp)`（Kyant 示例值） |
 * | 折射 | `lens(24, 24)` | `lens(24, 24)`（同） |
 * | 外阴影 | `Modifier.dropShadow` | **`shadow = { Shadow(alpha) }`**（库内置，跟随进度淡入） |
 * | 内阴影 | 自绘 `innerShadow()` | **`innerShadow = { InnerShadow(radius, alpha) }`** |
 * | 色散 | `chromaticAberration = 0.5f` | `chromaticAberration = true`（**API 类型不同**） |
 * | 选中指示器宽度 | `width(tabWidthDp)` | **`fillMaxWidth(1f / tabsCount)`** |
 * | 选中态着色 | `activeContentColor` | **`ColorFilter.tint(accentColor)`** + `alpha(0f)` 叠层 |
 *
 * ## 苹果 HIG 对齐（developer.apple.com/design/human-interface-guidelines/tab-bars）
 *
 * 原文要点 → 本实现：
 *
 *  1. **"A tab bar floats above content at the bottom of the screen. Its items rest
 *     on a Liquid Glass background that allows content beneath to peek through."**
 *     → 悬浮胶囊 + `lens` 折射，内容从玻璃**下方穿过**（不抬高内容区）。
 *  2. **"Include tab labels to help with navigation. ... Use single words whenever
 *     possible."** → 保留标签（消息/联系人/发现/我的，全部单词）。
 *  3. **"Prefer filled symbols or icons for consistency with the platform."**
 *     → 选中态用填充变体（`SuchatIcons.forKeySelected`）。
 *  4. **"Avoid applying a similar color to tab labels and content layer
 *     backgrounds. ... prefer a monochromatic appearance for tab bars."**
 *     → 未选中项用中性色（`onSurfaceSecondary`），仅**选中项**着色
 *     （`ColorFilter.tint`）—— 避免与内容层抢色。
 *  5. **"Make sure the tab bar is visible when people navigate to different
 *     sections."** → 二级/三级页**盖住**底栏而非隐藏它（本项目的层序已如此）。
 *  6. **"Use a badge to indicate that critical information is available."**
 *     → `SuchatUnreadBadge` 已提供（红底白字）。
 *
 * ## 保留的重力感应高光（唯一一处偏离 Kyant 示例）
 *
 * Kyant 示例用一个**静态** `Highlight.Default`。但苹果描述的是**反射环境光的玻璃**，
 * 静止高光看起来是"死"的。本项目改用 miuix 的 `rememberDeviceTilt()` 驱动高光方位
 * （见 [gravityHighlight]），转手机时高光会滑动 —— 更贴近 HIG 的 Liquid Glass 语义。
 * 这一处是刻意的增强，不是遗漏。
 */
@Composable
fun LiquidGlassTabBarKyant(
    items: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    mode: TabBarMode = TabBarMode.LiquidGlass,
    colors: LiquidGlassTabBarColors = LiquidGlassTabBarDefaults.colors(),
    /** 是否启用重力感应高光（默认开；关闭则退化为 Kyant 的静态高光）。 */
    dynamicGravityHighlight: Boolean = true,
) {
    if (items.isEmpty()) return

    val isLight = !isSystemInDarkTheme()
    val tabsCount = items.size
    val animationScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    val isLiquidGlass = mode == TabBarMode.LiquidGlass
    val isBlur = mode == TabBarMode.Blur

    // 选中项着色（HIG：只给选中项上色，其余保持中性 —— 避免与内容层抢色）。
    val accentColor = colors.activeContentColor
    val containerColor = when {
        !isLiquidGlass -> colors.containerColor
        else -> colors.containerColor.copy(alpha = 0.4f)
    }

    val tabsBackdrop = rememberLayerBackdrop()
    var tabWidthPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var totalWidthPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val panelOffset by remember {
        derivedStateOf {
            if (totalWidthPx == 0f) 0f
            else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                with(density) { 4.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction)) }
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val gestureIndices = remember { IntArray(2) }

    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = currentIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            onDragStarted = { position ->
                gestureIndices[0] = currentIndex
                gestureIndices[1] = indexAt(position.x, tabWidthPx, totalWidthPx, density, isLtr, currentIndex)
                updateValue(gestureIndices[1].toFloat())
            },
            onDragStopped = {
                val target = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                if (currentIndex != target) {
                    currentIndex = target
                    onSelect(target)
                }
                updateValue(target.toFloat())
                animationScope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onDragCancelled = {
                currentIndex = gestureIndices[0]
                updateValue(gestureIndices[0].toFloat())
                animationScope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f && dragAmount.x != 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    animationScope.launch { offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x) }
                }
            },
            onTap = {
                if (gestureIndices[1] == gestureIndices[0]) onSelect(gestureIndices[1])
            },
        )
    }

    // 外部选中索引 → 内部值（点击底栏 / 滑页面都会走到这里）。
    LaunchedEffect(selectedIndex) {
        snapshotFlow { selectedIndex }.collectLatest { index ->
            if (currentIndex != index) {
                currentIndex = index
                dampedDragAnimation.animateToValue(index.toFloat())
            }
        }
    }

    // 进入预览（HIG 未规定，但 Kyant 示例用它做「选中前预高亮」）。
    val previewIndex by remember(dampedDragAnimation, tabsCount) {
        derivedStateOf {
            dampedDragAnimation.targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
        }
    }

    // 重力感应高光（保底：传感器不可用时等价于静态高光）。
    val tilt = if (isLiquidGlass && dynamicGravityHighlight) {
        top.yukonga.miuix.kmp.blur.sensor.rememberDeviceTilt().value
    } else {
        top.yukonga.miuix.kmp.blur.sensor.DeviceTilt.Zero
    }
    val baseHighlight = gravityHighlight(Highlight.Default, tilt, extraDegrees = -45f)
    val pillHighlight = gravityHighlight(Highlight.Default, tilt, extraDegrees = 90f)

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { size, _ ->
                Offset(
                    if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                    else size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset,
                    size.height / 2f,
                )
            },
        )
    }

    // tab 内容（HIG：标签 + 图标；选中项用填充图标并着色）。
    val tabContent: @Composable RowScope.() -> Unit = {
        val scaleProvider = LocalKyantTabScale.current
        val contentColor = LocalKyantTabContentColor.current
        items.forEachIndexed { index, item ->
            val isActive = index == currentIndex
            Column(
                modifier = Modifier
                    // ⚠️ Kyant 示例用 defaultMinSize(76.dp)；此处保留同一数值，
                    //    与 HIG「图标 + 标签」的呼吸感一致。
                    .clip(Capsule())
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        role = Role.Tab,
                    ) {
                        if (currentIndex != index) {
                            currentIndex = index
                            onSelect(index)
                        }
                        dampedDragAnimation.animateToValue(index.toFloat())
                    }
                    .semantics(mergeDescendants = true) {
                        selected = isActive
                        role = Role.Tab
                    }
                    .fillMaxHeight()
                    .weight(1f)
                    .graphicsLayer {
                        val s = scaleProvider()
                        scaleX = s
                        scaleY = s
                    },
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MiuixIcon(
                    imageVector = if (isActive) {
                        SuchatIcons.forKeySelected(item.iconKey)
                    } else {
                        SuchatIcons.forKey(item.iconKey)
                    },
                    contentDescription = item.label,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp),
                )
                MiuixText(
                    text = item.label,
                    fontSize = 11.sp,
                    color = contentColor,
                    maxLines = 1,
                )
            }
        }
    }

    // ★ 这里必须是普通 `Box`，**不能**用 `BoxWithConstraints`。
    //
    // ## 崩溃复盘（2026-10-02 真机闪退，用户报告「点击进入预览后闪退」）
    //
    // 崩因：
    // ```
    // java.lang.IllegalStateException: Asking for intrinsic measurements of
    // SubcomposeLayout layouts is not supported. This includes components that
    // are built on top of SubcomposeLayout, such as lazy lists, BoxWithConstraints,
    // TabRow, etc.
    //   at IntrinsicWidthNode.calculateContentConstraints(Intrinsic.kt:182)
    //   at IntrinsicSizeModifier.measure(Intrinsic.kt:281)
    // ```
    //
    // 我上一版把两处写法**混用**了：
    //  · `BoxWithConstraints`（抄自 Kyant 的 `LiquidBottomTabs.kt`）
    //  · `.width(IntrinsicSize.Min)`（旧版 miuix 实现留下的）
    //
    // `BoxWithConstraints` 内部是 **`SubcomposeLayout`**，而 `IntrinsicSize.Min`
    // 要求父节点先做 **intrinsic 测量** —— SubcomposeLayout 不支持，直接抛异常。
    // 旧版没事是因为它用的是普通 `Box`（普通布局支持 intrinsic）。
    //
    // 而且 `BoxWithConstraints` 的 `constraints` 本文件**一次都没用到** ——
    // 纯粹是照抄示例时的冗余。改用普通 `Box` 后，`IntrinsicSize.Min` 也能正常工作，
    // 且少一层 subcompose，测量更快。
    Box(
        modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Min),
        contentAlignment = Alignment.CenterStart,
    ) {
        // 底栏主体：Kyant 官方范式 —— vibrancy + blur + lens，顺序固定。
        CompositionLocalProvider(LocalKyantTabContentColor provides colors.contentColor) {
            Row(
                Modifier
                    .onGloballyPositioned { coords ->
                        totalWidthPx = coords.size.width.toFloat()
                        val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                        tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                    }
                    .graphicsLayer { translationX = panelOffset }
                    .then(
                        if (isLiquidGlass) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { Capsule() },
                                effects = {
                                    vibrancy()
                                    blur(8.dp.toPx())
                                    lens(24.dp.toPx(), 24.dp.toPx())
                                },
                                highlight = { baseHighlight.copy(alpha = 0.75f) },
                                shadow = { Shadow(alpha = 0.25f) },
                                layerBlock = {
                                    val width = size.width.coerceAtLeast(1f)
                                    val s = lerp(1f, 1f + 16.dp.toPx() / width, dampedDragAnimation.pressProgress)
                                    scaleX = s
                                    scaleY = s
                                },
                                onDrawSurface = { drawRect(containerColor) },
                            )
                        } else if (isBlur) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { Capsule() },
                                effects = { blur(25.dp.toPx()) },
                                onDrawSurface = { drawRect(containerColor.copy(alpha = 0.65f)) },
                            )
                        } else {
                            Modifier.clip(Capsule()).then(
                                Modifier.graphicsLayer { }.let { Modifier },
                            )
                        },
                    )
                    .then(
                        if (isLiquidGlass) {
                            interactiveHighlight.modifier.then(interactiveHighlight.gestureModifier)
                        } else {
                            Modifier
                        },
                    )
                    .then(dampedDragAnimation.modifier)
                    .height(64.dp)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = tabContent,
            )
        }

        // 选中态叠层（Kyant 范式：alpha(0) 的副本 + ColorFilter.tint 着色 +
        // 按压缩放 1→1.2），它让**选中项**上色并微放大。
        if (isLiquidGlass) {
            CompositionLocalProvider(
                LocalKyantTabScale provides { lerp(1f, 1.2f, dampedDragAnimation.pressProgress) },
                LocalKyantTabContentColor provides accentColor,
            ) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer { translationX = panelOffset }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { Capsule() },
                            effects = {
                                vibrancy()
                                blur(8.dp.toPx())
                                lens(
                                    24.dp.toPx() * dampedDragAnimation.pressProgress,
                                    24.dp.toPx() * dampedDragAnimation.pressProgress,
                                )
                            },
                            highlight = { Highlight.Default.copy(alpha = dampedDragAnimation.pressProgress) },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                        .then(interactiveHighlight.modifier)
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = tabContent,
                )
            }
        }

        // 选中指示器（Kyant 范式：fillMaxWidth(1/tabsCount) + lens 深度折射 + 色散 +
        // Highlight + Shadow + InnerShadow）。
        if (tabWidthPx > 0f && isLiquidGlass) {
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        translationX = if (isLtr) {
                            dampedDragAnimation.value * tabWidthPx + panelOffset
                        } else {
                            size.width - (dampedDragAnimation.value + 1f) * tabWidthPx + panelOffset
                        }
                    }
                    .then(interactiveHighlight.gestureModifier)
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                        shape = { Capsule() },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            lens(
                                10.dp.toPx() * progress,
                                14.dp.toPx() * progress,
                                depthEffect = true,
                                chromaticAberration = true,
                            )
                        },
                        highlight = { pillHighlight.copy(alpha = dampedDragAnimation.pressProgress) },
                        shadow = { Shadow(alpha = dampedDragAnimation.pressProgress) },
                        innerShadow = {
                            InnerShadow(
                                radius = 8.dp * dampedDragAnimation.pressProgress,
                                alpha = dampedDragAnimation.pressProgress,
                            )
                        },
                        layerBlock = {
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                            val velocity = dampedDragAnimation.velocity / 10f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = dampedDragAnimation.pressProgress
                            drawRect(
                                color = if (isLight) Color.Black.copy(alpha = 0.1f)
                                else Color.White.copy(alpha = 0.1f),
                                alpha = 1f - progress,
                            )
                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
                        },
                    )
                    .height(56.dp)
                    .fillMaxWidth(1f / tabsCount),
            )
        }
    }
}

/** 内容色（未选中）。 */
private val LocalKyantTabContentColor = staticCompositionLocalOf { Color.Unspecified }

/** 按压缩放提供者。 */
private val LocalKyantTabScale = staticCompositionLocalOf { { 1f } }

/** 按位置算 tab 索引（与旧实现共用同一算法）。 */
private fun indexAt(
    positionX: Float,
    tabWidthPx: Float,
    totalWidthPx: Float,
    density: androidx.compose.ui.unit.Density,
    isLtr: Boolean,
    fallback: Int,
): Int {
    if (tabWidthPx == 0f) return fallback
    val horizontalPaddingPx = with(density) { 4.dp.toPx() }
    val logicalX = if (isLtr) positionX else totalWidthPx - positionX
    return ((logicalX - horizontalPaddingPx) / tabWidthPx).toInt().coerceAtLeast(0)
}