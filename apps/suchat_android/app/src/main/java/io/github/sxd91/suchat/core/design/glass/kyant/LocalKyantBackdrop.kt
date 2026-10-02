package io.github.sxd91.suchat.core.design.glass.kyant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop

/**
 * 当前页面的 Kyant 采样源（液态玻璃控件的「背景」）。
 *
 * ## 为什么需要这个 CompositionLocal
 *
 * Kyant 的 `drawBackdrop` 系列控件**采样的是"它下面已绘制的内容"** ——
 * 要让「设置页里的开关」折射出「页面内容」，就必须有一个
 * 覆盖整个页面内容层的 [com.kyant.backdrop.backdrops.LayerBackdrop]。
 *
 * 页面骨架（`SuchatScaffold`）负责建立并记录这个 layer，然后 provide 进来；
 * 控件（[LiquidToggle] / [LiquidSlider] / [LiquidGlassButton]）从 local 读。
 *
 * 这样做的好处：
 *  1. **调用点零改动**：`SwitchRow` 等封装组件内部读取，24 处调用不用传参；
 *  2. **同一页面共享一次记录**：一个页面只建一个 layer，N 个控件共用；
 *  3. **可空回退**：页面没提供时（未迁移骨架的页面）控件退化为
 *     「纯色 canvas 采样」——仍然渲染玻璃形状，只是不折射动态内容。
 */
val LocalKyantBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * 从 [LocalKyantBackdrop] 取采样源；页面未提供时回退到一个**纯色**采样源。
 *
 * 回退色用 `surface`（页面的实底色），这样玻璃控件在未迁移页面上
 * 仍呈现「半透明表层」的合理外观，不会变成全透明或黑块。
 */
@Composable
fun rememberPageBackdropOrFallback(surface: Color): Backdrop {
    val provided = LocalKyantBackdrop.current
    // 注意：两个分支都是 composable 调用，分支依 provided 是否为空而稳定，
    // 不会违反组合顺序（同一页面内 provided 的形态不变）。
    val fallback = rememberCanvasBackdrop { drawRect(surface) }
    return provided ?: fallback
}