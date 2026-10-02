package io.github.sxd91.suchat.ui.page.chats

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.core.nav.SuchatPage
import io.github.sxd91.suchat.data.SampleData
import io.github.sxd91.suchat.ui.component.SuchatAvatar
import io.github.sxd91.suchat.ui.component.SuchatChatRow
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import io.github.sxd91.suchat.core.design.icon.SuchatIcons

/**
 * 消息页（微信「消息」tab）。
 *
 * ## 本轮修正（用户第 8 条）
 *
 * 顶栏从「标题 '消息' + 搜索/加号」改为 **WeKit 同款的三段式头部**：
 *
 * ```
 * [ 头像 ]  名字              [ 搜索 ] [ + ]
 *           状态文字
 * ```
 *
 *  - **点击头像** → 打开负一屏（WeKit 空间抽屉）；
 *  - **左滑**（在页面任意处）→ 也能打开负一屏（由外层 [WeKitDrawer] 处理）；
 *  - 状态文字显示账号状态（如「在线」/「已连接」）。
 *
 * 头像用 [SuchatAvatar]（莫奈取色），无色块底、无 emoji。
 *
 * @param onAvatarClick 点击头像的回调（由外层打开抽屉）。
 */
@Composable
fun ChatsScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
    onAvatarClick: () -> Unit = {},
    statusText: String = "在线",
) {
    val c = MiuixTheme.colorScheme
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val me = SampleData.me

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        // --- WeKit 三段式头部 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .height(64.dp)
                .background(c.surfaceContainer)
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 头像（点击 → 负一屏）。
            SuchatAvatar(
                name = me.name,
                seed = me.suchatId,
                size = 40.dp,
                corner = 8.dp,
                modifier = Modifier.clickable(onClick = onAvatarClick),
            )
            // 名字 + 状态。
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
                    .clickable(onClick = onAvatarClick),
            ) {
                MiuixText(
                    text = me.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onSurface,
                )
                MiuixText(
                    text = statusText,
                    fontSize = 12.sp,
                    color = c.onSurfaceSecondary,
                )
            }
            // 搜索。
            MiuixIconButton(onClick = { nav.push(SuchatPage.Search) }) {
                MiuixIcon(
                    imageVector = SuchatIcons.Search,
                    contentDescription = "搜索",
                    tint = c.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
            // 「+」菜单。
            MiuixIconButton(onClick = { nav.push(SuchatPage.AddMenu) }) {
                MiuixIcon(
                    imageVector = SuchatIcons.Add,
                    contentDescription = "添加",
                    tint = c.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // --- 会话列表 ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(c.surface),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            // 搜索条。
            item(key = "search_bar") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(c.surfaceContainerHigh)
                            .clickable { nav.push(SuchatPage.Search) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) {
                        MiuixIcon(
                            imageVector = SuchatIcons.Search,
                            contentDescription = null,
                            tint = c.onSurfaceSecondary,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        MiuixText(
                            text = "搜索",
                            fontSize = 15.sp,
                            color = c.onSurfaceSecondary,
                        )
                    }
                }
            }

            items(SampleData.chats, key = { it.id }) { chat ->
                SuchatChatRow(
                    title = chat.name,
                    subtitle = chat.lastMessage,
                    time = chat.time,
                    avatarName = chat.name,
                    avatarSeed = chat.id,
                    unreadCount = chat.unreadCount,
                    muted = chat.muted,
                    onClick = { nav.push(SuchatPage.ChatDetail(chat.id)) },
                )
            }

            item(key = "footer") {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(c.surface),
                )
            }
        }
    }
}