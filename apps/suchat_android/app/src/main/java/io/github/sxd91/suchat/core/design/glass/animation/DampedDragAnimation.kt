// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
//
// Portions of this file are derived from weishu/KernelSU
// (https://github.com/tiann/KernelSU)
// Copyright (C) KernelSU contributors
// Licensed under GPL-3.0
// 移植自 WeKit (dev.ujhhgtg.wekit.ui.content.animation.DampedDragAnimation)
package io.github.sxd91.suchat.core.design.glass.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import io.github.sxd91.suchat.core.design.glass.inspectDragGestures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val canDrag: (Offset) -> Boolean = { true },
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDragCancelled: DampedDragAnimation.() -> Unit = onDragStopped,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
    val onTap: DampedDragAnimation.() -> Unit = {},
    val onLongPress: DampedDragAnimation.() -> Boolean = { false },
) {

    private val valueAnimationSpec =
        spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec =
        spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec =
        spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec =
        spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec =
        spring(0.7f, 250f, 0.001f)

    private val valueAnimation =
        Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation =
        Animatable(0f, 5f)
    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val scaleXAnimation =
        Animatable(initialScale, 0.001f)
    private val scaleYAnimation =
        Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()

    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    /**
     * 归一化进度（0..1）—— 把 [value] 从 [valueRange] 映射到 [0, 1]。
     *
     * ★ 2026-10-02 新增（移植 Kyant 控件时补齐）：
     * 上游的 `DampedDragAnimation` 有此属性，滑块用它换算滑块位置、
     * 填充轨宽度；本文件此前只在内部用 `value`，故补上以对齐上游 API。
     * 纯增量：不改变任何既有行为。
     */
    val progress: Float
        get() = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        val touchSlopSquared = viewConfiguration.touchSlop.let { it * it }
        val longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis
        var downPosition = Offset.Zero
        var movedBeyondTouchSlop = false
        var longPressTriggered = false
        var longPressConsumed = false
        var longPressJob: Job? = null

        inspectDragGestures(
            consumeOnDrag = true, // 防下层 HorizontalPager scrollable 抢手势（页面不乱动）
            onDragStart = { down ->
                downPosition = down.position
                movedBeyondTouchSlop = false
                longPressTriggered = false
                longPressConsumed = false
                onDragStarted(down.position)
                press()
                longPressJob = animationScope.launch {
                    delay(longPressTimeoutMillis)
                    longPressTriggered = true
                    if (onLongPress()) {
                        longPressConsumed = true
                        onDragCancelled()
                        release()
                    }
                }
            },
            onDragEnd = {
                longPressJob?.cancel()
                longPressJob = null
                if (!longPressConsumed) {
                    onDragStopped()
                    release()
                    if (!longPressTriggered && !movedBeyondTouchSlop) {
                        onTap()
                    }
                }
            },
            onDragCancel = {
                longPressJob?.cancel()
                longPressJob = null
                if (!longPressConsumed) {
                    onDragCancelled()
                    release()
                }
            }
        ) { change, dragAmount ->
            if (longPressConsumed) return@inspectDragGestures

            val position = change.position
            val previousPosition = change.previousPosition

            if (!movedBeyondTouchSlop) {
                val displacement = position - downPosition
                movedBeyondTouchSlop = displacement.x * displacement.x +
                    displacement.y * displacement.y > touchSlopSquared
                if (movedBeyondTouchSlop) {
                    longPressJob?.cancel()
                    longPressJob = null
                }
            }

            val isInside = canDrag(position)
            val wasInside = canDrag(previousPosition)

            if (isInside && wasInside) {
                onDrag(size, dragAmount)
            }
        }
    }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            awaitFrame()
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) { updateVelocity() } }
        }
    }

    /**
     * 拖动跟手：直接设置值（无动画）。拖动中高频调用时用 snap（spring 每次重启会
     * 追不上手指，表现为「越拖越慢」，wekit 原版 onDrag 用 updateValue 有此问题）；
     * 松手回位仍走 [animateToValue]/[updateValue] 动画。
     */
    fun snapToValue(value: Float) {
        val target = value.coerceIn(valueRange)
        animationScope.launch { valueAnimation.snapTo(target) }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val targetValue = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            System.currentTimeMillis(),
            Offset(value, 0f)
        )
        val targetVelocity = velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}