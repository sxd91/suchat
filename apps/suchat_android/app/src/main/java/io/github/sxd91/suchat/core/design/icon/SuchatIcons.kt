package io.github.sxd91.suchat.core.design.icon

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Clear
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Email
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Forward
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Help
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Mic
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Music
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Phone
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Promotions
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Recording
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Timer
import top.yukonga.miuix.kmp.icon.extended.Translate
import top.yukonga.miuix.kmp.icon.extended.Undo
import top.yukonga.miuix.kmp.icon.extended.Update
import top.yukonga.miuix.kmp.icon.extended.WorldClock
import top.yukonga.miuix.kmp.icon.extended.ZoomOut

/**
 * Suchat 图标统一出口 —— **全部使用 miuix 矢量图标，不使用 emoji**。
 *
 * ## 为什么必须换成矢量图标
 *
 * 之前的实现用 emoji（🍾 / 🌊 / 📡…）当图标，问题有三：
 *
 *  1. **审美**：emoji 是彩色位图字体，与 miuix 的线性矢量风格完全冲突，
 *     在设置列表里像贴纸，破坏整体质感；
 *  2. **不可着色**：emoji 无法跟随主题取色，在深色/莫奈配色下不可控；
 *  3. **尺寸与基线不齐**：emoji 的视觉重心与文字基线不一致，排版会飘。
 *
 * 现在全部改用 `top.yukonga.miuix.kmp.icon`（miuix 官方矢量库，
 * 已解包核对：156 个图标可用）。它们：
 *  - 是 `ImageVector`，**可随主题 tint 着色**；
 *  - 有 Light / Normal / Regular / Medium / Demibold 五档笔重，视觉统一；
 *  - 与 miuix 组件（Icon / Card / TopAppBar）同源设计。
 *
 * ## 用法
 *
 * 调用侧只认语义键（[SuchatIcons.Chats] 之类），换图标库只需改本文件。
 */
object SuchatIcons {

    // --- 底部导航（四大主 Tab） ---

    /** 消息。 */
    val Chats: ImageVector get() = MiuixIcons.Regular.Messages

    /** 联系人。 */
    val Contacts: ImageVector get() = MiuixIcons.Regular.Contacts

    /** 发现。 */
    val Discover: ImageVector get() = MiuixIcons.Regular.Community

    /** 我的。 */
    val Me: ImageVector get() = MiuixIcons.Regular.ContactsCircle

    // --- 通用操作 ---

    /** 返回。 */
    val Back: ImageVector get() = MiuixIcons.Regular.Back

    /** 右箭头（列表项可进入）。 */
    val ChevronForward: ImageVector get() = MiuixIcons.Regular.ChevronForward

    /** 左箭头。 */
    val ChevronBackward: ImageVector get() = MiuixIcons.Regular.ChevronBackward

    /** 更多（右上角菜单）。 */
    val More: ImageVector get() = MiuixIcons.Regular.More

    /** 添加（「+」）。 */
    val Add: ImageVector get() = MiuixIcons.Regular.Add

    /** 关闭。 */
    val Close: ImageVector get() = MiuixIcons.Regular.Close

    /** 复制（长按菜单）。 */
    val Copy: ImageVector get() = MiuixIcons.Regular.Copy

    /** 转发（长按菜单）。 */
    val Forward: ImageVector get() = MiuixIcons.Regular.Forward

    /** 删除（长按菜单）。 */
    val Delete: ImageVector get() = MiuixIcons.Regular.Delete

    /** 清空（扫一扫清结果 / 清除缓存）。 */
    val Clear: ImageVector get() = MiuixIcons.Regular.Clear

    /** 文件（收藏·文件）。 */
    val File: ImageVector get() = MiuixIcons.Regular.File

    /** 网格（小程序宫格 / 表情宫格）。 */
    val GridView: ImageVector get() = MiuixIcons.Regular.GridView

    /** 帮助（关于 / 帮助与反馈）。 */
    val Help: ImageVector get() = MiuixIcons.Regular.Help

    /** 链接（收藏·链接）。 */
    val Link: ImageVector get() = MiuixIcons.Regular.Link

    /** 音乐（收藏·音乐 / 音乐播放）。 */
    val Music: ImageVector get() = MiuixIcons.Regular.Music

    /** 优惠券（卡包·券）。 */
    val Promotions: ImageVector get() = MiuixIcons.Regular.Promotions

    /** 最近使用（小程序·最近）。 */
    val Recent: ImageVector get() = MiuixIcons.Regular.Recent

    /** 更新（检查更新 / 版本）。 */
    val Update: ImageVector get() = MiuixIcons.Regular.Update

    /** 世界时钟（看一看·资讯 / 时间类条目）。 */
    val WorldClock: ImageVector get() = MiuixIcons.Regular.WorldClock

    /** 播放（视频号中央标识 / 电影购票）。 */
    val Play: ImageVector get() = MiuixIcons.Regular.Play

    /** 点赞·填充（视频号 / 看一看的点赞状态）。 */
    val FavoritesFill: ImageVector get() = MiuixIcons.Regular.FavoritesFill

    /** 商店（小程序 / 购物图标）。 */
    val Store: ImageVector get() = MiuixIcons.Regular.Store

    /** 计时器（12306 / 出行 / 摇一摇）。 */
    val Timer: ImageVector get() = MiuixIcons.Regular.Timer

    /** 主题（手电筒开关等通用图标）。 */
    val Theme: ImageVector get() = MiuixIcons.Regular.Theme

    /** 搜索。 */
    val Search: ImageVector get() = MiuixIcons.Regular.Search

    /** 发送。 */
    val Send: ImageVector get() = MiuixIcons.Regular.Send

    /** 扫一扫。 */
    val Scan: ImageVector get() = MiuixIcons.Regular.Scan

    /** 设置。 */
    val Settings: ImageVector get() = MiuixIcons.Regular.Settings

    /** 分享。 */
    val Share: ImageVector get() = MiuixIcons.Regular.Share

    // --- 聊天输入栏 ---

    /** 语音输入。 */
    val Mic: ImageVector get() = MiuixIcons.Regular.Mic

    /** 语音消息（波形图标）。 */
    val Voice: ImageVector get() = MiuixIcons.Regular.Recording

    /** 图片。 */
    val Image: ImageVector get() = MiuixIcons.Regular.Image

    /** 拍摄（相机）。 */
    val Camera: ImageVector get() = MiuixIcons.Regular.Image

    /** 视频通话。 */
    val VideoCall: ImageVector get() = MiuixIcons.Regular.Play

    /** 语音通话。 */
    val Phone: ImageVector get() = MiuixIcons.Regular.Phone

    /** 位置。 */
    val Location: ImageVector get() = MiuixIcons.Regular.Location

    /** 红包 / 转账（钱包）。 */
    val Wallet: ImageVector get() = MiuixIcons.Regular.BankCards

    /** 收藏（列表用）。 */
    val Favorites: ImageVector get() = MiuixIcons.Regular.Favorites

    // --- 发现页 ---

    /** 朋友圈。 */
    val Moments: ImageVector get() = MiuixIcons.Regular.Notes

    /** 视频号。 */
    val Channels: ImageVector get() = MiuixIcons.Regular.Messages

    /** 直播。 */
    val Live: ImageVector get() = MiuixIcons.Regular.Recording

    /** 看一看。 */
    val TopStories: ImageVector get() = MiuixIcons.Regular.FavoritesFill

    /** 搜一搜。 */
    val SearchDiscover: ImageVector get() = MiuixIcons.Regular.Search

    /** 小程序。 */
    val MiniPrograms: ImageVector get() = MiuixIcons.Regular.Store

    /** 摇一摇。 */
    val Shake: ImageVector get() = MiuixIcons.Regular.Timer

    /** 购物（图标库无购物车，用商家袋替代）。 */
    val Shopping: ImageVector get() = MiuixIcons.Regular.Store

    /** 游戏（图标库无游戏手柄，用主题图标替代）。 */
    val Games: ImageVector get() = MiuixIcons.Regular.Theme

    /** 漂流瓶（用信封意象：把话交给远方）。 */
    val DriftBottle: ImageVector get() = MiuixIcons.Regular.Email

    /** 翻译。 */
    val Translate: ImageVector get() = MiuixIcons.Regular.Translate

    /** 缩放（识物）。 */
    val Zoom: ImageVector get() = MiuixIcons.Regular.ZoomOut

    /** 撤销（扔回海里）。 */
    val Undo: ImageVector get() = MiuixIcons.Regular.Undo

    /** 对话（「聊天」设置项）。 */
    val Messages: ImageVector get() = MiuixIcons.Regular.Messages

    /** 锁（隐私设置项）。 */
    val Lock: ImageVector get() = MiuixIcons.Regular.Lock

    /** 按 iconKey 取未选中态图标；未知 key 回退「消息」图标。 */
    fun forKey(key: String): ImageVector = when (key) {
        "Chats" -> Chats
        "Contacts" -> Contacts
        "Discover" -> Discover
        "Me" -> Me
        else -> Chats
    }

    /**
     * 按 iconKey 取选中态图标。
     *
     * miuix-icons 只有一套字形（无 outline/filled 成对变体），
     * 选中差异由**颜色**（切主题色）与**底栏指示器**（Q 弹玻璃胶囊）表达。
     * 若后续引入填充变体，只需改这一处。
     */
    fun forKeySelected(key: String): ImageVector = forKey(key)
}