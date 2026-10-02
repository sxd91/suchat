package io.github.sxd91.suchat.ui.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 预测性返回（Predictive Back）包装器。
 *
 * ## 为什么需要单独一层
 *
 * `AndroidManifest` 里早已经写了 `android:enableOnBackInvokedCallback="true"`，
 * 但那只是**系统侧的开关** —— 它让系统在做返回手势时把 `BackEvent` 逐帧
 * 分发给应用。如果应用只是用 `BackHandler` 消费返回键，就变成
 * 「手势拖到一半 → 页面纹丝不动 → 松手才瞬间返回」，完全没有预览感。
 *
 * 这个组件把 `BackEvent` 的 progress（0→1）转成一个可读的 `Float`，
 * 交给 [content] 做**跟手预览变换**（如整层右移 = 露出上一页），
 * 实现真正的「按住拖动预览上一页 → 松手确认 / 上滑取消」。
 *
 * ## 提交与取消的时序（关键，容易做错）
 *
 * | 阶段 | 处理 |
 * |---|---|
 * | 手势进行中 | `snapTo(event.progress)` —— 跟手，零延迟 |
 * | 松手确认（流正常结束） | 调 [onBack] 真正出栈；**同时**把预览位移在 [EXIT_MS] 内收回 0 |
 * | 上滑取消（流抛 CancellationException） | 预览位移在 [CANCEL_MS] 内弹回 0，不出栈 |
 *
 * ### 为什么提交时要「一边出栈、一边把预览收回 0」
 *
 * 页面的退出动画（`AnimatedVisibility` 的 `slideOutHorizontally`）是从
 * **位移 0** 开始算的。若在提交瞬间把预览位移直接归零，页面会先"跳"回原位
 * 再滑出去 —— 肉眼可见的闪跳。
 *
 * 让「退出动画 0→+W」与「预览位移 p·W→0」在相同时长内并行：
 * 合计位移从 `p·W` 连续过渡到 `W`，全程没有跳变。
 *
 * ## `enabled = false` 时的行为
 *
 * 不注册 handler，返回手势完全交还系统（例如在主 Tab 根层时，
 * 系统会用自己的「退出应用」预测动画，这比应用自绘更合适）。
 *
 * @param enabled 是否接管返回（有二级页可退时传 true）。
 * @param onBack 确认返回时执行（通常是 `nav.pop()`）。
 * @param content 内容；参数 `progress` 是 `() -> Float` 的读取器 ——
 *   **务必在 `graphicsLayer { }` 这类绘制期 lambda 里读**，
 *   这样每帧只失效图层、不触发重组（性能关键）。
 */
@Composable
fun PredictiveBackContent(
    enabled: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (progress: () -> Float) -> Unit,
) {
    // 预览进度：0 = 原位，1 = 完全退出（同手势的 progress 语义）。
    val progress = remember { Animatable(0f) }
    // 恢复动画必须挂在**外部**作用域：取消时 handler 自身协程已被取消。
    val scope = rememberCoroutineScope()

    PredictiveBackHandler(enabled = enabled) { backEvents ->
        try {
            backEvents.collect { event ->
                // 跟手：直接写值，不做插值（否则手感"隔一层"）。
                progress.snapTo(event.progress.coerceIn(0f, 1f))
            }
            // 流正常结束 = 用户松手确认返回。
            scope.launch {
                progress.animateTo(0f, tween(EXIT_MS))
            }
            onBack()
        } catch (cancelled: CancellationException) {
            // 手势取消（上滑撤回）→ 弹回原位。
            scope.launch {
                progress.animateTo(0f, tween(CANCEL_MS))
            }
        }
    }

    content { progress.value }
}

/** 提交返回时，预览位移收回原位所需的时长（与页面 exit 动画一致）。 */
private const val EXIT_MS = 260

/** 手势取消时弹回原位的时长（略慢一点，回弹更有"手感"）。 */
private const val CANCEL_MS = 200

/**
 * 预测性返回（回调式）—— 用于直接驱动**已存在的动画值**。
 *
 * 与 [PredictiveBackContent] 的区别：那个是「给你一个进度、你自己画」，
 * 这个是「进度来了直接回调你」—— 适合驱动页面层已经持有的
 * `enter` 进度（见 `MainActivity.PageLayerHost`），
 * 免得为了读进度再套一层 CompositionLocal 或 remember 状态。
 *
 * ## 三个回调的确切语义
 *
 * | 回调 | 时机 | 调用方该做什么 |
 * |---|---|---|
 * | [onProgress] | 手势进行中，每帧 | 把进度写进动画值（**必须用 snapTo**，不能用 animateTo —— 否则手感像隔着弹簧） |
 * | [onCommit] | 松手确认返回 | 真正出栈（`nav.pop()`） |
 * | [onCancel] | 上滑撤回 | 把动画值弹回原位 |
 *
 * ## 为什么 `onProgress` 传的是「已推进的进度」而不是「剩余进度」
 *
 * 直接用系统给的语义：`progress = 0`（刚按下）→ `1`（拖到底）。
 * 页面层自己换算 `enter = 1 - progress`。这样回调契约与系统一致，
 * 换到别处复用不需要记"这个项目是不是反过来的"。
 *
 * @param enabled 是否接管（无页可退时传 false，把手势交还系统）。
 */
@Composable
fun PredictiveBackEffect(
    enabled: Boolean,
    onProgress: (Float) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
) {
    // 回调用 rememberUpdatedState 包装 —— 它们在每帧/每次手势结束时被调用，
    // 若直接捕获进 handler，闭包里会拿到**创建时**的旧 lambda
    // （上一轮 tab 栏踩过的同款坑：lambda 当 key/闭包捕获导致行为滞后）。
    val currentProgress by rememberUpdatedState(onProgress)
    val currentCommit by rememberUpdatedState(onCommit)
    val currentCancel by rememberUpdatedState(onCancel)

    PredictiveBackHandler(enabled = enabled) { backEvents ->
        try {
            backEvents.collect { event ->
                currentProgress(event.progress.coerceIn(0f, 1f))
            }
            currentCommit()
        } catch (cancelled: CancellationException) {
            currentCancel()
        }
    }
}
