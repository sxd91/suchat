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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import io.github.sxd91.suchat.core.design.theme.LocalSuchatTokens
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.data.SampleData
import io.github.sxd91.suchat.ui.component.Avatar
import io.github.sxd91.suchat.ui.component.WeChatListItem
import io.github.sxd91.suchat.ui.component.WeChatTopBar

/**
 * 二级页面集合。
 *
 * 前端先行阶段这些页面用**占位内容**填充（甲方要求：没有的数据用占位示例代替），
 * 但结构、导航、交互都是最终形态，接真数据时只换内容源。
 */

// ============================== 通讯录子页 ==============================

/** 「新的朋友」。 */
@Composable
fun NewFriendsScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val colors = LocalSuchatTokens.current
    SecondaryScaffold(title = "新的朋友", onBack = { nav.pop() }, bottomInset = bottomInset) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            item(key = "nf_search") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.cardBackground)
                        .padding(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.pageBackground),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🔍  账号 / 手机号", fontSize = 15.sp, color = colors.textHint)
                    }
                }
            }
            items(SampleData.friendRequests, key = { it.id }) { req ->
                WeChatListItem(
                    leading = { Avatar(req.name, req.avatarColor, 44.dp) },
                    title = req.name,
                    subtitle = req.message,
                    trailing = {
                        if (req.accepted) {
                            Text(
                                text = "已添加",
                                fontSize = 14.sp,
                                color = colors.textHint,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.brand)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text("接受", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    },
                )
            }
        }
    }
}

/** 「群聊」列表。 */
@Composable
fun GroupChatsScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val colors = LocalSuchatTokens.current
    SecondaryScaffold(title = "群聊", onBack = { nav.pop() }, bottomInset = bottomInset) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            item(key = "gc_create") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.cardBackground)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("＋ 发起群聊", fontSize = 16.sp, color = colors.brand)
                }
            }
            items(SampleData.chats.filter { it.isGroup }, key = { it.id }) { group ->
                WeChatListItem(
                    leading = { Avatar(group.name, group.avatarColor, 44.dp, isGroup = true) },
                    title = group.name,
                    onClick = { nav.push(io.github.sxd91.suchat.core.nav.SuchatPage.ChatDetail(group.id)) },
                )
            }
        }
    }
}

/** 「标签」。 */
@Composable
fun TagsScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val colors = LocalSuchatTokens.current
    SecondaryScaffold(title = "标签", onBack = { nav.pop() }, bottomInset = bottomInset) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            item(key = "tag_new") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.cardBackground)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("＋ 新建标签", fontSize = 16.sp, color = colors.brand)
                }
            }
            item(key = "tag_list") {
                Column(Modifier.background(colors.cardBackground)) {
                    listOf(
                        "同事" to 8,
                        "家人" to 4,
                        "大学同学" to 15,
                        "健身伙伴" to 3,
                    ).forEachIndexed { index, (name, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(name, fontSize = 16.sp, color = colors.textPrimary)
                            Spacer(Modifier.weight(1f))
                            Text("$count 人", fontSize = 14.sp, color = colors.textSecondary)
                        }
                        if (index != 3) {
                            Box(
                                Modifier
                                    .padding(start = 16.dp)
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(colors.divider),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 「公众号」。 */
@Composable
fun OfficialAccountsScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val colors = LocalSuchatTokens.current
    SecondaryScaffold(title = "公众号", onBack = { nav.pop() }, bottomInset = bottomInset) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            item(key = "oa_grid") {
                Column(Modifier.background(colors.cardBackground)) {
                    listOf(
                        "人民日报" to Color(0xFFD32F2F),
                        "新华社" to Color(0xFFC62828),
                        "环球时报" to Color(0xFF1565C0),
                        "央视新闻" to Color(0xFFEF6C00),
                    ).forEachIndexed { index, (name, color) ->
                        WeChatListItem(
                            leading = { Avatar(name, color, 44.dp) },
                            title = name,
                            subtitle = "点击查看最新文章",
                            showDivider = index != 3,
                            dividerStart = 72.dp,
                        )
                    }
                }
            }
        }
    }
}

/** 联系人详情。 */
@Composable
fun ContactDetailScreen(nav: SuchatNavigator, contactId: String, bottomInset: Dp = 0.dp) {
    val colors = LocalSuchatTokens.current
    val contact = remember(contactId) { SampleData.contacts.firstOrNull { it.id == contactId } }

    SecondaryScaffold(
        title = contact?.remark ?: contact?.name ?: "联系人",
        onBack = { nav.pop() },
        bottomInset = bottomInset,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            item(key = "cd_profile") {
                Column(Modifier.background(colors.cardBackground)) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Avatar(
                            name = contact?.name ?: "?",
                            color = contact?.avatarColor ?: colors.brand,
                            size = 64.dp,
                            corner = 6.dp,
                        )
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(
                                contact?.name ?: "未知",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                            )
                            if (contact?.remark != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "备注：${contact.remark}",
                                    fontSize = 14.sp,
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            item(key = "cd_actions") {
                Column(Modifier.background(colors.cardBackground)) {
                    listOf("发消息", "音视频通话").forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (label == "发消息") {
                                        val chat = SampleData.chats.firstOrNull {
                                            it.name == contact?.name
                                        }
                                        if (chat != null) {
                                            nav.push(
                                                io.github.sxd91.suchat.core.nav.SuchatPage.ChatDetail(chat.id)
                                            )
                                        } else {
                                            nav.push(
                                                io.github.sxd91.suchat.core.nav.SuchatPage.ChatDetail("chat_003")
                                            )
                                        }
                                    }
                                }
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                        ) {
                            Text(label, fontSize = 16.sp, color = colors.brand)
                        }
                        if (index == 0) {
                            Box(
                                Modifier
                                    .padding(start = 20.dp)
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(colors.divider),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================== 发现子页 ==============================

/** 朋友圈（简化版：动态列表）。 */
@Composable
fun MomentsScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val colors = LocalSuchatTokens.current
    SecondaryScaffold(title = "朋友圈", onBack = { nav.pop() }, bottomInset = bottomInset) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            items(SampleData.moments, key = { it.id }) { moment ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.cardBackground)
                        .padding(16.dp),
                ) {
                    Row {
                        Avatar(moment.authorName, moment.authorColor, 44.dp)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        ) {
                            Text(
                                moment.authorName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.link,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                moment.content,
                                fontSize = 16.sp,
                                color = colors.textPrimary,
                            )
                            // 图片九宫格（占位色块）
                            if (moment.imageColors.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                MomentImageGrid(moment.imageColors)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                moment.timeAgo,
                                fontSize = 12.sp,
                                color = colors.textHint,
                            )
                            // 点赞
                            if (moment.likes.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.pageBackground)
                                        .padding(8.dp),
                                ) {
                                    Text(
                                        "❤ ${moment.likes}",
                                        fontSize = 14.sp,
                                        color = colors.link,
                                    )
                                }
                            }
                            // 评论
                            if (moment.comments.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.pageBackground)
                                        .padding(8.dp),
                                ) {
                                    Column {
                                        moment.comments.forEach { (who, text) ->
                                            Text(
                                                "$who：$text",
                                                fontSize = 14.sp,
                                                color = colors.textPrimary,
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
                        .background(colors.pageBackground),
                )
            }
        }
    }
}

/** 朋友圈九宫格图片占位。 */
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
                            .clip(RoundedCornerShape(4.dp))
                            .background(color),
                    )
                }
            }
        }
    }
}

/** 视频号 / 看一看 / 搜一搜 / 小程序 / 直播 —— 统一占位页。 */
@Composable
fun PlaceholderScreen(
    nav: SuchatNavigator,
    title: String,
    description: String,
    bottomInset: Dp = 0.dp,
) {
    val colors = LocalSuchatTokens.current
    SecondaryScaffold(title = title, onBack = { nav.pop() }, bottomInset = bottomInset) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.pageBackground),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🚧", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    description,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "（前端占位页 · 待接入）",
                    fontSize = 12.sp,
                    color = colors.textHint,
                )
            }
        }
    }
}

// ============================== 我子页 ==============================

/** 个人信息页。 */
@Composable
fun ProfileScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val colors = LocalSuchatTokens.current
    val me = SampleData.me

    SecondaryScaffold(title = "个人信息", onBack = { nav.pop() }, bottomInset = bottomInset) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            item(key = "p_avatar") {
                Column(Modifier.background(colors.cardBackground)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("头像", fontSize = 16.sp, color = colors.textPrimary)
                        Spacer(Modifier.weight(1f))
                        Avatar(me.name, me.avatarColor, 56.dp, 6.dp)
                        Text("›", fontSize = 20.sp, color = colors.textHint, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            item(key = "p_fields") {
                Spacer(Modifier.height(8.dp))
                Column(Modifier.background(colors.cardBackground)) {
                    listOf(
                        "名字" to me.name,
                        "微信号" to me.suchatId,
                        "我的二维码" to "›",
                        "更多" to "›",
                    ).forEachIndexed { index, (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, fontSize = 16.sp, color = colors.textPrimary)
                            Spacer(Modifier.weight(1f))
                            Text(
                                value,
                                fontSize = 15.sp,
                                color = colors.textSecondary,
                            )
                        }
                        if (index != 3) {
                            Box(
                                Modifier
                                    .padding(start = 20.dp)
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(colors.divider),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 设置页（微信风格分组列表）。 */
@Composable
fun SettingsScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val colors = LocalSuchatTokens.current
    SecondaryScaffold(title = "设置", onBack = { nav.pop() }, bottomInset = bottomInset) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            item(key = "s_group1") {
                Column(Modifier.background(colors.cardBackground)) {
                    listOf("账号与安全", "青少年模式", "关怀模式").forEachIndexed { i, label ->
                        SettingsRow(label, colors, showDivider = i != 2)
                    }
                }
            }
            item(key = "s_group2") {
                Spacer(Modifier.height(8.dp))
                Column(Modifier.background(colors.cardBackground)) {
                    listOf("新消息通知", "聊天", "隐私", "通用").forEachIndexed { i, label ->
                        SettingsRow(label, colors, showDivider = i != 3)
                    }
                }
            }
            item(key = "s_group3") {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.cardBackground)
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.cardBackground)
                            .clickable { /* 退出登录（占位） */ },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("退出登录", fontSize = 16.sp, color = colors.danger)
                    }
                }
            }
            item(key = "s_about") {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.cardBackground)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("suchat", fontSize = 14.sp, color = colors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "版本 0.1.0",
                        fontSize = 12.sp,
                        color = colors.textHint,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    colors: io.github.sxd91.suchat.core.design.theme.SuchatTokens,
    showDivider: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 16.sp, color = colors.textPrimary)
        Spacer(Modifier.weight(1f))
        Text("›", fontSize = 20.sp, color = colors.textHint)
    }
    if (showDivider) {
        Box(
            Modifier
                .padding(start = 20.dp)
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.divider),
        )
    }
}

// ============================== 通用二级页脚手架 ==============================

/**
 * 二级页脚手架：顶栏（带返回）+ 内容。
 *
 * 二级页是**全屏覆盖**的（不显示底栏）—— 与微信一致：
 * 进入二级页后底栏消失，返回时恢复。
 *
 * 这里不做转场动画（转场由外层 `SecondaryHost` 统一处理，见 MainActivity）。
 */
@Composable
private fun SecondaryScaffold(
    title: String,
    onBack: () -> Unit,
    bottomInset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val colors = LocalSuchatTokens.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.pageBackground),
    ) {
        Box(Modifier.padding(top = statusBarPadding)) {
            WeChatTopBar(title = title, onBack = onBack)
        }
        content()
    }
}