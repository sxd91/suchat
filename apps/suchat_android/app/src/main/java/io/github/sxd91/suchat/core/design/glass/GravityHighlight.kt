package io.github.sxd91.suchat.core.design.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import top.yukonga.miuix.kmp.blur.sensor.DeviceTilt
import kotlin.math.PI
import kotlin.math.atan2

/**
 * 重力感应高光（Kyant backdrop 版）。
 *
 * ## 为什么需要（HIG 语义）
 *
 * 苹果 HIG 把 tab bar 描述成 "items rest on a **Liquid Glass** background"，
 * 而 Liquid Glass 的语义是**反射环境光的玻璃**。高光静止不动时，
 * 玻璃看着像"贴上去的塑料"；随重力滑动才有真实反射感。
 *
 * Kyant 官方示例用**静态** `Highlight.Default`（固定 `angle = 45f`）；
 * 这里用 miuix 的 `rememberDeviceTilt()` 驱动它的 `angle` —— 刻意的增强。
 *
 * ## 为什么改 `angle` 而不是像 miuix 那样改光源坐标
 *
 * 两套库的高光模型不同：
 *  - **miuix** 的 `HighlightStyle.BloomStroke` 有 `primaryLight: LightSource`，
 *    位置是 **UV 坐标**，可以「平移」光源；
 *  - **Kyant** 的 `HighlightStyle.Default` 只有一个 **`angle`（弧度）** ——
 *    它是**方向性渐变**（`DefaultHighlightShaderString` 按角度铺光），
 *    没有"位置"可平移。
 *
 * 所以这里把重力方向换算成**角度**（`atan2`）直接驱 `angle`。
 * 视觉上等价：光带随手机倾斜而旋转。
 *
 * ## 算法
 *
 * ```
 * g = (gravityX, gravityY)                      // 重力在屏幕平面的投影
 * |g|² > 0.01 ? angle = atan2(gy, gx) : 默认角    // 平放时回落默认角（防抖）
 * angle += extraDegrees * π/180                  // 多块高光错开
 * ```
 *
 * @param base 基准高光（仅 [HighlightStyle.Default] 有 angle 可转）。
 * @param tilt 设备倾斜。
 * @param extraDegrees 额外旋转角（度）。
 */
@Composable
fun gravityHighlight(
    base: Highlight,
    tilt: DeviceTilt,
    extraDegrees: Float = 0f,
): Highlight {
    val style = base.style as? HighlightStyle.Default ?: return base
    val angle = remember(tilt, style.angle, extraDegrees) {
        val gx = tilt.gravityX
        val gy = tilt.gravityY
        val magSq = gx * gx + gy * gy
        val fromGravity = if (magSq > GRAVITY_EPS_SQ) {
            atan2(gy, gx).toFloat()
        } else {
            style.angle
        }
        fromGravity + extraDegrees * (PI / 180f).toFloat()
    }
    return remember(base, angle) { base.copy(style = style.copy(angle = angle)) }
}

/** 重力方向可用的最小模长平方（|g_xy| > 0.1，约 6° 倾斜）。 */
private const val GRAVITY_EPS_SQ = 0.01f