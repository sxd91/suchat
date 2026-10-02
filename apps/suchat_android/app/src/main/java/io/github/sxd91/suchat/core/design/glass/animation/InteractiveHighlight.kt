// InstallerX-Revived
// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
//
// Portions of this file are derived from weishu/KernelSU
// (https://github.com/tiann/KernelSU)
// Copyright (C) KernelSU contributors
// Licensed under GPL-3.0
// 移植自 WeKit (dev.ujhhgtg.wekit.ui.content.animation.InteractiveHighlight)
package io.github.sxd91.suchat.core.design.glass.animation

import android.annotation.SuppressLint
import android.graphics.RuntimeShader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastCoerceIn
import io.github.sxd91.suchat.core.design.glass.inspectDragGestures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 触摸处的「光标高光」—— 手指按在底栏上时，在按压点画一圈柔和白光。
 *
 * ## ★ 2026-10-02 恢复 AGSL 实现（此前被我降级为 radialGradient，是错的）
 *
 * 之前我用 `Brush.radialGradient` 近似这个光斑，理由是"担心每帧全屏 shader 重绘掉帧"。
 * 但那是**凭担心的错误优化**，带来两个可见差距：
 *
 *  1. **边缘不对**：`radialGradient` 是线性插值，光斑边缘是一条硬直线段；
 *     AGSL 用 `smoothstep(radius, radius * 0.5, dist)` —— 内圈 50% 半径内全亮、
 *     之后平滑衰减到 0，边缘是**软过渡**，才是液态玻璃该有的样子。
 *  2. **半径不同**：Gradient 版用了 `minDimension * 0.8f`，原版是 `* 1.2f`
 *     （要盖满整个胶囊高度，否则顶部/底部会看到光斑截断）。
 *
 * 另外 shader 的 `size` uniform 在原版里是**声明了但没参与计算**（只用 `distance`），
 * 保持原样以便 1:1 对齐。
 *
 * ## 为什么用 `BlendMode.Plus`
 *
 * 两层叠加（整块 6% 白 + 径向 12% 白）都走加色混合 —— 玻璃上打光应该是「提亮」
 * 而不是「覆盖」，加色才能保持底下的玻璃折射可见。
 */
@SuppressLint("NewApi")
class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
) {

    private val pressProgressAnimationSpec =
        spring(0.5f, 300f, 0.001f)
    private val positionAnimationSpec =
        spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val positionAnimation =
        Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero
    val offset: Offset get() = positionAnimation.value - startPosition

    /**
     * AGSL 径向光斑。
     *
     * `smoothstep(radius, radius * 0.5, dist)` 的两个边界是**反的**（从大半径到小半径），
     * 所以 t 在中心为 1、到 radius 处为 0 —— 这正是要的"中心亮、边缘软"。
     */
    private val shader =
        RuntimeShader(
            """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}
"""
        )

    val modifier: Modifier =
        Modifier.drawWithContent {
            val progress = pressProgressAnimation.value
            if (progress > 0f) {
                // ① 整块微亮（6%），让整个胶囊都有"被按下"的反馈。
                drawRect(
                    Color.White.copy(0.06f * progress),
                    blendMode = BlendMode.Plus
                )
                // ② 手指处的聚焦光斑（12%），位置跟随手指。
                shader.apply {
                    val pos = position(size, positionAnimation.value)
                    setFloatUniform("size", size.width, size.height)
                    setColorUniform("color", Color.White.copy(0.12f * progress).toArgb())
                    // 半径 1.2 × 短边：保证上下都盖满（0.8 会让光斑在胶囊里"缩"一圈）。
                    setFloatUniform("radius", size.minDimension * 1.2f)
                    setFloatUniform(
                        "position",
                        pos.x.fastCoerceIn(0f, size.width),
                        pos.y.fastCoerceIn(0f, size.height)
                    )
                }
                drawRect(
                    ShaderBrush(shader),
                    blendMode = BlendMode.Plus
                )
            }

            drawContent()
        }

    val gestureModifier: Modifier =
        Modifier.pointerInput(animationScope) {
            inspectDragGestures(
                // 观察型手势：不消费事件，且忽略事件已被消费（同节点 DampedDragAnimation
                // 内层先 consume，若不忽略，外层高光会被 cancel 而消失）
                ignoreConsumed = true,
                onDragStart = { down ->
                    startPosition = down.position
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                        launch { positionAnimation.snapTo(startPosition) }
                    }
                },
                onDragEnd = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                    }
                },
                onDragCancel = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                        launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                    }
                }
            ) { change, _ ->
                animationScope.launch { positionAnimation.snapTo(change.position) }
            }
        }
}
