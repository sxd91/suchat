package io.github.sxd91.suchat.ui.page.functional

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.icon.SuchatIcons
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.core.nav.SuchatPage
import io.github.sxd91.suchat.data.SampleData
import io.github.sxd91.suchat.ui.component.LocalTopBarInset
import io.github.sxd91.suchat.ui.component.SuchatAvatar
import io.github.sxd91.suchat.ui.component.SuchatChatRow
import io.github.sxd91.suchat.ui.component.SuchatScaffold
import kotlin.random.Random
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 功能页面集合 —— 把此前 11 个占位页（「太多空功能了」）全部实体化。
 *
 * ## 每个页面都至少有真实交互（不是空壳）
 *
 * | 页面 | 交互 |
 * |---|---|
 * | 搜索 | 实时筛选联系人 / 会话；搜索历史点选回填 |
 * | 添加 | 四宫格 → 真实导航（群聊/朋友/扫码/收付款） |
 * | 扫一扫 | 模式切换（扫码/识物/翻译）、扫描线动画、模拟扫描出结果、结果可复制 |
 * | 视频号 | 全屏竖向翻页（VerticalPager）、点赞状态切换 |
 * | 看一看 | 文章列表、点赞切换（计数跟随） |
 * | 搜一搜 | 输入状态机：空 → 热搜榜；有词 → 搜索建议 + 去搜索 |
 * | 小程序 | 点应用 → 置顶「最近使用」（可感知的重排） |
 * | 服务 | 收付款码（二维码/条形码、付款码/收款码切换）、钱包、生活服务详情 |
 * | 收藏 | 分类 Tab 切换（全部/图片/链接/文件/音乐） |
 * | 卡包 | 点卡展开条形码 |
 * | 表情 | 点贴纸选中、表情包「添加/已添加」状态 |
 */

// ============================================================================
// 一、搜索
// ============================================================================

/** 搜索页：实时筛选联系人 / 会话。 */
@Composable
fun SearchScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val history = remember {
        mutableStateListOf("提拉米苏", "群聊", "陈思雨", "跑步")
    }

    val contactsHit = remember(query) {
        if (query.isBlank()) emptyList()
        else SampleData.contacts.filter {
            it.name.contains(query, ignoreCase = true) ||
                (it.remark?.contains(query, ignoreCase = true) == true)
        }
    }
    val chatsHit = remember(query) {
        if (query.isBlank()) emptyList()
        else SampleData.chats.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.lastMessage.contains(query, ignoreCase = true)
        }
    }

    SuchatScaffold(title = "搜索", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            // --- 输入框 ---
            item(key = "input") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(c.surfaceContainerHigh)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MiuixIcon(
                            imageVector = SuchatIcons.Search,
                            contentDescription = null,
                            tint = c.onSurfaceSecondary,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                MiuixText(
                                    text = "搜索聊天记录、联系人、朋友圈",
                                    fontSize = 15.sp,
                                    color = c.onSurfaceVariantSummary,
                                    maxLines = 1,
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                textStyle = TextStyle(fontSize = 15.sp, color = c.onSurface),
                                cursorBrush = SolidColor(c.primary),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                            )
                        }
                        if (query.isNotEmpty()) {
                            Box(
                                Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .clickable { query = "" },
                                contentAlignment = Alignment.Center,
                            ) {
                                MiuixIcon(
                                    imageVector = SuchatIcons.Close,
                                    contentDescription = "清空",
                                    tint = c.onSurfaceSecondary,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (query.isBlank()) {
                // --- 搜索历史 ---
                item(key = "history_header") {
                    MiuixText(
                        text = "搜索历史",
                        fontSize = 13.sp,
                        color = c.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp),
                    )
                }
                item(key = "history") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(history, key = { it }) { word ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(c.surfaceContainerHigh)
                                    .clickable { query = word }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                            ) {
                                MiuixText(text = word, fontSize = 13.sp, color = c.onSurface)
                            }
                        }
                    }
                }
            } else {
                // --- 结果 ---
                if (contactsHit.isEmpty() && chatsHit.isEmpty()) {
                    item(key = "empty") {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            MiuixIcon(
                                imageVector = SuchatIcons.Search,
                                contentDescription = null,
                                tint = c.onSurfaceVariantSummary,
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            MiuixText(
                                text = "未找到「$query」相关内容",
                                fontSize = 14.sp,
                                color = c.onSurfaceSecondary,
                            )
                        }
                    }
                }
                if (contactsHit.isNotEmpty()) {
                    item(key = "contacts_header") {
                        MiuixText(
                            text = "联系人",
                            fontSize = 13.sp,
                            color = c.onSurfaceVariantSummary,
                            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
                        )
                    }
                    items(contactsHit, key = { it.id }) { contact ->
                        SuchatChatRow(
                            title = contact.remark ?: contact.name,
                            subtitle = "联系人",
                            time = "",
                            avatarName = contact.name,
                            avatarSeed = contact.id,
                            onClick = { nav.push(SuchatPage.ContactDetail(contact.id)) },
                        )
                    }
                }
                if (chatsHit.isNotEmpty()) {
                    item(key = "chats_header") {
                        MiuixText(
                            text = "聊天记录",
                            fontSize = 13.sp,
                            color = c.onSurfaceVariantSummary,
                            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
                        )
                    }
                    items(chatsHit, key = { it.id }) { chat ->
                        SuchatChatRow(
                            title = chat.name,
                            subtitle = chat.lastMessage,
                            time = chat.time,
                            avatarName = chat.name,
                            avatarSeed = chat.id,
                            onClick = { nav.push(SuchatPage.ChatDetail(chat.id)) },
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 二、添加菜单
// ============================================================================

/** 添加菜单：四宫格 → 真实导航。 */
@Composable
fun AddMenuScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    val actions: List<Triple<String, ImageVector, SuchatPage>> = listOf(
        Triple("发起群聊", SuchatIcons.Chats, SuchatPage.GroupChats),
        Triple("添加朋友", SuchatIcons.Contacts, SuchatPage.NewFriends),
        Triple("扫一扫", SuchatIcons.Scan, SuchatPage.Scan),
        Triple("收付款", SuchatIcons.Wallet, SuchatPage.Services),
    )

    SuchatScaffold(title = "添加", onBack = { nav.pop() }, bottomInset = bottomInset) { _ ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = LocalTopBarInset.current)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            actions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (title, icon, page) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(c.surfaceContainerHigh)
                                .clickable { nav.push(page) }
                                .padding(vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            MiuixIcon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = c.primary,
                                modifier = Modifier.size(28.dp),
                            )
                            MiuixText(text = title, fontSize = 15.sp, color = c.onSurface)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 三、扫一扫
// ============================================================================

/** 扫一扫：模式切换 + 扫描线动画 + 模拟扫描 + 剪贴板复制。 */
@Composable
fun ScanScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    val context = LocalContext.current
    var mode by remember { mutableStateOf("扫码") }
    var torch by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    val modes = listOf("扫码", "识物", "翻译")

    // 扫描线动画（来回滚动）。
    val transition = rememberInfiniteTransition(label = "scanline")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "line",
    )

    SuchatScaffold(title = "扫一扫", onBack = { nav.pop() }, bottomInset = bottomInset) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = LocalTopBarInset.current)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // --- 取景框 ---
            Box(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .size(260.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF16191D)),
            ) {
                // 边框。
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(1.dp, c.primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                )
                // 扫描线（仅未出结果时滚动）。
                if (result == null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .offset(y = (progress * 240).dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, c.primary, Color.Transparent),
                                ),
                            ),
                    )
                }
                // 提示 / 结果。
                MiuixText(
                    text = if (result == null) "将${mode}目标放入框内，即可自动识别" else "识别完成",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp),
                )
            }

            // --- 模式切换 ---
            Row(
                modifier = Modifier.padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                modes.forEach { m ->
                    val selected = m == mode
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) c.primary else c.surfaceContainerHigh)
                            .clickable {
                                mode = m
                                result = null
                                copied = false
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                    ) {
                        MiuixText(
                            text = m,
                            fontSize = 13.sp,
                            color = if (selected) c.onPrimary else c.onSurface,
                        )
                    }
                }
            }

            // --- 结果卡 ---
            if (result != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.surfaceContainerHigh)
                        .padding(16.dp),
                ) {
                    MiuixText(
                        text = "扫描结果",
                        fontSize = 13.sp,
                        color = c.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.height(6.dp))
                    MiuixText(
                        text = result ?: "",
                        fontSize = 15.sp,
                        color = c.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallPill(
                            text = if (copied) "已复制" else "复制",
                        ) {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("suchat_scan", result ?: ""))
                            copied = true
                        }
                        SmallPill(text = "重新扫描") {
                            result = null
                            copied = false
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // --- 底部操作 ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ScanAction(label = "相册", icon = SuchatIcons.Image) {
                    result = "图片识别：SUCHAT-2026-8F3K2（相册导入）"
                }
                ScanAction(label = "模拟扫描", icon = SuchatIcons.Scan) {
                    result = "https://suchat.dev/invite/8F3K2"
                }
                ScanAction(
                    label = if (torch) "关灯" else "手电筒",
                    icon = SuchatIcons.Theme,
                    active = torch,
                ) {
                    torch = !torch
                }
            }
        }
    }
}

/** 扫一扫底部操作按钮。 */
@Composable
private fun ScanAction(
    label: String,
    icon: ImageVector,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val c = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MiuixIcon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) c.primary else c.onSurface,
            modifier = Modifier.size(24.dp),
        )
        MiuixText(
            text = label,
            fontSize = 12.sp,
            color = if (active) c.primary else c.onSurfaceSecondary,
        )
    }
}

/** 胶囊小按钮（扫一扫结果操作）。 */
@Composable
private fun SmallPill(text: String, onClick: () -> Unit) {
    val c = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(c.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        MiuixText(text = text, fontSize = 13.sp, color = c.onSurface)
    }
}

// ============================================================================
// 四、视频号（全屏，不走模糊顶栏 —— 用户指定的三个例外之一）
// ============================================================================

/** 视频号占位数据。 */
private object ChannelSamples {
    data class Video(
        val author: String,
        val desc: String,
        val music: String,
        val likes: Int,
        val comments: Int,
        val colors: List<Color>,
    )

    val videos = listOf(
        Video(
            author = "山野阿远", desc = "清晨五点的山顶，值回所有早起。",
            music = "《起风了》", likes = 2361, comments = 174,
            colors = listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF)),
        ),
        Video(
            author = "深夜食堂", desc = "一碗热汤面，治愈所有加班。",
            music = "原声 - 深夜食堂", likes = 982, comments = 66,
            colors = listOf(Color(0xFF5D4157), Color(0xFFA8CABA)),
        ),
        Video(
            author = "小鹿的旅行", desc = "川西自驾第 3 天，云海翻过垭口。",
            music = "《Traveling Light》", likes = 5643, comments = 320,
            colors = listOf(Color(0xFF232526), Color(0xFF414345)),
        ),
        Video(
            author = "阿哲弹吉他", desc = "考级曲目练了 400 遍，终于顺了。",
            music = "原声 - 阿哲", likes = 1275, comments = 89,
            colors = listOf(Color(0xFF134E5E), Color(0xFF71B280)),
        ),
        Video(
            author = "菜菜的猫", desc = "它蹲在门口等了三个小时。",
            music = "《可爱女人》", likes = 8890, comments = 512,
            colors = listOf(Color(0xFFAA4B6B), Color(0xFF6B6B83)),
        ),
    )
}

/**
 * 视频号：全屏竖向翻页视频流（用户指定不走模糊顶栏）。
 *
 * 交互：上下滑动翻视频、双击式点赞（点按钮）、点赞计数即时变化。
 */
@Composable
fun ChannelsScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val videos = remember { ChannelSamples.videos }
    val pagerState = rememberPagerState(pageCount = { videos.size })
    val liked = remember { mutableStateListOf<Boolean>().apply { repeat(videos.size) { add(false) } } }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // --- 竖向翻页 ---
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val video = videos[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(video.colors)),
            ) {
                // 中央播放标识。
                MiuixIcon(
                    imageVector = SuchatIcons.Play,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp),
                )

                // --- 右侧操作列 ---
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 120.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    // 点赞（可切换）。
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MiuixIcon(
                            imageVector = SuchatIcons.FavoritesFill,
                            contentDescription = "点赞",
                            tint = if (liked[page]) Color(0xFFFF4E6E) else Color.White,
                            modifier = Modifier
                                .size(30.dp)
                                .clickable { liked[page] = !liked[page] },
                        )
                        Spacer(Modifier.height(4.dp))
                        MiuixText(
                            text = "${video.likes + if (liked[page]) 1 else 0}",
                            fontSize = 12.sp,
                            color = Color.White,
                        )
                    }
                    // 评论。
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MiuixIcon(
                            imageVector = SuchatIcons.Chats,
                            contentDescription = "评论",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        MiuixText(text = "${video.comments}", fontSize = 12.sp, color = Color.White)
                    }
                    // 转发。
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        MiuixIcon(
                            imageVector = SuchatIcons.Share,
                            contentDescription = "转发",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        MiuixText(text = "转发", fontSize = 12.sp, color = Color.White)
                    }
                }

                // --- 底部信息 ---
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 90.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SuchatAvatar(name = video.author, seed = video.author, size = 32.dp, corner = 16.dp)
                        Spacer(Modifier.width(8.dp))
                        MiuixText(
                            text = "@${video.author}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                    MiuixText(text = video.desc, fontSize = 14.sp, color = Color.White)
                    MiuixText(
                        text = "♫ ${video.music}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            }
        }

        // --- 顶部浮动栏（返回 + 标题） ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiuixIconButton(onClick = { nav.pop() }) {
                MiuixIcon(
                    imageVector = SuchatIcons.Back,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            MiuixText(
                text = "视频号",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(Modifier.weight(1f))
            // 占位保持标题居中。
            Spacer(Modifier.size(48.dp))
        }
    }
}

// ============================================================================
// 五、看一看
// ============================================================================

/** 看一看占位数据。 */
private object TopStoriesSamples {
    data class Article(
        val title: String,
        val source: String,
        val timeAgo: String,
        val likes: Int,
        val cover: Color,
    )

    val articles = listOf(
        Article("为什么说秋天是最适合开始一件新事的季节", "三联生活周刊", "2小时前", 128, Color(0xFF8E9EAB)),
        Article("一口气看懂：高铁为什么很少晚点", "新华社", "3小时前", 342, Color(0xFF4CA1AF)),
        Article("程序员的一天：从晨跑到深夜的代码", "极客公园", "5小时前", 89, Color(0xFF2C3E50)),
        Article("这道提拉米苏，我试了七次才做对", "下厨房精选", "昨天", 516, Color(0xFFD2A679)),
        Article("凌晨四点的菜市场，藏着城市最真实的烟火气", "人间theLivings", "昨天", 233, Color(0xFF5D4157)),
        Article("把跑步坚持一年的普通人，后来都怎么样了", "Keep 跑者说", "2天前", 761, Color(0xFF71B280)),
    )
}

/** 看一看：文章列表 + 点赞切换。 */
@Composable
fun TopStoriesScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    val articles = remember { TopStoriesSamples.articles }
    val liked = remember { mutableStateListOf<Boolean>().apply { repeat(articles.size) { add(false) } } }

    SuchatScaffold(title = "看一看", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "section") {
                MiuixText(
                    text = "朋友在看",
                    fontSize = 13.sp,
                    color = c.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp),
                )
            }
            itemsIndexed(articles, key = { _, a -> a.title }) { index, article ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { liked[index] = !liked[index] }
                        .padding(16.dp),
                ) {
                    Row {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MiuixText(
                                text = article.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = c.onSurface,
                                lineHeight = 22.sp,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MiuixText(
                                    text = "${article.source} · ${article.timeAgo}",
                                    fontSize = 12.sp,
                                    color = c.onSurfaceVariantSummary,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MiuixIcon(
                                    imageVector = SuchatIcons.FavoritesFill,
                                    contentDescription = "点赞",
                                    tint = if (liked[index]) c.error else c.onSurfaceVariantSummary,
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                                MiuixText(
                                    text = "${article.likes + if (liked[index]) 1 else 0}",
                                    fontSize = 12.sp,
                                    color = if (liked[index]) c.error else c.onSurfaceVariantSummary,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 92.dp, height = 92.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(article.cover),
                        )
                    }
                }
                Box(
                    Modifier
                        .padding(start = 16.dp)
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(c.outline.copy(alpha = 0.35f)),
                )
            }
        }
    }
}

// ============================================================================
// 六、搜一搜
// ============================================================================

/** 搜一搜：热搜榜 + 输入状态机。 */
@Composable
fun SearchDiscoverScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    var query by remember { mutableStateOf("") }
    val hot = remember {
        listOf(
            "国庆假期出行攻略" to true,
            "秋天的第一杯奶茶" to false,
            "高铁晚点查询" to true,
            "提拉米苏做法" to false,
            "跑步膝盖保护" to false,
            "读书清单推荐" to false,
            "露营装备入门" to true,
            "午休的正确姿势" to false,
        )
    }

    SuchatScaffold(title = "搜一搜", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            // --- 输入框 ---
            item(key = "input") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(c.surfaceContainerHigh)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MiuixIcon(
                            imageVector = SuchatIcons.Search,
                            contentDescription = null,
                            tint = c.onSurfaceSecondary,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                MiuixText(
                                    text = "搜索全网内容",
                                    fontSize = 15.sp,
                                    color = c.onSurfaceVariantSummary,
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                textStyle = TextStyle(fontSize = 15.sp, color = c.onSurface),
                                cursorBrush = SolidColor(c.primary),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            if (query.isBlank()) {
                item(key = "hot_header") {
                    MiuixText(
                        text = "微信热搜",
                        fontSize = 13.sp,
                        color = c.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp),
                    )
                }
                itemsIndexed(hot, key = { _, item -> item.first }) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { query = item.first }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MiuixText(
                            text = "${index + 1}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when (index) {
                                0 -> Color(0xFFFF3B30)
                                1 -> Color(0xFFFF9500)
                                2 -> Color(0xFFFFCC00)
                                else -> c.onSurfaceVariantSummary
                            },
                            modifier = Modifier.width(26.dp),
                        )
                        MiuixText(
                            text = item.first,
                            fontSize = 15.sp,
                            color = c.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (item.second) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(c.error.copy(alpha = 0.12f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                            ) {
                                MiuixText(text = "热", fontSize = 10.sp, color = c.error)
                            }
                        }
                    }
                }
            } else {
                item(key = "go") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* 演示：结果固定 */}
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MiuixIcon(
                            imageVector = SuchatIcons.Search,
                            contentDescription = null,
                            tint = c.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        MiuixText(
                            text = "搜索「$query」",
                            fontSize = 15.sp,
                            color = c.onSurface,
                        )
                    }
                }
                item(key = "sugg_header") {
                    MiuixText(
                        text = "相关搜索",
                        fontSize = 13.sp,
                        color = c.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 4.dp),
                    )
                }
                items(
                    listOf(
                        "$query 是什么",
                        "$query 最新消息",
                        "$query 怎么做",
                    ),
                    key = { it },
                ) { suggestion ->
                    MiuixText(
                        text = suggestion,
                        fontSize = 15.sp,
                        color = c.onSurfaceSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { query = suggestion }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                    )
                }
            }
        }
    }
}

// ============================================================================
// 七、小程序
// ============================================================================

/** 小程序占位数据。 */
private object MiniProgramSamples {
    data class MiniApp(val name: String, val icon: ImageVector, val color: Color)

    val items = listOf(
        MiniApp("星巴克", SuchatIcons.Store, Color(0xFF00704A)),
        MiniApp("美团外卖", SuchatIcons.Store, Color(0xFFFFC300)),
        MiniApp("滴滴出行", SuchatIcons.Location, Color(0xFFFF7B00)),
        MiniApp("京东购物", SuchatIcons.Promotions, Color(0xFFE2231A)),
        MiniApp("腾讯文档", SuchatIcons.File, Color(0xFF2B7CD3)),
        MiniApp("顺丰速运", SuchatIcons.Send, Color(0xFF1B1B1B)),
        MiniApp("铁路12306", SuchatIcons.Timer, Color(0xFF3B6EA5)),
        MiniApp("电影购票", SuchatIcons.Play, Color(0xFF8E44AD)),
        MiniApp("天气", SuchatIcons.WorldClock, Color(0xFF3498DB)),
        MiniApp("计算器", SuchatIcons.GridView, Color(0xFF34495E)),
        MiniApp("翻译", SuchatIcons.Translate, Color(0xFF16A085)),
        MiniApp("音乐", SuchatIcons.Music, Color(0xFFE74C3C)),
    )
}

/** 小程序：点应用 → 置顶「最近使用」。 */
@Composable
fun MiniProgramsScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    val all = remember { MiniProgramSamples.items }
    // 「最近使用」：点击任意应用会把它插到最前（可感知的重排交互）。
    val recent = remember {
        mutableStateListOf<MiniProgramSamples.MiniApp>().apply { addAll(all.take(4)) }
    }

    fun use(app: MiniProgramSamples.MiniApp) {
        recent.remove(app)
        recent.add(0, app)
        if (recent.size > 8) recent.removeAt(recent.lastIndex)
    }

    SuchatScaffold(title = "小程序", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "recent_header") {
                MiuixText(
                    text = "最近使用",
                    fontSize = 13.sp,
                    color = c.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp),
                )
            }
            item(key = "recent_grid") {
                Column(Modifier.padding(horizontal = 10.dp)) {
                    recent.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { app ->
                                MiniAppCell(app, Modifier.weight(1f)) { use(app) }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            item(key = "mine_header") {
                MiuixText(
                    text = "我的小程序",
                    fontSize = 13.sp,
                    color = c.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 8.dp),
                )
            }
            item(key = "mine_grid") {
                Column(Modifier.padding(horizontal = 10.dp)) {
                    all.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { app ->
                                MiniAppCell(app, Modifier.weight(1f)) { use(app) }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            item(key = "footer") { Spacer(Modifier.height(16.dp)) }
        }
    }
}

/** 小程序格子（图标 + 名字）。 */
@Composable
private fun MiniAppCell(
    app: MiniProgramSamples.MiniApp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = MiuixTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(app.color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            MiuixIcon(
                imageVector = app.icon,
                contentDescription = app.name,
                tint = app.color,
                modifier = Modifier.size(24.dp),
            )
        }
        MiuixText(
            text = app.name,
            fontSize = 11.sp,
            color = c.onSurfaceSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ============================================================================
// 八、服务（收付款 / 钱包 / 生活服务）
// ============================================================================

/** 服务页内部状态。 */
private sealed interface ServiceState {
    data object Home : ServiceState
    data object PayCode : ServiceState
    data object Wallet : ServiceState
    data class Detail(val title: String, val desc: String, val done: Boolean = false) : ServiceState
}

/** 生活服务条目。 */
private data class LifeService(val title: String, val icon: ImageVector, val desc: String)

/** 服务：收付款码 / 钱包 / 生活服务详情（页内多状态导航）。 */
@Composable
fun ServicesScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    var state by remember { mutableStateOf<ServiceState>(ServiceState.Home) }

    val lifeServices = remember {
        listOf(
            LifeService("手机充值", SuchatIcons.Phone, "支持三网话费与流量充值，到账秒级。"),
            LifeService("生活缴费", SuchatIcons.Wallet, "水、电、燃气、宽带账单，一站缴清。"),
            LifeService("城市服务", SuchatIcons.Location, "社保、公积金、挂号，本地服务聚合。"),
            LifeService("医疗健康", SuchatIcons.Favorites, "在线问诊、体检预约、健康档案。"),
            LifeService("出行", SuchatIcons.Timer, "火车票、机票、打车一口价。"),
            LifeService("公益", SuchatIcons.Favorites, "捐步、助学、环保项目，随手做好事。"),
        )
    }

    SuchatScaffold(
        title = when (val s = state) {
            ServiceState.Home -> "服务"
            ServiceState.PayCode -> "收付款"
            ServiceState.Wallet -> "钱包"
            is ServiceState.Detail -> s.title
        },
        onBack = {
            // 页内多状态：优先回 Home，其次退出页面。
            if (state != ServiceState.Home) state = ServiceState.Home else nav.pop()
        },
        bottomInset = bottomInset,
    ) { pad ->
        when (val s = state) {
            ServiceState.Home -> ServicesHome(
                lifeServices = lifeServices,
                topInset = LocalTopBarInset.current,
                bottomInset = pad.calculateBottomPadding(),
                onPayCode = { state = ServiceState.PayCode },
                onWallet = { state = ServiceState.Wallet },
                onService = { svc -> state = ServiceState.Detail(svc.title, svc.desc) },
            )
            ServiceState.PayCode -> PayCodeView()
            ServiceState.Wallet -> WalletView()
            is ServiceState.Detail -> ServiceDetailView(
                detail = s,
                onToggle = { state = s.copy(done = !s.done) },
            )
        }
    }
}

/** 服务首页。 */
@Composable
private fun ServicesHome(
    lifeServices: List<LifeService>,
    topInset: Dp,
    bottomInset: Dp,
    onPayCode: () -> Unit,
    onWallet: () -> Unit,
    onService: (LifeService) -> Unit,
) {
    val c = MiuixTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topInset, bottom = bottomInset),
    ) {
        item(key = "hero") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 收付款。
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(c.primary)
                        .clickable(onClick = onPayCode)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MiuixIcon(
                        imageVector = SuchatIcons.Scan,
                        contentDescription = null,
                        tint = c.onPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                    MiuixText(text = "收付款", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.onPrimary)
                    MiuixText(
                        text = "向商家付款 · 二维码收款",
                        fontSize = 11.sp,
                        color = c.onPrimary.copy(alpha = 0.8f),
                    )
                }
                // 钱包。
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(c.surfaceContainerHigh)
                        .clickable(onClick = onWallet)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MiuixIcon(
                        imageVector = SuchatIcons.Wallet,
                        contentDescription = null,
                        tint = c.primary,
                        modifier = Modifier.size(26.dp),
                    )
                    MiuixText(text = "钱包", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.onSurface)
                    MiuixText(text = "零钱 · 银行卡 · 账单", fontSize = 11.sp, color = c.onSurfaceSecondary)
                }
            }
        }
        item(key = "life_header") {
            MiuixText(
                text = "生活服务",
                fontSize = 13.sp,
                color = c.onSurfaceVariantSummary,
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp),
            )
        }
        item(key = "life_grid") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                lifeServices.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { svc ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(c.surfaceContainerHigh)
                                    .clickable { onService(svc) }
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                MiuixIcon(
                                    imageVector = svc.icon,
                                    contentDescription = svc.title,
                                    tint = c.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                                MiuixText(text = svc.title, fontSize = 12.sp, color = c.onSurface)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

/** 收付款码视图：二维码 + 条形码 + 付款/收款切换。 */
@Composable
private fun PayCodeView() {
    val c = MiuixTheme.colorScheme
    var receiveMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 二维码。
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(18.dp),
        ) {
            QrCodePattern(modifier = Modifier.size(190.dp))
        }
        Spacer(Modifier.height(14.dp))
        MiuixText(
            text = if (receiveMode) "扫一扫上面的二维码，向我付款" else "向商家出示此码完成付款",
            fontSize = 13.sp,
            color = c.onSurfaceSecondary,
        )
        Spacer(Modifier.height(18.dp))
        // 条形码。
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            BarcodePattern(modifier = Modifier.size(width = 220.dp, height = 56.dp))
        }
        Spacer(Modifier.height(10.dp))
        MiuixText(
            text = "数字码 · 8821 4430 1295",
            fontSize = 13.sp,
            color = c.onSurface,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(24.dp))
        // 付款 / 收款 切换。
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (!receiveMode) c.primary else c.surfaceContainerHigh)
                    .clickable { receiveMode = false }
                    .padding(horizontal = 22.dp, vertical = 9.dp),
            ) {
                MiuixText(
                    text = "付款码",
                    fontSize = 14.sp,
                    color = if (!receiveMode) c.onPrimary else c.onSurface,
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (receiveMode) c.primary else c.surfaceContainerHigh)
                    .clickable { receiveMode = true }
                    .padding(horizontal = 22.dp, vertical = 9.dp),
            ) {
                MiuixText(
                    text = "收款码",
                    fontSize = 14.sp,
                    color = if (receiveMode) c.onPrimary else c.onSurface,
                )
            }
        }
    }
}

/** 钱包视图：余额 + 功能行。 */
@Composable
private fun WalletView() {
    val c = MiuixTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = LocalTopBarInset.current, bottom = 24.dp),
    ) {
        item(key = "balance") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.primaryContainer)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MiuixText(text = "零钱余额", fontSize = 13.sp, color = c.onPrimaryContainer.copy(alpha = 0.75f))
                MiuixText(
                    text = "¥ 1,286.50",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onPrimaryContainer,
                )
                MiuixText(
                    text = "昨日收益 +0.34",
                    fontSize = 12.sp,
                    color = c.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
        val rows = listOf(
            "零钱通" to "7日年化 1.82%",
            "银行卡" to "已绑定 2 张",
            "账单" to "本月支出 ¥ 642.10",
            "亲属卡" to "未设置",
        )
        items(rows, key = { it.first }) { (title, trailing) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiuixText(text = title, fontSize = 16.sp, color = c.onSurface, modifier = Modifier.weight(1f))
                MiuixText(text = trailing, fontSize = 13.sp, color = c.onSurfaceSecondary)
                Spacer(Modifier.width(6.dp))
                MiuixIcon(
                    imageVector = SuchatIcons.ChevronForward,
                    contentDescription = null,
                    tint = c.onSurfaceVariantSummary,
                    modifier = Modifier.size(16.dp),
                )
            }
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

/** 生活服务详情视图。 */
@Composable
private fun ServiceDetailView(
    detail: ServiceState.Detail,
    onToggle: () -> Unit,
) {
    val c = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = LocalTopBarInset.current)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        MiuixIcon(
            imageVector = SuchatIcons.Wallet,
            contentDescription = null,
            tint = c.primary,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(14.dp))
        MiuixText(
            text = detail.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        MiuixText(
            text = detail.desc,
            fontSize = 14.sp,
            color = c.onSurfaceSecondary,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(26.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (detail.done) c.surfaceContainerHigh else c.primary)
                .clickable(onClick = onToggle)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            MiuixText(
                text = if (detail.done) "已办理 ✓（点击撤销）" else "去办理",
                fontSize = 16.sp,
                color = if (detail.done) c.onSurfaceSecondary else c.onPrimary,
            )
        }
    }
}

/** 二维码图案（Canvas 确定性伪随机 + 三个定位角）。 */
@Composable
private fun QrCodePattern(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val n = 21
        val cell = size.width / n
        val rnd = Random(20261002)
        // 数据点。
        for (row in 0 until n) {
            for (col in 0 until n) {
                val inEye = (row < 7 && col < 7) || (row < 7 && col >= n - 7) || (row >= n - 7 && col < 7)
                if (!inEye && rnd.nextFloat() > 0.52f) {
                    drawRect(
                        color = Color(0xFF111111),
                        topLeft = Offset(col * cell, row * cell),
                        size = Size(cell, cell),
                    )
                }
            }
        }
        // 三个定位角。
        fun eye(x: Int, y: Int) {
            drawRect(Color(0xFF111111), Offset(x * cell, y * cell), Size(cell * 7, cell * 7))
            drawRect(Color.White, Offset((x + 1) * cell, (y + 1) * cell), Size(cell * 5, cell * 5))
            drawRect(Color(0xFF111111), Offset((x + 2) * cell, (y + 2) * cell), Size(cell * 3, cell * 3))
        }
        eye(0, 0)
        eye(n - 7, 0)
        eye(0, n - 7)
    }
}

/** 条形码图案（确定性伪随机竖条）。 */
@Composable
private fun BarcodePattern(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val rnd = Random(20261003)
        var x = 0f
        var dark = true
        while (x < size.width) {
            val w = 1f + rnd.nextFloat() * 3.2f
            if (dark) {
                drawRect(
                    color = Color(0xFF111111),
                    topLeft = Offset(x, 0f),
                    size = Size(w, size.height),
                )
            }
            x += w + 1.2f
            dark = !dark
        }
    }
}

// ============================================================================
// 九、收藏
// ============================================================================

/** 收藏占位数据。 */
private object FavoritesSamples {
    data class Item(val type: String, val title: String, val source: String, val time: String)

    val items = listOf(
        Item("图片", "山顶日出九宫格", "王芳 · 朋友圈", "10分钟前"),
        Item("链接", "为什么说秋天是最适合开始一件新事的季节", "三联生活周刊", "2小时前"),
        Item("文件", "项目周报-第39周.pdf", "工作群聊", "3小时前"),
        Item("音乐", "起风了（Cover）", "阿哲分享", "昨天"),
        Item("链接", "高铁为什么很少晚点", "新华社", "昨天"),
        Item("图片", "提拉米苏成品图", "陈思雨 · 聊天", "昨天"),
        Item("文件", "家庭账单-九月.xlsx", "家庭群聊", "2天前"),
        Item("音乐", "夜的钢琴曲五", "收藏夹导入", "3天前"),
    )
}

/** 收藏：分类 Tab 切换。 */
@Composable
fun FavoritesScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    val tabs = remember { listOf("全部", "图片", "链接", "文件", "音乐") }
    var tab by remember { mutableIntStateOf(0) }
    val items = remember { FavoritesSamples.items }
    val shown = remember(tab) {
        if (tab == 0) items else items.filter { it.type == tabs[tab] }
    }

    SuchatScaffold(title = "收藏", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        Column(Modifier.fillMaxSize()) {
            // --- 分类 Tab ---
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = LocalTopBarInset.current),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(tabs, key = { _, t -> t }) { index, t ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (index == tab) c.primary else c.surfaceContainerHigh)
                            .clickable { tab = index }
                            .padding(horizontal = 15.dp, vertical = 7.dp),
                    ) {
                        MiuixText(
                            text = t,
                            fontSize = 13.sp,
                            color = if (index == tab) c.onPrimary else c.onSurface,
                        )
                    }
                }
            }

            // --- 收藏列表 ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = pad.calculateBottomPadding()),
            ) {
                if (shown.isEmpty()) {
                    item(key = "empty") {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            MiuixIcon(
                                imageVector = SuchatIcons.Favorites,
                                contentDescription = null,
                                tint = c.onSurfaceVariantSummary,
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            MiuixText(
                                text = "还没有${tabs[tab]}类收藏",
                                fontSize = 14.sp,
                                color = c.onSurfaceSecondary,
                            )
                        }
                    }
                }
                items(shown, key = { it.title }) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(c.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            MiuixIcon(
                                imageVector = when (item.type) {
                                    "图片" -> SuchatIcons.Image
                                    "链接" -> SuchatIcons.Link
                                    "文件" -> SuchatIcons.File
                                    else -> SuchatIcons.Music
                                },
                                contentDescription = item.type,
                                tint = c.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            MiuixText(
                                text = item.title,
                                fontSize = 15.sp,
                                color = c.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            MiuixText(
                                text = "${item.source} · ${item.time}",
                                fontSize = 12.sp,
                                color = c.onSurfaceVariantSummary,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(c.surfaceContainerHigh)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            MiuixText(text = item.type, fontSize = 10.sp, color = c.onSurfaceSecondary)
                        }
                    }
                    Box(
                        Modifier
                            .padding(start = 70.dp)
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(c.outline.copy(alpha = 0.35f)),
                    )
                }
            }
        }
    }
}

// ============================================================================
// 十、卡包
// ============================================================================

/** 卡包占位数据。 */
private object CardsSamples {
    data class ShopCard(
        val title: String,
        val subtitle: String,
        val expire: String,
        val color: Color,
    )

    val cards = listOf(
        ShopCard("满减券 · 满 100 减 30", "星巴克专享", "有效期至 2026-10-31", Color(0xFF00704A)),
        ShopCard("会员卡", "山姆会员商店 · 卓越会员", "有效期至 2027-03-15", Color(0xFF2B7CD3)),
        ShopCard("交通卡", "城市一卡通 · 余额 ¥ 42.50", "长期有效", Color(0xFFFF7B00)),
        ShopCard("电影券 ×2", "万达影城 · 2D/3D 通兑", "有效期至 2026-12-30", Color(0xFF8E44AD)),
    )
}

/** 卡包：点卡展开条形码。 */
@Composable
fun CardsScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    val cards = remember { CardsSamples.cards }
    var expanded by remember { mutableIntStateOf(-1) }

    SuchatScaffold(title = "卡包", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "header") {
                MiuixText(
                    text = "我的卡券（${cards.size}）",
                    fontSize = 13.sp,
                    color = c.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp),
                )
            }
            itemsIndexed(cards, key = { _, card -> card.title }) { index, card ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(card.color)
                        .clickable { expanded = if (expanded == index) -1 else index }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MiuixText(
                        text = card.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    MiuixText(
                        text = card.subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    MiuixText(
                        text = card.expire,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    // 展开：显示条形码。
                    if (expanded == index) {
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            BarcodePattern(modifier = Modifier.size(width = 220.dp, height = 52.dp))
                        }
                        MiuixText(
                            text = "使用时向店员出示此码",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
            item(key = "footer") { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ============================================================================
// 十一、表情
// ============================================================================

/** 表情占位数据：单贴纸 + 表情包。 */
private object StickerSamples {
    val singles = listOf("😄", "😂", "🥹", "😎", "🤔", "😴", "🥳", "😭", "😡", "🤯", "🫠", "🙃")

    data class Pack(val name: String, val samples: List<String>, val desc: String)

    val packs = listOf(
        Pack("小黄脸日常", listOf("😄", "😂", "🥹", "😎"), "微信官方 · 24 个表情"),
        Pack("打工人系列", listOf("🫠", "🤯", "😭", "🙃"), "职场吐槽 · 16 个表情"),
        Pack("猫猫头", listOf("😺", "😸", "😹", "😻"), "萌宠 · 20 个表情"),
        Pack("加油鸭", listOf("🦆", "💪", "🔥", "✨"), "元气 · 12 个表情"),
    )
}

/** 表情：贴纸选中 + 表情包「添加/已添加」。 */
@Composable
fun StickersScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    var selected by remember { mutableStateOf<String?>(null) }
    val added = remember { mutableStateListOf<Boolean>().apply { repeat(StickerSamples.packs.size) { add(false) } } }

    SuchatScaffold(title = "表情", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            // --- 我的表情 ---
            item(key = "mine_header") {
                MiuixText(
                    text = "我的表情",
                    fontSize = 13.sp,
                    color = c.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 8.dp),
                )
            }
            item(key = "singles") {
                Column(Modifier.padding(horizontal = 10.dp)) {
                    StickerSamples.singles.chunked(6).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { s ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected == s) c.primaryContainer else c.surfaceContainerHigh,
                                        )
                                        .clickable { selected = if (selected == s) null else s }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    MiuixText(text = s, fontSize = 24.sp)
                                }
                            }
                            repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            if (selected != null) {
                item(key = "preview") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.surfaceContainerHigh)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MiuixText(text = selected ?: "", fontSize = 34.sp)
                        Spacer(Modifier.width(14.dp))
                        MiuixText(
                            text = "已选中该表情，可在聊天输入框中发送",
                            fontSize = 13.sp,
                            color = c.onSurfaceSecondary,
                        )
                    }
                }
            }

            // --- 表情商店 ---
            item(key = "store_header") {
                MiuixText(
                    text = "表情商店",
                    fontSize = 13.sp,
                    color = c.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
                )
            }
            itemsIndexed(StickerSamples.packs, key = { _, p -> p.name }) { index, pack ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 样品。
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        pack.samples.take(4).forEach { s ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(c.surfaceContainerHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                MiuixText(text = s, fontSize = 19.sp)
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        MiuixText(text = pack.name, fontSize = 15.sp, color = c.onSurface)
                        MiuixText(
                            text = pack.desc,
                            fontSize = 11.sp,
                            color = c.onSurfaceVariantSummary,
                        )
                    }
                    // 添加 / 已添加。
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (added[index]) c.surfaceContainerHigh else c.primary)
                            .clickable { added[index] = !added[index] }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        MiuixText(
                            text = if (added[index]) "已添加" else "添加",
                            fontSize = 13.sp,
                            color = if (added[index]) c.onSurfaceSecondary else c.onPrimary,
                        )
                    }
                }
                Box(
                    Modifier
                        .padding(start = 16.dp)
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(c.outline.copy(alpha = 0.35f)),
                )
            }
        }
    }
}
