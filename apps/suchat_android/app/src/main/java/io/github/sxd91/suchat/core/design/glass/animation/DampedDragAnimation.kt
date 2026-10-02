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
     * 手势是否进行中（手指按住未松开）。
     *
     * ★ 2026-10-02 新增（用户反馈「玻璃消失的时机」）。
     *
     * ## 为什么需要它
     *
     * 拖拽底栏时，pager 翻页会让调用方的 `selectedIndex` 参数变化；
     * 若调用方不加区分地把这个变化同步回来（animateToValue），
     * 就会与手指的 onDrag 高频率互抢 `valueAnimation` ——
     * 表现为指示器跳变、玻璃特效在手指未松开时提前消失。
     *
     * 语义：**手指按住期间，一切以手势为准**；只有手势结束（松手）
     * 后才允许外部状态同步介入。这就是用户要求的优先级：
     * 「优先识别是否松手（手动）→ 再识别是否切换页面」。
     */
    var isGestureActive: Boolean = false
        private set

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
                // ★ 手指按下：手势接管，外部同步让位（用户要求的优先级）。
                isGestureActive = true
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
                    // ★ 手指松开：先清手势标志，再跑收尾逻辑 ——
                    // 这样 onDragStopped 里触发的切页、以及随后的外部同步
                    // 都会在「已松手」的前提下进行（玻璃收尾由 release() 负责，
                    // 不会与手势抢）。
                    isGestureActive = false
                    onDragStopped()
                    release()
                    if (!longPressTriggered && !movedBeyondTouchSlop) {
                        onTap()
                    }
                } else {
                    isGestureActive = false
                }
            },
            onDragCancel = {
                longPressJob?.cancel()
                longPressJob = null
                if (!longPressConsumed) {
                    isGestureActive = false
                    onDragCancelled()
                    release()
                } else {
                    isGestureActive = false
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
                // ★ 2026-10-02 修正（用户反馈「玻璃消失的时机」）：
                //
                // 旧实现在这里无条件 `press()` + `release()`。问题：
                // 拖拽底栏时，`pagerState.targetPage` 每翻过一页就会翻转一次 →
                // 调用方（MainActivity）的 LaunchedEffect 触发 animateToValue →
                // 其中的 release() 把 pressProgress 提前弹回 0 ——
                // **玻璃特效在手指还没松开时就消失了**（用户看到"玻璃自己消失"）。
                //
                // 正确时机：玻璃特效的消失（release + pressProgress 归零）
                // 只应由**手势结束**（onDragStopped/onDragCancelled 里调用 release）
                // 触发，而不是页数变化触发。
                //
                // 修法：animateToValue 不再调用 press/release，
                // 只做「把值动画到目标」这一件事；手势结束的 release 由
                // DampedDragAnimation 内部（onDragEnd/onDragCancel）负责。
                // 若正处于按压态（pressProgress > 0），也保持按压态不动 ——
                // 等手指真正松开，onDragEnd 的 release() 自然收尾。
                val targetValue = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
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