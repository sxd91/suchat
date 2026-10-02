package io.github.sxd91.suchat.ui.page.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import io.github.sxd91.suchat.ui.component.SuchatChatRow
import io.github.sxd91.suchat.ui.component.SuchatEntryRow
import io.github.sxd91.suchat.ui.component.SuchatScaffold
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/**
 * 联系人页。
 *
 * ## 本轮修正（用户第 7 条）
 *
 * 字母索引从「一列独立小字母（每个 20x16dp 点击区）」改成
 * **一根长条胶囊**，并支持：
 *
 *  1. **滑动选字母** —— 手指在胶囊上上下滑动即连续选字母（与微信一致），
 *     不是只能逐个点击；
 *  2. **滑动时左侧气泡** —— 当前选中的字母以一个圆角气泡显示在胶囊左侧，
 *     手指抬起后气泡淡出；
 *  3. 胶囊本身是一条**整体圆角背景**（`RoundedCornerShape(50%)`），
 *     不是散落的字母。
 *
 * 实现要点：
 *  - 胶囊内字母用 `Column` 等分排列，通过 `onSizeChanged` 记下高度；
 *  - 手指 y 坐标 → 字母索引：`index = (y / itemHeight).roundToInt()`；
 *  - 用 `detectDragGestures` + `detectTapGestures` 组合（点按与滑动都能选）；
 *  - 气泡位置跟随手指 y（`bubbleY`），不固定居中，符合直觉。
 */
@Composable
fun ContactsScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 按首字母分组（保持字母顺序）。
    val grouped: List<Pair<String, List<io.github.sxd91.suchat.data.model.Contact>>> = remember {
        SampleData.contacts
            .groupBy { it.initial }
            .toSortedMap()
            .map { (initial, list) -> initial to list }
    }
    val letters = remember(grouped) { grouped.map { it.first } }

    // 每个字母分组头在 LazyColumn 中的下标（结构：[固定入口][头][人…][头][人…]）。
    val headerIndexByLetter = remember(grouped) {
        buildMap {
            var index = 1 // 跳过「固定入口」整块
            grouped.forEach { (letter, contacts) ->
                put(letter, index)
                index += 1 + contacts.size
            }
        }
    }

    // 索引条状态。
    var barHeightPx by remember { mutableFloatStateOf(0f) }
    var activeLetter by remember { mutableStateOf<String?>(null) }
    var bubbleY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    // 选字母 → 滚列表（统一入口，点按/滑动都走这里）。
    fun selectLetter(letter: String) {
        if (activeLetter != letter) {
            activeLetter = letter
            headerIndexByLetter[letter]?.let { index ->
                scope.launch { listState.scrollToItem(index) }
            }
        }
    }

    SuchatScaffold(
        title = "联系人",
        onBack = null,
        bottomInset = bottomInset,
        actions = {
            // 顶栏右侧「+」：去「新的朋友」（与旧版行为一致）。
            MiuixIconButton(onClick = { nav.push(SuchatPage.NewFriends) }) {
                MiuixIcon(
                    imageVector = SuchatIcons.Add,
                    contentDescription = "添加",
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
    ) { pad ->
        Box(modifier = Modifier.fillMaxSize()) {
            // --- 列表 ---
            // 顶栏高度（LocalTopBarInset）进 contentPadding.top：
            // 内容初始落在顶栏下方、滚动时**穿过**顶栏下方 —— 模糊层才采得到内容。
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = LocalTopBarInset.current,
                    bottom = pad.calculateBottomPadding(),
                ),
            ) {
                // 固定入口
                item(key = "fixed_entries") {
                    Column(Modifier.background(c.surface)) {
                        SuchatEntryRow(
                            title = "新的朋友",
                            icon = SuchatIcons.Contacts,
                            onClick = { nav.push(SuchatPage.NewFriends) },
                            showDivider = false,
                        )
                        SuchatEntryRow(
                            title = "群聊",
                            icon = SuchatIcons.Chats,
                            onClick = { nav.push(SuchatPage.GroupChats) },
                            showDivider = false,
                        )
                        SuchatEntryRow(
                            title = "标签",
                            icon = SuchatIcons.Favorites,
                            onClick = { nav.push(SuchatPage.Tags) },
                            showDivider = false,
                        )
                        SuchatEntryRow(
                            title = "公众号",
                            icon = SuchatIcons.Channels,
                            onClick = { nav.push(SuchatPage.OfficialAccounts) },
                            showDivider = false,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // 字母分组
                grouped.forEach { (letter, contacts) ->
                    item(key = "header_$letter") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(c.surfaceContainer)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            MiuixText(
                                text = letter,
                                fontSize = 13.sp,
                                color = c.onSurfaceSecondary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    items(
                        count = contacts.size,
                        key = { i -> contacts[i].id },
                    ) { i ->
                        val contact = contacts[i]
                        SuchatChatRow(
                            title = contact.remark ?: contact.name,
                            subtitle = "",
                            time = "",
                            avatarName = contact.name,
                            avatarSeed = contact.id,
                            showDivider = i != contacts.lastIndex,
                            dividerStart = 68.dp,
                            onClick = { nav.push(SuchatPage.ContactDetail(contact.id)) },
                        )
                        // 联系人行没有副标题与时间，这里隐藏掉对应空位；
                        // 说明（历史）—— 之前用 WeChatListItem 的 subtitle/time 可为空，
                        // 现在 SuchatChatRow 强制要求，故传空串并在下方补偿高度。
                    }
                }

                item(key = "contact_footer") {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(c.surface),
                    )
                }
            }

        // --- 右侧字母索引：长条胶囊 + 滑动 + 气泡 ---
        //
        // ★ 修正（用户反馈"滑动时胶囊向左瞬移"）：
        // 旧结构是 `Box { 气泡; 索引条 }`，Box 宽度 wrap-content —— 气泡（44dp +
        // 44dp 间距 = 88dp）比索引条（28dp）宽得多，气泡一出现就把 Box 撑宽，
        // 而 Box 用 `align(CenterEnd)` 贴右，于是**索引条被挤向左边**，看起来"瞬移"。
        //
        // 修法：外层 Box 给**固定宽度**（气泡位 + 间距 + 条宽），气泡用绝对定位
        // 画在左侧、不参与布局，索引条固定贴右 —— 气泡出现不再影响任何布局。
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.62f)
                .width(IndexBubbleWidth + IndexGap + IndexBarWidth)
                .padding(end = 6.dp),
        ) {
            // 气泡：绝对定位在左侧，垂直位置跟随手指（不参与布局）。
            if (activeLetter != null) {
                LetterBubble(
                    letter = activeLetter ?: "",
                    offsetY = with(density) {
                        (bubbleY - barHeightPx / 2f).toDp()
                    },
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }

            // 长条胶囊：固定贴右。
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(IndexBarWidth)
                    .clip(RoundedCornerShape(IndexBarWidth / 2))
                    .background(c.surfaceContainerHigh.copy(alpha = 0.9f))
                    .onSizeChanged { barHeightPx = it.height.toFloat() }
                    .pointerInput(letters) {
                        // 滑动选字母：手指 y → 字母索引。
                        detectDragGestures(
                            onDragStart = { offset ->
                                bubbleY = offset.y
                                val idx = ((offset.y / barHeightPx) * letters.size)
                                    .toInt().coerceIn(0, letters.lastIndex)
                                selectLetter(letters[idx])
                            },
                            onDragEnd = { activeLetter = null },
                            onDragCancel = { activeLetter = null },
                        ) { change, _ ->
                            bubbleY = change.position.y
                            val idx = ((change.position.y / barHeightPx) * letters.size)
                                .toInt().coerceIn(0, letters.lastIndex)
                            selectLetter(letters[idx])
                            change.consume()
                        }
                    }
                    .pointerInput(letters) {
                        // 点按也选（与滑动共用同一 selectLetter）。
                        detectTapGestures(
                            onPress = { offset ->
                                bubbleY = offset.y
                                val idx = ((offset.y / barHeightPx) * letters.size)
                                    .toInt().coerceIn(0, letters.lastIndex)
                                selectLetter(letters[idx])
                            },
                            onTap = { activeLetter = null },
                        )
                    },
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                letters.forEach { letter ->
                    MiuixText(
                        text = letter,
                        fontSize = 10.sp,
                        fontWeight = if (letter == activeLetter) FontWeight.Bold else FontWeight.Normal,
                        color = if (letter == activeLetter) c.primary else c.onSurfaceSecondary,
                    )
                }
            }
        }
    }
    }
}

/** 索引条宽。 */
private val IndexBarWidth = 28.dp

/** 气泡占位宽。 */
private val IndexBubbleWidth = 44.dp

/** 气泡与索引条之间的间距。 */
private val IndexGap = 10.dp

/**
 * 索引气泡 —— 手指按住索引条时显示当前字母。
 *
 * @param offsetY 相对索引条中心点的垂直偏移（跟随手指）。
 */
@Composable
private fun LetterBubble(
    letter: String,
    offsetY: Dp,
    modifier: Modifier = Modifier,
) {
    val c = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .graphicsLayer { translationY = offsetY.toPx() }
            .size(IndexBubbleWidth)
            .clip(RoundedCornerShape(14.dp))
            .background(c.primary),
        contentAlignment = Alignment.Center,
    ) {
        MiuixText(
            text = letter,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.onPrimary,
        )
    }
}