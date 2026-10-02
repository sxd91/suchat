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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.icon.SuchatIcons
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.core.nav.SuchatPage
import io.github.sxd91.suchat.ui.component.SuchatEntryRow
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 发现页。
 *
 * ## 契约要求
 *
 * 「发现 contains 朋友圈 and 漂流瓶」—— 这两项排在首位。
 *
 * ## 本轮修正（用户第 3、6 条）
 *
 * 图标从 emoji（◉ / ▶ / 📡…）换成 **miuix 矢量图标**（见 [SuchatIcons]），
 * 并**去掉彩色色块背板**（图标直接着主题色，透明底）。
 */
@Composable
fun DiscoverScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // 分组：每组是 (标题, 图标, 路由) 的列表。
    val groups: List<List<Triple<String, ImageVector, SuchatPage?>>> = listOf(
        // 组 1：朋友圈 + 漂流瓶（契约核心）
        listOf(
            Triple("朋友圈", SuchatIcons.Moments, SuchatPage.Moments),
            Triple("漂流瓶", SuchatIcons.DriftBottle, SuchatPage.DriftBottle),
        ),
        // 组 2：视频号 / 直播
        listOf(
            Triple("视频号", SuchatIcons.Channels, SuchatPage.Channels),
            Triple("直播", SuchatIcons.Live, SuchatPage.Channels),
        ),
        // 组 3：扫一扫 / 摇一摇 / 看一看 / 搜一搜
        listOf(
            Triple("扫一扫", SuchatIcons.Scan, SuchatPage.Scan),
            Triple("摇一摇", SuchatIcons.Shake, SuchatPage.Scan),
            Triple("看一看", SuchatIcons.TopStories, SuchatPage.TopStories),
            Triple("搜一搜", SuchatIcons.SearchDiscover, SuchatPage.SearchDiscover),
        ),
        // 组 4：购物 / 游戏
        listOf(
            Triple("购物", SuchatIcons.Shopping, null),
            Triple("游戏", SuchatIcons.Games, null),
        ),
        // 组 5：小程序
        listOf(
            Triple("小程序", SuchatIcons.MiniPrograms, SuchatPage.MiniPrograms),
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        // 顶栏（发现页无左右操作）。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .height(48.dp)
                .background(c.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            MiuixText(
                text = "发现",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.onSurface,
            )
        }

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
                                .background(c.surfaceContainer),
                        )
                        Column(Modifier.background(c.surface)) {
                            group.forEachIndexed { rowIndex, (title, icon, page) ->
                                SuchatEntryRow(
                                    title = title,
                                    icon = icon,
                                    onClick = page?.let { p -> { nav.push(p) } },
                                    showDivider = rowIndex != group.lastIndex,
                                )
                            }
                        }
                    }
                }
            }
            item(key = "footer") {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(c.surfaceContainer),
                )
            }
        }
    }
}
