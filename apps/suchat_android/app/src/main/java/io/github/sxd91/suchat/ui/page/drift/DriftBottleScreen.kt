package io.github.sxd91.suchat.ui.page.drift

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.icon.SuchatIcons
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.ui.page.secondary.SuchatSecondaryScaffold
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 漂流瓶 —— Suchat 的特色功能。
 *
 * 契约（`docs/android-experience.md`）明确：「发现 contains 朋友圈 and 漂流瓶」。
 *
 * ## 本轮修正（用户第 3、6 条）
 *
 * - 图标全部换成 miuix 矢量图标（[SuchatIcons]），不再用 🍾 / 🌊 这类 emoji；
 * - 卡片颜色改用 miuix 语义色（随莫奈取色变化），不再硬编码深蓝渐变。
 *
 * ## 页面结构
 *
 * ```
 * [ ‹ 漂流瓶                         ]
 * [   海域卡（可捞 / 已扔 / 已捞）      ]
 * [ 扔一个瓶子  |  捞一个瓶子          ]
 * [ 捞取结果（点「捞」后出现）          ]
 * [ 我捞到的瓶子（列表）                ]
 * ```
 */
@Composable
fun DriftBottleScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    var picked by remember { mutableStateOf<DriftBottle?>(null) }

    SuchatSecondaryScaffold(
        title = "漂流瓶",
        onBack = { nav.pop() },
        bottomInset = bottomInset,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            // --- 海域卡 ---
            item(key = "sea") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(c.primaryContainer)
                        .padding(20.dp),
                ) {
                    Column {
                        MiuixText(
                            text = "漂流瓶海域",
                            color = c.onPrimaryContainer,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        MiuixText(
                            text = "把一句心事交给未知的远方",
                            color = c.onPrimaryContainer.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            StatChip("今日可捞", "3 / 3")
                            StatChip("已扔出", "12")
                            StatChip("已捞到", "27")
                        }
                    }
                }
            }

            // --- 两个主操作 ---
            item(key = "actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ActionCard(
                        icon = SuchatIcons.DriftBottle,
                        title = "扔一个瓶子",
                        subtitle = "写下一句话，随波而去",
                        modifier = Modifier.weight(1f),
                        onClick = { },
                    )
                    ActionCard(
                        icon = SuchatIcons.Undo,
                        title = "捞一个瓶子",
                        subtitle = "看看远方的陌生人",
                        modifier = Modifier.weight(1f),
                        onClick = { picked = DriftBottleSamples.randomOne() },
                    )
                }
            }

            // --- 捞取结果 ---
            if (picked != null) {
                item(key = "picked") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(c.surfaceContainerHigh)
                            .padding(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MiuixIcon(
                                imageVector = SuchatIcons.DriftBottle,
                                contentDescription = null,
                                tint = c.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            MiuixText(
                                text = "你捞到了一个瓶子",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = c.onSurface,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        MiuixText(
                            text = picked!!.content,
                            fontSize = 16.sp,
                            color = c.onSurface,
                        )
                        Spacer(Modifier.height(10.dp))
                        MiuixText(
                            text = "${picked!!.from} · ${picked!!.timeAgo}",
                            fontSize = 12.sp,
                            color = c.onSurfaceVariantSummary,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PillButton("回复") { }
                            PillButton("扔回海里") { picked = null }
                        }
                    }
                }
            }

            // --- 我捞到的瓶子 ---
            item(key = "history_header") {
                MiuixText(
                    text = "我捞到的瓶子",
                    fontSize = 13.sp,
                    color = c.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp),
                )
            }

            items(DriftBottleSamples.history.size) { index ->
                val bottle = DriftBottleSamples.history[index]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    MiuixText(
                        text = bottle.content,
                        fontSize = 15.sp,
                        color = c.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    MiuixText(
                        text = "${bottle.from} · ${bottle.timeAgo}",
                        fontSize = 12.sp,
                        color = c.onSurfaceVariantSummary,
                    )
                }
                Box(
                    Modifier
                        .padding(start = 16.dp)
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(c.outline.copy(alpha = 0.35f)),
                )
            }
        }
    }
}

/** 单条漂流瓶。 */
data class DriftBottle(
    val content: String,
    val from: String,
    val timeAgo: String,
)

/** 漂流瓶占位数据（甲方要求：没有的数据用占位示例代替）。 */
object DriftBottleSamples {
    val history = listOf(
        DriftBottle("愿你在每个陌生的地方，都能遇见温柔。", "来自 青岛的瓶子", "2小时前"),
        DriftBottle("今天加班到很晚，但项目终于上线了。", "来自 成都的瓶子", "昨天"),
        DriftBottle("有人也喜欢下雨天吗？", "来自 杭州的瓶子", "3天前"),
    )

    private val pool = history + listOf(
        DriftBottle("第一次一个人旅行，有点紧张。", "来自 西安的瓶子", "刚刚"),
        DriftBottle("希望明年能考上理想的学校。", "来自 长沙的瓶子", "刚捞到"),
        DriftBottle("深夜emo，有没有还没睡的人。", "来自 南京的瓶子", "刚捞到"),
    )

    fun randomOne(): DriftBottle = pool.random()
}

/** 海域卡上的统计小格。 */
@Composable
private fun StatChip(label: String, value: String) {
    val c = MiuixTheme.colorScheme
    Column {
        MiuixText(
            text = value,
            color = c.onPrimaryContainer,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        MiuixText(
            text = label,
            color = c.onPrimaryContainer.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
    }
}

/** 主操作卡（扔 / 捞）—— 图标为 miuix 矢量，无 emoji。 */
@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = MiuixTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MiuixIcon(
            imageVector = icon,
            contentDescription = null,
            tint = c.primary,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.height(8.dp))
        MiuixText(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = c.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        MiuixText(
            text = subtitle,
            fontSize = 11.sp,
            color = c.onSurfaceVariantSummary,
            lineHeight = 15.sp,
        )
    }
}

/** 胶囊小按钮。 */
@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    val c = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(c.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        MiuixText(label, fontSize = 13.sp, color = c.onSurface)
    }
}