package io.github.sxd91.suchat.core.design.glass.kyant

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import io.github.sxd91.suchat.core.design.glass.animation.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 液态玻璃按钮（Kyant backdrop 版）。
 *
 * ## 来源与改造
 *
 * 移植自 Kyant0/AndroidLiquidGlass 的
 * `catalog/components/LiquidButton.kt`（Apache-2.0）。
 *
 * **改造 1 —— 颜色接入莫奈（不硬编码）**：
 * 上游的 `tint`/`surfaceColor` 由调用方用裸 `Color(0xFF...)` 传（示例里是蓝/橙），
 * 本实现改成**语义化的三档**（[LiquidButtonTone]），全部取自 [MiuixTheme]：
 *
 * | 档位 | 用途 | 色源 |
 * |---|---|---|
 * | [LiquidButtonTone.Surface]（默认） | 普通次要动作 | `colorScheme.surfaceContainerHigh`（半透明表面） |
 * | [LiquidButtonTone.Primary] | 主操作 | `colorScheme.primary` 色调（Hue 叠色） |
 * | [LiquidButtonTone.Danger] | 危险操作 | `colorScheme.error` 色调 |
 * | [LiquidButtonTone.Plain] | 纯玻璃（无表面色） | 只折射，不叠色 |
 *
 * 这样在 `MonetSystem` 下按钮跟随壁纸莫奈色，不会退化成固定蓝/橙。
 *
 * **改造 2 —— 内容色走 [LocalContentColor]**：
 * 正文颜色由 tone 决定并下发给内容（`content` 里直接 `Text` 即可），
 * 与 miuix `Button` 的 `contentColor` 行为对齐，不在调用处手写颜色。
 *
 * ## 交互（与上游一致）
 *
 * - 悬停/按压时：`InteractiveHighlight` 的 AGSL 光斑跟随手指（按下点亮、抬起熄灭）；
 * - 玻璃形变：按压时按指数曲线（`tanh`）向反方向推挤 + 沿拖拽角度做各向异性缩放；
 * - 效果顺序严格遵循官方文档：`vibrancy ⇒ blur ⇒ lens`。
 *
 * @param onClick 点击回调。
 * @param backdrop 采样源 —— 由调用方提供。
 * @param tone 色调档位（默认 [LiquidButtonTone.Surface]）。
 * @param isInteractive 是否启用按压光斑与形变（false 时退回静态 + 系统涟漪）。
 * @param content 按钮内容（文字/图标；颜色由 ContentColor 下发）。
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    tone: LiquidButtonTone = LiquidButtonTone.Surface,
    isInteractive: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val animationScope = rememberCoroutineScope()

    // ★ 莫奈色源（替代上游调用处的裸 Color）。
    val colors = MiuixTheme.colorScheme
    val tint: Color = when (tone) {
        LiquidButtonTone.Plain -> Color.Unspecified
        LiquidButtonTone.Surface -> Color.Unspecified
        LiquidButtonTone.Primary -> colors.primary
        LiquidButtonTone.Danger -> colors.error
    }
    val surfaceColor: Color = when (tone) {
        LiquidButtonTone.Plain -> Color.Unspecified
        LiquidButtonTone.Surface -> colors.surfaceContainerHigh.copy(alpha = 0.55f)
        LiquidButtonTone.Primary -> colors.primary
        LiquidButtonTone.Danger -> colors.error
    }
    // 内容色：tone 决定前景，保证与叠色对比正确。
    val contentColor: Color = when (tone) {
        LiquidButtonTone.Plain, LiquidButtonTone.Surface -> colors.onSurface
        LiquidButtonTone.Primary -> colors.onPrimary
        LiquidButtonTone.Danger -> colors.onError
    }

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule() },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(12f.dp.toPx(), 24f.dp.toPx())
                },
                layerBlock = if (isInteractive) {
                    {
                        val width = size.width
                        val height = size.height

                        // 按压整体轻微膨胀（+4dp / 高）→ 液体的"鼓包"。
                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                        // 拖拽位移：指数把大位移压回（tanh 饱和），避免按钮"飞出去"。
                        val maxOffset = size.minDimension
                        val initialDerivative = 0.05f
                        val offset = interactiveHighlight.offset
                        translationX =
                            maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                        translationY =
                            maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                        // 沿拖拽方向的各向异性挤压（横向/纵向独立）。
                        val maxDragScale = 4f.dp.toPx() / size.height
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX =
                            scale +
                                maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                (width / height).fastCoerceAtMost(1f)
                        scaleY =
                            scale +
                                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                (height / width).fastCoerceAtMost(1f)
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    // 色调叠层：Hue 混合保持玻璃亮度，再补一层 α0.75 实色。
                    if (tint.isSpecifiedCompat()) {
                        drawRect(tint, blendMode = BlendMode.Hue)
                        drawRect(tint.copy(alpha = 0.75f))
                    }
                    if (surfaceColor.isSpecifiedCompat()) {
                        drawRect(surfaceColor)
                    }
                },
            )
            .clickable(
                interactionSource = null,
                indication = if (isInteractive) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .then(
                if (isInteractive) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                },
            )
            .height(48f.dp)
            .padding(horizontal = 16f.dp),
        horizontalArrangement = Arrangement.spacedBy(8f.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/** 按钮色调档位（语义化，替代上游的裸 Color 参数）。 */
enum class LiquidButtonTone {
    /** 纯玻璃：只折射，不叠任何色。 */
    Plain,

    /** 半透明表面（默认，适用于次要动作）。 */
    Surface,

    /** 主操作（莫奈主色）。 */
    Primary,

    /** 危险操作（主题错误色）。 */
    Danger,
}

/** `Color.Unspecified` 的兼容判断（避免直接依赖 `isSpecified` 扩展的可读性差异）。 */
private fun Color.isSpecifiedCompat(): Boolean = this != Color.Unspecified