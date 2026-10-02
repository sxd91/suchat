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
import kotlin.math.sign

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

    private val valueAnimationSpec
        get() = spring<Float>(
            dampingRatio = TunableParams.valueDampingRatio,
            stiffness = TunableParams.valueStiffness,
            visibilityThreshold = visibilityThreshold,
        )
    private val velocityAnimationSpec
        get() = spring<Float>(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec
        get() = spring<Float>(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec
        get() = spring<Float>(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec
        get() = spring<Float>(0.7f, 250f, 0.001f)

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

    /**
     * 跟手值通道（CONFLATED = 只保留最新值）。
     *
     * ## ★ 2026-10-02 修正（用户反馈「划着一卡一卡的」）
     *
     * 拖拽时 [snapToValue] 每帧被调用。旧实现每帧 `launch` 一个新协程去
     * 写 `valueAnimation`（内部有 MutatorMutex）—— 一帧几十个协程排队互等，
     * 表现就是「一卡一卡的」。
     *
     * 改成：所有跟手写入走这个 CONFLATED 通道，由**单个**消费协程串行处理。
     *  - CONFLATED：中间帧自动丢弃，只处理最新的 → 天然丢帧、不积压；
     *  - 单协程：不存在 Mutex 争抢 → 零竞争。
     */
    private val dragValueChannel = kotlinx.coroutines.channels.Channel<Float>(
        kotlinx.coroutines.channels.Channel.CONFLATED,
    )

    /**
     * 跟随目标（**页面手势**专用）。null = 无跟随。
     *
     * ## ★ 2026-10-02 三次修正（用户反馈）
     *
     * 用户原话：
     *  1. 「页面滑动做成线性速度，不然直接锁死跟随看起来不好看」
     *  2. 「是曲线速度先快后慢」
     *  3. 「玻璃怎么一抽一抽的」
     *
     * ### 为什么"一抽一抽"
     *
     * 第二版用**固定步长**逼近：`value += step * sign(diff)`。
     * 当 |diff| 接近 step 时，值会在目标两侧**反复过冲-回退**
     * （当前值跳到目标左边 → 下一步又跳到右边 → 来回抖），
     * 视觉上就是"一抽一抽"。且 `delay(8ms)` 与屏幕刷新率
     * （60Hz=16.7ms / 120Hz=8.3ms）不同步，采样点漂移加剧抖动。
     *
     * ### 正确做法：帧同步 + 指数逼近
     *
     *  - **帧同步**（[withFrameNanos]）：每次回调与渲染帧严格对齐，
     *    不会有多余/缺失的更新点；
     *  - **指数逼近**：`value += diff * (1 - exp(-dt/tau))`，
     *    速度与距离成正比 —— 距离大时快、接近时自然放慢（**先快后慢**），
     *    且数学上**永不过冲**（每步都朝目标方向、步长单调收缩）。
     *
     * 这就是临界阻尼的观感：起步有速度感，收尾柔和无抖动。
     */
    private val followTarget = kotlinx.coroutines.flow.MutableStateFlow<Float?>(null)

    /**
     * 跟随时间常数（秒）。越小越快。
     *
     * 语义：距离衰减到 1/e ≈ 37% 所需的秒数。
     * 0.06s 时，一页距离约 150ms 内走完 90% —— 有速度感又不拖沓。
     *
     * ★ 热调：改为可变（[TunableParams] 运行时改，无需重编译）。
     */
    private var followTauSeconds: Float
        get() = TunableParams.followTauSeconds
        set(value) {
            TunableParams.followTauSeconds = value
        }

    init {
        // 唯一的消费协程：串行把跟手值写进动画值（手指拖动 = 即时跟手）。
        animationScope.launch {
            for (value in dragValueChannel) {
                valueAnimation.snapTo(value)
            }
        }
        // ★ 跟随协程：帧同步 + 指数逼近（先快后慢、无过冲、无抖动）。
        animationScope.launch {
            var lastFrameNs = 0L
            while (true) {
                // withFrameNanos 挂起直到下一帧 —— 与渲染节拍严格对齐。
                val frameNs = androidx.compose.runtime.withFrameNanos { it }
                val target = followTarget.value
                if (target == null) {
                    lastFrameNs = frameNs
                    continue
                }
                // dt：本帧与上帧的间隔（秒）。首帧按 16ms 估。
                val dtSeconds = if (lastFrameNs == 0L) {
                    0.016f
                } else {
                    ((frameNs - lastFrameNs) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
                }
                lastFrameNs = frameNs

                val current = valueAnimation.value
                val diff = target - current
                if (abs(diff) < 0.0005f) {
                    // 足够接近：直接对齐（消除亚像素级残差）。
                    valueAnimation.snapTo(target)
                    continue
                }
                // 指数逼近系数：1 - e^(-dt/tau)。
                // 距离越大步长越大（先快），距离越小步长越小（后慢），永不过冲。
                val alpha = 1f - kotlin.math.exp(-dtSeconds / followTauSeconds)
                valueAnimation.snapTo(current + diff * alpha)
            }
        }
    }

    /** 页面手势驱动：设置跟随目标（玻璃以曲线速度追向它）。 */
    fun followValueLinearly(value: Float) {
        followTarget.value = value.coerceIn(valueRange)
    }

    /** 清除跟随目标（页面手势结束，交给 [release] 的弹簧收尾）。 */
    fun clearFollowTarget() {
        followTarget.value = null
    }

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
     * 拖动跟手：设置值（无动画）。
     *
     * ## ★ 2026-10-02 修正（用户反馈「划着一卡一卡的」）
     *
     * 旧实现每次调用都 `animationScope.launch { valueAnimation.snapTo(...) }`。
     * 拖拽时本方法**每帧被调用**，于是每帧新建一个协程去抢 `valueAnimation`
     * 的 MutatorMutex —— 大量协程排队互等，表现就是「一卡一卡的」。
     *
     * 修法：`trySend` 到 CONFLATED 通道（非挂起、无协程分配）——
     * 中间帧自动合并，由 [dragValueChannel] 的单个消费协程串行写入。
     */
    fun snapToValue(value: Float) {
        dragValueChannel.trySend(value.coerceIn(valueRange))
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