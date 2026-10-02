package io.github.sxd91.suchat.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * WeKit 空间抽屉（「负一屏」）—— **1:1 对齐 WeKit 上游实现**。
 *
 * ## 规格来源（逐条抄自 WeKit 源码）
 *
 * 源：`Ujhhgtg/WeKit` · `features/items/beautify/home_screen_panel/HomeSidePanel.kt`
 *
 * | 项 | WeKit 原值 | 位置 |
 * |---|---|---|
 * | 抽屉宽度 | `DRAWER_WIDTH_FRACTION = 0.84f`（屏宽 84%） | HomeSidePanel.kt:1551 |
 * | 遮罩最大透明度 | `DIM_MAX_ALPHA = 0.52f` | HomeSidePanel.kt:1552 |
 * | 主内容缩放 | `scale = 1f - 0.05f * eased`（缩 5%） | :86 |
 * | 主内容位移 | `translationX = 7dp * eased`、`translationY = 8dp * eased` | :87-88 |
 * | 圆角 | `28dp * easedProgress` | :1331 |
 * | 缓动 | `eased = 1 - (1-p)^1.35` | :83 |
 * | 开合动画 | `duration = 120 + 120·|from-target| ms` + 减速插值 | :1049-1050 |
 * | 抽屉滑入 | `panelView.translationX = -drawerWidth * (1-p)` | :1342 |
 * | 开合阈值 | `openThreshold = 0.38`（含 160ms 速度投影） | GestureState:9,12 |
 * | 触摸斜率 | `touchSlop = 8dp`、方向判定 `|dy| > |dx|*1.15` 则放弃 | GestureState:7,10 |
 *
 * ## 层级（关键：盖在主页上层）
 *
 * WeKit 用 `overlayRoot`（含 dim + panel）加到 `decorRoot` 顶部，
 * **盖住**主内容（`contentWrapper`）：
 *
 * ```
 * Box
 *  ├─ 主内容层（缩放/位移/圆角 + 被遮罩压暗）  ← 下层
 *  └─ 抽屉层（dim 遮罩 + 面板，从左侧滑入）      ← 上层，盖住主内容
 * ```
 *
 * 用户要求「负一屏要覆盖一点主页内容在主页面上层」指的就是这条：
 * 面板从左侧滑入并**压在主内容之上**（84% 宽，右侧露出 16% 被遮罩压暗的主页）。
 *
 * ## 手势
 *
 *  - 左滑打开 / 右滑关闭（在内容层上识别水平拖拽）；
 *  - 进度跟手：`progress += dx / drawerWidth`；
 *  - 松手吸附：`progress + velocity*160ms/drawerWidth >= 0.38` 则开，否则关。
 *
 * @param panel 面板内容，参数为抽屉宽度（= 屏宽 × 0.84）。
 */
@Composable
fun WeKitDrawer(
    drawerOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    panel: @Composable (drawerWidth: Dp) -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // 进度：0 = 关闭，1 = 完全打开。用 Animatable 以便跟手 snapTo + 松手动画。
    val progress = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    var screenWidthPx by remember { mutableFloatStateOf(0f) }
    var velocityPxPerMs by remember { mutableFloatStateOf(0f) }
    var lastX by remember { mutableFloatStateOf(0f) }
    var lastTimeMs by remember { mutableFloatStateOf(0f) }

    // ★ 2026-10-02：手势的**同步**进度镜像。
    //
    // 跟手写入改为异步通道后（见下），若手势回调里再读 `progress.value`
    // 会读到「上一帧」的旧值 —— 累加会丢步、松手判定会滞后。
    // 所以维护一个与手势同帧同步推进的镜像值，手势计算只读它。
    var dragProgressSync by remember { mutableFloatStateOf(0f) }

    // ★ 2026-10-02：跟手值通道（CONFLATED）。
    //
    // 旧实现每帧 `scope.launch { progress.snapTo() }` → 大量协程抢
    // Animatable 的 MutatorMutex → 拖拽卡顿。通道只保留最新值，
    // 由单个消费者串行写入 —— 零协程分配、零锁竞争。
    val dragProgressChannel = remember {
        kotlinx.coroutines.channels.Channel<Float>(kotlinx.coroutines.channels.Channel.CONFLATED)
    }
    LaunchedEffect(Unit) {
        for (value in dragProgressChannel) {
            progress.snapTo(value)
        }
    }

    // WeKit：抽屉宽 = 屏宽 × 0.84。
    val drawerWidthPx = screenWidthPx * 0.84f
    val drawerWidthDp = with(density) { drawerWidthPx.toDp() }

    /** WeKit 的减速插值：`1 - (1-t)^1.4`（对应 DecelerateInterpolator(1.4f)）。 */
    val decelerate = remember { Easing { t -> 1f - (1f - t).let { it * it * it * it }.powApprox(1.4f) } }

    // 外部开关 → 动画（手势期间让位，见优先级说明）。
    LaunchedEffect(drawerOpen, dragging) {
        if (dragging) return@LaunchedEffect
        val target = if (drawerOpen) 1f else 0f
        val from = progress.value
        if (abs(from - target) < 0.001f) {
            progress.snapTo(target)
            return@LaunchedEffect
        }
        // WeKit：duration = 120 + 120·|from-target|（ms）。
        val duration = (120 + 120 * abs(target - from)).toInt()
        progress.animateTo(target, tween(duration, easing = decelerate))
    }

    // 缓动进度（WeKit：eased = 1 - (1-p)^1.35）。
    val p = progress.value.coerceIn(0f, 1f)
    val eased = 1f - (1f - p).let { it * it * it }.powApprox(1.35f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { screenWidthPx = it.width.toFloat() },
    ) {
        // ================= 下层：主内容（缩放/位移 + 被遮罩压暗） =================
        Box(
            Modifier
                .fillMaxSize()
                // 手势：左滑打开 / 右滑关闭。
                .pointerInput(drawerOpen) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            lastX = offset.x
                            lastTimeMs = System.currentTimeMillis().toFloat()
                            velocityPxPerMs = 0f
                            // 镜像对齐当前动画值（起手时不跳变）。
                            dragProgressSync = progress.value
                        },
                        onDragEnd = {
                            // WeKit：带上速度投影的开合判定（160ms 投影 + 0.38 阈值）。
                            // 用同步镜像值判定，避免读到异步滞后的动画值。
                            val projected = dragProgressSync +
                                velocityPxPerMs * 160f / drawerWidthPx.coerceAtLeast(1f)
                            val open = projected >= 0.38f
                            dragging = false
                            // 同步外部状态；随后 LaunchedEffect 会动画到位。
                            if (open) onOpen() else onClose()
                        },
                        onDragCancel = {
                            dragging = false
                            val open = dragProgressSync >= 0.38f
                            if (open) onOpen() else onClose()
                        },
                    ) { change, dragAmount ->
                        // 进度跟手：progress -= dx / drawerWidth（左滑 dx<0 → 进度增大）。
                        val now = System.currentTimeMillis().toFloat()
                        val dt = (now - lastTimeMs).coerceAtLeast(1f)
                        velocityPxPerMs = (change.position.x - lastX) / dt
                        lastX = change.position.x
                        lastTimeMs = now
                        // ★ 同步推进镜像值（手势计算唯一依据），再异步推给动画。
                        dragProgressSync = (dragProgressSync - dragAmount / drawerWidthPx.coerceAtLeast(1f))
                            .coerceIn(0f, 1f)
                        dragProgressChannel.trySend(dragProgressSync)
                    }
                }
                .graphicsLayer {
                    // WeKit：缩 5%、右移 7dp、下移 8dp（围绕中心缩放）。
                    val scale = 1f - 0.05f * eased
                    scaleX = scale
                    scaleY = scale
                    translationX = 7.dp.toPx() * eased
                    translationY = 8.dp.toPx() * eased
                    // WeKit：圆角 28dp × eased（夹紧防弹簧过冲出负值 → 崩溃）。
                    clip = eased > 0f
                    shape = RoundedCornerShape(28.dp * eased.coerceIn(0f, 1f))
                }
                .clip(RoundedCornerShape(28.dp * eased.coerceIn(0f, 1f)))
                .background(MiuixTheme.colorScheme.surface),
        ) {
            content()

            // 遮罩：点击关闭（WeKit：dimView 点击 → close）。
            if (eased > 0.01f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.52f * eased))
                        .clickable(onClick = onClose),
                )
            }
        }

        // ================= 上层：抽屉面板（从左侧滑入，盖住主内容） =================
        //
        // WeKit：panelView 宽 = drawerWidth，translationX = -drawerWidth × (1-p)。
        // 即「完全关闭时整体移到屏幕左外侧，打开时归位」——盖在主页上层。
        Box(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxHeight()
                .width(drawerWidthDp)
                .graphicsLayer {
                    translationX = -drawerWidthPx * (1f - p)
                },
        ) {
            panel(drawerWidthDp)
        }
    }
}

/** `x^exp` 的快速近似（仅用于缓动曲线，精度足够）。 */
private fun Float.powApprox(exp: Float): Float {
    if (this <= 0f) return 0f
    return kotlin.math.exp(exp * kotlin.math.ln(this))
}

// ============================================================================
// 面板内容（1:1 对齐 WeKit 的卡片布局）
// ============================================================================

/** 面板动作项（图标 + 标题 + 可选右侧文字）。 */
private data class PanelAction(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val trailing: String? = null,
)

/**
 * 负一屏面板内容 —— **1:1 对齐 WeKit 的 HomeSidePanelHome**。
 *
 * ## 结构（自上而下，与 WeKit 一致）
 *
 *  1. **用户头部**：头像 58dp + 昵称（titleLarge/SemiBold）+ 状态 + 右箭头；
 *  2. **时间卡**：`HH:mm`（displaySmall/Bold）+ 日期 + 问候语，
 *     容器 `surfaceContainerLow`、圆角 24dp、内边距 18dp；
 *  3. **收付款卡**：`primaryContainer` + 圆角 24dp（WeKit 的钱包卡同款；
 *     我们无钱包数据，改为「收付款」入口）；
 *  4. **竖排动作卡**：列表项（圆角 22dp / `surfaceContainerLow`），
 *     项内容「图标 + 文字 + 右箭头」——对齐 WeKit 的 LIST_ITEM 排布；
 *  5. **今日一句卡**：`surfaceContainerHighest` + 圆角 22dp，
 *     标题行（引号图标 + 标题）+ 正文 + 右下角出处。
 *
 * ## 宽度
 *
 * 抽屉宽 = 屏宽 × 84%，卡片内边距 18dp（与 WeKit 的 `padding(horizontal = 18.dp)` 一致）。
 *
 * @param drawerWidth 抽屉宽度（= 屏宽 × 0.84）。
 */
@Composable
fun WeKitPanelContent(
    userName: String,
    statusText: String,
    drawerWidth: Dp,
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit = {},
) {
    val c = MiuixTheme.colorScheme
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // 时间（WeKit：每分钟刷新一次）。
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = LocalDateTime.now()
            now = current
            val nextMinute = current.plusMinutes(1).withSecond(0).withNano(0)
            val waitMs = java.time.Duration.between(current, nextMinute).toMillis()
            kotlinx.coroutines.delay(waitMs.coerceAtLeast(1L))
        }
    }
    val timeText = now.format(DateTimeFormatter.ofPattern("HH:mm"))
    val dateText = now.format(DateTimeFormatter.ofPattern("M月d日 EEEE"))
    val greeting = when (now.hour) {
        in 5..11 -> "早上好，今天也要加油。"
        in 12..17 -> "下午好，希望一切顺利。"
        else -> "晚上好，好好休息。"
    }

    val actions = remember {
        listOf(
            PanelAction("add_friend", "添加朋友", SuchatIcons.Contacts),
            PanelAction("moments", "朋友圈", SuchatIcons.Moments),
            PanelAction("channels", "视频号", SuchatIcons.Channels),
            PanelAction("mark_read", "一键已读", SuchatIcons.Messages, "标记全部会话"),
            PanelAction("settings", "设置", SuchatIcons.Settings),
        )
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(drawerWidth)
            .background(c.surface),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = statusBarPadding + 14.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // --- 1. 用户头部（WeKit：avatar 58dp + 名字 + 状态 + chevron） ---
            item(key = "profile") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onItemClick("profile") }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SuchatAvatar(name = userName, seed = userName, size = 58.dp, corner = 12.dp)
                    Column(Modifier.weight(1f)) {
                        MiuixText(
                            text = userName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = c.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MiuixText(
                                text = statusText,
                                fontSize = 13.sp,
                                color = c.onSurfaceSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            MiuixIcon(
                                imageVector = SuchatIcons.ChevronForward,
                                contentDescription = null,
                                tint = c.onSurfaceVariantSummary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            // --- 2. 时间卡（WeKit：surfaceContainerLow / 24dp / 18dp 内边距） ---
            item(key = "datetime") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.surfaceContainerHigh)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        MiuixText(
                            text = timeText,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = c.onSurface,
                        )
                        Spacer(Modifier.width(10.dp))
                        MiuixText(
                            text = dateText,
                            fontSize = 12.sp,
                            color = c.onSurfaceSecondary,
                            modifier = Modifier.padding(bottom = 7.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    MiuixText(
                        text = greeting,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = c.onSurface,
                    )
                }
            }

            // --- 3. 收付款卡（WeKit：primaryContainer / 24dp） ---
            item(key = "wallet") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.primaryContainer)
                        .clickable { onItemClick("pay") }
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MiuixIcon(
                        imageVector = SuchatIcons.Wallet,
                        contentDescription = null,
                        tint = c.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        MiuixText(
                            text = "收付款",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = c.onPrimaryContainer,
                        )
                        MiuixText(
                            text = "向商家付款 · 二维码收款",
                            fontSize = 12.sp,
                            color = c.onPrimaryContainer.copy(alpha = 0.75f),
                        )
                    }
                    MiuixIcon(
                        imageVector = SuchatIcons.ChevronForward,
                        contentDescription = null,
                        tint = c.onPrimaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // --- 4. 竖排动作卡（WeKit：LIST_ITEM 排布） ---
            item(key = "actions") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.surfaceContainerHigh),
                ) {
                    actions.forEachIndexed { index, action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(action.key) }
                                .padding(horizontal = 18.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MiuixIcon(
                                imageVector = action.icon,
                                contentDescription = action.title,
                                tint = c.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            MiuixText(
                                text = action.title,
                                fontSize = 16.sp,
                                color = c.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 14.dp),
                                maxLines = 1,
                            )
                            if (action.trailing != null) {
                                MiuixText(
                                    text = action.trailing,
                                    fontSize = 12.sp,
                                    color = c.onSurfaceVariantSummary,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            }
                            MiuixIcon(
                                imageVector = SuchatIcons.ChevronForward,
                                contentDescription = null,
                                tint = c.onSurfaceVariantSummary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        if (index != actions.lastIndex) {
                            Box(
                                Modifier
                                    .padding(start = 54.dp)
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(c.outline.copy(alpha = 0.3f)),
                            )
                        }
                    }
                }
            }

            // --- 5. 今日一句卡（WeKit：surfaceContainerHighest / 22dp） ---
            item(key = "hitokoto") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(c.surfaceContainerHighest)
                        .clickable { onItemClick("drift") }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MiuixIcon(
                            imageVector = SuchatIcons.DriftBottle,
                            contentDescription = null,
                            tint = c.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        MiuixText(
                            text = "漂流瓶 · 今日一句",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = c.onSurface,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    MiuixText(
                        text = "愿你在每个陌生的地方，都能遇见温柔。",
                        fontSize = 16.sp,
                        color = c.onSurface,
                    )
                    MiuixText(
                        text = "—— 来自 青岛的瓶子",
                        fontSize = 11.sp,
                        color = c.onSurfaceSecondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item(key = "bottom_space") { Box(Modifier.size(8.dp)) }
        }
    }
}
