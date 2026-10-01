package io.github.sxd91.suchat.ui.page.drift

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.theme.LocalSuchatTokens
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.ui.component.WeChatTopBar

/**
 * 漂流瓶 —— Suchat 的特色功能。
 *
 * 契约（`docs/android-experience.md`）明确：「发现 contains 朋友圈 and 漂流瓶」。
 * 这是 Suchat 区别于微信的**跨时代**社交玩法：把一句心事交给未知的远方，
 * 也可能捞起别人的瓶子。
 *
 * ## 页面结构
 *
 * ```
 * [ ‹ 漂流瓶                        ]   ← 顶栏
 * [   海面渐变卡（我的瓶子数 / 捞取额度）   ]
 * [ 🍾 扔一个瓶子  |  🌊 捞一个瓶子      ]   ← 两个主操作
 * [ 我捞到的瓶子（列表）                    ]
 * ```
 *
 * 前端阶段数据为占位示例（见 [DriftBottleSamples]）。
 */
@Composable
fun DriftBottleScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val tokens = LocalSuchatTokens.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // 捞取状态：点击「捞一个」后展示一个随机瓶子（前端占位交互）。
    var picked by remember { mutableStateOf<DriftBottle?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.pageBackground),
    ) {
        Box(Modifier.padding(top = statusBarPadding)) {
            WeChatTopBar(title = "漂流瓶", onBack = { nav.pop() })
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            // --- 海面卡 ---
            item(key = "sea") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1E5A78), Color(0xFF2E8B9E), Color(0xFF7EC8D8)),
                            )
                        )
                        .padding(20.dp),
                ) {
                    Column {
                        Text(
                            text = "漂流瓶海域",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "把一句心事交给未知的远方",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
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
                        emoji = "🍾",
                        title = "扔一个瓶子",
                        subtitle = "写下一句话，随波而去",
                        modifier = Modifier.weight(1f),
                        onClick = { /* 占位：后续接入发布流程 */ },
                    )
                    ActionCard(
                        emoji = "🌊",
                        title = "捞一个瓶子",
                        subtitle = "看看远方的陌生人在想什么",
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(tokens.cardBackground)
                            .padding(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🌊", fontSize = 20.sp)
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = "你捞到了一个瓶子",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = tokens.textPrimary,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = picked!!.content,
                            fontSize = 16.sp,
                            color = tokens.textPrimary,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "${picked!!.from} · ${picked!!.timeAgo}",
                            fontSize = 12.sp,
                            color = tokens.textHint,
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
                Text(
                    text = "我捞到的瓶子",
                    fontSize = 13.sp,
                    color = tokens.textSecondary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp),
                )
            }

            items(DriftBottleSamples.history.size) { index ->
                val bottle = DriftBottleSamples.history[index]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.cardBackground)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = bottle.content,
                        fontSize = 15.sp,
                        color = tokens.textPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${bottle.from} · ${bottle.timeAgo}",
                        fontSize = 12.sp,
                        color = tokens.textHint,
                    )
                }
                Box(
                    Modifier
                        .padding(start = 16.dp)
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(tokens.divider),
                )
            }

            item(key = "drift_footer") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(tokens.pageBackground),
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

/** 海面卡上的统计小格。 */
@Composable
private fun StatChip(label: String, value: String) {
    Column {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
    }
}

/** 主操作卡（扔 / 捞）。 */
@Composable
private fun ActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tokens = LocalSuchatTokens.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.cardBackground)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 32.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = tokens.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = tokens.textHint,
            lineHeight = 15.sp,
        )
    }
}

/** 胶囊小按钮（回复 / 扔回海里）。 */
@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    val tokens = LocalSuchatTokens.current
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(tokens.pageBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        Text(label, fontSize = 13.sp, color = tokens.textPrimary)
    }
}