package io.github.sxd91.suchat.core.design.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Suchat 静态设计令牌 —— **不随莫奈取色变化**的固定色板。
 *
 * ## 为什么需要静态色板
 *
 * 契约（`docs/android-experience.md`）规定外观有 `LiquidGlass` / `Blur` / `None`
 * 三档、主题走莫奈取色。但聊天气泡、顶栏、底栏这类**观感锚点**如果被动态
 * 取色覆盖，界面会立刻失去产品辨识度。
 *
 * 因此采用双轨：
 *  - **动态层**：`MaterialTheme.colorScheme`（由 `SuchatTheme.kt` 用莫奈生成）
 *    负责系统级组件、设置页、列表底色；
 *  - **静态层**：本文件负责品牌锚点色（气泡绿、顶栏灰、警示红 …）。
 *
 * ## 与既有 `ui/theme/SuchatTheme.kt` 的关系
 *
 * 既有的 `ui.theme.SuchatTheme` 定义了 `SuchatAppearance`（外观/性能/转场三档
 * 设置的结构体），是契约的**配置载体**；本文件只提供**颜色令牌**。
 * 两者由 `SuchatSessionTheme`（见 `SuchatAppTheme.kt`）统一组装。
 *
 * @property brand 品牌绿（主操作色）
 * @property brandPressed 品牌绿按下态
 * @property chatBubbleOut 自己发出的气泡（绿）
 * @property chatBubbleIn 收到的气泡（白/深灰）
 * @property chatBackground 聊天窗口背景
 * @property pageBackground 列表页背景
 * @property topBar 顶栏背景
 * @property tabBar 底栏背景（未启用液态玻璃时的兜底色）
 * @property divider 分割线
 * @property textPrimary 主文本
 * @property textSecondary 次级文本（时间戳、摘要）
 * @property textHint 占位文本
 * @property link 链接蓝
 * @property danger 警示红（红点、删除）
 * @property cardBackground 卡片/列表项背景
 */
data class SuchatTokens(
    val brand: Color,
    val brandPressed: Color,
    val chatBubbleOut: Color,
    val chatBubbleIn: Color,
    val chatBackground: Color,
    val pageBackground: Color,
    val topBar: Color,
    val tabBar: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textHint: Color,
    val link: Color,
    val danger: Color,
    val cardBackground: Color,
) {
    companion object {
        /**
         * 浅色模式。
         *
         * 色值来源：微信 Android 版客户端实测取值。作为参照的
         * `wavky/Compose-WeChatDemo` 其 `weChatGreen = #56BD6A`、
         * `chatGreen = #94EB68` 是 iOS 版取样；本项目按 Android 实际值取
         * `#07C160` / `#95EC69`。
         */
        fun light() = SuchatTokens(
            brand = Color(0xFF07C160),
            brandPressed = Color(0xFF06AD56),
            chatBubbleOut = Color(0xFF95EC69),
            chatBubbleIn = Color(0xFFFFFFFF),
            chatBackground = Color(0xFFEDEDED),
            pageBackground = Color(0xFFEDEDED),
            topBar = Color(0xFFEDEDED),
            tabBar = Color(0xFFF7F7F7),
            divider = Color(0xFFE5E5E5),
            textPrimary = Color(0xFF191919),
            textSecondary = Color(0xFF888888),
            textHint = Color(0xFFB2B2B2),
            link = Color(0xFF576B95),
            danger = Color(0xFFFA5151),
            cardBackground = Color(0xFFFFFFFF),
        )

        /** 深色模式：气泡绿亮度略压低以避免刺眼，其余按深色主题取值。 */
        fun dark() = SuchatTokens(
            brand = Color(0xFF07C160),
            brandPressed = Color(0xFF06AD56),
            chatBubbleOut = Color(0xFF3EB575),
            chatBubbleIn = Color(0xFF2C2C2C),
            chatBackground = Color(0xFF111111),
            pageBackground = Color(0xFF111111),
            topBar = Color(0xFF1E1E1E),
            tabBar = Color(0xFF1E1E1E),
            divider = Color(0xFF2E2E2E),
            textPrimary = Color(0xFFD5D5D5),
            textSecondary = Color(0xFF888888),
            textHint = Color(0xFF5E5E5E),
            link = Color(0xFF7D90B3),
            danger = Color(0xFFFA5151),
            cardBackground = Color(0xFF1E1E1E),
        )
    }
}

/**
 * 静态令牌的 CompositionLocal。
 *
 * 页面用 `LocalSuchatTokens.current.brand` 读取；由 [SuchatRootTheme] 统一提供，
 * 随深浅色自动切换。
 */
val LocalSuchatTokens = staticCompositionLocalOf { SuchatTokens.light() }