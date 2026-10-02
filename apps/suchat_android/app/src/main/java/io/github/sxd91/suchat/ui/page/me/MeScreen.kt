package io.github.sxd91.suchat.ui.page.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import io.github.sxd91.suchat.data.SampleData
import io.github.sxd91.suchat.ui.component.SuchatAvatar
import io.github.sxd91.suchat.ui.component.SuchatEntryRow
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 「我的」tab。
 *
 * ## 本轮修正（用户第 3、6 条）
 *
 * - 图标从 emoji 换成 miuix 矢量图标；
 * - 去掉彩色色块背板（图标透明底 + 主题色）；
 * - 头像用 [SuchatAvatar]（**莫奈取色**，用户第 1 条）。
 */
@Composable
fun MeScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val me = SampleData.me

    // (标题, 图标, 路由)
    val entries: List<Triple<String, ImageVector, SuchatPage>> = listOf(
        Triple("服务", SuchatIcons.Wallet, SuchatPage.Services),
        Triple("收藏", SuchatIcons.Favorites, SuchatPage.Favorites),
        Triple("朋友圈", SuchatIcons.Moments, SuchatPage.MyMoments),
        Triple("卡包", SuchatIcons.Wallet, SuchatPage.Cards),
        Triple("表情", SuchatIcons.Favorites, SuchatPage.Stickers),
        Triple("设置", SuchatIcons.Settings, SuchatPage.Settings),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        // 顶栏（「我的」页无标题）。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .height(48.dp)
                .background(c.surfaceContainer),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            // 个人卡片。
            item(key = "profile") {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(c.surfaceContainer),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .clickable { nav.push(SuchatPage.Profile) }
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SuchatAvatar(
                        name = me.name,
                        seed = me.suchatId,
                        size = 64.dp,
                        corner = 12.dp,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                    ) {
                        MiuixText(
                            text = me.name,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = c.onSurface,
                        )
                        Spacer(Modifier.height(6.dp))
                        MiuixText(
                            text = "Suchat 号：${me.suchatId}",
                            fontSize = 13.sp,
                            color = c.onSurfaceSecondary,
                        )
                    }
                    MiuixIcon(
                        imageVector = SuchatIcons.ChevronForward,
                        contentDescription = null,
                        tint = c.onSurfaceSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // 各项（每项单独一组，组间 8dp 灰缝）。
            entries.forEachIndexed { index, (title, icon, page) ->
                item(key = "entry_$index") {
                    Column {
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(c.surfaceContainer),
                        )
                        Column(Modifier.background(c.surface)) {
                            SuchatEntryRow(
                                title = title,
                                icon = icon,
                                onClick = { nav.push(page) },
                                showDivider = false,
                            )
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
