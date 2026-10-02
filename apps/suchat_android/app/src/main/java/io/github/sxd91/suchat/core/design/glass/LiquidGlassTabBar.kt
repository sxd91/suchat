package io.github.sxd91.suchat.core.design.glass

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import io.github.sxd91.suchat.core.design.glass.animation.DampedDragAnimation
import io.github.sxd91.suchat.core.design.glass.animation.InteractiveHighlight
import io.github.sxd91.suchat.core.design.glass.liquid.InnerShadow
import io.github.sxd91.suchat.core.design.glass.liquid.innerShadow
import io.github.sxd91.suchat.core.design.glass.liquid.lens
import io.github.sxd91.suchat.core.design.glass.liquid.rememberCombinedBackdrop
import io.github.sxd91.suchat.core.design.glass.liquid.vibrancy
import io.github.sxd91.suchat.core.design.icon.SuchatIcons
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import kotlin.math.abs
import kotlin.math.sign

private val LocalTabBarContentColor = staticCompositionLocalOf { Color.Unspecified }
private val LocalTabBarScale = staticCompositionLocalOf { { 1f } }

/** Tab 项：标题 + 图标语义键（对齐 AppIcons.forKey）。 */
data class TabItem(val label: String, val iconKey: String)

@Immutable
class LiquidGlassTabBarColors(
    val containerColor: Color,
    val indicatorColor: Color,
    val contentColor: Color,
    val activeContentColor: Color,
)

object LiquidGlassTabBarDefaults {
    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        indicatorColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        activeContentColor: Color = MaterialTheme.colorScheme.primary,
    ) = LiquidGlassTabBarColors(containerColor, indicatorColor, contentColor, activeContentColor)
}

enum class TabBarMode { LiquidGlass, Blur, None }

private val iosIndicatorSpecular = Highlight(
    width = 1.dp,
    alpha = 1f,
    style = BloomStroke(
        color = Color.White.copy(alpha = 0.12f),
        innerBlurRadius = 2.0.dp,
        primaryLight = LightSource(
            position = LightPosition(0.5f, -0.3f, -0.05f),
            color = Color.White,
            intensity = 1f,
        ),
        secondaryLight = LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            color = Color.White,
            intensity = 0.4f,
        ),
        dualPeak = true,
    ),
)

/**
 * 液态玻璃悬浮 Tab 栏。
 *
 * 玻璃质感由 miuix-blur 提供（非手搓）：
 *  - 内容层 `.layerBackdrop(backdrop)` 把背景录成可采样纹理；
 *  - 底栏 `drawBackdrop(backdrop, shape)` 在其上链 `vibrancy() -> blur() -> lens()`；
 *  - `lens(refractionHeight = 24.dp, refractionAmount = 24.dp)` 做圆角矩形 SDF 边缘折射；
 *  - 选中指示器额外走 `lens(..., depthEffect = true, chromaticAberration = 0.5f)` 出七通道色散。
 *
 * ## 用户第 5 条：划到哪里就切到哪里
 *
 * 原实现只在**松手时**（`onDragStopped`）才通知选中变化 —— 拖拽过程中
 * 内容层不动，视觉上"指示器先跑、页面后跳"，割裂。
 *
 * 现在新增 [onDragFraction] 回调：拖拽过程中**持续**回报当前的小数索引
 * （如 1.37 表示"在 1 与 2 之间偏 2"）。调用方（AppShell）据此让
 * `HorizontalPager` 实时跟随拖拽位置，做到指示器与页面内容同步移动 ——
 * 这就是"划到哪切到哪"。
 *
 * 松手后仍通过 [onSelect] 落定最终页（并触发吸附动画）。
 *
 * @param backdrop 由调用方（AppShell）通过 `rememberLayerBackdrop()` 创建，
 *   并在内容层用 `.layerBackdrop(backdrop)` 录制。
 * @param onDragFraction 拖拽过程中的实时进度（小数索引）；未拖拽时为 null。
 */
@Composable
fun LiquidGlassTabBar(
    items: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    mode: TabBarMode = TabBarMode.LiquidGlass,
    colors: LiquidGlassTabBarColors = LiquidGlassTabBarDefaults.colors(),
    liquidGlassBlurRadius: Dp = 4.dp,
    onDragFraction: ((Float) -> Unit)? = null,
) {
    if (items.isEmpty()) return

    val isInDark = isSystemInDarkTheme()
    val pillShape = remember { CircleShape }
    val isLiquidGlassMode = mode == TabBarMode.LiquidGlass
    val isBlurMode = mode == TabBarMode.Blur
    val isGlassTransparent = isLiquidGlassMode && liquidGlassBlurRadius <= 0.dp
    val containerColor = when {
        isGlassTransparent -> Color.Transparent
        isLiquidGlassMode -> colors.containerColor.copy(alpha = 0.4f)
        else -> colors.containerColor
    }

    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val tabsCount = items.size

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) 0f
            else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val selectedIndexUpdated by rememberUpdatedState(selectedIndex)
    val onSelectUpdated by rememberUpdatedState(onSelect)
    val onDragFractionUpdated by rememberUpdatedState(onDragFraction)
    val gestureIndices = remember { IntArray(2) }

    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { 4.dp.toPx() }
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx)
            .toInt()
            .fastCoerceIn(0, tabsCount - 1)
    }

    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = currentIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { position -> position.x in 0f..totalWidthPx },
            onDragStarted = { position ->
                gestureIndices[0] = currentIndex
                gestureIndices[1] = indexAt(position.x)
                updateValue(gestureIndices[1].toFloat())
            },
            onDragStopped = {
                val target = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                if (currentIndex != target) {
                    currentIndex = target
                    onSelectUpdated(target)
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
                    // 用 updateValue 而非 snapToValue：拖动中走 spring 动画，
                    // velocityTracker 持续喂速度、pressProgress 保持弹簧进度；
                    // snapTo 会让值瞬间跳到位，松手时 value == targetValue，
                    // release() 里的弹簧等待被跳过，Q 弹回弹势能全丢。
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    // ★ 用户第 5 条「划到哪切到哪」：
                    // 把当前的小数进度实时回报给调用方，让内容层（HorizontalPager）
                    // 跟着指示器同步移动，而不是等松手才跳。
                    onDragFractionUpdated?.invoke(value)
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            },
            onTap = {
                if (gestureIndices[1] == gestureIndices[0]) {
                    onSelectUpdated(gestureIndices[1])
                }
            },
        )
    }

    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { selectedIndexUpdated }.collectLatest { index ->
            if (currentIndex != index) {
                currentIndex = index
                dampedDragAnimation.animateToValue(index.toFloat())
            }
        }
    }

    val activateTab = remember(dampedDragAnimation) {
        { index: Int ->
            if (currentIndex != index) {
                currentIndex = index
                onSelectUpdated(index)
            }
            dampedDragAnimation.animateToValue(index.toFloat())
        }
    }

    val tabsContent: @Composable RowScope.() -> Unit = {
        val scale = LocalTabBarScale.current
        val contentColor = LocalTabBarContentColor.current
        items.forEachIndexed { index, item ->
            val isActive = index == currentIndex
            Column(
                modifier = Modifier
                    .defaultMinSize(minWidth = 64.dp)
                    .semantics(mergeDescendants = true) {
                        selected = isActive
                        role = Role.Tab
                        onClick {
                            activateTab(index)
                            true
                        }
                    }
                    .onKeyEvent { event ->
                        val isActivationKey = event.key == Key.Enter ||
                            event.key == Key.NumPadEnter ||
                            event.key == Key.Spacebar
                        if (isActivationKey) {
                            if (event.type == KeyEventType.KeyUp) activateTab(index)
                            true
                        } else false
                    }
                    .focusable()
                    .fillMaxHeight()
                    .weight(1f)
                    .graphicsLayer {
                        val s = scale()
                        scaleX = s
                        scaleY = s
                    },
                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    MiuixIcon(
                        imageVector = if (isActive) SuchatIcons.forKey(item.iconKey)
                        else SuchatIcons.forKey(item.iconKey),
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp),
                    )
                    MiuixText(
                        text = item.label,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }

    val interactiveHighlight =
        if (isLiquidGlassMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            remember(animationScope, tabWidthPx) {
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
        } else null

    val combinedBackdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop)

    Box(
        modifier = modifier.width(IntrinsicSize.Min),
        contentAlignment = Alignment.CenterStart,
    ) {
        // 底栏主体
        CompositionLocalProvider(LocalTabBarContentColor provides colors.contentColor) {
            Row(
                Modifier
                    .onGloballyPositioned { coords ->
                        totalWidthPx = coords.size.width.toFloat()
                        val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                        tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                    }
                    .graphicsLayer { translationX = panelOffset }
                    .dropShadow(
                        shape = pillShape,
                        shadow = Shadow(
                            radius = 10.dp,
                            color = Color.Black,
                            alpha = if (isInDark) 0.2f else 0.1f,
                        ),
                    )
                    .then(
                        if (isLiquidGlassMode) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { pillShape },
                                effects = {
                                    if (!isGlassTransparent) {
                                        vibrancy()
                                        blur(liquidGlassBlurRadius.toPx(), liquidGlassBlurRadius.toPx())
                                        lens(
                                            refractionHeight = 24.dp.toPx(),
                                            refractionAmount = 24.dp.toPx(),
                                        )
                                    }
                                },
                                highlight = { iosIndicatorSpecular.copy(alpha = 0.75f) },
                                layerBlock = {
                                    val width = size.width.coerceAtLeast(1f)
                                    val s = lerp(1f, 1f + 16.dp.toPx() / width, dampedDragAnimation.pressProgress)
                                    scaleX = s
                                    scaleY = s
                                },
                                onDrawSurface = { drawRect(containerColor) },
                            )
                        } else if (isBlurMode) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { pillShape },
                                effects = { blur(25.dp.toPx(), 25.dp.toPx()) },
                                onDrawSurface = { drawRect(containerColor.copy(alpha = 0.65f)) },
                            )
                        } else {
                            Modifier.background(containerColor, pillShape)
                        }
                    )
                    .then(
                        if (isLiquidGlassMode && interactiveHighlight != null) {
                            interactiveHighlight.modifier.then(interactiveHighlight.gestureModifier)
                        } else Modifier
                    )
                    .then(dampedDragAnimation.modifier)
                    .height(64.dp)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = tabsContent,
            )
        }

        // 选中态文字层（放大 + activeContentColor）
        if (isLiquidGlassMode) {
            CompositionLocalProvider(
                LocalTabBarScale provides { lerp(1f, 1.2f, dampedDragAnimation.pressProgress) },
                LocalTabBarContentColor provides colors.activeContentColor,
            ) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer { translationX = panelOffset }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { pillShape },
                            effects = {
                                if (!isGlassTransparent) {
                                    vibrancy()
                                    blur(liquidGlassBlurRadius.toPx(), liquidGlassBlurRadius.toPx())
                                    lens(
                                        refractionHeight = 24.dp.toPx(),
                                        refractionAmount = 24.dp.toPx(),
                                    )
                                }
                            },
                            onDrawSurface = { drawRect(containerColor) },
                        )
                        .then(interactiveHighlight?.modifier ?: Modifier)
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabsContent()
                }
            }
        }

        // 选中指示器：走深度折射 + 色散
        if (tabWidthPx > 0f) {
            val tabWidthDp = with(density) { tabWidthPx.toDp() }
            if (isLiquidGlassMode) {
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val progressOffset = dampedDragAnimation.value * tabWidthPx
                            translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                        }
                        .drawBackdrop(
                            backdrop = combinedBackdrop,
                            shape = { pillShape },
                            effects = {
                                if (!isGlassTransparent) {
                                    val progress = dampedDragAnimation.pressProgress
                                    lens(
                                        refractionHeight = 10.dp.toPx() * progress,
                                        refractionAmount = 14.dp.toPx() * progress,
                                        depthEffect = true,
                                        chromaticAberration = 0.5f,
                                    )
                                }
                            },
                            highlight = { iosIndicatorSpecular.copy(alpha = dampedDragAnimation.pressProgress) },
                            layerBlock = {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                if (!isGlassTransparent) {
                                    val progress = dampedDragAnimation.pressProgress
                                    drawRect(
                                        color = if (!isInDark) Color.Black.copy(alpha = 0.1f)
                                        else Color.White.copy(alpha = 0.1f),
                                        alpha = 1f - progress,
                                    )
                                    drawRect(Color.Black.copy(alpha = 0.03f * progress))
                                }
                            },
                        )
                        .innerShadow(shape = pillShape) {
                            InnerShadow(
                                radius = 8.dp * dampedDragAnimation.pressProgress,
                                color = Color.Black.copy(alpha = 0.15f),
                                alpha = dampedDragAnimation.pressProgress,
                            )
                        }
                        .height(56.dp)
                        .width(tabWidthDp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .graphicsLayer {
                            val progressOffset = dampedDragAnimation.value * tabWidthPx
                            translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                        }
                        .clip(pillShape)
                        .background(colors.indicatorColor.copy(alpha = 0.15f), pillShape)
                        .height(56.dp)
                        .width(tabWidthDp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    CompositionLocalProvider(LocalTabBarContentColor provides colors.activeContentColor) {
                        Row(
                            Modifier
                                .clearAndSetSemantics {}
                                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                                .requiredWidth(with(density) { (totalWidthPx - 8.dp.toPx()).toDp() })
                                .height(56.dp)
                                .graphicsLayer {
                                    val progressOffset = dampedDragAnimation.value * tabWidthPx
                                    translationX = if (isLtr) -progressOffset else progressOffset
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            content = tabsContent,
                        )
                    }
                }
            }
        }
    }
}
