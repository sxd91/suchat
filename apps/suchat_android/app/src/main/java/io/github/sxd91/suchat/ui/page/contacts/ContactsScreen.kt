package io.github.sxd91.suchat.ui.page.contacts

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import io.github.sxd91.suchat.data.model.Contact
import io.github.sxd91.suchat.ui.component.Avatar
import io.github.sxd91.suchat.ui.component.EntryRow
import io.github.sxd91.suchat.ui.component.WeChatListItem
import kotlinx.coroutines.launch

/**
 * 「联系人」tab。
 *
 * 微信结构：
 * ```
 *  [ 通讯录                    ＋ ]      ← 顶栏
 *  [ 新的朋友 ][ 群聊 ][ 标签 ][ 公众号 ]  ← 固定入口（橙色系图标）
 *  ────────────────────────────
 *  [ A ]                               ← 字母分组头
 *  [ 头像 | 姓名 ]
 *  ...
 *                                       [A][B][C]…  ← 右侧字母索引
 * ```
 *
 * 右侧字母索引可点击跳转（微信行为）。本实现用 LazyListState 的
 * `scrollToItem` 精确跳转 —— 需要知道每个分组头在列表中的 index，
 * 故先构建「索引表」，再据此渲染。
 */
@Composable
fun ContactsScreen(
    nav: SuchatNavigator,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val colors = LocalSuchatTokens.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 按首字母分组（保持字母顺序）
    val grouped: List<Pair<String, List<Contact>>> = remember {
        SampleData.contacts
            .groupBy { it.initial }
            .toSortedMap()
            .map { (initial, list) -> initial to list }
    }
    val letters = remember(grouped) { grouped.map { it.first } }

    // 每个字母分组头在 LazyColumn 中的下标。
    // 列表结构：[固定入口区][分组头1][联系人…][分组头2][联系人…]…
    // 故「字母 -> item index」的映射要用累计偏移算，不能直接用分组序号。
    val headerIndexByLetter = remember(grouped) {
        buildMap {
            var index = 1 // 跳过「固定入口」这一整个 item
            grouped.forEach { (letter, contacts) ->
                put(letter, index)
                index += 1 + contacts.size // 分组头 1 项 + 联系人 N 项
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.pageBackground),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
                    text = "联系人",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "＋",
                    fontSize = 20.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(4.dp),
                )
            }

            // --- 列表 ---
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.cardBackground),
                contentPadding = PaddingValues(bottom = bottomInset),
            ) {
                // 顶部固定入口（微信的「新的朋友 / 群聊 / 标签 / 公众号」）
                item(key = "fixed_entries") {
                    Column(Modifier.background(colors.cardBackground)) {
                        EntryRow(
                            title = "新的朋友",
                            iconColor = Color(0xFFFA9D3B),
                            glyph = "👤",
                            onClick = { nav.push(SuchatPage.NewFriends) },
                        )
                        EntryRow(
                            title = "仅聊天的朋友",
                            iconColor = Color(0xFF07C160),
                            glyph = "💬",
                            onClick = { /* 占位 */ },
                        )
                        EntryRow(
                            title = "群聊",
                            iconColor = Color(0xFF07C160),
                            glyph = "👥",
                            onClick = { nav.push(SuchatPage.GroupChats) },
                        )
                        EntryRow(
                            title = "标签",
                            iconColor = Color(0xFF3E7BFA),
                            glyph = "🏷",
                            onClick = { nav.push(SuchatPage.Tags) },
                        )
                        EntryRow(
                            title = "公众号",
                            iconColor = Color(0xFF3E7BFA),
                            glyph = "📢",
                            onClick = { nav.push(SuchatPage.OfficialAccounts) },
                            showDivider = false,
                        )
                    }
                }

                // 字母分组
                grouped.forEach { (letter, contacts) ->
                    item(key = "header_$letter") {
                        // 分组头：灰底 + 左缩进 16dp + 12sp 灰字（微信规范）
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.pageBackground)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = letter,
                                fontSize = 13.sp,
                                color = colors.textSecondary,
                            )
                        }
                    }
                    items(
                        count = contacts.size,
                        key = { i -> contacts[i].id },
                    ) { i ->
                        val contact = contacts[i]
                        WeChatListItem(
                            leading = {
                                Avatar(
                                    name = contact.name,
                                    color = contact.avatarColor,
                                    size = 40.dp,
                                )
                            },
                            title = contact.remark ?: contact.name,
                            onClick = {
                                nav.push(SuchatPage.ContactDetail(contact.id))
                            },
                            showDivider = i != contacts.lastIndex,
                            dividerStart = 68.dp,
                        )
                    }
                }

                item(key = "contact_footer") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(colors.cardBackground),
                    )
                }
            }
        }

        // --- 右侧字母索引条（浮在列表之上） ---
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 2.dp)
                .fillMaxHeight(0.7f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEach { letter ->
                Box(
                    modifier = Modifier
                        .size(width = 20.dp, height = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            // 点击字母 → 平滑滚到该分组头
                            headerIndexByLetter[letter]?.let { index ->
                                scope.launch { listState.animateScrollToItem(index) }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = letter,
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}