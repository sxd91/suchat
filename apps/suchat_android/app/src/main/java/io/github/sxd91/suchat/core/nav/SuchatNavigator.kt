package io.github.sxd91.suchat.core.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * suchat 轻量导航。
 *
 * ## 为什么自研而不用 navigation-compose
 *
 * 微信的导航模型很简单：**四个常驻主 Tab + 一条二级页返回栈**。
 * 而 navigation-compose 的模型是「每条路由都是栈上的一个 entry」，
 * 直接用会出现两个问题：
 *
 *  1. 四个 Tab 若各挂一个 NavHost，会各自注册预测性返回（互抢手势）；
 *     若共用一个 NavHost，切 Tab 会污染返回栈（微信切 Tab 不重置栈、
 *     但也不该把 A 的二级页留在 B 的历史里）。
 *  2. 微信的转场很特殊：**二级页从右侧滑入、底栏被盖住**，
 *     且主 Tab 之间是平移动画（横向 pager 手感），两套动画语义不同。
 *
 * 自己实现一个 40 行的状态机反而更贴手、行为完全可控：
 *  - [SuchatNavigator.currentTab]：当前主 Tab（切换走平移动画）；
 *  - [SuchatNavigator.backStack]：二级页栈（滑入/滑出）。
 *
 * ## 栈语义
 *
 * 二级页栈是**全局共享**的（不分 Tab）—— 这是微信的实际行为：
 * 从「我」进设置后切到「微信」，再切回来，设置页仍在最上层。
 */
class SuchatNavigator(
    initialTab: SuchatTab = SuchatTab.Chats,
) {
    /** 当前主 Tab。 */
    var currentTab by mutableStateOf(initialTab)
        private set

    /** 二级页返回栈（栈顶为当前显示的二级页；为空表示显示主 Tab）。 */
    val backStack = mutableStateListOf<SuchatPage>()

    /** 栈顶二级页；无二级页时为 null。 */
    val topPage: SuchatPage? get() = backStack.lastOrNull()

    /** 切换主 Tab（清掉在途的二级页？不清 —— 见类注释「栈语义」）。 */
    fun switchTab(tab: SuchatTab) {
        currentTab = tab
    }

    /** 压入二级页。 */
    fun push(page: SuchatPage) {
        backStack.add(page)
    }

    /** 弹出栈顶二级页；栈空返回 false（调用方可据此把返回键交给系统）。 */
    fun pop(): Boolean {
        if (backStack.isEmpty()) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    /** 一路弹到根（点底栏当前 Tab 时的行为）。 */
    fun popToRoot() {
        backStack.clear()
    }

    companion object {
        /**
         * rememberSaveable 用的 Saver。
         *
         * 只序列化「当前 tab + 页面标识列表」：页面参数（如聊天 id）
         * 由页面自己的 rememberSaveable 保存，导航层不重复存。
         */
        val Saver: Saver<SuchatNavigator, List<String>> = Saver(
            save = { nav ->
                buildList {
                    add(nav.currentTab.name)
                    nav.backStack.forEach { add(it.routeKey) }
                }
            },
            restore = { list ->
                val tab = list.firstOrNull()?.let { name ->
                    SuchatTab.entries.firstOrNull { it.name == name }
                } ?: SuchatTab.Chats
                SuchatNavigator(tab).apply {
                    list.drop(1).forEach { key ->
                        SuchatPage.fromRouteKey(key)?.let { push(it) }
                    }
                }
            },
        )
    }
}

/** 四个主 Tab（顺序即底栏顺序）。
 *
 * 命名对齐契约 `docs/android-experience.md`：
 * 「The Android root tabs are **消息**, **联系人**, **发现**, and **我的**」
 */
enum class SuchatTab(val label: String, val iconKey: String) {
    Chats("消息", "Chats"),
    Contacts("联系人", "Contacts"),
    Discover("发现", "Discover"),
    Me("我的", "Me"),
}

/**
 * 全部二级页。
 *
 * 用密封类 + [routeKey] 做可序列化标识（Saver 用），
 * 页面参数尽量少（只带必要 id）。
 */
sealed class SuchatPage {
    /** 路由标识：用于 Saveable 的序列化，值与页面一一对应。 */
    abstract val routeKey: String

    /**
     * 页面层级：1 = 二级页，2 = 三级页。
     *
     * ★ 2026-10-02 新增（用户反馈「二级页面进入三级没有动画」）。
     *
     * ## 为什么需要「层级」这个概念
     *
     * 旧实现把整个二级层写成一个 `AnimatedVisibility(visible = topPage != null)` ——
     * `visible` 只在「有没有二级页」这个 0/1 边界上翻转，
     * 而**二级页 → 三级页时 topPage 只是换了个对象，visible 始终是 true**，
     * 于是转场根本不触发，页面是**瞬间**切换的。
     *
     * 要让转场有「层级」语义，得先知道新页面是**同位**还是**更深一层**：
     *  - 同层（二级 → 二级，如 设置 → 关于）：旧的右滑出、新的右滑入；
     *  - 更深（二级 → 三级，如 设置 → 账号与安全）：新页从右侧滑入盖住，
     *    底层页按 [DEPTH_PARALLAX] 左移并轻微缩小（微信/iOS 的导航堆叠观感）。
     *
     * 层级不当数据存（避免 push 时还要带 level 参数），而是页面的**静态属性**：
     * 层级由页面在信息架构中的位置唯一决定，跟「怎么进去的」无关 ——
     * 从「我」直接进「账号与安全」（假如以后这样做）它仍然是三级页。
     */
    open val depth: Int get() = 1

    // --- 微信 ---
    /** 聊天会话页。 */
    data class ChatDetail(val chatId: String) : SuchatPage() {
        override val routeKey: String get() = "chat/$chatId"
    }

    /** 搜索页（微信首页顶部搜索）。 */
    data object Search : SuchatPage() {
        override val routeKey: String get() = "search"
    }

    /** 发起群聊 / 添加菜单的「+」弹层（不是整页，用底部弹层表达）。 */
    data object AddMenu : SuchatPage() {
        override val routeKey: String get() = "add_menu"
    }

    // --- 通讯录 ---
    /** 新的朋友。 */
    data object NewFriends : SuchatPage() {
        override val routeKey: String get() = "new_friends"
    }

    /** 群聊列表。 */
    data object GroupChats : SuchatPage() {
        override val routeKey: String get() = "group_chats"
    }

    /** 标签。 */
    data object Tags : SuchatPage() {
        override val routeKey: String get() = "tags"
    }

    /** 公众号。 */
    data object OfficialAccounts : SuchatPage() {
        override val routeKey: String get() = "official_accounts"
    }

    /** 联系人详情。 */
    data class ContactDetail(val contactId: String) : SuchatPage() {
        override val routeKey: String get() = "contact/$contactId"
    }

    // --- 发现 ---
    /** 朋友圈。 */
    data object Moments : SuchatPage() {
        override val routeKey: String get() = "moments"
    }

    /**
     * 漂流瓶。
     *
     * 契约明确：「发现 contains 朋友圈 and 漂流瓶」——
     * 这是 Suchat 相对微信的特色功能，必须有独立页面。
     */
    data object DriftBottle : SuchatPage() {
        override val routeKey: String get() = "drift_bottle"
    }

    /** 视频号。 */
    data object Channels : SuchatPage() {
        override val routeKey: String get() = "channels"
    }

    /** 扫一扫。 */
    data object Scan : SuchatPage() {
        override val routeKey: String get() = "scan"
    }

    /** 看一看。 */
    data object TopStories : SuchatPage() {
        override val routeKey: String get() = "top_stories"
    }

    /** 搜一搜。 */
    data object SearchDiscover : SuchatPage() {
        override val routeKey: String get() = "search_discover"
    }

    /** 小程序。 */
    data object MiniPrograms : SuchatPage() {
        override val routeKey: String get() = "mini_programs"
    }

    // --- 我 ---
    /** 服务（支付）。 */
    data object Services : SuchatPage() {
        override val routeKey: String get() = "services"
    }

    /** 收藏。 */
    data object Favorites : SuchatPage() {
        override val routeKey: String get() = "favorites"
    }

    /** 朋友圈（我页进入，同 [Moments] 但入口不同；复用同一页面）。 */
    data object MyMoments : SuchatPage() {
        override val routeKey: String get() = "my_moments"
    }

    /** 卡包。 */
    data object Cards : SuchatPage() {
        override val routeKey: String get() = "cards"
    }

    /** 表情。 */
    data object Stickers : SuchatPage() {
        override val routeKey: String get() = "stickers"
    }

    /** 设置。 */
    data object Settings : SuchatPage() {
        override val routeKey: String get() = "settings"
    }

    /** 个人信息页（我 → 头像卡片）。 */
    data object Profile : SuchatPage() {
        override val routeKey: String get() = "profile"
    }

    // --- 设置子页（三级，depth = 2） ---
    //
    // 这些页面挂在「设置」下面，属于三级导航层级。
    // 命名统一加 Set 前缀，避免与其它页面重名（Kotlin 的 sealed 子类名唯一）。

    /** 账号与安全（三级）。 */
    data object SetAccountSecurity : SuchatPage() {
        override val routeKey: String get() = "settings/account_security"
        override val depth: Int get() = 2
    }

    /** 新消息通知（三级）。 */
    data object SetNotifications : SuchatPage() {
        override val routeKey: String get() = "settings/notifications"
        override val depth: Int get() = 2
    }

    /** 聊天设置（三级）。 */
    data object SetChat : SuchatPage() {
        override val routeKey: String get() = "settings/chat"
        override val depth: Int get() = 2
    }

    /** 隐私（三级）。 */
    data object SetPrivacy : SuchatPage() {
        override val routeKey: String get() = "settings/privacy"
        override val depth: Int get() = 2
    }

    /** 通用（三级）。 */
    data object SetGeneral : SuchatPage() {
        override val routeKey: String get() = "settings/general"
        override val depth: Int get() = 2
    }

    /** 青少年模式（三级）。 */
    data object SetTeenMode : SuchatPage() {
        override val routeKey: String get() = "settings/teen_mode"
        override val depth: Int get() = 2
    }

    /** 关怀模式（三级）。 */
    data object SetCareMode : SuchatPage() {
        override val routeKey: String get() = "settings/care_mode"
        override val depth: Int get() = 2
    }

    /** 外观（三级）：主题/玻璃档/性能档/页面转场，改完即时生效。 */
    data object SetAppearance : SuchatPage() {
        override val routeKey: String get() = "settings/appearance"
        override val depth: Int get() = 2
    }

    /** 存储空间（三级）。 */
    data object SetStorage : SuchatPage() {
        override val routeKey: String get() = "settings/storage"
        override val depth: Int get() = 2
    }

    /** 字体大小（三级）。 */
    data object SetFontSize : SuchatPage() {
        override val routeKey: String get() = "settings/font_size"
        override val depth: Int get() = 2
    }

    /** 关于 Suchat（三级）。 */
    data object SetAbout : SuchatPage() {
        override val routeKey: String get() = "settings/about"
        override val depth: Int get() = 2
    }

    /** 帮助与反馈（三级）。 */
    data object SetHelp : SuchatPage() {
        override val routeKey: String get() = "settings/help"
        override val depth: Int get() = 2
    }

    /** 解除封禁 / 登录设备管理（三级）。 */
    data object SetDevices : SuchatPage() {
        override val routeKey: String get() = "settings/devices"
        override val depth: Int get() = 2
    }

    companion object {
        /**
         * 由 [routeKey] 反解页面。
         *
         * 带参数的页面（chat/xxx、contact/xxx）从 key 中切出参数。
         * 未知 key 返回 null（向前兼容：老版本保存的状态遇到新页面时安全跳过）。
         */
        fun fromRouteKey(key: String): SuchatPage? = when {
            key == "search" -> Search
            key == "add_menu" -> AddMenu
            key == "new_friends" -> NewFriends
            key == "group_chats" -> GroupChats
            key == "tags" -> Tags
            key == "official_accounts" -> OfficialAccounts
            key == "moments" -> Moments
            key == "drift_bottle" -> DriftBottle
            key == "channels" -> Channels
            key == "scan" -> Scan
            key == "top_stories" -> TopStories
            key == "search_discover" -> SearchDiscover
            key == "mini_programs" -> MiniPrograms
            key == "services" -> Services
            key == "favorites" -> Favorites
            key == "my_moments" -> MyMoments
            key == "cards" -> Cards
            key == "stickers" -> Stickers
            key == "settings" -> Settings
            key == "profile" -> Profile
            key == "settings/account_security" -> SetAccountSecurity
            key == "settings/notifications" -> SetNotifications
            key == "settings/chat" -> SetChat
            key == "settings/privacy" -> SetPrivacy
            key == "settings/general" -> SetGeneral
            key == "settings/teen_mode" -> SetTeenMode
            key == "settings/care_mode" -> SetCareMode
            key == "settings/appearance" -> SetAppearance
            key == "settings/storage" -> SetStorage
            key == "settings/font_size" -> SetFontSize
            key == "settings/about" -> SetAbout
            key == "settings/help" -> SetHelp
            key == "settings/devices" -> SetDevices
            key.startsWith("chat/") -> ChatDetail(key.removePrefix("chat/"))
            key.startsWith("contact/") -> ContactDetail(key.removePrefix("contact/"))
            else -> null
        }
    }
}

/** 记忆化导航器（进程内 + 配置变更存活）。 */
@Composable
fun rememberSuchatNavigator(): SuchatNavigator = rememberSaveable(saver = SuchatNavigator.Saver) {
    SuchatNavigator()
}

/**
 * 导航层「层级视差」常量（由 `MainActivity.SecondaryHost` 消费）。
 *
 * ## 语义：底层页在新页滑入时如何让位
 *
 * 微信/iOS 的导航堆叠观感来自「上层推出、下层退后」：
 *  - [DEPTH_PARALLAX] = 底层页左移的比例（相对屏宽）；
 *  - [DEPTH_SCALE] = 底层页缩小的幅度。
 *
 * 这两个值只在「跨层级 push」（如 二级 → 三级）时生效；
 * 同层 push（二级 → 二级）时旧页是整屏滑出，不做视差 —— 否则会看着像"陷进去"。
 */
const val DEPTH_PARALLAX: Float = 0.28f

/** 见 [DEPTH_PARALLAX]。 */
const val DEPTH_SCALE: Float = 0.94f