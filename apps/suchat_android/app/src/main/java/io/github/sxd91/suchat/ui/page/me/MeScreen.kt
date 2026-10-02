package io.github.sxd91.suchat.ui.page.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import io.github.sxd91.suchat.ui.component.LocalTopBarInset
import io.github.sxd91.suchat.ui.component.SuchatAvatar
import io.github.sxd91.suchat.ui.component.SuchatEntryRow
import io.github.sxd91.suchat.ui.component.SuchatScaffold
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 「我的」tab。
 *
 * ## 本轮修正（用户新需求 1）
 *
 * 顶栏切换为「老挂同款」渐变模糊顶栏（[SuchatScaffold]）。
 * 个人卡片与此前一致（头像莫奈取色、miuix 矢量图标、透明底）。
 */
@Composable
fun MeScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
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

    SuchatScaffold(
        title = "我的",
        onBack = null,
        bottomInset = bottomInset,
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
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
