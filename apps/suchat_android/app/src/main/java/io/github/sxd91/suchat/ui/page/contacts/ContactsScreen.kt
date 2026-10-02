package io.github.sxd91.suchat.ui.page.contacts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import io.github.sxd91.suchat.ui.component.SuchatAvatar
import io.github.sxd91.suchat.ui.component.SuchatChatRow
import io.github.sxd91.suchat.ui.component.SuchatEntryRow
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        Column(Modifier.fillMaxSize()) {
            // --- 顶栏 ---
            Row_(
                statusBarPadding = statusBarPadding,
                title = "联系人",
                onAdd = { nav.push(SuchatPage.NewFriends) },
            )

            // --- 列表 ---
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(c.surface),
                contentPadding = PaddingValues(bottom = bottomInset),
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
        }

        // --- 右侧字母索引：长条胶囊 + 滑动 + 气泡 ---
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp),
        ) {
            // 气泡（在胶囊左侧）。仅在按住时可见，位置跟随手指。
            AnimatedVisibility(
                visible = activeLetter != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 44.dp),
            ) {
                LetterBubble(
                    letter = activeLetter ?: "",
                    offsetY = with(density) { (bubbleY - barHeightPx / 2f).toDp() },
                )
            }

            // 长条胶囊。
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.62f)
                    .width(28.dp)
                    .clip(RoundedCornerShape(14.dp))
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
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
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

/**
 * 索引气泡 —— 手指按住索引条时显示当前字母。
 *
 * 位置用 [offsetY] 跟随手指相对索引条中心点的偏移；
 * 视觉是「圆角方形 + 大号字母 + 主题色底」，与 miuix 的 Tooltip 风格一致。
 */
@Composable
private fun LetterBubble(
    letter: String,
    offsetY: Dp,
) {
    val c = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .graphicsLayer { translationY = offsetY.toPx() }
            .size(44.dp)
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

/** 顶栏：标题 + 右上角「+」。 */
@Composable
private fun Row_(
    statusBarPadding: Dp,
    title: String,
    onAdd: () -> Unit,
) {
    val c = MiuixTheme.colorScheme
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = statusBarPadding)
            .height(48.dp)
            .background(c.surfaceContainer)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixText(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.onSurface,
            modifier = Modifier.weight(1f),
        )
        MiuixIconButton(onClick = onAdd) {
            MiuixIcon(
                imageVector = SuchatIcons.Add,
                contentDescription = "添加",
                tint = c.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}