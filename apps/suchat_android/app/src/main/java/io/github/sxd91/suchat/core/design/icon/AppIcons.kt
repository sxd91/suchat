package io.github.sxd91.suchat.core.design.icon

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Store

/**
 * 图标统一出口。
 *
 * 底座：**miuix-icons**（`top.yukonga.miuix.kmp:miuix-icons-android`）——
 * miuix 官方矢量图标库，笔重分 Light / Normal / Regular / Medium / Demibold 五档。
 * 选用它是为了与 miuix 主题（MiuixTheme / SmallTopAppBar / 悬浮底栏）保持同一设计语言，
 * 即任务要求里的「参照微信 + WeKit」中的 WeKit 侧。
 *
 * ## 为什么不用 material-icons
 *
 * material-icons-core 的图标集只有几十个基础形状（Home/Search/Settings…），
 * 缺少聊天、通讯录、发现场景所需的图形，且线条风格与 miuix 不搭。
 * miuix-icons 的 aar 已实际解包核对（156 个图标类，含 Messages / Contacts /
 * Community / ContactsCircle 等），是完整可用的图标库。
 *
 * ## 四大 Tab 的图标映射（对齐微信语义）
 *
 * | 微信 Tab | miuix 图标 | 语义 |
 * |---|---|---|
 * | 微信 | [Messages] | 对话气泡 |
 * | 通讯录 | [Contacts] | 联系人册 |
 * | 发现 | [Community] | 社区/探索 |
 * | 我 | [ContactsCircle] | 个人 |
 *
 * 键名与 Tab 定义一一对应，调用侧只认 [forKey] / [forKeySelected]；
 * 换图标库只需改这两张 map，调用侧不动。
 */
object AppIcons {

    // --- 顶栏 / 通用操作 ---

    /** 顶栏返回键。miuix `Regular` 档（顶栏默认字重）。 */
    val Back: ImageVector get() = MiuixIcons.Regular.Back

    /** 右向箭头（列表项「可进入」提示）。 */
    val ChevronForward: ImageVector get() = MiuixIcons.Regular.ChevronForward

    /** 左向箭头（返回/上一级）。 */
    val ChevronBackward: ImageVector get() = MiuixIcons.Regular.ChevronBackward

    /** 「更多」/ 右上角加号菜单入口。 */
    val More: ImageVector get() = MiuixIcons.Regular.More

    /** 添加（加号）——聊天输入栏右侧「+」扩展面板。 */
    val Add: ImageVector get() = MiuixIcons.Regular.Add

    /** 关闭（X）。 */
    val Close: ImageVector get() = MiuixIcons.Regular.Close

    /** 搜索。 */
    val Search: ImageVector get() = MiuixIcons.Regular.Search

    /** 发送（输入栏右侧）。 */
    val Send: ImageVector get() = MiuixIcons.Regular.Send

    /** 扫一扫（发现页入口）。 */
    val Scan: ImageVector get() = MiuixIcons.Regular.Scan

    /** 设置。 */
    val Settings: ImageVector get() = MiuixIcons.Regular.Settings

    /** 分享。 */
    val Share: ImageVector get() = MiuixIcons.Regular.Share

    /** 收藏（我 → 收藏）。 */
    val Favorites: ImageVector get() = MiuixIcons.Regular.Favorites

    /** 商店/服务（我 → 服务）。 */
    val Store: ImageVector get() = MiuixIcons.Regular.Store

    // --- Tab 键映射 ---

    private val tabIcons = mapOf(
        "Chats" to MiuixIcons.Regular.Messages,
        "Contacts" to MiuixIcons.Regular.Contacts,
        "Discover" to MiuixIcons.Regular.Community,
        "Me" to MiuixIcons.Regular.ContactsCircle,
    )

    /** 按 iconKey 取未选中态图标；未知 key 回退「微信」页签图标。 */
    fun forKey(key: String): ImageVector = tabIcons[key] ?: MiuixIcons.Regular.Messages

    /**
     * 按 iconKey 取选中态图标。
     *
     * 当前采用与未选中态同源的做法：微信本身的 tab 选中态是「填充 + 主题绿」，
     * 而 miuix-icons 只有一套字形（无 outline/filled 成对变体），
     * 选中差异由**颜色与底栏指示器**表达（底栏选中项切到主题色 + Q 弹放大）。
     * 若后续引入填充变体，只需改这一处。
     */
    fun forKeySelected(key: String): ImageVector = tabIcons[key] ?: MiuixIcons.Regular.Messages
}