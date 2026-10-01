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
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.theme.LocalSuchatTokens
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.core.nav.SuchatPage
import io.github.sxd91.suchat.data.SampleData
import io.github.sxd91.suchat.data.model.Message
import io.github.sxd91.suchat.data.model.MessageType
import io.github.sxd91.suchat.ui.component.Avatar

/**
 * 聊天会话页。
 *
 * 结构（自下而上）：输入栏固定在底部，消息列表填充其余空间，
 * 表情/扩展面板从输入栏下方**展开**（微信是顶起内容，不是覆盖）。
 *
 * ```
 * [ ‹ 名字                              ⋯ ]   ← 顶栏
 * [               消息气泡区                ]
 * [ 😊  输入框                        ＋  ]   ← 输入栏（贴底）
 * [ 表情面板 / 扩展面板（展开时出现）        ]
 * ```
 */
@Composable
fun ChatDetailScreen(
    nav: SuchatNavigator,
    chatId: String,
) {
    val colors = LocalSuchatTokens.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val chat = remember(chatId) { SampleData.chats.firstOrNull { it.id == chatId } }
    val chatName = chat?.name ?: "聊天"

    // 消息列表：进入页面时取占位数据，发送后追加到本地状态。
    val messages = remember(chatId) {
        mutableStateListOf<Message>().apply { addAll(SampleData.messagesFor(chatId)) }
    }
    var inputText by remember { mutableStateOf("") }
    // 底部面板模式：无 / 表情 / 扩展
    var panelMode by remember { mutableStateOf(PanelMode.None) }

    val listState = rememberLazyListState()

    // 新消息自动滚到底部（微信行为）。
    LaunchedEffect(messages.size, panelMode) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.chatBackground)
            .imePadding(),
    ) {
        // --- 顶栏 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .height(48.dp)
                .background(colors.topBar)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { nav.pop() },
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", fontSize = 28.sp, color = colors.textPrimary)
            }
            Text(
                text = chatName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { /* 更多菜单（占位） */ },
                contentAlignment = Alignment.Center,
            ) {
                Text("⋯", fontSize = 22.sp, color = colors.textPrimary)
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
                    messages.add(
                        Message(
                            id = "local_${messages.size}_${System.currentTimeMillis()}",
                            senderId = "me",
                            senderColor = SampleData.me.avatarColor,
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
            emojiActive = panelMode == PanelMode.Emoji,
            extensionActive = panelMode == PanelMode.Extension,
        )

        // --- 底部面板（表情 / 扩展） ---
        AnimatedVisibility(
            visible = panelMode != PanelMode.None,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            when (panelMode) {
                PanelMode.Emoji -> EmojiPanel(
                    onPick = { emoji -> inputText += emoji },
                )
                PanelMode.Extension -> ExtensionPanel()
                PanelMode.None -> Spacer(Modifier.height(0.dp))
            }
        }

        // 底部手势条留白
        Spacer(
            Modifier
                .fillMaxWidth()
                .background(
                    if (panelMode == PanelMode.None) colors.chatBackground else colors.cardBackground
                )
                .navigationBarsPadding(),
        )
    }
}

/** 底部面板模式。 */
private enum class PanelMode { None, Emoji, Extension }

/**
 * 单条消息行：头像 + 气泡。
 *
 * 微信规则：
 *  - 别人的消息：头像在左、气泡白色、小尖角朝左；
 *  - 自己的消息：头像在右、气泡绿色（#95EC69）、尖角朝右；
 *  - 群聊里别人的消息在气泡上方显示发送者名字（12sp 灰字）。
 */
@Composable
private fun MessageRow(message: Message, isGroupChat: Boolean) {
    val colors = LocalSuchatTokens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!message.isMine) {
            Avatar(
                name = message.senderName ?: "友",
                color = message.senderColor,
                size = 40.dp,
                corner = 4.dp,
            )
            Spacer(Modifier.width(10.dp))
        }

        Column(
            horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 260.dp),
        ) {
            // 群聊中显示发送者名
            if (!message.isMine && isGroupChat && message.senderName != null) {
                Text(
                    text = message.senderName,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
            }

            // 气泡
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (message.isMine) colors.chatBubbleOut else colors.chatBubbleIn
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                when (message.type) {
                    MessageType.Voice -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔊",
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${message.content}″",
                            fontSize = 16.sp,
                            color = colors.textPrimary,
                        )
                    }
                    MessageType.Image -> Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 90.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(message.senderColor.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🖼", fontSize = 26.sp)
                    }
                    else -> Text(
                        text = message.content,
                        fontSize = 16.sp,
                        color = colors.textPrimary,
                    )
                }
            }
        }

        if (message.isMine) {
            Spacer(Modifier.width(10.dp))
            Avatar(
                name = SampleData.me.name,
                color = SampleData.me.avatarColor,
                size = 40.dp,
                corner = 4.dp,
            )
        }
    }
}

/**
 * 输入栏（微信底部那条）。
 *
 * ```
 * [ 🔊 ]  [   输入框   ]  [ 😊 ]  [ ＋ ]
 * ```
 */
@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleEmoji: () -> Unit,
    onToggleExtension: () -> Unit,
    emojiActive: Boolean,
    extensionActive: Boolean,
) {
    val colors = LocalSuchatTokens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.tabBar)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // 语音键（占位）
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("🔊", fontSize = 20.sp)
        }

        // 输入框
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.cardBackground)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (text.isEmpty()) {
                Text(
                    text = "输入消息…",
                    fontSize = 16.sp,
                    color = colors.textHint,
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 16.sp,
                    color = colors.textPrimary,
                ),
                cursorBrush = SolidColor(colors.brand),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
            )
        }

        // 发送 / 表情按钮：有文字时显示「发送」，否则显示表情键（微信行为）
        if (text.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.brand)
                    .clickable(onClick = onSend)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("发送", color = Color.White, fontSize = 15.sp)
            }
        } else {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .clickable(onClick = onToggleEmoji),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "😊",
                    fontSize = 20.sp,
                    modifier = Modifier.then(
                        if (emojiActive) Modifier.background(Color.Transparent) else Modifier
                    ),
                )
            }
        }

        // 加号（扩展面板）
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .clickable(onClick = onToggleExtension),
            contentAlignment = Alignment.Center,
        ) {
            Text("＋", fontSize = 24.sp, color = colors.textPrimary)
        }
    }
}

/** 表情面板（微信 8 列网格）。 */
@Composable
private fun EmojiPanel(onPick: (String) -> Unit) {
    val colors = LocalSuchatTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(colors.cardBackground)
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
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onPick(emoji) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji, fontSize = 24.sp)
                }
            }
        }
    }
}

/** 扩展面板（相册 / 拍摄 / 视频通话 …，微信 4 列两行）。 */
@Composable
private fun ExtensionPanel() {
    val colors = LocalSuchatTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(colors.cardBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SampleData.chatInputExtensions.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                rowItems.forEach { (label, color) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(color),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label.take(1),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                }
                // 补齐最后一行空位，保持左对齐
                repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}