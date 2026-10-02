package io.github.sxd91.suchat.ui.page.secondary

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.icon.SuchatIcons
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.core.nav.SuchatPage
import io.github.sxd91.suchat.data.SampleData
import io.github.sxd91.suchat.ui.component.SuchatAvatar
import io.github.sxd91.suchat.ui.component.SuchatChatRow
import io.github.sxd91.suchat.ui.component.SuchatEntryRow
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 二级页面集合 —— 全部使用 miuix 组件与语义色（无色块底、无 emoji）。
 */

/** 通用二级页脚手架：顶栏（返回 + 标题）+ 内容。 */
@Composable
fun SuchatSecondaryScaffold(
    title: String,
    onBack: () -> Unit,
    bottomInset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val c = MiuixTheme.colorScheme
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        // 顶栏：返回键 + 标题（miuix 矢量返回图标）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .height(48.dp)
                .background(c.surfaceContainer)
                .padding(start = 6.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiuixIconButton(onClick = onBack) {
                MiuixIcon(
                    imageVector = SuchatIcons.Back,
                    contentDescription = "返回",
                    tint = c.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
            MiuixText(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.onSurface,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        content()
    }
}

// ============================== 联系人子页 ==============================

/** 「新的朋友」。 */
@Composable
fun NewFriendsScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    SuchatSecondaryScaffold(title = "新的朋友", onBack = { nav.pop() }, bottomInset = bottomInset) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            items(SampleData.friendRequests, key = { it.id }) { req ->
                SuchatChatRow(
                    title = req.name,
                    subtitle = req.message,
                    time = "",
                    avatarName = req.name,
                    avatarSeed = req.id,
                    onClick = { nav.push(SuchatPage.ContactDetail(req.id)) },
                )
            }
        }
    }
}

/** 「群聊」。 */
@Composable
fun GroupChatsScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    SuchatSecondaryScaffold(title = "群聊", onBack = { nav.pop() }, bottomInset = bottomInset) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            items(SampleData.chats.filter { it.isGroup }, key = { it.id }) { group ->
                SuchatChatRow(
                    title = group.name,
                    subtitle = group.lastMessage,
                    time = group.time,
                    avatarName = group.name,
                    avatarSeed = group.id,
                    onClick = { nav.push(SuchatPage.ChatDetail(group.id)) },
                )
            }
        }
    }
}

/** 「标签」。 */
@Composable
fun TagsScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    SuchatSecondaryScaffold(title = "标签", onBack = { nav.pop() }, bottomInset = bottomInset) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            items(listOf("同事" to 8, "家人" to 4, "大学同学" to 15, "健身伙伴" to 3)) { (name, count) ->
                SuchatEntryRow(
                    title = name,
                    icon = SuchatIcons.Favorites,
                    trailingText = "$count 人",
                    onClick = { nav.pop() },
                )
            }
        }
    }
}

/** 「公众号」。 */
@Composable
fun OfficialAccountsScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    SuchatSecondaryScaffold(title = "公众号", onBack = { nav.pop() }, bottomInset = bottomInset) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            items(
                listOf("人民日报", "新华社", "环球时报", "央视新闻"),
                key = { it },
            ) { name ->
                SuchatChatRow(
                    title = name,
                    subtitle = "点击查看最新文章",
                    time = "",
                    avatarName = name,
                    avatarSeed = name,
                )
            }
        }
    }
}

/** 联系人详情。 */
@Composable
fun ContactDetailScreen(nav: SuchatNavigator, contactId: String, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    val contact = remember(contactId) { SampleData.contacts.firstOrNull { it.id == contactId } }

    SuchatSecondaryScaffold(
        title = contact?.remark ?: contact?.name ?: "联系人",
        onBack = { nav.pop() },
        bottomInset = bottomInset,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            item(key = "profile") {
                Column(Modifier.background(c.surface)) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SuchatAvatar(
                            name = contact?.name ?: "?",
                            seed = contactId,
                            size = 64.dp,
                            corner = 10.dp,
                        )
                        Column(Modifier.padding(start = 16.dp)) {
                            MiuixText(
                                text = contact?.name ?: "未知",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = c.onSurface,
                            )
                            if (contact?.remark != null) {
                                Spacer(Modifier.height(4.dp))
                                MiuixText(
                                    text = "备注：${contact.remark}",
                                    fontSize = 13.sp,
                                    color = c.onSurfaceSecondary,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            item(key = "actions") {
                Column(Modifier.background(c.surface)) {
                    SuchatEntryRow(
                        title = "发消息",
                        icon = SuchatIcons.Chats,
                        onClick = {
                            val chat = SampleData.chats.firstOrNull { it.name == contact?.name }
                            nav.push(SuchatPage.ChatDetail(chat?.id ?: "chat_003"))
                        },
                    )
                    SuchatEntryRow(
                        title = "音视频通话",
                        icon = SuchatIcons.VideoCall,
                        onClick = { },
                        showDivider = false,
                    )
                }
            }
        }
    }
}

// ============================== 发现子页 ==============================

/** 朋友圈。 */
@Composable
fun MomentsScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    SuchatSecondaryScaffold(title = "朋友圈", onBack = { nav.pop() }, bottomInset = bottomInset) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            items(SampleData.moments, key = { it.id }) { moment ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .padding(16.dp),
                ) {
                    Row {
                        SuchatAvatar(moment.authorName, seed = moment.id, size = 44.dp)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        ) {
                            MiuixText(
                                text = moment.authorName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = c.primary,
                            )
                            Spacer(Modifier.height(6.dp))
                            MiuixText(
                                text = moment.content,
                                fontSize = 15.sp,
                                color = c.onSurface,
                            )
                            if (moment.imageColors.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                MomentImageGrid(moment.imageColors)
                            }
                            Spacer(Modifier.height(10.dp))
                            MiuixText(
                                text = moment.timeAgo,
                                fontSize = 12.sp,
                                color = c.onSurfaceVariantSummary,
                            )
                            if (moment.likes.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(c.surfaceContainerHigh)
                                        .padding(10.dp),
                                ) {
                                    MiuixText(
                                        text = moment.likes,
                                        fontSize = 13.sp,
                                        color = c.onSurfaceSecondary,
                                    )
                                }
                            }
                            if (moment.comments.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(c.surfaceContainerHigh)
                                        .padding(10.dp),
                                ) {
                                    Column {
                                        moment.comments.forEach { (who, text) ->
                                            MiuixText(
                                                text = "$who：$text",
                                                fontSize = 13.sp,
                                                color = c.onSurface,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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

/** 朋友圈九宫格图片占位（用语义色块，不使用 emoji）。 */
@Composable
private fun MomentImageGrid(colors: List<Color>) {
    val rows = colors.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rowColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color),
                    )
                }
            }
        }
    }
}

/** 占位页（视频号 / 扫一扫 / 看一看 / 搜一搜 / 小程序 / 服务 / 收藏 / 卡包 / 表情）。 */
@Composable
fun PlaceholderScreen(
    nav: SuchatNavigator,
    title: String,
    description: String,
    bottomInset: Dp = 0.dp,
    icon: androidx.compose.ui.graphics.vector.ImageVector = SuchatIcons.Discover,
) {
    val c = MiuixTheme.colorScheme
    SuchatSecondaryScaffold(title = title, onBack = { nav.pop() }, bottomInset = bottomInset) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.surface),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MiuixIcon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = c.onSurfaceSecondary,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(16.dp))
                MiuixText(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = c.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                MiuixText(
                    text = description,
                    fontSize = 13.sp,
                    color = c.onSurfaceSecondary,
                )
                Spacer(Modifier.height(4.dp))
                MiuixText(
                    text = "前端占位页 · 待接入",
                    fontSize = 12.sp,
                    color = c.onSurfaceVariantSummary,
                )
            }
        }
    }
}

// ============================== 我的子页 ==============================

/** 个人信息页。 */
@Composable
fun ProfileScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    val me = SampleData.me

    SuchatSecondaryScaffold(title = "个人信息", onBack = { nav.pop() }, bottomInset = bottomInset) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            item(key = "avatar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiuixText("头像", fontSize = 16.sp, color = c.onSurface)
                    Spacer(Modifier.weight(1f))
                    SuchatAvatar(me.name, seed = me.suchatId, size = 56.dp, corner = 10.dp)
                }
            }
            item(key = "fields") {
                Spacer(Modifier.height(8.dp))
                Column(Modifier.background(c.surface)) {
                    listOf(
                        "名字" to me.name,
                        "Suchat 号" to me.suchatId,
                        "我的二维码" to "",
                    ).forEachIndexed { index, (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MiuixText(label, fontSize = 16.sp, color = c.onSurface)
                            Spacer(Modifier.weight(1f))
                            MiuixText(value, fontSize = 14.sp, color = c.onSurfaceSecondary)
                        }
                        if (index != 2) {
                            Box(
                                Modifier
                                    .padding(start = 20.dp)
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(c.outline.copy(alpha = 0.35f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 设置页。 */
@Composable
fun SettingsScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    SuchatSecondaryScaffold(title = "设置", onBack = { nav.pop() }, bottomInset = bottomInset) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            item(key = "g1") {
                Column(Modifier.background(c.surface)) {
                    SuchatEntryRow("账号与安全", SuchatIcons.Me, onClick = {})
                    SuchatEntryRow("青少年模式", SuchatIcons.Favorites, onClick = {})
                    SuchatEntryRow("关怀模式", SuchatIcons.Settings, onClick = {}, showDivider = false)
                }
            }
            item(key = "g2") {
                Spacer(Modifier.height(8.dp))
                Column(Modifier.background(c.surface)) {
                    SuchatEntryRow("新消息通知", SuchatIcons.Chats, onClick = {})
                    SuchatEntryRow("聊天", SuchatIcons.Messages, onClick = {})
                    SuchatEntryRow("隐私", SuchatIcons.Lock, onClick = {})
                    SuchatEntryRow("通用", SuchatIcons.Settings, onClick = {}, showDivider = false)
                }
            }
            item(key = "about") {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MiuixText("Suchat", fontSize = 14.sp, color = c.onSurfaceSecondary)
                    Spacer(Modifier.height(4.dp))
                    MiuixText(
                        "版本 0.1.0",
                        fontSize = 12.sp,
                        color = c.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}