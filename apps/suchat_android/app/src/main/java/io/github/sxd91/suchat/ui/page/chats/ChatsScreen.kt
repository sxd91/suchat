package io.github.sxd91.suchat.ui.page.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.theme.LocalSuchatTokens
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.core.nav.SuchatPage
import io.github.sxd91.suchat.data.SampleData
import io.github.sxd91.suchat.ui.component.Avatar
import io.github.sxd91.suchat.ui.component.UnreadBadge
import io.github.sxd91.suchat.ui.component.WeChatListItem

/**
 * 「消息」tab —— 会话列表页。
 *
 * 布局对齐微信 Android 版：
 *  ```
 *  [ 微信 (标题)              🔍  ＋ ]   ← 顶栏（浅灰）
 *  [ 🔍 搜索                    ]       ← 搜索框（灰底圆角条）
 *  [ 头像 | 名字 / 摘要 | 时间 / 红点 ]  ← 会话列表
 *  ...
 *  ```
 *
 * @param bottomInset 底部内容限位（悬浮底栏高度 + 手势条）——
 *   只作用于列表的**滚动终点**，不做内容区 padding，
 *   保证列表能滑到底栏下方产生折射（液态玻璃观感前提）。
 */
@Composable
fun ChatsScreen(
    nav: SuchatNavigator,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val colors = LocalSuchatTokens.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.pageBackground),
    ) {
        // --- 顶栏 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .height(48.dp)
                .background(colors.topBar)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "消息",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            // 搜索入口
            Text(
                text = "🔍",
                fontSize = 18.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { nav.push(SuchatPage.Search) }
                    .padding(4.dp),
            )
            // 「+」菜单入口
            Text(
                text = "＋",
                fontSize = 20.sp,
                color = colors.textPrimary,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { nav.push(SuchatPage.AddMenu) }
                    .padding(4.dp),
            )
        }

        // --- 会话列表 ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.cardBackground),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            // 搜索条（微信列表顶部那条灰底搜索框）
            item(key = "search_bar") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.cardBackground)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.pageBackground)
                            .clickable { nav.push(SuchatPage.Search) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "🔍  搜索",
                            fontSize = 15.sp,
                            color = colors.textHint,
                        )
                    }
                }
            }

            items(SampleData.chats, key = { it.id }) { chat ->
                WeChatListItem(
                    leading = {
                        Avatar(
                            name = chat.name,
                            color = chat.avatarColor,
                            size = 48.dp,
                            isGroup = chat.isGroup,
                        )
                    },
                    title = chat.name,
                    subtitle = chat.lastMessage,
                    onClick = { nav.push(SuchatPage.ChatDetail(chat.id)) },
                    trailingTop = {
                        Text(
                            text = chat.time,
                            fontSize = 12.sp,
                            color = colors.textHint,
                        )
                    },
                    trailing = {
                        if (chat.unreadCount > 0) {
                            UnreadBadge(count = chat.unreadCount, muted = chat.muted)
                        }
                    },
                )
            }

            // 列表底部留白（会话少时也保证能滑到底栏下方）
            item(key = "footer") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(colors.cardBackground),
                )
            }
        }
    }
}