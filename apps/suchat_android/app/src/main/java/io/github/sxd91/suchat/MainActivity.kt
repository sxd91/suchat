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
import io.github.sxd91.suchat.core.design.theme.SuchatRootTheme
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.core.nav.SuchatPage
import io.github.sxd91.suchat.core.nav.SuchatTab
import io.github.sxd91.suchat.core.nav.rememberSuchatNavigator
import io.github.sxd91.suchat.ui.component.WeKitDrawer
import io.github.sxd91.suchat.ui.component.WeKitPanelContent
import io.github.sxd91.suchat.ui.page.chat.ChatDetailScreen
import io.github.sxd91.suchat.ui.page.chats.ChatsScreen
import io.github.sxd91.suchat.ui.page.contacts.ContactsScreen
import io.github.sxd91.suchat.ui.page.discover.DiscoverScreen
import io.github.sxd91.suchat.ui.page.drift.DriftBottleScreen
import io.github.sxd91.suchat.ui.page.functional.AddMenuScreen
import io.github.sxd91.suchat.ui.page.functional.CardsScreen
import io.github.sxd91.suchat.ui.page.functional.ChannelsScreen
import io.github.sxd91.suchat.ui.page.functional.FavoritesScreen
import io.github.sxd91.suchat.ui.page.functional.MiniProgramsScreen
import io.github.sxd91.suchat.ui.page.functional.ScanScreen
import io.github.sxd91.suchat.ui.page.functional.SearchDiscoverScreen
import io.github.sxd91.suchat.ui.page.functional.SearchScreen
import io.github.sxd91.suchat.ui.page.functional.ServicesScreen
import io.github.sxd91.suchat.ui.page.functional.StickersScreen
import io.github.sxd91.suchat.ui.page.functional.TopStoriesScreen
import io.github.sxd91.suchat.ui.page.me.MeScreen
import io.github.sxd91.suchat.ui.page.secondary.ContactDetailScreen
import io.github.sxd91.suchat.ui.page.secondary.GroupChatsScreen
import io.github.sxd91.suchat.ui.page.secondary.MomentsScreen
import io.github.sxd91.suchat.ui.page.secondary.NewFriendsScreen
import io.github.sxd91.suchat.ui.page.secondary.OfficialAccountsScreen
import io.github.sxd91.suchat.ui.page.secondary.ProfileScreen
import io.github.sxd91.suchat.ui.page.secondary.SettingsScreen
import io.github.sxd91.suchat.ui.page.secondary.TagsScreen
import io.github.sxd91.suchat.ui.setup.ServerSetupScreen
import io.github.sxd91.suchat.ui.theme.SuchatAppearance
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Suchat Android 主 Activity。
 *
 * ## 结构
 *
 * ```
 * SuchatRootTheme（莫奈取色 + miuix 主题）
 *   └ SuchatLauncher（服务器连接 → 主界面）
 *        └ WeKitDrawer（负一屏空间抽屉）
 *             ├ panel: WeKitPanelContent（负一屏内容）
 *             └ content: 主 Tab 层
 *                  ├ HorizontalPager（四页）
 *                  └ LiquidGlassTabBar（液态玻璃底栏，可拖拽联动）
 * ```
 *
 * ## 本轮落实的用户反馈
 *
 * | 编号 | 要求 | 实现位置 |
 * |---|---|---|
 * | 1 | 头像用莫奈取色 | `SuchatAvatar`（读 `MiuixTheme.colorScheme`） |
 * | 2 | 全换 miuix 控件 | 全部页面（Icon/Text/IconButton/theme 色） |
 * | 3 | 图标不用 emoji | `SuchatIcons`（miuix 矢量库） |
 * | 4 | SVG 图标 + 莫奈取色 | `res/drawable/ic_suchat_*` + `<monochrome>` |
 * | 5 | 划到哪切到哪 | `onDragFraction` → pager 实时跟随 |
 * | 6 | 图标背景透明 | `SuchatEntryRow`（无色块背板） |
 * | 7 | 胶囊索引 + 滑动 + 气泡 | `ContactsScreen` |
 * | 8 | 头像+名字+状态 / 负一屏 | 本文件 + `WeKitDrawer` |
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearance = remember { SuchatAppearance() }
            SuchatRootTheme(appearance) {
                SuchatLauncher()
            }
        }
    }
}

/** 会话状态：未连接 / 预览 / 已连接。 */
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
        is AppSession.Preview -> SuchatAppShell(statusText = "前端预览 · 未连接")
        is AppSession.Connected -> SuchatAppShell(statusText = "已连接")
    }
}

/**
 * 应用外壳：负一屏抽屉包住整个主 Tab 层。
 *
 * ## 负一屏打开方式（用户第 8 条）
 *
 *  1. **点击消息页左上角头像 / 名字** → `onAvatarClick` → `drawerOpen = true`；
 *  2. **左滑** → `WeKitDrawer` 内部的手势识别（拖拽跟手）；
 *  3. 关闭：右滑 / 点遮罩 / 系统返回键。
 */
@Composable
private fun SuchatAppShell(statusText: String) {
    val nav = rememberSuchatNavigator()
    val scope = rememberCoroutineScope()
    var drawerOpen by remember { mutableStateOf(false) }

    // 系统返回键：优先关抽屉，再弹二级页。
    BackHandler(enabled = drawerOpen || nav.backStack.isNotEmpty()) {
        if (drawerOpen) drawerOpen = false else nav.pop()
    }

    WeKitDrawer(
        drawerOpen = drawerOpen,
        onOpen = { drawerOpen = true },
        onClose = { drawerOpen = false },
        panel = { drawerWidth ->
            WeKitPanelContent(
                userName = io.github.sxd91.suchat.data.SampleData.me.name,
                statusText = statusText,
                drawerWidth = drawerWidth,
                onItemClick = { key ->
                    drawerOpen = false
                    // 负一屏全部入口接线（每个 key 对应真实页面）。
                    when (key) {
                        "profile" -> nav.push(SuchatPage.Profile)
                        "add_friend" -> nav.push(SuchatPage.NewFriends)
                        "pay" -> nav.push(SuchatPage.Services)
                        "moments" -> nav.push(SuchatPage.Moments)
                        "channels" -> nav.push(SuchatPage.Channels)
                        "mark_read" -> { /* 演示：仅关闭面板 */ }
                        "settings" -> nav.push(SuchatPage.Settings)
                        "drift" -> nav.push(SuchatPage.DriftBottle)
                    }
                },
            )
        },
    ) {
        MainTabs(
            nav = nav,
            statusText = statusText,
            onAvatarClick = { drawerOpen = true },
            onSelectTab = { index -> scope.launch { /* 由 pager 处理 */ } },
        )

        // ---------- 二级页层 ----------
        // 覆盖在主 Tab 层之上，从右滑入 / 右滑出；打开负一屏时不可交互
        // （被抽屉内容层盖住，由 WeKitDrawer 的遮罩接管）。
        val topPage = nav.topPage
        AnimatedVisibility(
            visible = topPage != null,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(260)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(260)),
        ) {
            // 退出动画期间 topPage 先变 null，需记住最后一个非空页面。
            val renderedPage = remember { mutableStateOf<SuchatPage?>(null) }
            LaunchedEffect(topPage) {
                if (topPage != null) renderedPage.value = topPage
            }
            val page = renderedPage.value ?: topPage
            if (page != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MiuixTheme.colorScheme.surface),
                ) {
                    SecondaryHost(
                        page = page,
                        nav = nav,
                        bottomInset = 64.dp +
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    )
                }
            }
        }
    }
}

/** 主 Tab 层：pager + 悬浮液态玻璃底栏（兄弟节点）。 */
@Composable
private fun MainTabs(
    nav: SuchatNavigator,
    statusText: String,
    onAvatarClick: () -> Unit,
    onSelectTab: (Int) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    val scope = rememberCoroutineScope()

    val tabs = remember { SuchatTab.entries.map { TabItem(it.label, it.iconKey) } }
    val pagerState = rememberPagerState(
        initialPage = nav.currentTab.ordinal,
        pageCount = { SuchatTab.entries.size },
    )

    // 底栏限位：胶囊 64dp + 12dp + 手势条（只抬高滚动终点，不挡内容折射）。
    val barBottomPadding = 12.dp +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = TAB_BAR_HEIGHT + barBottomPadding

    // pager 停稳 → 写回导航状态。
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val tab = SuchatTab.entries.getOrNull(page)
            if (tab != null && nav.currentTab != tab) nav.switchTab(tab)
        }
    }
    // 导航状态 → pager（点击底栏时的跳转）。
    LaunchedEffect(nav.currentTab) {
        val target = nav.currentTab.ordinal
        if (pagerState.currentPage != target || pagerState.targetPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
        ) { page ->
            when (SuchatTab.entries[page]) {
                SuchatTab.Chats -> ChatsScreen(
                    nav = nav,
                    bottomInset = bottomInset,
                    onAvatarClick = onAvatarClick,
                    statusText = statusText,
                )
                SuchatTab.Contacts -> ContactsScreen(nav = nav, bottomInset = bottomInset)
                SuchatTab.Discover -> DiscoverScreen(nav = nav, bottomInset = bottomInset)
                SuchatTab.Me -> MeScreen(nav = nav, bottomInset = bottomInset)
            }
        }

        LiquidGlassTabBar(
            items = tabs,
            selectedIndex = pagerState.targetPage,
            onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
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
        SuchatPage.Search -> SearchScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.AddMenu -> AddMenuScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.NewFriends -> NewFriendsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.GroupChats -> GroupChatsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.Tags -> TagsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.OfficialAccounts -> OfficialAccountsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.Moments, SuchatPage.MyMoments -> MomentsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.DriftBottle -> DriftBottleScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.Channels -> ChannelsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.Scan -> ScanScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.TopStories -> TopStoriesScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.SearchDiscover -> SearchDiscoverScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.MiniPrograms -> MiniProgramsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.Services -> ServicesScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.Favorites -> FavoritesScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.Cards -> CardsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.Stickers -> StickersScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.Settings -> SettingsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.Profile -> ProfileScreen(nav = nav, bottomInset = bottomInset)
    }
}

/** 悬浮底栏的滚动限位高度（与 `LiquidGlassTabBar` 胶囊一致）。 */
private val TAB_BAR_HEIGHT: Dp = 64.dp