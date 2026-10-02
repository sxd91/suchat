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
import androidx.compose.runtime.mutableStateOf
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
import io.github.sxd91.suchat.core.design.glass.animation.TunableParams
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
 * ## ★ 2026-10-02 修正：不能用 `value`，要用 `targetValue`
 *
 * `value` 是**弹簧动画的当前值**（跟随手指但有弹簧延迟），`targetValue`
 * 才是**手指直接映射的目标**。传 `value` 会让页面跟在指示器弹簧后面追，
 * 观感是"拖快了页面跟不上"。
 *
 * 同理新增 [onDragEnd]：松手时把 `targetValue` 交给调用方做**吸附动画** ——
 * 否则页面会停在半页位置。
 *
 * @param backdrop 由调用方（AppShell）通过 `rememberLayerBackdrop()` 创建，
 *   并在内容层用 `.layerBackdrop(backdrop)` 录制。
 * @param onDragFraction 拖拽过程中的实时进度（小数索引，基于 targetValue）。
 * @param onDragEnd 松手回调，参数为最终小数索引（调用方据此吸附）。
 * @param externalFractionProvider 外部驱动源（页面手势）。
 *
 * ## ★ 2026-10-02 双向联动（用户澄清的完整语义）
 *
 * 底栏与页面必须是**双向**联动，而不是单向：
 *
 *  1. **拖玻璃 → 页面跟随**（[onDragFraction] / [onDragEnd]）：
 *     手指按住玻璃拖动时，页面按玻璃的实时进度平移；玻璃回位时页面同步回位。
 *  2. **滑页面 → 玻璃跟随**（[externalFractionProvider]）：
 *     手指在内容区左右滑动时，玻璃指示器"出现"并跟随页面滑动进度移动，
 *     松手后页面吸附、玻璃同步回位。
 *
 * ## 优先级（用户明确要求）
 *
 * **优先识别是否松手（手动）→ 再识别是否切换页面**：
 * 手指按住期间（`isGestureActive`）一切以手势为准，外部同步一律让位；
 * 只有手势结束后，页面状态才允许回写玻璃。
 * 两条链路各自有自己的手势源，互不抢占。
 *
 * @param externalFractionProvider 页面手势驱动源：返回当前页面小数索引
 *   （`currentPage + currentPageOffsetFraction`）；返回 `null` 表示页面未在手势中。
 *   用 `() -> Float?` 而非直接传值，是为了让内部 `snapshotFlow` 读取状态，
 *   避免每一帧都触发调用方（MainActivity）重组。
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
    onDragEnd: ((Float) -> Unit)? = null,
    externalFractionProvider: (() -> Float?)? = null,
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
    val onDragEndUpdated by rememberUpdatedState(onDragEnd)
    // ★ 2026-10-02：外部驱动源同样必须 rememberUpdatedState 包装。
    //
    // 调用方（MainActivity）传进来的 lambda 每次重组都是**新实例**（捕获了 pagerState）。
    // 若直接把它当 LaunchedEffect 的 key，每个重组都会重启该 effect →
    // snapshotFlow 反复重建 → 协程风暴 → 卡顿。
    // 包装成 UpdatedState 后：effect 只启动一次，内部始终读到最新的 provider。
    val externalFractionUpdated by rememberUpdatedState(externalFractionProvider)
    val gestureIndices = remember { IntArray(2) }

    // ★ 2026-10-02：「页面手势 → 玻璃跟随」链路的活跃状态。
    //
    //  - [pageDragFraction]：页面手势中的当前小数索引；非 null 表示本链路活跃。
    //    它同时作为「让位信号」——`selectedIndexUpdated` 流的同步在非 null 时跳过，
    //    避免两条链路同时写 `valueAnimation`（双写入者冲突 → 抖动/掉帧）。
    //  - [pagePressActive]：按压态是否已激活。press() 会启动 3 个协程，
    //    用标志位保证一次手势只调一次（否则每帧调用 = 协程风暴）。
    var pageDragFraction by remember { mutableStateOf<Float?>(null) }
    var pagePressActive by remember { mutableStateOf(false) }

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
            // 热调：TunableParams.pressedScale。
            pressedScale = TunableParams.pressedScale,
            canDrag = { position -> position.x in 0f..totalWidthPx },
            onDragStarted = { position ->
                gestureIndices[0] = currentIndex
                gestureIndices[1] = indexAt(position.x)
                // ★ 2026-10-02 修正（用户反馈「玻璃怎么一抽一抽的」）：
                //
                // 拖玻璃时存在**反馈环**：
                //   手指拖玻璃 → onDragFraction 推页面 → 页面动 →
                //   externalFractionProvider 又驱动玻璃跟随 → 与手指抢 valueAnimation。
                //
                // 两条写入源（手指的 updateValue / 跟随环的 snapTo）交替覆盖，
                // 值在两侧跳变 —— 这就是"一抽一抽"。
                //
                // 修法：手指接手瞬间**清空跟随目标**，让循环停止拉拽玻璃；
                // 手指完全接管（跟手优先）。
                clearFollowTarget()
                updateValue(gestureIndices[1].toFloat())
            },
            onDragStopped = {
                val target = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                if (currentIndex != target) {
                    currentIndex = target
                    onSelectUpdated(target)
                }
                // ★ 把最终小数进度交给调用方做吸附动画（否则页面停在半页）。
                onDragEndUpdated?.invoke(targetValue)
                updateValue(target.toFloat())
                animationScope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onDragCancelled = {
                currentIndex = gestureIndices[0]
                // 取消也通知调用方归位（否则页面停在半页）。
                onDragEndUpdated?.invoke(gestureIndices[0].toFloat())
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
                    //
                    // 用 targetValue 而非 value：value 是弹簧当前值（有延迟），
                    // targetValue 才是手指直接映射的目标 —— 用 value 会"拖快了跟不上"。
                    onDragFractionUpdated?.invoke(targetValue)
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
            // ★ 2026-10-02 修正（用户反馈「玻璃消失的时机」+ 优先级 + 卡顿）：
            //
            // 优先级规则：**优先识别是否松手（手动拖动中）→ 再识别是否切换页面**。
            //
            // 两类手势期间，外部同步都必须让位：
            //  ① 手指按住玻璃（`isGestureActive`）：会与 onDrag 抢 valueAnimation；
            //  ② 手指滑动页面（`pageDragFraction != null`）：会与跟手 snapTo 抢
            //     （这是"划着一卡一卡的"的主因：spring 与 snapTo 互相取消重启）。
            if (dampedDragAnimation.isGestureActive) return@collectLatest
            if (pageDragFraction != null) return@collectLatest
            if (currentIndex != index) {
                currentIndex = index
                dampedDragAnimation.animateToValue(index.toFloat())
            }
        }
    }

    // ★ 2026-10-02 新增：页面手势 → 玻璃跟随（双向联动的第二向）。
    //
    // 用户在内容区左右滑动页面（HorizontalPager 自己处理手势）时：
    //  - 玻璃指示器"出现"（进入按压态，玻璃特效亮起）；
    //  - 指示器实时跟随页面的滑动进度（currentPage + offsetFraction）；
    //  - 手指松开 → 页面吸附整页 → 指示器同步收尾（回位）。
    //
    // 与「拖玻璃」链路的分工：
    //  - 拖玻璃时 `isGestureActive == true` → 本链路让位，不抢；
    //  - 滑页面时 `isGestureActive == false` → 本链路驱动。
    //
    // ## ★ 2026-10-02 二次修正（用户反馈「划着一卡一卡的」）
    //
    // 第一版有个**双写入者冲突**：滑页面时除了本链路 snapTo 跟手，
    // 上面的 `selectedIndexUpdated` 流也会因 pager 翻页而触发
    // `animateToValue`（spring 动画）—— 两条链路同时写 `valueAnimation`，
    // 互相取消重启 → 指示器抖动、掉帧（就是"一卡一卡的"）。
    //
    // 修法：把「本链路是否活跃」也用 `pageDragFraction` 状态登记下来，
    // 让 `selectedIndexUpdated` 流在页面手势期间**同样让位**。
    if (externalFractionProvider != null) {
        // ★ key 里不再放 externalFractionProvider（每次重组都是新实例）——
        // 只放 dampedDragAnimation，effect 生命周期与动画对象绑定。
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { externalFractionUpdated?.invoke() }.collectLatest { fraction ->
                if (fraction == null) {
                    // 页面手势结束（或未在滑动）：清除线性跟随目标，
                    // 再交给 release() 的弹簧做收尾（回位）。
                    if (pageDragFraction != null && !dampedDragAnimation.isGestureActive) {
                        dampedDragAnimation.clearFollowTarget()
                        dampedDragAnimation.release()
                    }
                    pageDragFraction = null
                    // 复位按压标志，下次手势可重新激活。
                    pagePressActive = false
                    return@collectLatest
                }
                // 拖玻璃期间本链路完全让位（优先级：手动 > 页面）。
                if (dampedDragAnimation.isGestureActive) return@collectLatest

                pageDragFraction = fraction

                // 玻璃"出现"：进入按压态（玻璃特效亮起）。
                // ★ 用标志位守卫：press() 会启动 3 个协程，
                // 若每帧都调（pressProgress 尚未越过 0.5 时）就是协程风暴 → 卡顿。
                if (!pagePressActive) {
                    pagePressActive = true
                    dampedDragAnimation.press()
                }
                // ★ 用户要求「线性速度」：不 snapTo 硬锁死，改为匀速逼近目标。
                // 玻璃以恒定速度追向页面进度 —— 平滑、有速度感、不生硬。
                dampedDragAnimation.followValueLinearly(fraction)
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
