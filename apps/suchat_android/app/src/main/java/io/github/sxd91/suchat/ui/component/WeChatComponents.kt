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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.theme.LocalSuchatTokens

/**
 * 通用 UI 组件 —— 微信风格的复用原件。
 *
 * 设计口径（对齐微信 Android 版）：
 *  - 头像是 **圆角方形**（4dp 圆角），不是正圆 —— 这是微信与多数 IM 的显著差异；
 *  - 列表项高度 64dp，左内边距 16dp、头像 48dp、间距 12dp；
 *  - 未读红点：圆形 #FA5151，最小 18dp（两位数自动加宽）；
 *  - 分割线：从左 72dp 处开始（微信的分割线不顶到左边缘，给头像让位）；
 *  - 分组列表：上下各留 8dp 白边，组内项之间是内缩分割线。
 */

/** 微信风格头像：圆角方形 + 首字（无图占位）。 */
@Composable
fun Avatar(
    name: String,
    color: Color,
    size: Dp = 48.dp,
    corner: Dp = 4.dp,
    isGroup: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        // 无图时用「名字首字」占位。
        // 群聊与单聊此阶段视觉一致：真头像接入后，群聊会换成九宫格拼图。
        Text(
            text = name.take(1),
            color = Color.White,
            fontSize = (size.value / 2.2f).sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * 未读消息红点。
 *
 * @param count 未读数：0 → 不渲染；1..99 → 显示数字；> 99 → "99+"。
 * @param muted 免打扰：灰点（不带数字），微信语义。
 */
@Composable
fun UnreadBadge(
    count: Int,
    muted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (count <= 0) return
    val colors = LocalSuchatTokens.current

    if (muted) {
        // 免打扰：一个实心灰点，尺寸固定，不显示数字。
        Box(
            modifier = modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(colors.textHint),
        )
        return
    }

    val text = if (count > 99) "99+" else count.toString()
    // 两位数及以上自动加宽：微信的胶囊形红点。
    val width = if (text.length > 1) 26.dp else 18.dp
    Box(
        modifier = modifier
            .height(18.dp)
            .width(width)
            .clip(CircleShape)
            .background(colors.danger),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/**
 * 微信风格列表项（单行）。
 *
 * @param leading 左侧内容（头像等）。
 * @param title 主标题。
 * @param subtitle 次级文字（灰色小字，如会话摘要）。
 * @param trailing 右侧内容（时间、红点等），垂直居中。
 * @param trailingTop 右侧顶行内容（时间），与标题顶对齐 —— 微信会话列表布局。
 */
@Composable
fun WeChatListItem(
    leading: @Composable () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color? = null,
    trailingTop: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    dividerStart: Dp = 76.dp,
) {
    val colors = LocalSuchatTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(start = 16.dp, end = 16.dp)
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 14.sp,
                            color = subtitleColor ?: colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (trailingTop != null) {
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        trailingTop()
                    }
                }
            }
            if (trailing != null) {
                Box(Modifier.padding(start = 8.dp)) { trailing() }
            }
        }
        if (showDivider) {
            // 微信分割线：从左 76dp 开始（对齐头像右侧），单像素。
            Box(
                modifier = Modifier
                    .padding(start = dividerStart)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.divider),
            )
        }
    }
}

/**
 * 微信风格分组卡片。
 *
 * 一组列表项，包在白色背景里、圆角 8dp、上下留 8dp 间距；
 * 组内最后一项**不画分割线**（微信行为）。
 */
@Composable
fun WeChatGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalSuchatTokens.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp)
            .background(colors.cardBackground),
    ) {
        content()
    }
}

/**
 * 带彩色图标块的列表项（发现页 / 我页 / 设置页通用）。
 *
 * 微信的这些页面每行是「彩色小图标 + 标题 + 右箭头」，
 * 图标是 24dp 左右的小方块，颜色各不相同。
 *
 * @param glyph 图标占位字符（前端阶段用 emoji/符号代替真图标素材）。
 */
@Composable
fun EntryRow(
    title: String,
    iconColor: Color,
    glyph: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    hasDot: Boolean = false,
    showDivider: Boolean = true,
    dividerStart: Dp = 56.dp,
    onClick: (() -> Unit)? = null,
) {
    val colors = LocalSuchatTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 彩色图标块（微信风格：圆角小方块 + 白色符号）
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(iconColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = glyph,
                    color = Color.White,
                    fontSize = 14.sp,
                )
            }
            Text(
                text = title,
                fontSize = 16.sp,
                color = colors.textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
                maxLines = 1,
            )
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
            if (hasDot) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(colors.danger),
                )
            }
            if (onClick != null) {
                Text(
                    text = "›",
                    fontSize = 20.sp,
                    color = colors.textHint,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .padding(start = dividerStart)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.divider),
            )
        }
    }
}

/**
 * 顶栏（仿微信）。
 *
 * 微信顶栏是**纯色**的（浅灰 #EDEDED / 深色 #1E1E1E），
 * 标题居中、左右操作位于两侧 —— 与 miuix / Material 的「大标题」风格不同，
 * 这里照微信来：44dp 高、居中标题 17sp、左右各一个 24dp 图标位。
 *
 * > 说明：甲方要求「UI 参照微信和 wekit」。微信侧体现在**布局与色板**
 * > （本组件、列表项、气泡）；wekit/miuix 侧体现在**底栏液态玻璃、
 * > 主题取色、组件质感**（LiquidGlassTabBar、SuchatTheme）。
 */
@Composable
fun WeChatTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    val colors = LocalSuchatTokens.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(colors.topBar),
    ) {
        // 标题居中（微信规范）
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            modifier = Modifier.align(Alignment.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // 左侧返回
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", fontSize = 28.sp, color = colors.textPrimary)
            }
        }
        // 右侧操作
        if (actions != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
            ) { actions() }
        }
    }
}