package io.github.sxd91.suchat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.glass.animation.TunableParams
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 动效调参面板（**热调**：拖动即生效，无需重新编译安装）。
 *
 * ## 为什么做这个
 *
 * 调动效参数（跟随速度 τ、弹簧刚度/阻尼、抽屉宽度…）时，
 * 「改代码 → 编译 → 装包 → 重启」一轮数分钟，试参极慢。
 *
 * 本面板把参数暴露成滑块：
 *  - 底层是 `TunableParams`（Compose `mutableStateOf` 持有）；
 *  - 拖动滑块 → 状态变化 → 所有读取该参数的组件**立即重组** → 即时预览；
 *  - 满意后点「导出」得到一组数值，写回源码默认值即可固化。
 *
 * ## 唤出方式
 *
 * **长按底栏任意位置**（不移动）→ 弹出本面板。关闭：点面板外 / 点「关闭」。
 *
 * @param onDismiss 关闭回调。
 */
@Composable
fun TuningPanel(onDismiss: () -> Unit) {
    val c = MiuixTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onDismiss),
    ) {
        // 面板本体（阻止点击穿透到遮罩）。
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(c.surfaceContainerHigh)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // --- 标题栏 ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiuixText(
                    text = "动效调参（拖动即生效）",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.surfaceContainerHighest)
                        .clickable { TunableParams.resetAll() }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    MiuixText(text = "重置", fontSize = 13.sp, color = c.onSurface)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.primary)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    MiuixText(text = "关闭", fontSize = 13.sp, color = c.onPrimary)
                }
            }

            Spacer(Modifier.height(6.dp))

            // --- 参数滑块列表（可滚动） ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TunableParams.specs.forEach { spec ->
                    // 读当前值 —— 读操作发生在组合里，值变化会触发本块重组。
                    val current = spec.read()
                    Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MiuixText(
                                text = spec.label,
                                fontSize = 13.sp,
                                color = c.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            MiuixText(
                                text = "%.3f".format(current),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = c.primary,
                            )
                        }
                        Slider(
                            value = current,
                            onValueChange = { spec.write(it) },
                            valueRange = spec.min..spec.max,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // --- 导出（把当前值抄回源码） ---
                var exported by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surfaceContainerHighest)
                        .padding(12.dp),
                ) {
                    MiuixText(
                        text = if (exported) TunableParams.export() else "点「显示当前值」查看可写回源码的参数",
                        fontSize = 11.sp,
                        color = c.onSurfaceSecondary,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surfaceContainerHighest)
                        .clickable { exported = !exported }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MiuixText(
                        text = if (exported) "隐藏当前值" else "显示当前值",
                        fontSize = 13.sp,
                        color = c.onSurface,
                    )
                }
            }
        }
    }
}