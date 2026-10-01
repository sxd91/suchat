package io.github.sxd91.suchat.ui.page.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.theme.LocalSuchatTokens
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.core.nav.SuchatPage
import io.github.sxd91.suchat.data.SampleData
import io.github.sxd91.suchat.ui.component.Avatar
import io.github.sxd91.suchat.ui.component.EntryRow

/**
 * 「我」tab。
 *
 * 微信结构：
 * ```
 * [ 头像 | 名字 / 微信号            › ]   ← 个人卡片（大字号、白底、圆角）
 * ────────────────────────────────
 * [ 服务 ]                                ← 服务（支付）
 * ────────────────────────────────
 * [ 收藏 ][ 朋友圈 ][ 卡包 ][ 表情 ]      ← 收藏 / 朋友圈 / 卡包 / 表情
 * ────────────────────────────────
 * [ 设置 ]                                ← 设置
 * ```
 */
@Composable
fun MeScreen(
    nav: SuchatNavigator,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val colors = LocalSuchatTokens.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val me = SampleData.me

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.pageBackground),
    ) {
        // --- 顶栏（「我」页无标题文字，微信是一块留白，此处保持纯色条） ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding)
                .height(48.dp)
                .background(colors.topBar),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            // --- 个人卡片 ---
            item(key = "profile_card") {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(colors.pageBackground),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.cardBackground)
                        .clickable { nav.push(SuchatPage.Profile) }
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(
                        name = me.name,
                        color = me.avatarColor,
                        size = 64.dp,
                        corner = 6.dp,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                    ) {
                        Text(
                            text = me.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "微信号：${me.suchatId}",
                            fontSize = 14.sp,
                            color = colors.textSecondary,
                        )
                    }
                    Text(
                        text = "›",
                        fontSize = 22.sp,
                        color = colors.textHint,
                    )
                }
            }

            // --- 各分组 ---
            val entries = SampleData.meEntries()
            entries.forEachIndexed { index, entry ->
                item(key = "me_entry_$index") {
                    Column {
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(colors.pageBackground),
                        )
                        EntryRow(
                            title = entry.title,
                            iconColor = entry.iconColor,
                            glyph = entry.iconGlyph,
                            showDivider = false,
                            dividerStart = 56.dp,
                            onClick = {
                                val page = when (entry.title) {
                                    "服务" -> SuchatPage.Services
                                    "收藏" -> SuchatPage.Favorites
                                    "朋友圈" -> SuchatPage.MyMoments
                                    "卡包" -> SuchatPage.Cards
                                    "表情" -> SuchatPage.Stickers
                                    "设置" -> SuchatPage.Settings
                                    else -> null
                                }
                                if (page != null) nav.push(page)
                            },
                        )
                    }
                }
            }

            // 底部留白
            item(key = "me_footer") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(colors.pageBackground),
                )
            }
        }
    }
}