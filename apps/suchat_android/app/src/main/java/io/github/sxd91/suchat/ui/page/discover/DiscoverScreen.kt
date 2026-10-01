package io.github.sxd91.suchat.ui.page.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.theme.LocalSuchatTokens
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.core.nav.SuchatPage
import io.github.sxd91.suchat.data.SampleData
import io.github.sxd91.suchat.ui.component.EntryRow

/**
 * 「发现」tab。
 *
 * 微信结构：几个白色分组，组内每行「彩色图标 + 标题 + 右箭头」，
 * 分组之间用浅灰间隔（8dp）。
 *
 * ```
 * [ 朋友圈                       ]   ← 第一组（单行）
 * ────────────────────────────
 * [ 视频号                       ]   ← 第二组（视频号 / 直播）
 * ────────────────────────────
 * [ 扫一扫 ][ 摇一摇            ]   ← 第三组
 * [ 看一看 ][ 搜一搜            ]
 * ────────────────────────────
 * [ 购物 ][ 游戏                ]   ← 第四组（占位）
 * ```
 */
@Composable
fun DiscoverScreen(
    nav: SuchatNavigator,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val colors = LocalSuchatTokens.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // 按「分组」组织条目：每个子列表是一组，组间留灰缝。
    val groups = remember {
        listOf(
            // 组 1：朋友圈 + 漂流瓶（契约明确要求：发现 contains 朋友圈 and 漂流瓶）
            listOf(
                Triple("朋友圈", 0xFF3F7FBF, "◉"),
                Triple("漂流瓶", 0xFF2E8B9E, "🍾"),
            ),
            // 组 2：视频号 / 直播
            listOf(
                Triple("视频号", 0xFFF9961D, "▶"),
                Triple("直播", 0xFFFF5636, "📡"),
            ),
            // 组 3：扫一扫 / 摇一摇 / 看一看 / 搜一搜
            listOf(
                Triple("扫一扫", 0xFF006BED, "⊞"),
                Triple("摇一摇", 0xFF2988EE, "📳"),
                Triple("看一看", 0xFFFEC206, "☰"),
                Triple("搜一搜", 0xFFF83734, "⌕"),
            ),
            // 组 4：购物 / 游戏（占位）
            listOf(
                Triple("购物", 0xFFFA5151, "🛒"),
                Triple("游戏", 0xFF07C160, "🎮"),
            ),
            // 组 5：小程序
            listOf(Triple("小程序", 0xFF343BEE, "⬡")),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.pageBackground),
    ) {
        // --- 顶栏（发现页无左右操作，仅标题） ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .height(48.dp)
                .background(colors.topBar),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "发现",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
            )
        }

        // --- 分组列表 ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            groups.forEachIndexed { groupIndex, group ->
                item(key = "group_$groupIndex") {
                    Column {
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(colors.pageBackground),
                        )
                        Column(Modifier.background(colors.cardBackground)) {
                            group.forEachIndexed { rowIndex, (title, colorHex, glyph) ->
                                EntryRow(
                                    title = title,
                                    iconColor = androidx.compose.ui.graphics.Color(colorHex),
                                    glyph = glyph,
                                    onClick = {
                                        val page = when (title) {
                                            "朋友圈" -> SuchatPage.Moments
                                            "漂流瓶" -> SuchatPage.DriftBottle
                                            "视频号" -> SuchatPage.Channels
                                            "扫一扫" -> SuchatPage.Scan
                                            "看一看" -> SuchatPage.TopStories
                                            "搜一搜" -> SuchatPage.SearchDiscover
                                            "小程序" -> SuchatPage.MiniPrograms
                                            else -> null
                                        }
                                        if (page != null) nav.push(page)
                                    },
                                    showDivider = rowIndex != group.lastIndex,
                                )
                            }
                        }
                    }
                }
            }
            item(key = "discover_footer") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(colors.pageBackground),
                )
            }
        }
    }
}