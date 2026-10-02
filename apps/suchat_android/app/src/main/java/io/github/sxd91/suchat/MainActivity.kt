package io.github.sxd91.suchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import io.github.sxd91.suchat.core.design.glass.LiquidGlassTabBarKyant
import io.github.sxd91.suchat.core.design.glass.TabBarMode
import io.github.sxd91.suchat.core.design.glass.TabItem
import io.github.sxd91.suchat.core.design.theme.SuchatRootTheme
import io.github.sxd91.suchat.core.nav.DEPTH_PARALLAX
import io.github.sxd91.suchat.core.nav.DEPTH_SCALE
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.core.nav.SuchatPage
import io.github.sxd91.suchat.core.nav.SuchatTab
import io.github.sxd91.suchat.core.nav.rememberSuchatNavigator
import io.github.sxd91.suchat.ui.component.WeKitDrawer
import io.github.sxd91.suchat.ui.component.WeKitPanelContent
import io.github.sxd91.suchat.ui.component.blockPointerInput
import io.github.sxd91.suchat.ui.navigation.PredictiveBackContent
import io.github.sxd91.suchat.ui.navigation.PredictiveBackEffect
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
import io.github.sxd91.suchat.ui.page.settings.AboutScreen
import io.github.sxd91.suchat.ui.page.settings.AccountSecurityScreen
import io.github.sxd91.suchat.ui.page.settings.AppearanceScreen
import io.github.sxd91.suchat.ui.page.settings.CareModeScreen
import io.github.sxd91.suchat.ui.page.settings.ChatSettingsScreen
import io.github.sxd91.suchat.ui.page.settings.DevicesScreen
import io.github.sxd91.suchat.ui.page.settings.FontSizeScreen
import io.github.sxd91.suchat.ui.page.settings.GeneralScreen
import io.github.sxd91.suchat.ui.page.settings.HelpScreen
import io.github.sxd91.suchat.ui.page.settings.IntKey
import io.github.sxd91.suchat.ui.page.settings.LocalSuchatSettings
import io.github.sxd91.suchat.ui.page.settings.NotificationsScreen
import io.github.sxd91.suchat.ui.page.settings.PrivacyScreen
import io.github.sxd91.suchat.ui.page.settings.StorageScreen
import io.github.sxd91.suchat.ui.page.settings.TeenModeScreen
import io.github.sxd91.suchat.ui.page.settings.rememberSuchatSettings
import io.github.sxd91.suchat.ui.setup.ServerSetupScreen
import io.github.sxd91.suchat.ui.theme.SuchatAppearance
import kotlinx.coroutines.launch
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
 *                  └ LiquidGlassTabBarKyant（液态玻璃底栏，Kyant backdrop 库）
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
            SuchatLauncher()
        }
    }
}

/**
 * 把持久化设置同步进运行时 [SuchatAppearance]。
 *
 * ## 为什么需要（否则「重启后设置失效」）
 *
 * [SuchatAppearance] 是**运行时**状态（可观察，驱动即时生效），
 * 而设置值落在 SharedPreferences（[io.github.sxd91.suchat.ui.page.settings.SuchatSettings]）。
 * 两者必须**启动时对齐一次** —— 否则用户上次选了「自定义颜色/深色/底栏 120%」，
 * 重启后 [SuchatAppearance] 又回到构造默认值，界面与设置页显示不一致。
 *
 * 只在进入主界面前同步一次；之后由设置页的写入同时更新两处。
 */
@Composable
private fun SyncAppearanceFromSettings(
    appearance: SuchatAppearance,
    settings: io.github.sxd91.suchat.ui.page.settings.SuchatSettings?,
) {
    LaunchedEffect(settings) {
        val s = settings ?: return@LaunchedEffect
        appearance.colorSource = s.choice(io.github.sxd91.suchat.ui.page.settings.ChoiceKey.ColorSource)
        appearance.paletteStyleName = s.choice(io.github.sxd91.suchat.ui.page.settings.ChoiceKey.PaletteStyle)
        appearance.seedColor = s.int(io.github.sxd91.suchat.ui.page.settings.IntKey.SeedColor)
        appearance.glassMode = s.choice(io.github.sxd91.suchat.ui.page.settings.ChoiceKey.GlassMode)
        appearance.performance = s.choice(io.github.sxd91.suchat.ui.page.settings.ChoiceKey.Performance)
        appearance.transition = s.choice(io.github.sxd91.suchat.ui.page.settings.ChoiceKey.Transition)
        appearance.reduceMotion = s.bool(io.github.sxd91.suchat.ui.page.settings.BoolKey.ReduceMotion)
        appearance.themeMode = when (s.choice(io.github.sxd91.suchat.ui.page.settings.ChoiceKey.ThemeMode)) {
            "Light" -> io.github.sxd91.suchat.ui.theme.SuchatThemeMode.Light
            "Dark" -> io.github.sxd91.suchat.ui.theme.SuchatThemeMode.Dark
            else -> io.github.sxd91.suchat.ui.theme.SuchatThemeMode.System
        }
    }
}

/** 会话状态：未连接 / 预览 / 已连接。 */
private sealed interface AppSession {
    data object Preview : AppSession
    data class Connected(val endpoint: String) : AppSession
}

/**
 * 应用根 —— **外观实例与设置实例在这里创建，全树共享**。
 *
 * ## 为什么要提到这一层
 *
 * 设置页（三级）要改主题、玻璃档、性能档，而这三个值必须**立刻影响整个应用**。
 * 若把 `SuchatAppearance()` 放在更深处（或每处各造一个），改完只有局部生效。
 *
 * 这里的结构保证：
 *  - [SuchatAppearance] 只有一个实例，同时传给主题层与设置页；
 *  - [SuchatSettings] 只在根层建一次（`rememberSuchatSettings`），
 *    通过 `LocalSuchatSettings` 下发给所有页面（含设置子页）。
 *
 * 注意：`SuchatRootTheme` 必须在 [CompositionLocalProvider] **内部**读
 * appearance —— 它自身读的是同一实例的属性，写值触发重组，主题随之重算。
 */
@Composable
private fun SuchatLauncher() {
    var session by remember { mutableStateOf<AppSession?>(null) }
    val appearance = remember { SuchatAppearance() }
    val settings = rememberSuchatSettings()

    // 启动时把持久化设置回填进运行时实例（否则重启后设置「失效」）。
    SyncAppearanceFromSettings(appearance, settings)

    SuchatRootTheme(appearance) {
        CompositionLocalProvider(LocalSuchatSettings provides settings) {
            when (session) {
                null -> ServerSetupScreen(
                    onPreview = { session = AppSession.Preview },
                    onConnected = { session = AppSession.Connected(it) },
                )
                is AppSession.Preview -> SuchatAppShell(
                    statusText = "前端预览 · 未连接",
                    appearance = appearance,
                )
                is AppSession.Connected -> SuchatAppShell(
                    statusText = "已连接",
                    appearance = appearance,
                )
            }
        }
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
/**
 * 应用外壳：负一屏抽屉包住整个主 Tab 层 + 页面层。
 *
 * ## 层序（自下而上）
 *
 * ```
 * WeKitDrawer
 *  ├ content: 主 Tab 层（HorizontalPager + 液态玻璃底栏）
 *  ├ 页面层（二级/三级，可堆叠，见 [PageLayerHost]）   ← 盖住底栏
 *  └ 抽屉面板（负一屏）                                ← 盖住一切
 * ```
 *
 * ## 返回键的归属（优先级从高到低）
 *
 *  1. **负一屏打开** → 关抽屉（`BackHandler`，占位不可预测）；
 *  2. **页面层非空** → 交 [PredictiveBackEffect]（预测性返回：可跟手预览）；
 *  3. **都没有** → 不注册任何回调，交还系统（系统播「退出应用」动画）。
 */
@Composable
private fun SuchatAppShell(statusText: String, appearance: SuchatAppearance) {
    val nav = rememberSuchatNavigator()
    var drawerOpen by remember { mutableStateOf(false) }

    // 抽屉开着时，返回键先关抽屉。
    BackHandler(enabled = drawerOpen) { drawerOpen = false }

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
            appearance = appearance,
            onAvatarClick = { drawerOpen = true },
        )

        // ---------- 页面层（二级 / 三级） ----------
        PageLayerHost(
            nav = nav,
            appearance = appearance,
            drawerOpen = drawerOpen,
            bottomInset = 64.dp +
                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        )
    }
}

/**
 * 页面层宿主 —— 支持**多级堆叠**与**真·转场**。
 *
 * ## 为什么不能再用一个 `AnimatedVisibility`（旧实现的病根）
 *
 * 旧写法：
 * ```kotlin
 * AnimatedVisibility(visible = topPage != null) { SecondaryHost(topPage) }
 * ```
 * `visible` 只在「有没有二级页」这个 0/1 边界翻转。二级 → 三级时
 * `topPage` 只是**换了个对象**，`visible` 恒为 true ⇒ **转场根本不触发**，
 * 页面瞬间切换 —— 这正是用户说的「二级页面进入三级没有动画」。
 *
 * ## 现在的模型：槽位（slot）栈 + 每槽一个进入进度
 *
 * 每个页面是一个 [PageSlot]，持有 `enter`：
 *
 *  - `enter = 0`：停在屏幕右侧之外；
 *  - `enter = 1`：完全就位。
 *
 * 于是转场变成**一个浮点值的动画**，而不是布局的显隐：
 *
 * | 场景 | 做法 |
 * |---|---|
 * | push | 新槽 `enter` 从 0 动画到 1（右滑入） |
 * | pop | 栈顶槽 `enter` 从 1 动画到 0，**动画播完才从槽位表移除** |
 * | 同层替换 | 旧槽动画退出 + 新槽动画入场 |
 *
 * ## 底层页的「退让」是**派生**的，不是第二套动画
 *
 * 底层页的位移/缩放直接取「上层槽的 `enter` 值」：
 * ```
 * 底层 translationX = -屏宽 × 0.28 × 上层Enter
 * ```
 * 上层滑进来多少，底层就退后多少；上层退出去，底层同步回位。
 *
 * 这样做的两个好处：
 *  1. **没有双写入者** —— 一个值驱动两层，
 *     不可能出现两层节奏对不上（上一轮 tab 栏踩过的坑）；
 *  2. **预测性返回零成本接入** —— 手指拖拽时直接写栈顶槽的值，
 *     底层会自动跟着回位，不需要为手势再写一套逻辑。
 *
 * ## 为什么不用 `Animatable`（重要）
 *
 * 试过 `Animatable` 后发现两个硬伤：
 *  1. `snapTo` / `animateTo` 都是 **suspend**，而预测性返回的
 *     `onProgress` 回调是同步的 —— 每帧 `launch` 一个协程会重演
 *     「拖拽卡顿」的老问题；
 *  2. `Animatable` 内部有 `MutatorMutex`，跟手写值与入场动画会互斥打架。
 *
 * 改用 **`mutableFloatStateOf` + 手写帧同步 tween**：
 *  - 跟手 = 一次普通赋值（零协程、零锁）；
 *  - 入场/退场 = 一个 `Job`，每帧 `withFrameNanos` 推进；
 *  - 互斥 = 跟手时 `animJob?.cancel()`，由调用方显式管理。
 *
 * ## 性能纪律（复用上一轮的教训）
 *
 * `enter` 只在 `graphicsLayer { }` 的**绘制期 lambda** 里读 ——
 * 每帧只失效图层，不触发重组。若在组合期读，会变成每帧重组整棵页面树。
 */
@Composable
private fun PageLayerHost(
    nav: SuchatNavigator,
    appearance: SuchatAppearance,
    drawerOpen: Boolean,
    bottomInset: Dp,
) {
    val slots = remember { mutableStateListOf<PageSlot>() }
    val scope = rememberCoroutineScope()

    // 导航栈 → 槽位表（推送即进入、弹出保留到退场动画播完）。
    LaunchedEffect(Unit) {
        snapshotFlow { nav.backStack.toList() }.collect { stack ->
            // 1) 新增页：以「屏外」为起点入场。
            while (slots.size < stack.size) {
                slots.add(PageSlot(stack[slots.size]).also { it.enter = 0f })
            }

            // 2) 同层替换（长度不变但栈顶换页）：旧槽退场、新槽入场。
            stack.forEachIndexed { index, page ->
                if (index < slots.size && slots[index].page !== page) {
                    val old = slots[index]
                    old.exiting = true
                    val fresh = PageSlot(page).also { it.enter = 0f }
                    slots[index] = fresh
                    scope.launch { old.slideTo(0f, EXIT_MS) }
                    scope.launch { fresh.slideTo(1f, ENTER_MS) }
                }
            }

            // 3) 多余槽位退场（**动画播完才移除**，否则看不到退出动画）。
            val doomed = slots.drop(stack.size).toList()
            if (doomed.isNotEmpty()) {
                doomed.forEach { it.exiting = true }
                // 并发播放退场动画，**等全部播完再移除槽位**（否则看不到动画）。
                coroutineScope {
                    doomed.forEach { slot -> launch { slot.slideTo(0f, EXIT_MS) } }
                }
                slots.removeAll(doomed)
            }

            // 4) 在场槽位就位。
            slots.forEach { slot ->
                if (!slot.exiting && slot.enter < 1f) {
                    scope.launch { slot.slideTo(1f, ENTER_MS) }
                }
            }
        }
    }

    val topSlot = slots.lastOrNull()

    // 预测性返回：直接驱动栈顶槽的进入进度（同步赋值，零协程）。
    PredictiveBackEffect(
        enabled = !drawerOpen && nav.backStack.isNotEmpty(),
        onProgress = { progress ->
            topSlot?.let { slot ->
                // 手指接管 → 先掐掉在途的入场动画（否则两个写入者互相拉）。
                slot.cancelSlide()
                slot.enter = (1f - progress).coerceIn(0f, 1f)
            }
        },
        onCommit = { nav.pop() },
        onCancel = {
            // 上滑撤回：栈没变，只需把进入进度弹回 1。
            topSlot?.let { slot ->
                if (!slot.exiting) scope.launch { slot.slideTo(1f, CANCEL_MS) }
            }
        },
    )

    if (slots.isEmpty()) return

    Box(Modifier.fillMaxSize()) {
        slots.forEachIndexed { index, slot ->
            key(slot) {
                // 上层槽（决定本层的退让量）；结构变化才会变，不进每帧热路径。
                val above = slots.getOrNull(index + 1)
                // ★ 2026-10-02 修「下面会穿透点击下层」：
                //
                // 页面层视觉上盖住了主 Tab 层，但**没有消费指针事件** ——
                // Compose 的命中测试只认带 pointerInput 的节点，页面里的空白处
                // （面板四周、列表尾部）点击会穿透到底下的会话列表。
                //
                // 这里给页面层挂上 blockPointerInput：在 `Main` 阶段兜底消费
                // 子节点**没处理**的事件（见该扩展的文档），既能挡住下层，
                // 又不影响页面内任何控件的正常响应。
                Box(
                    Modifier
                        .fillMaxSize()
                        .blockPointerInput()
                        .graphicsLayer {
                            val enter = slot.enter
                            val push = above?.enter ?: 0f
                            // ★ 2026-10-02：转场风格（设置 → 外观 → 页面转场）在此生效。
                            //
                            // 三种风格共用同一个 `enter` 进度，只在**如何映射到位移**上不同：
                            //  · Miuix：横向滑动 + 底层退让（默认，最具层级感）
                            //  · AOSP：Material 的纵向淡入 + 轻微上移（FadeThrough 观感）
                            //  · Fade：纯透明度，无任何位移
                            //
                            // `reduceMotion` 优先级最高（契约：减少动态 → 短淡化）。
                            val style = if (appearance.reduceMotion) "Fade" else appearance.transition
                            when (style) {
                                "Fade" -> {
                                    translationX = 0f
                                    translationY = 0f
                                    alpha = enter
                                }
                                "AOSP" -> {
                                    // Material 的「淡入上移」：位移量小（屏高 6%），
                                    // 主要靠透明度建立层次。
                                    translationX = 0f
                                    translationY = size.height * 0.06f * (1f - enter)
                                    alpha = enter
                                }
                                else -> {
                                    // Miuix：横向滑入 + 上层推挤底层。
                                    translationX = size.width * (1f - enter) -
                                        size.width * DEPTH_PARALLAX * push
                                    val scale = 1f - (1f - DEPTH_SCALE) * push
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 1f
                                }
                            }
                        },
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MiuixTheme.colorScheme.surface),
                    ) {
                        SecondaryHost(
                            page = slot.page,
                            nav = nav,
                            appearance = appearance,
                            bottomInset = bottomInset,
                        )
                    }
                }
            }
        }
    }
}

/** 进入动画时长（新页从右侧滑入就位）。 */
private const val ENTER_MS = 300

/** 退出动画时长（旧页滑出右侧；与预测性返回的提交时长保持一致）。 */
private const val EXIT_MS = 260

/** 取消动画时长（上滑撤回时弹回原位，略慢一点更有手感）。 */
private const val CANCEL_MS = 200

/**
 * 页面槽位 —— 一个正在渲染（或正在退场）的二级/三级页。
 *
 * @property page 对应页面。
 * @property exiting 是否已请求退场（退场动画结束前该槽仍留在槽位表中）。
 */
private class PageSlot(val page: SuchatPage) {

    /** 进入进度：0 = 屏外右侧，1 = 完全就位。绘制期读取（见 `graphicsLayer`）。 */
    var enter by mutableFloatStateOf(1f)

    var exiting by mutableStateOf(false)

    /** 在途的滑动动画；跟手时会被取消（避免两个写入者抢同一个值）。 */
    private var slideJob: Job? = null

    /** 掐掉在途动画（**非挂起**，可直接在返回手势回调里调）。 */
    fun cancelSlide() {
        slideJob?.cancel()
        slideJob = null
    }

    /**
     * 帧同步 tween 滑到 [target]。
     *
     * ## 两个刻意的设计
     *
     *  - **`withFrameNanos` 而不是 `delay(帧长)`** —— 后者与屏幕刷新率
     *    （60Hz = 16.7ms / 120Hz = 8.3ms）错拍，采样点漂移会让动画看着"抖"
     *    （上一轮跟随动画踩过的同款坑）；
     *  - **起点用 `withFrameNanos` 拿第一个帧时间戳**，而不是
     *    `System.currentTimeMillis()` —— 保证动画时间轴与 VSync 同源。
     */
    suspend fun slideTo(target: Float, durationMs: Int) {
        cancelSlide()
        val start = enter
        if (start == target) return
        // 登记当前协程的 Job，供 [cancelSlide] 取消（跟手接管时用）。
        val job = currentCoroutineContext()[Job]
        slideJob = job
        // 时间轴与 VSync 同源：起点也用帧时间戳，不用 currentTimeMillis。
        val startNanos = withFrameNanos { it }
        while (true) {
            val nowNanos = withFrameNanos { it }
            val t = ((nowNanos - startNanos) / 1_000_000f / durationMs).coerceIn(0f, 1f)
            enter = start + (target - start) * t
            if (t >= 1f) break
        }
        if (slideJob === job) slideJob = null
    }
}

/** 主 Tab 层：pager + 悬浮液态玻璃底栏（兄弟节点）。 */
@Composable
private fun MainTabs(
    nav: SuchatNavigator,
    statusText: String,
    appearance: SuchatAppearance,
    onAvatarClick: () -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    val scope = rememberCoroutineScope()
    // 设置实例（读「底栏大小」等即时生效项）。根层已 provide，这里取同一个。
    val settings = LocalSuchatSettings.current

    val tabs = remember { SuchatTab.entries.map { TabItem(it.label, it.iconKey) } }
    val pagerState = rememberPagerState(
        initialPage = nav.currentTab.ordinal,
        pageCount = { SuchatTab.entries.size },
    )

    // ★ 玻璃渲染档位（设置 → 外观 → 玻璃渲染）**实时生效**：
    //   读的是可观察的 appearance.glassMode，改设置立刻重组这里。
    val tabBarMode = when (appearance.glassMode) {
        "Blur" -> TabBarMode.Blur
        "None" -> TabBarMode.None
        else -> TabBarMode.LiquidGlass
    }

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

        // ★ 底栏尺寸（设置 → 外观 → 底栏大小，对齐 WeKit 的 nav_bar_scale）。
        //
        // WeKit 的做法（ReplaceNavigationBar.kt:648-651）：**覆盖 LocalDensity**
        // 而不是用 graphicsLayer 缩放 —— 底栏内每个 dp/sp（高度/图标/胶囊/模糊半径/
        // 阴影）都按新尺寸重新布局，玻璃纹理保持清晰、触摸区与所见一致。
        // 直接 scale 已渲染的位图会把玻璃糊掉。
        //
        // 只需包住底栏本体（不需要连内容层一起缩放）。
        val baseDensity = LocalDensity.current
        val barScale = (settings?.int(IntKey.TabBarScale) ?: 100) / 100f
        val scaledDensity = remember(baseDensity, barScale) {
            Density(baseDensity.density * barScale, baseDensity.fontScale)
        }

        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            LiquidGlassTabBarKyant(
                items = tabs,
                selectedIndex = pagerState.targetPage,
                onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                backdrop = backdrop,
                // 外观档位（设置页可改，改完即时生效）。
                mode = tabBarMode,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = barBottomPadding),
            )
        }
    }
}

/** 二级页路由分发。 */
@Composable
private fun SecondaryHost(
    page: SuchatPage,
    nav: SuchatNavigator,
    appearance: SuchatAppearance,
    bottomInset: Dp,
) {
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

        // --- 设置子页（三级，depth = 2） ---
        SuchatPage.SetAccountSecurity -> AccountSecurityScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.SetNotifications -> NotificationsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.SetChat -> ChatSettingsScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.SetPrivacy -> PrivacyScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.SetGeneral -> GeneralScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.SetTeenMode -> TeenModeScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.SetCareMode -> CareModeScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.SetAppearance -> AppearanceScreen(
            nav = nav, appearance = appearance, bottomInset = bottomInset,
        )
        SuchatPage.SetStorage -> StorageScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.SetFontSize -> FontSizeScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.SetAbout -> AboutScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.SetHelp -> HelpScreen(nav = nav, bottomInset = bottomInset)
        SuchatPage.SetDevices -> DevicesScreen(nav = nav, bottomInset = bottomInset)
    }
}

/** 悬浮底栏的滚动限位高度（与 `LiquidGlassTabBar` 胶囊一致）。 */
private val TAB_BAR_HEIGHT: Dp = 64.dp