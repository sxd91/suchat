package io.github.sxd91.suchat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Suchat 通用组件 —— **全部基于 miuix 组件与 miuix 主题色构建**。
 *
 * ## 本轮修正的三件事
 *
 * ### 1. 头像背景改为莫奈取色（用户第 1 条）
 *
 * 之前用固定 10 色低饱和调色板（硬编码 `#5B8DEF` 之类），与本机壁纸/主题无关。
 * 现在改为从 **miuix 主题色**派生：`MiuixTheme.colorScheme` 里的
 * primary / secondary / tertiary / surfaceContainerHigh 等语义色循环，
 * 于是头像会跟随系统莫奈取色自动变色（换壁纸即变）。
 *
 * ### 2. 图标一律用 miuix 矢量图标（用户第 3 条）
 *
 * 不再用 emoji。见 [SuchatIcons]。
 *
 * ### 3. 图标不再加底色块（用户第 6 条）
 *
 * 之前的「彩色圆角小方块 + 白色字形」是微信的图标风格，但用户明确要求
 * **背景透明**。现在图标直接着主题色、无背板，视觉更轻。
 */

/**
 * 头像取色池 —— 从 miuix 主题语义色派生。
 *
 * ## 为什么不用固定色板
 *
 * 用户要求「头像背景为莫奈取色」。莫奈取色的产出是**一整套语义色**
 * （primary / secondary / tertiary / surfaceContainer…），
 * 头像不需要自己发明颜色，直接从这套语义色里按 key 稳定取样即可：
 *
 *  - 换壁纸 → miuix 重新取色 → 头像跟着变（满足需求）；
 *  - 同一个人每次启动取到同一个色（按 [avatarColorIndex] 稳定散列）；
 *  - 深浅色模式下自动跟随（语义色本身随模式变）。
 */
@Immutable
private data class AvatarPalette(val colors: List<Color>)

/**
 * 读取当前 miuix 主题下的头像色池。
 *
 * 取样顺序（视觉区分度从高到低）：
 * primary → tertiary → secondary → primaryContainer → tertiaryContainer →
 * secondaryContainer → surfaceContainerHigh → surfaceContainerHighest
 */
@Composable
private fun rememberAvatarPalette(): AvatarPalette {
    val c = MiuixTheme.colorScheme
    return AvatarPalette(
        listOf(
            c.primary,
            c.tertiaryContainer,
            c.secondary,
            c.primaryContainer,
            c.tertiaryContainer,
            c.secondaryContainer,
            c.surfaceContainerHigh,
            c.surfaceContainerHighest,
        ),
    )
}

/** 稳定散列：同一个 key 永远取到同一个索引。 */
private fun avatarColorIndex(key: String, size: Int): Int {
    var hash = 7
    for (ch in key) {
        hash = hash * 31 + ch.code
    }
    return ((hash % size) + size) % size
}

/**
 * Suchat 头像 —— 圆角方形（微信规范），底色取自莫奈语义色。
 *
 * @param name 用于取色与首字展示。
 * @param seed 取色种子（默认用 name；传 id 可让同名不同人取不同色）。
 * @param size 边长。
 * @param corner 圆角半径。
 */
@Composable
fun SuchatAvatar(
    name: String,
    modifier: Modifier = Modifier,
    seed: String = name,
    size: Dp = 48.dp,
    corner: Dp = 6.dp,
) {
    val palette = rememberAvatarPalette()
    val bg = palette.colors[avatarColorIndex(seed, palette.colors.size)]

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        MiuixText(
            text = name.take(1),
            // 语义色配套的 on 色，保证对比度（莫奈取色的标准搭配）。
            color = onColorFor(bg),
            fontSize = (size.value / 2.3f).sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * 依据背景亮度选前景色。
 *
 * miuix 语义色的 on 色不好逐一定位（primaryContainer 的 on 色与 primary 的不同），
 * 这里用亮度阈值判定：亮底用深字、暗底用白字，通用且稳定。
 */
private fun onColorFor(background: Color): Color {
    val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (luminance > 0.6f) Color(0xFF1A1A1A) else Color.White
}

/**
 * 未读红点（miuix 语义色）。
 *
 * @param count 0 → 不渲染；1..99 → 数字；> 99 → "99+"。
 * @param muted 免打扰 → 灰点无数字。
 */
@Composable
fun SuchatUnreadBadge(
    count: Int,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    if (count <= 0) return
    val c = MiuixTheme.colorScheme

    if (muted) {
        Box(
            modifier = modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(c.onSurfaceSecondary.copy(alpha = 0.55f)),
        )
        return
    }

    val text = if (count > 99) "99+" else count.toString()
    val w = if (text.length > 1) 26.dp else 18.dp
    Box(
        modifier = modifier
            .height(18.dp)
            .width(w)
            .clip(CircleShape)
            .background(c.error),
        contentAlignment = Alignment.Center,
    ) {
        MiuixText(
            text = text,
            color = c.onError,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/**
 * 会话列表项（微信布局 + miuix 配色）。
 *
 * 布局沿用微信规范：64dp 行高、48dp 头像、摘要单行省略、
 * 时间贴右上、红点贴右下、分割线从头像右侧开始。
 */
@Composable
fun SuchatChatRow(
    title: String,
    subtitle: String,
    time: String,
    avatarName: String,
    modifier: Modifier = Modifier,
    avatarSeed: String = avatarName,
    unreadCount: Int = 0,
    muted: Boolean = false,
    showDivider: Boolean = true,
    dividerStart: Dp = 76.dp,
    onClick: (() -> Unit)? = null,
) {
    val c = MiuixTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(start = 16.dp, end = 16.dp)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SuchatAvatar(name = avatarName, seed = avatarSeed, size = 48.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                MiuixText(
                    text = title,
                    fontSize = 16.sp,
                    color = c.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MiuixText(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = c.onSurfaceSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                modifier = Modifier.padding(start = 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MiuixText(
                    text = time,
                    fontSize = 12.sp,
                    color = c.onSurfaceSecondary,
                )
                SuchatUnreadBadge(count = unreadCount, muted = muted)
            }
        }
        if (showDivider) {
            Box(
                Modifier
                    .padding(start = dividerStart)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(c.outline.copy(alpha = 0.35f)),
            )
        }
    }
}

/**
 * 带矢量图标的列表项（发现 / 我 / 设置页通用）。
 *
 * ## 与上一版的区别（用户第 6 条）
 *
 * 旧版是「彩色圆角小方块（28dp）+ 白色字形（emoji）」。
 * 现在**去掉色块背板**：图标直接以主题色绘制在透明底上，
 * 与 miuix 的 Preference / ArrowPreference 视觉一致 —— 更轻、更克制。
 *
 * @param icon miuix 矢量图标（见 [SuchatIcons]）。
 */
@Composable
fun SuchatEntryRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    hasDot: Boolean = false,
    showDivider: Boolean = true,
    dividerStart: Dp = 56.dp,
    onClick: (() -> Unit)? = null,
) {
    val c = MiuixTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 透明底 + 主题色矢量图标（无色块背板）。
            MiuixIcon(
                imageVector = icon,
                contentDescription = null,
                tint = c.onSurface,
                modifier = Modifier.size(23.dp),
            )
            MiuixText(
                text = title,
                fontSize = 16.sp,
                color = c.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
                maxLines = 1,
            )
            if (trailingText != null) {
                MiuixText(
                    text = trailingText,
                    fontSize = 14.sp,
                    color = c.onSurfaceSecondary,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
            if (hasDot) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(c.error),
                )
            }
            if (onClick != null) {
                MiuixIcon(
                    imageVector = SuchatIcons.ChevronForward,
                    contentDescription = null,
                    tint = c.onSurfaceSecondary.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(18.dp),
                )
            }
        }
        if (showDivider) {
            Box(
                Modifier
                    .padding(start = dividerStart)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(c.outline.copy(alpha = 0.35f)),
            )
        }
    }
}

/**
 * 分组间隔（微信的 8dp 灰缝）。
 */
@Composable
fun SuchatGroupGap(height: Dp = 8.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(MiuixTheme.colorScheme.surfaceContainer),
    )
}