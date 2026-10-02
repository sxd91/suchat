package io.github.sxd91.suchat.core.design.glass.kyant

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
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
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 液态玻璃滑块（Kyant backdrop 版）。
 *
 * ## 来源与改造
 *
 * 移植自 Kyant0/AndroidLiquidGlass 的
 * `catalog/components/LiquidSlider.kt`（Apache-2.0）。
 *
 * **颜色全部接入莫奈主题**（上游是硬编码）：
 * | 用途 | 上游（硬编码） | 本实现（莫奈） |
 * |---|---|---|
 * | 已填充轨道 | `0xFF0088FF` 蓝 | `colorScheme.primary` |
 * | 未填充轨道 | `0xFF787878` α0.2 | `colorScheme.surfaceContainerHighest` |
 * | 滑块表面 | `Color.White` | `colorScheme.surfaceContainerHigh` |
 *
 * ## 结构（与上游一致）
 *
 * 1. 轨道：`Capsule` × 2 —— 底轨（未填充色）+ 填充轨（宽度 = `progress × 轨道宽`，
 *    用 `layout{}` 手动量宽，避免重组开销）；
 * 2. 滑块：液态玻璃圆钮，`translationX` 随 `progress` 走，位置被
 *    `fastCoerceIn` 限制在轨道内（左右各留 1/4 宽，防溢出）；
 * 3. 拖拽：`DampedDragAnimation`（弹簧 + 速度挤压）；点按轨道 = `animateToValue` 跳变。
 *
 * ## 为什么没有用 `IntrinsicSize`（历史教训）
 *
 * 上游用的是 `BoxWithConstraints`，它内部是 `SubcomposeLayout` ——
 * **不支持 intrinsic 测量**。此前底栏崩溃（`863479c`）正是
 * `BoxWithConstraints × IntrinsicSize` 互斥所致。本文件全程只用
 * `constraints.maxWidth`，不碰 intrinsic，安全。
 *
 * @param value 当前值（读值，供组合期快照）。
 * @param onValueChange 值变化回调（拖拽中连续触发、点按一次触发）。
 * @param valueRange 值域。
 * @param visibilityThreshold 弹簧动画的可见性阈值（小值如 0.01f 更细腻）。
 * @param backdrop 采样源 —— 由调用方提供。
 * @param label 语义描述（无障碍）。
 */
@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    // ★ 莫奈色源（替代上游硬编码蓝色/灰）：
    val colors = MiuixTheme.colorScheme
    val accentColor = colors.primary
    val trackColor = colors.surfaceContainerHighest
    val thumbSurface = colors.surfaceContainerHigh

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidth = constraints.maxWidth

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {
                    if (didDrag) {
                        onValueChange(targetValue)
                    }
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    val delta =
                        (valueRange.endInclusive - valueRange.start) * (dragAmount.x / trackWidth)
                    onValueChange(
                        if (isLtr) (targetValue + delta).coerceIn(valueRange)
                        else (targetValue - delta).coerceIn(valueRange),
                    )
                },
            )
        }
        // 外部值 → 动画器（快照同步，避免与拖拽互相打架）。
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value() }
                .collectLatest { value ->
                    if (dampedDragAnimation.targetValue != value) {
                        dampedDragAnimation.updateValue(value)
                    }
                }
        }

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            // 底轨（未填充）+ 点按跳变手势。
            Box(
                Modifier
                    .clip(Capsule())
                    .background(trackColor)
                    .pointerInput(animationScope) {
                        detectTapGestures { position ->
                            val delta =
                                (valueRange.endInclusive - valueRange.start) * (position.x / trackWidth)
                            val targetValue =
                                (if (isLtr) valueRange.start + delta
                                else valueRange.endInclusive - delta)
                                    .coerceIn(valueRange)
                            dampedDragAnimation.animateToValue(targetValue)
                            onValueChange(targetValue)
                        }
                    }
                    .height(6f.dp)
                    .fillMaxWidth(),
            )

            // 填充轨（莫奈主色）：宽度动态，用 layout{} 直接量。
            Box(
                Modifier
                    .clip(Capsule())
                    .background(accentColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width =
                            (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    },
            )
        }

        // 滑块圆钮：液态玻璃，位置随 progress；夹取在轨道内（左右各 1/4 宽防溢出）。
        Box(
            Modifier
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(
                                -size.width / 4f,
                                trackWidth - size.width * 3f / 4f,
                            ) * if (isLtr) 1f else -1f
                }
                .then(dampedDragAnimation.modifier)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            // 按压时从"细线"铺开为完整圆钮（液体膨胀感）。
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, progress)
                            val scaleY = lerp(0f, 1f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        },
                    ),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
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
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        // 钮面：常态莫奈容器色，按下时逐渐透出玻璃。
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(thumbSurface.copy(alpha = 1f - progress * 0.6f))
                    },
                )
                .size(40f.dp, 24f.dp),
        )
    }
}