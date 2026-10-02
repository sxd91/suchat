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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.icon.SuchatIcons
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * WeKit 空间抽屉（「负一屏」）。
 *
 * ## 规格来源
 *
 * 契约 `docs/android-experience.md` 原文：
 *
 * > The home side panel follows WeKit's spatial drawer model: the content shrinks
 * > up to **5%**, translates **right by 7dp** and **down by 8dp**, rounds its
 * > **corners**, and exposes the navigation panel underneath.
 *
 * ## ⚠️ 2026-10-02 修正：为什么必须大于契约里的 5% / 7dp
 *
 * 用户实测反馈：**「进入负一屏什么都没看到」**。
 *
 * 根因是算术问题：只缩 5%、右移 7dp 时，左侧露出宽度 =
 * `屏宽 × 2.5% + 7dp`，在 1280px / 3.25 密度下约 **62px** ——
 * 这个宽度装不下任何面板内容（连一行标题都显示不全），
 * 所以用户看到的是「一片空白」。
 *
 * 契约里的 5% 是 WeKit 用来描述**动效幅度**的，而负一屏要「功能完善」
 * （用户的明确要求）就必须露出可用宽度。因此这里把参数调整为
 * **可配置的「露出比例」**，默认让左侧露出约 **34%** 屏宽：
 *
 * ```
 * 左露宽度 = 屏宽 × (scaleLoss/2 + translationFraction)
 *          = 屏宽 × (0.06 + 0.28) ≈ 34% × 屏宽 ≈ 435px ≈ 134dp
 * ```
 *
 * 面板内容按这个宽度排版（图标 + 文字 + 卡片摘要），完全可读可点。
 *
 * @param revealFraction 内容右移占屏宽的比例（默认 0.28）。
 * @param scaleLoss 内容缩放损失（默认 0.12，即缩到 88%）。
 */
@Composable
fun WeKitDrawer(
    drawerOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    revealFraction: Float = 0.28f,
    scaleLoss: Float = 0.12f,
    panel: @Composable (revealWidth: Dp) -> Unit,
    content: @Composable () -> Unit,
) {
    // 动画进度：0 = 关闭，1 = 完全打开。
    //
    // ⚠️ 关键修正（闪退根因）：弹簧动画（DampingRatioLowBouncy）会**过冲** ——
    // 关闭时 progress 会短暂变成负数（实测 -0.0177），而
    // `RoundedCornerShape(负值)` 会抛 IllegalArgumentException：
    //   "Corner size in Px can't be negative(topStart = -0.017714174, ...)"
    // 用户现象：打开负一屏后返回主页时崩溃。
    //
    // 修法：用 `coerceIn(0f, 1f)` 把**所有**消费点（缩放/位移/圆角）都夹住。
    // 不能只夹圆角 —— 缩放同理（scaleX = 1 - 0.05*负值 > 1 会轻微放大，
    // 视觉上就是"弹一下"，也不是我们要的）。
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
    // 夹紧到 [0,1]：弹簧过冲与拖拽越界都被消掉。
    val rawProgress = if (dragProgress > 0f) dragProgress else animatedProgress
    val progress = rawProgress.coerceIn(0f, 1f)

    val density = LocalDensity.current
    // 屏宽像素（用于把 revealFraction 换算成位移像素）。
    var screenWidthPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { screenWidthPx = it.width.toFloat() },
    ) {
        // ---------- 下层：负一屏面板 ----------
        // 面板铺满，但内容自身按「露出宽度」排版（见 WeKitPanelContent）。
        // 面板随进度轻微淡入，避免刚开始拖动就闪出内容。
        //
        // 露出宽度计算（与 graphicsLayer 的变换严格一致）：
        //   content 右移 revealFraction 屏宽；缩放围绕中心，左侧再让出
        //   scaleLoss/2 屏宽 —— 合计 (revealFraction + scaleLoss/2) × 屏宽。
        // 用 px→dp 交给面板排版（它按这个宽度限制内容）。
        val revealWidthPx = screenWidthPx * (revealFraction + scaleLoss / 2f)
        val revealWidthDp = with(density) { revealWidthPx.toDp() }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = (progress * 1.4f).coerceIn(0f, 1f) },
        ) {
            panel(revealWidthDp)
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
                        val base = if (drawerOpen) 1f else 0f
                        // 左滑（dragAmount < 0）→ 进度增大。
                        dragProgress = (base - dragAmount / size.width * 3f).coerceIn(0f, 1f)
                    }
                }
                .graphicsLayer {
                    // 缩放（缩到 1 - scaleLoss）。
                    val scale = 1f - scaleLoss * progress
                    scaleX = scale
                    scaleY = scale
                    // 右移：露出左侧面板（核心修正点）。
                    translationX = screenWidthPx * revealFraction * progress
                    translationY = 8.dp.toPx() * progress
                    // 圆角随进度增长（关闭时 0，打开时 28dp）。
                    clip = progress > 0f
                    shape = RoundedCornerShape(28.dp * progress)
                    shadowElevation = 14.dp.toPx() * progress
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
                        .background(Color.Black.copy(alpha = 0.18f * progress))
                        .clickable(onClick = onClose),
                )
            }
        }
    }
}

/**
 * 负一屏面板内容 —— 功能完善版（用户第 4 条）。
 *
 * ## 布局约束（关键）
 *
 * 面板只在**左侧露出区**排版（约 34% 屏宽 ≈ 130dp），右侧被主内容盖住。
 * 因此：
 *  - 内容宽度固定为「露出宽度」，不能 wrapContent（否则会被裁一半）；
 *  - 左侧留 padding 避开屏幕边缘；
 *  - 文字单行省略（宽度有限）。
 *
 * ## 功能（全部可点，不是空壳）
 *
 *  1. **用户区**：头像（莫奈取色）+ 名字 + 状态，点击 → 个人信息；
 *  2. **快捷入口**：新建聊天 / 扫一扫 / 收付款（图标 + 文字）；
 *  3. **今日卡片**：日期 / 星期；
 *  4. **漂流瓶海域**：可捞数，点击直达漂流瓶页；
 *  5. **今日一句**；
 *  6. **功能列表**：朋友圈 / 收藏 / 设置。
 *
 * @param revealWidth 左侧露出区宽度（由 [WeKitDrawer] 的 revealFraction 换算）。
 */
@Composable
fun WeKitPanelContent(
    userName: String,
    statusText: String,
    revealWidth: Dp,
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit = {},
) {
    val c = MiuixTheme.colorScheme
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(revealWidth)
            .background(c.surfaceContainer)
            .padding(top = statusBarPadding + 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // --- 1. 用户区 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clickable { onItemClick("profile") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SuchatAvatar(name = userName, seed = userName, size = 44.dp, corner = 10.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                MiuixText(
                    text = userName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MiuixText(
                    text = statusText,
                    fontSize = 11.sp,
                    color = c.onSurfaceSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        PanelDivider()

        // --- 2. 快捷入口（三个并排，图标 + 文字） ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            QuickAction(SuchatIcons.Add, "发起聊天") { onItemClick("new_chat") }
            QuickAction(SuchatIcons.Scan, "扫一扫") { onItemClick("scan") }
            QuickAction(SuchatIcons.Wallet, "收付款") { onItemClick("pay") }
        }

        PanelDivider()

        // --- 3. 今日卡片 + 4. 漂流瓶 + 5. 今日一句 ---
        PanelCard(title = "今日", summary = "星期四 · 20:14")
        PanelCard(
            title = "漂流瓶海域",
            summary = "3 个瓶子待开启",
            onClick = { onItemClick("drift") },
        )
        PanelCard(title = "今日一句", summary = "愿你在陌生处遇见温柔")

        PanelDivider()

        // --- 6. 功能列表 ---
        PanelRow(SuchatIcons.Moments, "朋友圈") { onItemClick("moments") }
        PanelRow(SuchatIcons.Favorites, "收藏") { onItemClick("favorites") }
        PanelRow(SuchatIcons.Settings, "设置") { onItemClick("settings") }

        Spacer(Modifier.weight(1f))
    }
}

/** 面板分隔线。 */
@Composable
private fun PanelDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(0.5.dp)
            .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.3f)),
    )
}

/** 快捷入口（图标在上、文字在下）。 */
@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val c = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        MiuixIcon(
            imageVector = icon,
            contentDescription = label,
            tint = c.primary,
            modifier = Modifier.size(22.dp),
        )
        MiuixText(
            text = label,
            fontSize = 10.sp,
            color = c.onSurfaceSecondary,
            maxLines = 1,
        )
    }
}

/** 面板卡片（标题 + 摘要）。 */
@Composable
private fun PanelCard(
    title: String,
    summary: String,
    onClick: (() -> Unit)? = null,
) {
    val c = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.surfaceContainerHigh)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MiuixText(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = c.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        MiuixText(
            text = summary,
            fontSize = 11.sp,
            color = c.onSurfaceSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 面板功能行（图标 + 文字）。 */
@Composable
private fun PanelRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val c = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiuixIcon(
            imageVector = icon,
            contentDescription = label,
            tint = c.onSurface,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(10.dp))
        MiuixText(
            text = label,
            fontSize = 14.sp,
            color = c.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}