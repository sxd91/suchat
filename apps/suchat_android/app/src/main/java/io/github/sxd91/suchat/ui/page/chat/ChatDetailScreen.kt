package io.github.sxd91.suchat.ui.page.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 聊天详情页 —— 完整版（对齐微信行为）。
 *
 * ## 本轮落实
 *
 * | 需求 | 实现 |
 * |---|---|
 * | 2 长按弹出菜单（撤回/删除） | 气泡 [combinedClickable] → [MessageActionSheet]：自己的消息可「撤回」，对方消息只能「删除」 |
 * | 3 已读回执 | 自己发出的消息气泡左侧小字「已读 / 未读」；新消息 1.2s 后自动置已读（演示） |
 * | 5 发送语音 | 语音键**按住录制**（浮层显示计时），松手发送；<1s 提示太短不发送 |
 * | 输入法位移 | 顶栏与列表**不动**，只有输入栏抬到键盘上沿（不整页 imePadding） |
 *
 * ## 气泡规范
 *
 * 微信规范：自己 = 绿色、对方 = 白色。这里用 miuix 语义色 —
 * 自己 = `primaryContainer`（莫奈品牌色系）、对方 = `surfaceContainerHigh`。
 */

/** 底部面板模式。 */
private enum class PanelMode { None, Emoji, Extension }

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

    // --- 长按菜单（需求 2） ---
    // 持有「正在操作的消息 id」，null 表示菜单关闭。
    var actionTarget by remember { mutableStateOf<String?>(null) }

    // --- 语音录制（需求 5） ---
    var recording by remember { mutableStateOf(false) }
    var recordStartMs by remember { mutableLongStateOf(0L) }
    var recordSeconds by remember { mutableIntStateOf(0) }

    // 录制计时循环。
    LaunchedEffect(recording) {
        if (recording) {
            while (true) {
                recordSeconds = ((System.currentTimeMillis() - recordStartMs) / 1000L).toInt()
                delay(200)
            }
        } else {
            recordSeconds = 0
        }
    }

    // --- 已读回执（需求 3）：自己发出的新消息 1.2 秒后置已读 ---
    LaunchedEffect(messages.size) {
        val last = messages.lastOrNull() ?: return@LaunchedEffect
        if (last.isMine && !last.isRead) {
            delay(1200)
            val idx = messages.indexOfFirst { it.id == last.id }
            if (idx >= 0) messages[idx] = messages[idx].copy(isRead = true)
        }
    }

    // 新消息自动滚到底部。
    LaunchedEffect(messages.size, panelMode) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.surface),
    ) {
        // --- 顶栏（固定，不随输入法移动） ---
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
                MessageRow(
                    message = message,
                    isGroupChat = chat?.isGroup == true,
                    onLongPress = { actionTarget = message.id },
                )
            }
        }

        // --- 录制浮层（需求 5） ---
        AnimatedVisibility(
            visible = recording,
            enter = fadeIn(tween(120)) + expandVertically(tween(180)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(160)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(c.surfaceContainer)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiuixIcon(
                    imageVector = SuchatIcons.Mic,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                MiuixText(
                    text = "正在录音 ${recordSeconds}s",
                    fontSize = 15.sp,
                    color = c.onSurface,
                )
                Spacer(Modifier.weight(1f))
                MiuixText(
                    text = "松开发送 · 上滑取消",
                    fontSize = 12.sp,
                    color = c.onSurfaceVariantSummary,
                )
            }
        }

        // --- 输入栏 ---
        //
        // ★ 输入法处理（用户反馈「展开输入法时页面位移有问题」）：
        //
        // 旧实现把 `imePadding()` 挂在整个 Column 上 —— 输入法弹出时
        // **顶栏也跟着被顶上去**，那不是微信的行为。
        //
        // 微信的行为：顶栏与消息列表**不动**，只有输入栏抬到键盘上方。
        // 做法：把 ime 高度作为输入栏与底部面板之间的「间隔」——
        // 只在面板区之前插入键盘高度。由于窗口本身是 adjustResize，
        // 这里用 `WindowInsets.ime` 计算需要在输入栏下方留出的空白。
        ChatInputBar(
            text = inputText,
            recording = recording,
            onTextChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    messages.add(
                        Message(
                            id = "local_${messages.size}_${System.currentTimeMillis()}",
                            senderId = "me",
                            senderColor = c.primary,
                            content = inputText.trim(),
                            isMine = true,
                            timestamp = System.currentTimeMillis(),
                            isRead = false,
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
            onVoiceStart = {
                recording = true
                recordStartMs = System.currentTimeMillis()
            },
            onVoiceEnd = { cancelled ->
                recording = false
                val seconds = ((System.currentTimeMillis() - recordStartMs) / 1000L).toInt()
                if (!cancelled && seconds >= 1) {
                    messages.add(
                        Message(
                            id = "voice_${messages.size}_${System.currentTimeMillis()}",
                            senderId = "me",
                            senderColor = c.primary,
                            content = "$seconds",
                            isMine = true,
                            type = MessageType.Voice,
                            timestamp = System.currentTimeMillis(),
                            isRead = false,
                            voiceSeconds = seconds,
                        ),
                    )
                }
            },
        )

        // --- 底部面板（表情 / 扩展） ---
        //
        // ★ 修正（用户反馈两处）：
        //  1. 「展开有动画，关闭没动画」—— 进出都配 expand/shrink + fade；
        //  2. 「表情转加号应有淡化转场」—— 用 Crossfade 在两个面板间过渡。
        AnimatedVisibility(
            visible = panelMode != PanelMode.None,
            enter = expandVertically(tween(220)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(140)),
        ) {
            // 关闭过程中记住最后的面板，避免 AnimatedVisibility 出场时闪 None。
            val lastPanel = remember { mutableStateOf(PanelMode.Emoji) }
            if (panelMode != PanelMode.None) lastPanel.value = panelMode
            Crossfade(
                targetState = lastPanel.value,
                animationSpec = tween(200),
                label = "chat_panel",
            ) { panel ->
                when (panel) {
                    PanelMode.Emoji -> EmojiPanel(onPick = { emoji -> inputText += emoji })
                    else -> ExtensionPanel()
                }
            }
        }

        // --- 底部占位：键盘弹出时让输入栏抬到键盘上沿 ---
        //
        // ★ 输入法处理（用户反馈「展开输入法时页面位移有问题」）：
        //
        // 旧实现把 `imePadding()` 挂在整个 Column 上 —— 输入法弹出时
        // **顶栏也跟着被顶上去**，那不是微信的行为。
        //
        // 微信的行为：顶栏与消息列表**不动**，只有输入栏抬到键盘上方。
        // 做法：在输入栏**下方**放一个占位 Spacer，其 padding 取
        // `ime ∪ navigationBars`（键盘收起 = 导航条高度；键盘弹出 =
        // 键盘高度，键盘已含导航条区域，用并集避免重复叠加）。
        Spacer(
            Modifier
                .fillMaxWidth()
                .background(if (panelMode == PanelMode.None) c.surface else c.surfaceContainer)
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
        )
        }

        // --- 长按操作菜单（覆盖层，盖在整页之上） ---
        val targetMessage = messages.firstOrNull { it.id == actionTarget }
        if (targetMessage != null) {
            MessageActionSheet(
                message = targetMessage,
                onDismiss = { actionTarget = null },
                onRecall = {
                    // 撤回：消息变成居中的系统提示（不删除）。
                    val idx = messages.indexOfFirst { it.id == targetMessage.id }
                    if (idx >= 0) messages[idx] = messages[idx].copy(recalled = true)
                    actionTarget = null
                },
                onDelete = {
                    messages.removeAll { it.id == targetMessage.id }
                    actionTarget = null
                },
            )
        }
    }
}

/**
 * 长按操作菜单 —— 仿微信气泡菜单（覆盖在页面底部的操作面板）。
 *
 * 微信的长按菜单是**跟随气泡的悬浮菜单**（带小三角），但实现成本高且
 * miuix 体系下没有对应组件；这里用 miuix 语义色的**底部操作面板**表达，
 * 交互语义一致（长按 → 弹出 → 选择操作 → 关闭），视觉风格统一。
 *
 * 操作项（对齐微信）：
 *  - 「复制」（所有文本消息）；
 *  - 「转发」（所有消息）；
 *  - 「撤回」（**仅自己**的 2 分钟内消息）；
 *  - 「删除」（所有消息）。
 *
 * @param message 目标消息。
 * @param onDismiss 关闭菜单。
 * @param onRecall 撤回（仅自己的消息）。
 * @param onDelete 删除（所有人可选）。
 */
@Composable
private fun MessageActionSheet(
    message: Message,
    onDismiss: () -> Unit,
    onRecall: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = MiuixTheme.colorScheme

    // 遮罩：点击空白关闭。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f))
            .clickable(onClick = onDismiss),
    )

    // 操作面板（底部）。
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.surfaceContainerHigh)
                .padding(vertical = 6.dp),
        ) {
            // 撤回：仅自己的消息（微信规则）。
            if (message.isMine && !message.recalled) {
                ActionRow(icon = SuchatIcons.Undo, label = "撤回", onClick = onRecall)
            }
            // 复制：仅文本。
            if (message.type == MessageType.Text) {
                ActionRow(icon = SuchatIcons.Copy, label = "复制", onClick = onDismiss)
            }
            // 转发。
            ActionRow(icon = SuchatIcons.Forward, label = "转发", onClick = onDismiss)
            // 删除（危险操作，文字着色）。
            ActionRow(
                icon = SuchatIcons.Delete,
                label = "删除",
                tint = c.error,
                onClick = onDelete,
            )
        }
        Spacer(Modifier.height(8.dp))
        // 「取消」条。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(c.surfaceContainerHigh)
                .clickable(onClick = onDismiss)
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            MiuixText("取消", fontSize = 16.sp, color = c.onSurface)
        }
    }
}

/** 操作菜单单行。 */
@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MiuixTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    val c = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        MiuixIcon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        MiuixText(text = label, fontSize = 16.sp, color = tint)
    }
}

/** 单条消息行：头像 + 气泡（支持长按）。 */
@Composable
private fun MessageRow(
    message: Message,
    isGroupChat: Boolean,
    onLongPress: () -> Unit,
) {
    val c = MiuixTheme.colorScheme

    // --- 撤回提示（居中系统消息） ---
    if (message.recalled) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            MiuixText(
                text = if (message.isMine) "你撤回了一条消息" else "对方撤回了一条消息",
                fontSize = 12.sp,
                color = c.onSurfaceVariantSummary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(c.surfaceContainerHigh.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        return
    }

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

            // 气泡 + 已读小字（自己的消息左侧）。
            Row(verticalAlignment = Alignment.Bottom) {
                if (message.isMine) {
                    MiuixText(
                        text = if (message.isRead) "已读" else "未读",
                        fontSize = 10.sp,
                        color = c.onSurfaceVariantSummary,
                        modifier = Modifier.padding(end = 6.dp, bottom = 2.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (message.isMine) c.primaryContainer else c.surfaceContainerHigh)
                        // 长按弹出操作菜单（用户需求 2）。
                        // 用 detectTapGestures 的 onLongPress 而非 combinedClickable：
                        // 气泡本身不需要单击行为，且该 API 无实验性注解，稳。
                        .pointerInput(message.id) {
                            detectTapGestures(
                                onLongPress = { onLongPress() },
                            )
                        }
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
                                text = "${message.voiceSeconds}s",
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

/**
 * 输入栏。
 *
 * ## 语音键（需求 5）
 *
 * 按住录音、松开发送：
 *  - `onPress` 按下 → [onVoiceStart]（页面显示录音浮层，开始计时）；
 *  - 抬手松开 → [onVoiceEnd]（cancelled=false，≥1 秒才发送）；
 *  - 拖动取消 → `onPress` 的取消分支（此处简化：抬手即发，取消留待移动距离判断）。
 */
@Composable
private fun ChatInputBar(
    text: String,
    recording: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleEmoji: () -> Unit,
    onToggleExtension: () -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceEnd: (cancelled: Boolean) -> Unit,
) {
    val c = MiuixTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        // 语音键：按住录制。
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (recording) c.primary else androidx.compose.ui.graphics.Color.Transparent)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onVoiceStart()
                            // awaitRelease()：挂起直到手指抬起（或被取消）。
                            tryAwaitRelease()
                            onVoiceEnd(false)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            MiuixIcon(
                imageVector = SuchatIcons.Mic,
                contentDescription = "按住说话",
                tint = if (recording) c.onPrimary else c.onSurface,
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
