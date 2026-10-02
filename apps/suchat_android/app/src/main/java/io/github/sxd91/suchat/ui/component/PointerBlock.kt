package io.github.sxd91.suchat.ui.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 消费「子节点没消费掉」的指针事件 —— 让这一层挡住**它下面的层**。
 *
 * ## 为什么需要它（★ 2026-10-02 修用户反馈「面板下面会穿透点击下层」）
 *
 * Compose 的命中测试**只认带 `pointerInput` 的节点**。
 * 一个只写了 `.fillMaxSize().background(...)` 的 `Box`、一个只有背景色的面板，
 * **都不是命中目标** —— 指针会继续往下落到更低 z 序、同样覆盖该位置的兄弟节点上。
 *
 * 于是出现两个真实故障：
 *
 *  1. **二级页层**（`MainActivity.PageLayerHost`）视觉上盖住了主 Tab 层，
 *     但页面里的空白处（面板周围、列表下方）点击会**穿透到底下的主 Tab 会话列表**
 *     （那里有 `clickable`）—— 用户看到「点着聊天页，却触发了主界面的会话」；
 *  2. **表情/加号面板**自身只有子项是 clickable，面板留白区同样会穿到下面。
 *
 * 用户的追问「按理来说下层不应该消失吗」正是指这个：
 * 上层出现时，下层就不该再接收事件了。
 *
 * ## 为什么在 `Main` 阶段消费、而不是 `Initial`
 *
 * 指针事件的三趟派发顺序：
 *
 * | 阶段 | 方向 |
 * |---|---|
 * | `Initial` | 父 → 子 |
 * | `Main` | **子 → 父** |
 * | `Final` | 父 → 子 |
 *
 * 如果在 `Initial` 消费，会**抢在子节点之前**把事件吃掉 —— 面板里的 emoji 按钮、
 * 页面里的所有控件都会失灵。
 *
 * 在 `Main` 阶段消费则相反：子节点先处理，**子节点没吃掉**（`isConsumed == false`）
 * 才由本层兜底消费，从而拦住下层的兄弟节点。这既能"挡住下面"，
 * 又不影响自己内部任何控件的正常响应。
 *
 * @param enabled false 时不注册（零开销），用于"上层不存在时不该阻挡"的场景。
 */
fun Modifier.blockPointerInput(enabled: Boolean = true): Modifier =
    if (!enabled) this
    else this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                event.changes.forEach { change ->
                    // 只在子节点没处理时兜底消费（见上面的阶段说明）。
                    if (!change.isConsumed) change.consume()
                }
            }
        }
    }