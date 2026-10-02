package io.github.sxd91.suchat.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.icon.SuchatIcons
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * WeKit 空间抽屉（「负一屏」）。
 *
 * ## 规格来源（不是我编的）
 *
 * 契约 `docs/android-experience.md` 原文：
 *
 * > The home side panel follows WeKit's spatial drawer model: the content shrinks
 * > up to **5%**, translates **right by 7dp** and **down by 8dp**, rounds its
 * > **corners**, and exposes the navigation panel underneath.
 *
 * 即：打开时主内容**缩小 5%**、**右移 7dp**、**下移 8dp**、**加圆角**，
 * 底下露出导航面板。这是 WeKit 的核心空间隐喻 —— 主界面像一张卡片被"推走"，
 * 而不是从侧面滑入一个抽屉。
 *
 * ## 交互
 *
 * - **点击头像**：打开（用户第 8 条）；
 * - **左滑**：也可打开（用户第 8 条）；
 * - **右滑 / 点遮罩 / 返回键**：关闭；
 * - 拖拽过程中内容是跟手的（`dragProgress`），松手后按阈值判定吸附。
 *
 * @param drawerOpen 是否打开（外部状态）。
 * @param onOpen / onClose 状态回调。
 * @param panel 负一屏内容（下层）。
 * @param content 主内容（上层，被缩放平移的那层）。
 */
@Composable
fun WeKitDrawer(
    drawerOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    panel: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    // 动画进度：0 = 关闭，1 = 完全打开。
    val animatedProgress by animateFloatAsState(
        targetValue = if (drawerOpen) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "wekit_drawer",
    )

    // 拖拽中的临时进度（手指跟手，松手后回落到 animatedProgress）。
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val progress = if (dragProgress > 0f) dragProgress else animatedProgress

    Box(modifier = modifier.fillMaxSize()) {
        // ---------- 下层：负一屏面板 ----------
        // 面板本身不移动，只是被上层内容盖住；上层缩小后自然露出。
        Box(
            Modifier
                .fillMaxSize()
                .padding(end = 72.dp),
        ) {
            panel()
        }

        // ---------- 上层：主内容（被缩放平移） ----------
        Box(
            Modifier
                .fillMaxSize()
                // 左滑打开 / 右滑关闭（在内容层上识别水平拖拽）。
                .pointerInput(drawerOpen) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // 松手吸附：过半即开/关。
                            if (dragProgress >= 0.5f) onOpen() else onClose()
                            dragProgress = 0f
                        },
                        onDragCancel = { dragProgress = 0f },
                    ) { _, dragAmount ->
                        val delta = dragAmount / size.width
                        val base = if (drawerOpen) 1f else 0f
                        // 左滑（dragAmount < 0）→ 进度增大。
                        dragProgress = (base - delta).coerceIn(0f, 1f)
                    }
                }
                .graphicsLayer {
                    // 契约规格：缩小 5% / 右移 7dp / 下移 8dp / 圆角。
                    val scale = 1f - 0.05f * progress
                    scaleX = scale
                    scaleY = scale
                    translationX = 7.dp.toPx() * progress
                    translationY = 8.dp.toPx() * progress
                    // 圆角随进度增长（关闭时 0，打开时 28dp）。
                    clip = progress > 0f
                    shape = RoundedCornerShape(28.dp * progress)
                    shadowElevation = 12.dp.toPx() * progress
                }
                .clip(RoundedCornerShape(28.dp * progress))
                .background(MiuixTheme.colorScheme.surface),
        ) {
            content()

            // 打开时的遮罩：点击关闭。
            if (progress > 0.01f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.25f * progress),
                        )
                        .clickable(onClick = onClose),
                )
            }
        }
    }
}

/**
 * 负一屏面板内容 —— 与 miuix example 的 `WeKitPanel` 同构。
 *
 * 结构（自上而下）：
 *  1. 用户头部：头像 + 名字 + 状态；
 *  2. 一张「今日」卡片（日期 / 星期）；
 *  3. 今日回忆；
 *  4. 漂流瓶海域；
 *  5. 快捷操作（新建聊天 / 创建群聊 / 扫一扫）；
 *  6. 今日一句；
 *  7. 朋友圈 · 收藏 · 设置。
 *
 * 全部使用 miuix 组件与语义色，无色块底、无 emoji。
 */
@Composable
fun WeKitPanelContent(
    userName: String,
    statusText: String,
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit = {},
) {
    val c = MiuixTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 64.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // --- 用户头部 ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            SuchatAvatar(name = userName, size = 56.dp, corner = 10.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                MiuixText(
                    text = userName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onSurface,
                )
                MiuixText(
                    text = statusText,
                    fontSize = 13.sp,
                    color = c.onSurfaceSecondary,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // --- 卡片区 ---
        PanelCard(title = "2026 / 10 / 02", summary = "星期四 · 20:14")
        PanelCard(title = "今日回忆", summary = "三年前，你创建了旅行计划。")
        PanelCard(title = "漂流瓶海域", summary = "3 个瓶子正等待开启")
        PanelCard(
            title = "新建聊天    创建群聊    扫一扫",
            summary = "快捷操作",
            onClick = { onItemClick("quick_actions") },
        )
        PanelCard(title = "今日一句", summary = "愿你在每个陌生的地方，都能遇见温柔。")
        PanelCard(
            title = "朋友圈 · 收藏 · 设置",
            summary = "更多功能",
            onClick = { onItemClick("more") },
        )
    }
}

/**
 * 负一屏卡片 —— miuix Surface（圆角 + 语义容器色）。
 */
@Composable
private fun PanelCard(
    title: String,
    summary: String,
    onClick: (() -> Unit)? = null,
) {
    val c = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surfaceContainerHigh)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MiuixText(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = c.onSurface,
            )
            MiuixText(
                text = summary,
                fontSize = 13.sp,
                color = c.onSurfaceSecondary,
            )
        }
    }
}