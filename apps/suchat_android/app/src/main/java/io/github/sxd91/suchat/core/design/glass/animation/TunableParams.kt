package io.github.sxd91.suchat.core.design.glass.animation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 运行时可调参数（**热调**，改完立即生效，无需重新编译安装）。
 *
 * ## 为什么需要它
 *
 * 调试动效参数（跟随速度、弹簧软硬、阻尼）时，「改代码 → 编译 → 装包 → 重启」
 * 一轮要几分钟，且要反复试很多组值。这里把参数集中到一个全局单例：
 *
 *  - 用 Compose 的 `mutableStateOf` 持有 → 改值会**触发重组**，立即生效；
 *  - 通过调试面板（长按底栏唤出）实时调整；
 *  - 值可持久化到 SharedPreferences，重启后保留。
 *
 * ## 使用方式
 *
 * 1. 应用内**长按底栏空白处** → 弹出「动效调参」面板；
 * 2. 拖动滑块即时预览；
 * 3. 「重置」恢复默认；「保存」写入本地。
 *
 * ## 注意
 *
 * 这是**开发/调试用**的旁路。正式发布可保留（无害，默认值即当前最优），
 * 也可在 `BuildConfig.DEBUG` 下才显示面板入口。
 */
object TunableParams {

    /** 跟随时间常数（秒）。越小越快。默认 0.06 对应「先快后慢」的柔顺跟随。 */
    var followTauSeconds by mutableFloatStateOf(0.06f)

    /** 底栏按压缩放倍数（WeKit 为 78/56 ≈ 1.393）。 */
    var pressedScale by mutableFloatStateOf(78f / 56f)

    /** 值弹簧刚度（越大越快归位）。 */
    var valueStiffness by mutableFloatStateOf(1000f)

    /** 值弹簧阻尼比（1 = 临界阻尼，不弹跳）。 */
    var valueDampingRatio by mutableFloatStateOf(1f)

    /** 负一屏开合时长下限（ms）。 */
    var drawerAnimBaseMs by mutableFloatStateOf(120f)

    /** 负一屏开合时长增量（ms），总时长 = base + extra × |Δ|。 */
    var drawerAnimExtraMs by mutableFloatStateOf(120f)

    /** 负一屏抽屉宽度占屏宽比例（WeKit 原值 0.84）。 */
    var drawerWidthFraction by mutableFloatStateOf(0.84f)

    /** 所有参数的默认值（用于「重置」）。 */
    private val defaults: List<Pair<String, Float>> = listOf(
        "followTauSeconds" to 0.06f,
        "pressedScale" to 78f / 56f,
        "valueStiffness" to 1000f,
        "valueDampingRatio" to 1f,
        "drawerAnimBaseMs" to 120f,
        "drawerAnimExtraMs" to 120f,
        "drawerWidthFraction" to 0.84f,
    )

    /** 可调项描述（供面板渲染）。 */
    data class Spec(
        val key: String,
        val label: String,
        val min: Float,
        val max: Float,
        val read: () -> Float,
        val write: (Float) -> Unit,
    )

    /** 全部可调项。 */
    val specs: List<Spec> = listOf(
        Spec(
            key = "followTauSeconds",
            label = "跟随时间常数 τ（秒，越小越快）",
            min = 0.01f, max = 0.30f,
            read = { followTauSeconds },
            write = { followTauSeconds = it },
        ),
        Spec(
            key = "pressedScale",
            label = "按压缩放倍数",
            min = 1f, max = 1.8f,
            read = { pressedScale },
            write = { pressedScale = it },
        ),
        Spec(
            key = "valueStiffness",
            label = "值弹簧刚度",
            min = 200f, max = 3000f,
            read = { valueStiffness },
            write = { valueStiffness = it },
        ),
        Spec(
            key = "valueDampingRatio",
            label = "值弹簧阻尼比（1=不弹跳）",
            min = 0.3f, max = 1.5f,
            read = { valueDampingRatio },
            write = { valueDampingRatio = it },
        ),
        Spec(
            key = "drawerAnimBaseMs",
            label = "抽屉动画基础时长（ms）",
            min = 40f, max = 400f,
            read = { drawerAnimBaseMs },
            write = { drawerAnimBaseMs = it },
        ),
        Spec(
            key = "drawerAnimExtraMs",
            label = "抽屉动画增量时长（ms）",
            min = 0f, max = 400f,
            read = { drawerAnimExtraMs },
            write = { drawerAnimExtraMs = it },
        ),
        Spec(
            key = "drawerWidthFraction",
            label = "抽屉宽度占屏宽比例",
            min = 0.5f, max = 1f,
            read = { drawerWidthFraction },
            write = { drawerWidthFraction = it },
        ),
    )

    /** 重置为默认值。 */
    fun resetAll() {
        followTauSeconds = 0.06f
        pressedScale = 78f / 56f
        valueStiffness = 1000f
        valueDampingRatio = 1f
        drawerAnimBaseMs = 120f
        drawerAnimExtraMs = 120f
        drawerWidthFraction = 0.84f
    }

    /** 导出为可复制的文本（方便写回源码作默认值）。 */
    fun export(): String = buildString {
        appendLine("// 当前调参值（可直接写回源码默认值）：")
        specs.forEach { spec ->
            appendLine("${spec.key} = ${spec.read()}")
        }
    }
}