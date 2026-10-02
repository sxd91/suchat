package io.github.sxd91.suchat.ui.page.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.icon.SuchatIcons
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.data.SampleData
import io.github.sxd91.suchat.data.model.Message
import io.github.sxd91.suchat.data.model.MessageType
import io.github.sxd91.suchat.ui.component.SuchatAvatar
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 聊天详情页。
 *
 * ## 本轮修正（用户第 2、3、6 条）
 *
 * - 顶栏返回键换成 miuix 矢量图标（不再是「‹」字符）；
 * - 语音 / 图片 / 扩展面板的 emoji 全部换成 miuix 矢量图标；
 * - 颜色全部走 `MiuixTheme.colorScheme` 语义色（随莫奈取色变化）。
 *
 * ## 气泡规范
 *
 * 微信规范：自己发出的气泡为绿色、对方为白色；这里改用 miuix 语义色 —
 * 自己 = `primaryContainer`（莫奈取色下的品牌色系），
 * 对方 = `surfaceContainerHigh`（中性容器色）。
 * 既保留「左右 + 深浅」的辨识度，又跟随主题。
 */
@Composable
fun ChatDetailScreen(
    nav: SuchatNavigator,
    chatId: String,
) {
    val c = MiuixTheme.colorScheme
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val chat = remember(chatId) { SampleData.chats.firstOrNull { it.id == chatId } }
    val chatName = chat?.name ?: "聊天"

    val messages = remember(chatId) {
        mutableStateListOf<Message>().apply { addAll(SampleData.messagesFor(chatId)) }
    }
    var inputText by remember { mutableStateOf("") }
    var panelMode by remember { mutableStateOf(PanelMode.None) }
    val listState = rememberLazyListState()

    // 新消息自动滚到底部。
    LaunchedEffect(messages.size, panelMode) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.surface)
            .imePadding(),
    ) {
        // --- 顶栏 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .height(48.dp)
                .background(c.surfaceContainer)
                .padding(start = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiuixIconButton(onClick = { nav.pop() }) {
                MiuixIcon(
                    imageVector = SuchatIcons.Back,
                    contentDescription = "返回",
                    tint = c.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
            MiuixText(
                text = chatName,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = c.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            MiuixIconButton(onClick = { }) {
                MiuixIcon(
                    imageVector = SuchatIcons.More,
                    contentDescription = "更多",
                    tint = c.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // --- 消息列表 ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageRow(message = message, isGroupChat = chat?.isGroup == true)
            }
        }

        // --- 输入栏 ---
        ChatInputBar(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    // 注意：senderColor 是数据模型字段（示例数据用），
                    // 实际渲染时 MessageRow 走的是 miuix 语义色，这里传主题色占位。
                    messages.add(
                        Message(
                            id = "local_${messages.size}_${System.currentTimeMillis()}",
                            senderId = "me",
                            senderColor = c.primary,
                            content = inputText.trim(),
                            isMine = true,
                            timestamp = System.currentTimeMillis(),
                        ),
                    )
                    inputText = ""
                }
            },
            onToggleEmoji = {
                panelMode = if (panelMode == PanelMode.Emoji) PanelMode.None else PanelMode.Emoji
            },
            onToggleExtension = {
                panelMode = if (panelMode == PanelMode.Extension) PanelMode.None else PanelMode.Extension
            },
        )

        // --- 底部面板 ---
        AnimatedVisibility(
            visible = panelMode != PanelMode.None,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            when (panelMode) {
                PanelMode.Emoji -> EmojiPanel(onPick = { emoji -> inputText += emoji })
                PanelMode.Extension -> ExtensionPanel()
                PanelMode.None -> Spacer(Modifier.height(0.dp))
            }
        }

        Spacer(
            Modifier
                .fillMaxWidth()
                .background(if (panelMode == PanelMode.None) c.surface else c.surfaceContainer)
                .navigationBarsPadding(),
        )
    }
}

/** 底部面板模式。 */
private enum class PanelMode { None, Emoji, Extension }

/** 单条消息行：头像 + 气泡。 */
@Composable
private fun MessageRow(message: Message, isGroupChat: Boolean) {
    val c = MiuixTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!message.isMine) {
            SuchatAvatar(
                name = message.senderName ?: "友",
                seed = message.senderId,
                size = 40.dp,
                corner = 6.dp,
            )
            Spacer(Modifier.width(10.dp))
        }

        Column(
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 260.dp),
        ) {
            // 群聊显示发送者名。
            if (!message.isMine && isGroupChat && message.senderName != null) {
                MiuixText(
                    text = message.senderName,
                    fontSize = 12.sp,
                    color = c.onSurfaceVariantSummary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
            }

            // 气泡：自己 = primaryContainer（莫奈品牌色系），对方 = surfaceContainerHigh。
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (message.isMine) c.primaryContainer else c.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                val textColor = if (message.isMine) c.onPrimaryContainer else c.onSurface
                when (message.type) {
                    MessageType.Voice -> Row(verticalAlignment = Alignment.CenterVertically) {
                        MiuixIcon(
                            imageVector = SuchatIcons.Voice,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        MiuixText(
                            text = "${message.content}″",
                            fontSize = 16.sp,
                            color = textColor,
                        )
                    }
                    MessageType.Image -> Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 90.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(c.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        MiuixIcon(
                            imageVector = SuchatIcons.Image,
                            contentDescription = null,
                            tint = c.onSurfaceVariantSummary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    else -> MiuixText(
                        text = message.content,
                        fontSize = 16.sp,
                        color = textColor,
                    )
                }
            }
        }

        if (message.isMine) {
            Spacer(Modifier.width(10.dp))
            SuchatAvatar(
                name = SampleData.me.name,
                seed = SampleData.me.suchatId,
                size = 40.dp,
                corner = 6.dp,
            )
        }
    }
}

/** 输入栏。 */
@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleEmoji: () -> Unit,
    onToggleExtension: () -> Unit,
) {
    val c = MiuixTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // 语音键。
        MiuixIconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
            MiuixIcon(
                imageVector = SuchatIcons.Mic,
                contentDescription = "语音",
                tint = c.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }

        // 输入框。
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.surface)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            if (text.isEmpty()) {
                MiuixText(
                    text = "输入消息…",
                    fontSize = 16.sp,
                    color = c.onSurfaceVariantSummary,
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = TextStyle(fontSize = 16.sp, color = c.onSurface),
                cursorBrush = SolidColor(c.primary),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
            )
        }

        // 有文字 → 发送；否则 → 表情。
        if (text.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.primary)
                    .clickable(onClick = onSend)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                MiuixText("发送", color = c.onPrimary, fontSize = 15.sp)
            }
        } else {
            MiuixIconButton(onClick = onToggleEmoji, modifier = Modifier.size(36.dp)) {
                MiuixIcon(
                    imageVector = SuchatIcons.Favorites,
                    contentDescription = "表情",
                    tint = c.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // 加号（扩展面板）。
        MiuixIconButton(onClick = onToggleExtension, modifier = Modifier.size(36.dp)) {
            MiuixIcon(
                imageVector = SuchatIcons.Add,
                contentDescription = "更多",
                tint = c.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** 表情面板（emoji 字符本身保留 —— 它是**内容**，不是图标）。 */
@Composable
private fun EmojiPanel(onPick: (String) -> Unit) {
    val c = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(c.surfaceContainer)
            .padding(8.dp),
    ) {
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(8),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(SampleData.emojiPanel.size) { index ->
                val emoji = SampleData.emojiPanel[index]
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPick(emoji) },
                    contentAlignment = Alignment.Center,
                ) {
                    MiuixText(emoji, fontSize = 24.sp)
                }
            }
        }
    }
}

/** 扩展面板（相册 / 拍摄 / 视频通话 …）—— 图标为 miuix 矢量。 */
@Composable
private fun ExtensionPanel() {
    val c = MiuixTheme.colorScheme

    // (标签, 图标)
    val items = listOf(
        "相册" to SuchatIcons.Image,
        "拍摄" to SuchatIcons.Camera,
        "视频通话" to SuchatIcons.VideoCall,
        "位置" to SuchatIcons.Location,
        "红包" to SuchatIcons.Wallet,
        "转账" to SuchatIcons.Wallet,
        "语音输入" to SuchatIcons.Mic,
        "收藏" to SuchatIcons.Favorites,
    )
    val rows = items.chunked(4)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(c.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rowItems.forEach { (label, icon) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(c.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            MiuixIcon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = c.onSurface,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        MiuixText(
                            text = label,
                            fontSize = 12.sp,
                            color = c.onSurfaceSecondary,
                            maxLines = 1,
                        )
                    }
                }
                repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}