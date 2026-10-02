package io.github.sxd91.suchat.core.design.glass.kyant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import io.github.sxd91.suchat.core.design.glass.animation.DampedDragAnimation
import io.github.sxd91.suchat.core.design.glass.inspectDragGestures
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 液态玻璃开关（Kyant backdrop 版）—— 设置页开关的统一替代品。
 *
 * ## 来源与改造
 *
 * 移植自 Kyant0/AndroidLiquidGlass 的
 * `catalog/components/LiquidToggle.kt`（Apache-2.0）。
 *
 * **关键改造 —— 颜色全部接入莫奈主题（不是照抄硬编码）**：
 *
 * 上游示例里颜色是写死的：
 * ```
 * val accentColor = if (isLightTheme) Color(0xFF34C759) else Color(0xFF30D158)  // 苹果绿
 * val trackColor  = ...Color(0xFF787878).copy(0.2f)...
 * ```
 * 直接抄会用固定绿色，**丢掉莫奈取色**（本用户明确要求「把莫奈强行写入库里」）。
 * 本实现全部换成 [MiuixTheme] 语义色：
 *
 * | 用途 | 上游（硬编码） | 本实现（莫奈） |
 * |---|---|---|
 * | 选中轨道 | `0xFF34C759` 苹果绿 | `colorScheme.primary` |
 * | 未选中轨道 | `0xFF787878` α0.2 | `colorScheme.surfaceContainerHighest` |
 * | 滑块表面 | `Color.White` | `colorScheme.surfaceContainerHigh` |
 *
 * 这样 `ColorSchemeMode.MonetSystem` 下，开关颜色**跟随系统壁纸提取的莫奈色**；
 * `System` + 自选种子时跟随用户取色。运行时不读 `isSystemInDarkTheme()`，
 * 一律走主题的 `isDark` 语义（与 miuix 行为一致）。
 *
 * ## 视觉结构（与上游一致）
 *
 * 1. 轨道：`Capsule` 底 + 纯色填充（`lerp(track, accent, fraction)`）；
 * 2. 滑块：`drawBackdrop` 液态玻璃 —— 按下时 `blur/lens/高光/内阴影` 全部
 *    随 `pressProgress` 从 0 渐入（静止时可透出下层，按下时成为"活动玻璃"）；
 * 3. 拖拽：`DampedDragAnimation` 弹簧驱动（带速度修正的挤压变形）。
 *
 * ## 无障碍
 *
 * 语义 `Role.Switch`；点击（非拖拽）路径由 `onDragStopped` 统一处理 ——
 * 与上游行为一致（点按 = 拖拽 0 距离）。
 *
 * @param selected 当前是否开启（读值，供组合期快照）。
 * @param onSelect 状态切换回调（拖拽结束或点按时触发一次）。
 * @param backdrop 采样源 —— 由调用方提供（一般用页面级 `rememberLayerBackdrop`）。
 */
@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    // ★ 莫奈色源（替代上游硬编码苹果绿/灰）：
    val colors = MiuixTheme.colorScheme
    val accentColor = colors.primary
    val trackColor = colors.surfaceContainerHighest
    val thumbSurface = colors.surfaceContainerHigh

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20f.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    var didDrag by remember { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(if (selected()) 1f else 0f) }
    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {
                if (didDrag) {
                    fraction = if (targetValue >= 0.5f) 1f else 0f
                    onSelect(fraction == 1f)
                    didDrag = false
                } else {
                    // 点按：直接翻转（与上游逻辑一致）。
                    fraction = if (selected()) 0f else 1f
                    onSelect(fraction == 1f)
                }
            },
            onDrag = { _, dragAmount ->
                if (!didDrag) {
                    didDrag = dragAmount.x != 0f
                }
                val delta = dragAmount.x / dragWidth
                fraction =
                    if (isLtr) (fraction + delta).fastCoerceIn(0f, 1f)
                    else (fraction - delta).fastCoerceIn(0f, 1f)
            },
        )
    }
    // 拖拽值 → 动画器（双向同步：外部改 selected 也会动）。
    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { fraction ->
                dampedDragAnimation.updateValue(fraction)
            }
    }
    LaunchedEffect(selected) {
        snapshotFlow { selected() }
            .collectLatest { isSelected ->
                val target = if (isSelected) 1f else 0f
                if (target != fraction) {
                    fraction = target
                    dampedDragAnimation.animateToValue(target)
                }
            }
    }

    // 滑块玻璃的采样源：轨道（纯色层）+ 外部 backdrop 的组合，
    // 这样滑块既折射轨道本色、又折射底下页面内容。
    val trackBackdrop = rememberLayerBackdrop()

    Box(
        modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(Capsule())
                .drawBehind {
                    // 轨道色随 fraction 从「未选中灰」过渡到「莫奈主色」。
                    // ★ 读取放在 draw 阶段（对齐上游）：避免组合期重组，只触发重绘。
                    val fraction = dampedDragAnimation.value
                    drawRect(lerp(trackColor, accentColor, fraction))
                }
                .size(64f.dp, 28f.dp),
        )

        Box(
            Modifier
                .graphicsLayer {
                    val fraction = dampedDragAnimation.value
                    val padding = 2f.dp.toPx()
                    translationX =
                        if (isLtr) lerp(padding, padding + dragWidth, fraction)
                        else lerp(-padding, -(padding + dragWidth), fraction)
                }
                .semantics {
                    role = Role.Switch
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            // 按下时横向铺开（模拟液体被挤压前的"铺展"）。
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 0.75f, progress)
                            val scaleY = lerp(0f, 0.75f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        },
                    ),
                    shape = { Capsule() },
                    effects = {
                        // 按下才出现模糊/折射（静止时是透亮玻璃，省算力）。
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            5f.dp.toPx() * progress,
                            10f.dp.toPx() * progress,
                            chromaticAberration = true,
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress,
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f),
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress,
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 50f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        // 滑块表面：常态用主题容器色（莫奈），按下时透出更多玻璃质感。
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(thumbSurface.copy(alpha = 1f - progress * 0.6f))
                    },
                )
                .size(40f.dp, 24f.dp),
        )
    }
}
