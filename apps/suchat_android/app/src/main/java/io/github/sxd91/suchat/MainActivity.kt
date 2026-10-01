package io.github.sxd91.suchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.sxd91.suchat.core.design.glass.LiquidGlassTabBar
import io.github.sxd91.suchat.core.design.glass.TabBarMode
import io.github.sxd91.suchat.core.design.glass.TabItem
import io.github.sxd91.suchat.core.design.theme.LocalSuchatTokens
import io.github.sxd91.suchat.core.design.theme.SuchatRootTheme
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.core.nav.SuchatPage
import io.github.sxd91.suchat.core.nav.SuchatTab
import io.github.sxd91.suchat.core.nav.rememberSuchatNavigator
import io.github.sxd91.suchat.ui.page.chat.ChatDetailScreen
import io.github.sxd91.suchat.ui.page.chats.ChatsScreen
import io.github.sxd91.suchat.ui.page.contacts.ContactsScreen
import io.github.sxd91.suchat.ui.page.discover.DiscoverScreen
import io.github.sxd91.suchat.ui.page.drift.DriftBottleScreen
import io.github.sxd91.suchat.ui.page.me.MeScreen
import io.github.sxd91.suchat.ui.page.secondary.ContactDetailScreen
import io.github.sxd91.suchat.ui.page.secondary.GroupChatsScreen
import io.github.sxd91.suchat.ui.page.secondary.MomentsScreen
import io.github.sxd91.suchat.ui.page.secondary.NewFriendsScreen
import io.github.sxd91.suchat.ui.page.secondary.OfficialAccountsScreen
import io.github.sxd91.suchat.ui.page.secondary.PlaceholderScreen
import io.github.sxd91.suchat.ui.page.secondary.ProfileScreen
import io.github.sxd91.suchat.ui.page.secondary.SettingsScreen
import io.github.sxd91.suchat.ui.page.secondary.TagsScreen
import io.github.sxd91.suchat.ui.setup.ServerSetupScreen
import io.github.sxd91.suchat.ui.theme.SuchatAppearance
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * Suchat Android 主 Activity。
 *
 * ## 两个阶段
 *
 * 1. **服务器连接**（既有流程保留）：输入地址 → 健康检查 → 连接成功或进入预览；
 * 2. **主界面**：四 Tab 外壳 + 二级页栈（本次接入的完整前端）。
 *
 * ## 外壳结构（对齐契约 + WeKit）
 *
 * ```
 * Box
 *   ├ 主 Tab 层（HorizontalPager 四页 + 液态玻璃悬浮底栏）
 *   │    └ 底栏是 pager 的兄弟节点，叠在其上做折射采样
 *   └ 二级页层（栈顶页面全屏覆盖，右滑入 / 右滑出）
 * ```
 *
 * ## 底栏契约（docs/android-experience.md）
 *
 * 「The floating bottom bar follows the WeKit liquid-glass interaction model:
 * pill geometry, backdrop sampling, an elastic draggable selection indicator,
 * press scale, accessibility tab semantics, and optional dynamic highlight.」
 *
 * 由 [LiquidGlassTabBar] 完整实现；外观三档由 [SuchatAppearance.glassMode]
 * 显式驱动，**不做设备能力启发式判断**（契约要求）。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 外观配置来自契约结构体；当前为默认值，后续接设置页 / 服务端。
            val appearance = remember { SuchatAppearance() }
            SuchatRootTheme(appearance) {
                SuchatLauncher()
            }
        }
    }
}

/** 会话状态：未连接 / 预览模式 / 已连接。 */
private sealed interface AppSession {
    data object Preview : AppSession
    data class Connected(val endpoint: String) : AppSession
}

@Composable
private fun SuchatLauncher() {
    var session by remember { mutableStateOf<AppSession?>(null) }

    when (session) {
        null -> ServerSetupScreen(
            onPreview = { session = AppSession.Preview },
            onConnected = { session = AppSession.Connected(it) },
        )
        is AppSession.Preview -> SuchatAppShell()
        is AppSession.Connected -> SuchatAppShell()
    }
}

/**
 * 应用外壳：主 Tab 层 + 二级页层。
 *
 * 这是本次整合的核心 —— 把完整的前端页面体系接到既有的启动流程后面。
 */
@Composable
private fun SuchatAppShell() {
    val nav = rememberSuchatNavigator()
    val scope = rememberCoroutineScope()
    val tokens = LocalSuchatTokens.current

    // 底栏项：文字与图标键均对齐契约（消息/联系人/发现/我的）。
    val tabs = remember {
        SuchatTab.entries.map { TabItem(it.label, it.iconKey) }
    }

    val pagerState = rememberPagerState(
        initialPage = nav.currentTab.ordinal,
        pageCount = { SuchatTab.entries.size },
    )

    // 滚动限位：胶囊 64dp + 12dp 间距 + 手势条。
    // 注意：**只抬高滚动终点，不做内容区 padding** —— 内容要能从玻璃下方
    // 穿过，玻璃才有东西可折射（契约的液态玻璃成立前提）。
    val barBottomPadding = 12.dp +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = TAB_BAR_HEIGHT + barBottomPadding

    // 双向同步：点底栏 → pager 平移动画；滑 pager → 更新当前 tab。
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val tab = SuchatTab.entries.getOrNull(page)
            if (tab != null && nav.currentTab != tab) {
                nav.switchTab(tab)
            }
        }
    }
    LaunchedEffect(nav.currentTab) {
        val target = nav.currentTab.ordinal
        if (pagerState.currentPage != target || pagerState.targetPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    // 系统返回键：优先弹二级页栈（栈空则交给系统退出）。
    BackHandler(enabled = nav.backStack.isNotEmpty()) {
        nav.pop()
    }

    Box(Modifier.fillMaxSize()) {
        // ---------- 主 Tab 层 ----------
        MainTabs(
            tabs = tabs,
            nav = nav,
            pagerState = pagerState,
            bottomInset = bottomInset,
            barBottomPadding = barBottomPadding,
            onSelectTab = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
        )

        // ---------- 二级页层 ----------
        val topPage = nav.topPage
        AnimatedVisibility(
            visible = topPage != null,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(260)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(260)),
        ) {
            // 退出动画期间 topPage 会先变 null，需记住最后一个非空页面，
            // 否则滑出时内容会瞬间消失。
            val renderedPage = remember { mutableStateOf<SuchatPage?>(null) }
            LaunchedEffect(topPage) {
                if (topPage != null) renderedPage.value = topPage
            }
            val page = renderedPage.value ?: topPage
            if (page != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(tokens.pageBackground),
                ) {
                    SecondaryHost(page = page, nav = nav, bottomInset = bottomInset)
                }
            }
        }
    }
}

/** 主 Tab 层：pager + 悬浮液态玻璃底栏（兄弟节点）。 */
@Composable
private fun MainTabs(
    tabs: List<TabItem>,
    nav: SuchatNavigator,
    pagerState: androidx.compose.foundation.pager.PagerState,
    bottomInset: Dp,
    barBottomPadding: Dp,
    onSelectTab: (Int) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) { page ->
            when (SuchatTab.entries[page]) {
                SuchatTab.Chats -> ChatsScreen(nav = nav, bottomInset = bottomInset)
                SuchatTab.Contacts -> ContactsScreen(nav = nav, bottomInset = bottomInset)
                SuchatTab.Discover -> DiscoverScreen(nav = nav, bottomInset = bottomInset)
                SuchatTab.Me -> MeScreen(nav = nav, bottomInset = bottomInset)
            }
        }

        LiquidGlassTabBar(
            items = tabs,
            selectedIndex = pagerState.targetPage,
            onSelect = onSelectTab,
            backdrop = backdrop,
            // 外观三档：默认 LiquidGlass（契约默认值）。
            mode = TabBarMode.LiquidGlass,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = barBottomPadding),
        )
    }
}

/** 二级页路由分发。 */
@Composable
private fun SecondaryHost(page: SuchatPage, nav: SuchatNavigator, bottomInset: Dp) {
    when (page) {
        is SuchatPage.ChatDetail -> ChatDetailScreen(nav = nav, chatId = page.chatId)
        is SuchatPage.ContactDetail -> ContactDetailScreen(
            nav = nav, contactId = page.contactId, bottomInset = bottomInset,
        )
        SuchatPage.Search -> PlaceholderScreen(
            nav = nav, title = "搜索",
            description = "搜索聊天记录、联系人、朋友圈", bottomInset = bottomInset,
        )
        SuchatPage.AddMenu -> PlaceholderScreen(
            nav = nav, title = "＋",
            description = "发起群聊 / 添加朋友 / 扫一扫 / 收付款", bottomInset = bottomInset,
        )
        SuchatPage.NewFriends -> NewFriendsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.GroupChats -> GroupChatsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.Tags -> TagsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.OfficialAccounts -> OfficialAccountsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.Moments, SuchatPage.MyMoments -> MomentsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.DriftBottle -> DriftBottleScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.Channels -> PlaceholderScreen(
            nav = nav, title = "视频号",
            description = "短视频与直播内容流", bottomInset = bottomInset,
        )
        SuchatPage.Scan -> PlaceholderScreen(
            nav = nav, title = "扫一扫",
            description = "扫码 / 识物 / 翻译", bottomInset = bottomInset,
        )
        SuchatPage.TopStories -> PlaceholderScreen(
            nav = nav, title = "看一看",
            description = "朋友在看 / 精选内容", bottomInset = bottomInset,
        )
        SuchatPage.SearchDiscover -> PlaceholderScreen(
            nav = nav, title = "搜一搜",
            description = "搜索全网内容", bottomInset = bottomInset,
        )
        SuchatPage.MiniPrograms -> PlaceholderScreen(
            nav = nav, title = "小程序",
            description = "最近使用 / 我的小程序", bottomInset = bottomInset,
        )
        SuchatPage.Services -> PlaceholderScreen(
            nav = nav, title = "服务",
            description = "收付款 / 钱包 / 生活缴费", bottomInset = bottomInset,
        )
        SuchatPage.Favorites -> PlaceholderScreen(
            nav = nav, title = "收藏",
            description = "收藏的聊天记录、图片、链接", bottomInset = bottomInset,
        )
        SuchatPage.Cards -> PlaceholderScreen(
            nav = nav, title = "卡包",
            description = "卡券 / 会员卡 / 交通卡", bottomInset = bottomInset,
        )
        SuchatPage.Stickers -> PlaceholderScreen(
            nav = nav, title = "表情",
            description = "我的表情 / 表情商店", bottomInset = bottomInset,
        )
        SuchatPage.Settings -> SettingsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.Profile -> ProfileScreen(nav = nav, bottomInset = bottomInset)
    }
}

/** 悬浮底栏的滚动限位高度（与 `LiquidGlassTabBar` 胶囊一致）。 */
private val TAB_BAR_HEIGHT: Dp = 64.dp