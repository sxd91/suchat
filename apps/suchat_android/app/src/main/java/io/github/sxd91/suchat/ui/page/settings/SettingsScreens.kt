package io.github.sxd91.suchat.ui.page.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import io.github.sxd91.suchat.BuildConfig
import io.github.sxd91.suchat.core.design.icon.SuchatIcons
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.core.nav.SuchatPage
import io.github.sxd91.suchat.data.SampleData
import io.github.sxd91.suchat.ui.component.LocalTopBarInset
import io.github.sxd91.suchat.ui.component.SuchatEntryRow
import io.github.sxd91.suchat.ui.component.SuchatScaffold
import io.github.sxd91.suchat.ui.theme.SuchatAppearance
import io.github.sxd91.suchat.ui.theme.SuchatThemeMode
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置子页集合（全部为**三级页**，`depth = 2`）。
 *
 * ## 为什么要单独一个文件
 *
 * 原实现里 `SettingsScreen` 的 7 个入口 `onClick = {}` 全是空壳 ——
 * 点进去什么也没有。这个文件把每个入口都做成**有真实交互**的页面：
 *
 * | 入口 | 页面 | 真实行为 |
 * |---|---|---|
 * | 账号与安全 | [AccountSecurityScreen] | 显示账号信息、安全开关、登录设备管理入口 |
 * | 青少年模式 | [TeenModeScreen] | 开启/关闭 + 4 条受限项说明 |
 * | 关怀模式 | [CareModeScreen] | 大字体开关 + 字号档位 |
 * | 新消息通知 | [NotificationsScreen] | 4 个通知开关 |
 * | 聊天 | [ChatSettingsScreen] | 5 个聊天开关（含清缓存入口） |
 * | 隐私 | [PrivacyScreen] | 5 个隐私开关 |
 * | 通用 | [GeneralScreen] | 外观/存储/字体/辅助功能入口 + 4 个开关 |
 * | （通用 →）外观 | [AppearanceScreen] | 主题/玻璃档/性能档/转场，**改完即时生效** |
 * | （通用 →）存储空间 | [StorageScreen] | 分类占用条 + 清理按钮 |
 * | （通用 →）字体大小 | [FontSizeScreen] | 4 档字体预览 + 选择 |
 * | （通用 →）关于 | [AboutScreen] | 版本/构建信息/条款入口 |
 * | （关于 →）帮助与反馈 | [HelpScreen] | 常见问题 + 反馈入口 |
 *
 * ## 与 `SettingsPrefs` 的关系
 *
 * 所有开关都读写 [SuchatSettings]（SharedPreferences 落盘 + `mutableStateOf`
 * 可观察）。所以：
 *  1. 退出设置再进来，开关状态还在（持久化）；
 *  2. 改外观档位时，[SuchatAppearance] 同一实例被改写 →
 *     主题层立刻重算 → **不用重进页面就看到变化**。
 */

// ============================================================================
// 通用：开关行
// ============================================================================

/**
 * 带 miuix 开关的行。
 *
 * 用 miuix 的 `Switch` 而不是 Material3 的 —— 前者有 miuix 特有的
 * 拖拽回弹与触感反馈（见 miuix-ui 的 `Switch.kt`），与全应用组件同源。
 *
 * 注意：**整行不设 clickable**，只有开关本体响应点击。
 * 这是设置页的通用规范（微信亦然）—— 避免误触大面积开关。
 */
@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = MiuixTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp)
                .height(if (summary == null) 56.dp else 72.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                MiuixText(
                    text = title,
                    fontSize = 16.sp,
                    color = if (enabled) c.onSurface else c.onSurfaceVariantSummary,
                )
                if (summary != null) {
                    Spacer(Modifier.height(3.dp))
                    MiuixText(
                        text = summary,
                        fontSize = 12.sp,
                        color = c.onSurfaceVariantSummary,
                    )
                }
            }
            MiuixSwitch(
                checked = checked,
                onCheckedChange = { if (enabled) onCheckedChange(it) },
                enabled = enabled,
            )
        }
        if (showDivider) {
            Box(
                Modifier
                    .padding(start = 20.dp)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(c.outline.copy(alpha = 0.35f)),
            )
        }
    }
}

/**
 * 单选行（右侧显示当前选中值）。
 *
 * 用于「主题模式」「玻璃渲染」「性能档位」这类多选一设置 ——
 * 点击整行弹出选择面板。
 */
@Composable
private fun ChoiceRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    showDivider: Boolean = true,
) {
    val c = MiuixTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp)
                .height(if (summary == null) 56.dp else 68.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                MiuixText(text = title, fontSize = 16.sp, color = c.onSurface)
                if (summary != null) {
                    Spacer(Modifier.height(3.dp))
                    MiuixText(text = summary, fontSize = 12.sp, color = c.onSurfaceVariantSummary)
                }
            }
            MiuixText(
                text = value,
                fontSize = 14.sp,
                color = c.onSurfaceSecondary,
                modifier = Modifier.padding(end = 6.dp),
            )
            MiuixIcon(
                imageVector = SuchatIcons.ChevronForward,
                contentDescription = null,
                tint = c.onSurfaceSecondary.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
        if (showDivider) {
            Box(
                Modifier
                    .padding(start = 20.dp)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(c.outline.copy(alpha = 0.35f)),
            )
        }
    }
}

/** 分组灰缝（微信的 8dp）。 */
@Composable
private fun GroupGap(height: Dp = 8.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(MiuixTheme.colorScheme.surfaceContainer),
    )
}

/**
 * 整数滑块行（用于「底栏大小」这类百分比设置）。
 *
 * ## 为什么用 miuix 的 `Slider`
 *
 * 全应用组件必须同源（用户要求）。miuix 的 `Slider` 有它特有的
 * 拖拽吸附与触感反馈，跟 Material3 的不是一套观感。
 *
 * ## 拖动即时生效
 *
 * `onValueChange` 里直接写 [SuchatSettings.setInt] —— 该值由
 * `mutableStateMapOf` 持有，写入即触发读它的 `MainTabs` 重组，
 * 底栏尺寸当场变化，不用松手也不用重进页面。
 */
@Composable
private fun IntSliderRow(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
) {
    val c = MiuixTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiuixText(
                text = title,
                fontSize = 16.sp,
                color = c.onSurface,
                modifier = Modifier.weight(1f),
            )
            MiuixText(
                text = "$value%",
                fontSize = 14.sp,
                color = c.onSurfaceSecondary,
            )
        }
        MiuixSlider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = (max - min) / 5 - 1, // 每 5% 一档
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(10.dp))
    }
}

/** 段落小标题（组内说明文字）。 */
@Composable
private fun SectionHint(text: String) {
    MiuixText(
        text = text,
        fontSize = 12.sp,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
    )
}

/** 危险操作按钮（红字、居中，如「退出登录」「清空聊天记录」）。 */
@Composable
private fun DangerButton(
    text: String,
    onClick: () -> Unit,
) {
    val c = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        MiuixText(text = text, fontSize = 16.sp, color = c.error, fontWeight = FontWeight.Medium)
    }
}

// ============================================================================
// 账号与安全
// ============================================================================

@Composable
fun AccountSecurityScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    val settings = LocalSuchatSettings.current ?: return
    val me = SampleData.me

    SuchatScaffold(title = "账号与安全", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            // 账号卡片（真实展示当前会话里的账号信息）。
            item(key = "account") {
                GroupGap()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        MiuixText("Suchat 号", fontSize = 13.sp, color = c.onSurfaceSecondary)
                        Spacer(Modifier.height(4.dp))
                        MiuixText(
                            me.suchatId,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = c.onSurface,
                        )
                    }
                    MiuixText(
                        "复制",
                        fontSize = 14.sp,
                        color = c.primary,
                        modifier = Modifier.clickable { },
                    )
                }
            }

            item(key = "security_entries") {
                Spacer(Modifier.height(8.dp))
                Column(Modifier.background(c.surface)) {
                    SuchatEntryRow(
                        title = "登录设备管理",
                        icon = SuchatIcons.Lock,
                        trailingText = "2 台",
                        onClick = { nav.push(SuchatPage.SetDevices) },
                    )
                    SuchatEntryRow(
                        title = "修改密码",
                        icon = SuchatIcons.Lock,
                        onClick = { },
                    )
                    SuchatEntryRow(
                        title = "声音锁",
                        icon = SuchatIcons.Mic,
                        trailingText = "未设置",
                        onClick = { },
                        showDivider = false,
                    )
                }
            }

            item(key = "switches") {
                Spacer(Modifier.height(8.dp))
                Column(Modifier.background(c.surface)) {
                    SwitchRow(
                        title = "登录保护",
                        summary = "在新设备登录时需要短信验证",
                        checked = settings.bool(BoolKey.SecurityLoginProtect),
                        onCheckedChange = { settings.setBool(BoolKey.SecurityLoginProtect, it) },
                    )
                    SwitchRow(
                        title = "安全中心提醒",
                        summary = "账号存在风险时第一时间通知",
                        checked = settings.bool(BoolKey.SecuritySecurityCenter),
                        onCheckedChange = { settings.setBool(BoolKey.SecuritySecurityCenter, it) },
                        showDivider = false,
                    )
                }
            }
            item(key = "tail") { GroupGap(24.dp) }
        }
    }
}

/** 登录设备管理（三级页里的三级页，depth 仍为 2，走同层转场）。 */
@Composable
fun DevicesScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    SuchatScaffold(title = "登录设备管理", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "hint") {
                GroupGap()
                SectionHint("以下设备已登录此账号。删除后，该设备上的 Suchat 将被强制退出。")
            }
            item(key = "list") {
                Column(Modifier.background(c.surface)) {
                    listOf(
                        Triple("本机", "Android · 当前在线", true),
                        Triple("iPhone 15 Pro", "三天前", false),
                    ).forEachIndexed { index, (name, whenText, current) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(64.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                MiuixText(name, fontSize = 16.sp, color = c.onSurface)
                                Spacer(Modifier.height(3.dp))
                                MiuixText(whenText, fontSize = 12.sp, color = c.onSurfaceVariantSummary)
                            }
                            if (!current) {
                                MiuixText(
                                    "删除",
                                    fontSize = 14.sp,
                                    color = c.error,
                                    modifier = Modifier.clickable { },
                                )
                            }
                        }
                        if (index != 1) {
                            Box(
                                Modifier
                                    .padding(start = 20.dp)
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(c.outline.copy(alpha = 0.35f)),
                            )
                        }
                    }
                }
            }
            item(key = "tail") { GroupGap(24.dp) }
        }
    }
}

// ============================================================================
// 青少年模式 / 关怀模式
// ============================================================================

@Composable
fun TeenModeScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    val settings = LocalSuchatSettings.current ?: return
    val enabled = settings.bool(BoolKey.TeenModeEnabled)

    SuchatScaffold(title = "青少年模式", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "hero") {
                GroupGap()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    MiuixText(
                        text = if (enabled) "青少年模式已开启" else "青少年模式未开启",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    MiuixText(
                        text = "开启后，以下功能将受到限制，且不可自行关闭。",
                        fontSize = 13.sp,
                        color = c.onSurfaceSecondary,
                    )
                    Spacer(Modifier.height(14.dp))
                    MiuixSwitch(
                        checked = enabled,
                        onCheckedChange = {
                            settings.setBool(BoolKey.TeenModeEnabled, it)
                            // 青少年模式下强制开启「关怀模式的大字」的相反项 —— 不联动，
                            // 只提示是否进入受限态；真实产品此处会走监护人验证。
                        },
                    )
                }
            }
            item(key = "limits") {
                GroupGap()
                Column(Modifier.background(c.surface)) {
                    listOf(
                        "视频号" to "仅可浏览青少年精选内容",
                        "直播" to "不可开启与观看",
                        "小程序" to "不可使用游戏类小程序",
                        "搜一搜" to "不可搜索未收录内容",
                    ).forEachIndexed { index, (title, summary) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(64.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(if (enabled) c.primary else c.onSurfaceVariantSummary),
                            )
                            Column(Modifier.padding(start = 14.dp)) {
                                MiuixText(title, fontSize = 16.sp, color = c.onSurface)
                                Spacer(Modifier.height(3.dp))
                                MiuixText(summary, fontSize = 12.sp, color = c.onSurfaceVariantSummary)
                            }
                        }
                        if (index != 3) {
                            Box(
                                Modifier
                                    .padding(start = 43.dp)
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(c.outline.copy(alpha = 0.35f)),
                            )
                        }
                    }
                }
            }
            item(key = "tail") { GroupGap(24.dp) }
        }
    }
}

@Composable
fun CareModeScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    val settings = LocalSuchatSettings.current ?: return
    val enabled = settings.bool(BoolKey.CareModeEnabled)

    SuchatScaffold(title = "关怀模式", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "intro") {
                GroupGap()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    MiuixText(
                        "文字更大、色彩更强、按钮更大",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    MiuixText(
                        "开启关怀模式后，全应用文字与控件会放大，方便长辈使用。",
                        fontSize = 13.sp,
                        color = c.onSurfaceSecondary,
                    )
                }
            }
            item(key = "switches") {
                GroupGap()
                Column(Modifier.background(c.surface)) {
                    SwitchRow(
                        title = "开启关怀模式",
                        checked = enabled,
                        onCheckedChange = { settings.setBool(BoolKey.CareModeEnabled, it) },
                    )
                    SwitchRow(
                        title = "同时放大文字",
                        summary = "在关怀模式基础上进一步加大字号",
                        checked = settings.bool(BoolKey.CareModeLargeText),
                        enabled = enabled,
                        onCheckedChange = { settings.setBool(BoolKey.CareModeLargeText, it) },
                        showDivider = false,
                    )
                }
            }
            item(key = "jump") {
                Spacer(Modifier.height(8.dp))
                Column(Modifier.background(c.surface)) {
                    SuchatEntryRow(
                        title = "调整字体大小",
                        icon = SuchatIcons.Settings,
                        trailingText = settings.choice(ChoiceKey.FontScale),
                        onClick = { nav.push(SuchatPage.SetFontSize) },
                        showDivider = false,
                    )
                }
            }
            item(key = "tail") { GroupGap(24.dp) }
        }
    }
}

// ============================================================================
// 新消息通知
// ============================================================================

@Composable
fun NotificationsScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    val settings = LocalSuchatSettings.current ?: return
    val master = settings.bool(BoolKey.NotifyMessage)

    SuchatScaffold(title = "新消息通知", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "master") {
                GroupGap()
                Column(Modifier.background(c.surface)) {
                    SwitchRow(
                        title = "接收新消息通知",
                        summary = "关闭后，将不再收到任何消息提醒",
                        checked = master,
                        onCheckedChange = { settings.setBool(BoolKey.NotifyMessage, it) },
                        showDivider = false,
                    )
                }
            }
            item(key = "sub") {
                GroupGap()
                Column(Modifier.background(c.surface)) {
                    SwitchRow(
                        title = "声音",
                        checked = settings.bool(BoolKey.NotifySound),
                        enabled = master,
                        onCheckedChange = { settings.setBool(BoolKey.NotifySound, it) },
                    )
                    SwitchRow(
                        title = "振动",
                        checked = settings.bool(BoolKey.NotifyVibrate),
                        enabled = master,
                        onCheckedChange = { settings.setBool(BoolKey.NotifyVibrate, it) },
                    )
                    SwitchRow(
                        title = "通知显示消息详情",
                        summary = "关闭后，锁屏通知只显示「你收到一条新消息」",
                        checked = settings.bool(BoolKey.NotifyDetail),
                        enabled = master,
                        onCheckedChange = { settings.setBool(BoolKey.NotifyDetail, it) },
                        showDivider = false,
                    )
                }
            }
            item(key = "more") {
                Spacer(Modifier.height(8.dp))
                Column(Modifier.background(c.surface)) {
                    SuchatEntryRow(
                        title = "聊天系统通知",
                        icon = SuchatIcons.Messages,
                        trailingText = "全部",
                        onClick = { },
                    )
                    SuchatEntryRow(
                        title = "免打扰时段",
                        icon = SuchatIcons.Timer,
                        trailingText = "未设置",
                        onClick = { },
                        showDivider = false,
                    )
                }
            }
            item(key = "tail") { GroupGap(24.dp) }
        }
    }
}

// ============================================================================
// 聊天
// ============================================================================

@Composable
fun ChatSettingsScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    val settings = LocalSuchatSettings.current ?: return

    SuchatScaffold(title = "聊天", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "entries") {
                GroupGap()
                Column(Modifier.background(c.surface)) {
                    SuchatEntryRow(
                        title = "聊天记录迁移与备份",
                        icon = SuchatIcons.Link,
                        onClick = { },
                    )
                    SuchatEntryRow(
                        title = "聊天记录保留时长",
                        icon = SuchatIcons.Timer,
                        trailingText = settings.choice(ChoiceKey.ChatHistoryKeep),
                        onClick = { },
                    )
                    SuchatEntryRow(
                        title = "表情",
                        icon = SuchatIcons.Favorites,
                        onClick = { nav.push(SuchatPage.Stickers) },
                        showDivider = false,
                    )
                }
            }
            item(key = "switches") {
                GroupGap()
                Column(Modifier.background(c.surface)) {
                    SwitchRow(
                        title = "发送消息显示输入状态",
                        summary = "对方会看到「正在输入…」",
                        checked = settings.bool(BoolKey.ChatTypingIndicator),
                        onCheckedChange = { settings.setBool(BoolKey.ChatTypingIndicator, it) },
                    )
                    SwitchRow(
                        title = "回车键发送消息",
                        checked = settings.bool(BoolKey.ChatSendByEnter),
                        onCheckedChange = { settings.setBool(BoolKey.ChatSendByEnter, it) },
                    )
                    SwitchRow(
                        title = "聊天记录搜索",
                        checked = settings.bool(BoolKey.ChatHistorySearch),
                        onCheckedChange = { settings.setBool(BoolKey.ChatHistorySearch, it) },
                    )
                    SwitchRow(
                        title = "在排行中展示微信运动步数",
                        checked = settings.bool(BoolKey.ChatWeRunRank),
                        onCheckedChange = { settings.setBool(BoolKey.ChatWeRunRank, it) },
                        showDivider = false,
                    )
                }
            }
            item(key = "cache") {
                Spacer(Modifier.height(8.dp))
                Column(Modifier.background(c.surface)) {
                    SwitchRow(
                        title = "退出后清空聊天记录",
                        summary = "每次退出应用时自动清理本地消息",
                        checked = settings.bool(BoolKey.ChatClearCache),
                        onCheckedChange = { settings.setBool(BoolKey.ChatClearCache, it) },
                        showDivider = false,
                    )
                }
            }
            item(key = "danger") {
                Spacer(Modifier.height(8.dp))
                // 危险操作：清空聊天记录（真实产品会弹二次确认，这里做确认弹层）。
                var confirm by remember { mutableStateOf(false) }
                if (!confirm) {
                    DangerButton(text = "清空全部聊天记录") { confirm = true }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(c.surface)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        MiuixText(
                            "确定清空全部聊天记录？此操作不可恢复。",
                            fontSize = 14.sp,
                            color = c.onSurface,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(c.surfaceContainerHigh)
                                    .clickable { confirm = false }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                MiuixText("取消", fontSize = 15.sp, color = c.onSurface)
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(c.error)
                                    .clickable { confirm = false }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                MiuixText("确定清空", fontSize = 15.sp, color = c.onError)
                            }
                        }
                    }
                }
            }
            item(key = "tail") { GroupGap(24.dp) }
        }
    }
}

// ============================================================================
// 隐私
// ============================================================================

@Composable
fun PrivacyScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    val settings = LocalSuchatSettings.current ?: return

    SuchatScaffold(title = "隐私", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "add_me") {
                GroupGap()
                SectionHint("添加我的方式")
                Column(Modifier.background(c.surface)) {
                    SwitchRow(
                        title = "加我为朋友时需要验证",
                        checked = settings.bool(BoolKey.PrivacyAddMeVerify),
                        onCheckedChange = { settings.setBool(BoolKey.PrivacyAddMeVerify, it) },
                    )
                    SwitchRow(
                        title = "可通过手机号搜索到我",
                        checked = settings.bool(BoolKey.PrivacyPhoneSearch),
                        onCheckedChange = { settings.setBool(BoolKey.PrivacyPhoneSearch, it) },
                    )
                    SwitchRow(
                        title = "可通过 Suchat 号搜索到我",
                        checked = settings.bool(BoolKey.PrivacyIdSearch),
                        onCheckedChange = { settings.setBool(BoolKey.PrivacyIdSearch, it) },
                        showDivider = false,
                    )
                }
            }
            item(key = "moments") {
                GroupGap()
                SectionHint("朋友圈")
                Column(Modifier.background(c.surface)) {
                    SwitchRow(
                        title = "不让他（她）看我的朋友圈",
                        summary = "已选择 0 位联系人",
                        checked = settings.bool(BoolKey.PrivacyMomentsRange),
                        onCheckedChange = { settings.setBool(BoolKey.PrivacyMomentsRange, it) },
                    )
                    SwitchRow(
                        title = "允许朋友查看朋友圈的范围",
                        summary = "最近半年",
                        checked = settings.bool(BoolKey.PrivacyReadReceipt),
                        onCheckedChange = { settings.setBool(BoolKey.PrivacyReadReceipt, it) },
                        showDivider = false,
                    )
                }
            }
            item(key = "tail") { GroupGap(24.dp) }
        }
    }
}

// ============================================================================
// 通用
// ============================================================================

@Composable
fun GeneralScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    val settings = LocalSuchatSettings.current ?: return

    SuchatScaffold(title = "通用", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "appearance") {
                GroupGap()
                Column(Modifier.background(c.surface)) {
                    SuchatEntryRow(
                        title = "外观",
                        icon = SuchatIcons.Theme,
                        trailingText = settings.choice(ChoiceKey.ThemeMode),
                        onClick = { nav.push(SuchatPage.SetAppearance) },
                    )
                    SuchatEntryRow(
                        title = "字体大小",
                        icon = SuchatIcons.Settings,
                        trailingText = settings.choice(ChoiceKey.FontScale),
                        onClick = { nav.push(SuchatPage.SetFontSize) },
                    )
                    SuchatEntryRow(
                        title = "存储空间",
                        icon = SuchatIcons.File,
                        onClick = { nav.push(SuchatPage.SetStorage) },
                        showDivider = false,
                    )
                }
            }
            item(key = "switches") {
                GroupGap()
                Column(Modifier.background(c.surface)) {
                    SwitchRow(
                        title = "自动下载未读消息中的媒体",
                        checked = settings.bool(BoolKey.GeneralAutoDownload),
                        onCheckedChange = { settings.setBool(BoolKey.GeneralAutoDownload, it) },
                    )
                    SwitchRow(
                        title = "保存搜索历史",
                        checked = settings.bool(BoolKey.GeneralSearchHistory),
                        onCheckedChange = { settings.setBool(BoolKey.GeneralSearchHistory, it) },
                    )
                    SwitchRow(
                        title = "触感反馈",
                        summary = "开关、长按等操作时轻微振动",
                        checked = settings.bool(BoolKey.GeneralHaptics),
                        onCheckedChange = { settings.setBool(BoolKey.GeneralHaptics, it) },
                    )
                    SwitchRow(
                        title = "横屏时自动旋转",
                        checked = settings.bool(BoolKey.GeneralLandscapeFollow),
                        onCheckedChange = { settings.setBool(BoolKey.GeneralLandscapeFollow, it) },
                        showDivider = false,
                    )
                }
            }
            item(key = "entries") {
                GroupGap()
                Column(Modifier.background(c.surface)) {
                    SuchatEntryRow(
                        title = "帮助与反馈",
                        icon = SuchatIcons.Help,
                        onClick = { nav.push(SuchatPage.SetHelp) },
                    )
                    SuchatEntryRow(
                        title = "关于 Suchat",
                        icon = SuchatIcons.Help,
                        trailingText = "v${BuildConfig.VERSION_NAME}",
                        onClick = { nav.push(SuchatPage.SetAbout) },
                        showDivider = false,
                    )
                }
            }
            item(key = "tail") { GroupGap(24.dp) }
        }
    }
}

// ============================================================================
// 外观（改完即时生效）
// ============================================================================

/**
 * 外观设置 —— **改动立刻全应用生效**（用户第 3 条诉求的直接落地）。
 *
 * ## 两个数据源怎么分工
 *
 *  - [SuchatSettings] 负责**持久化**（下次启动还认得）；
 *  - [SuchatAppearance] 负责**当前运行时的即时生效**（主题层直接读它）。
 *
 * 每次写设置时**同时**写这两处 —— 持久化的同时触发重组，
 * 所以用户拖完选项立刻看到变化，不需要重进页面。
 */
@Composable
fun AppearanceScreen(
    nav: SuchatNavigator,
    appearance: SuchatAppearance,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    val settings = LocalSuchatSettings.current ?: return

    // 三个多选一的候选项。
    val themeOptions = listOf("System" to "跟随系统", "Light" to "浅色", "Dark" to "深色")
    val glassOptions = listOf(
        "LiquidGlass" to "液态玻璃（完整折射）",
        "Blur" to "半透明毛玻璃",
        "None" to "纯色（最省电）",
    )
    val perfOptions = listOf(
        "Full" to "完整（Lens + 高光 + 按压物理）",
        "Balanced" to "平衡",
        "Battery" to "省电",
    )

    var picker by remember { mutableStateOf<Pair<String, List<Pair<String, String>>>?>(null) }

    SuchatScaffold(title = "外观", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "hint") {
                GroupGap()
                SectionHint("以下设置由用户手动指定 —— 应用不会根据设备性能自动推断（契约 docs/android-experience.md）。")
            }
            item(key = "choices") {
                Column(Modifier.background(c.surface)) {
                    ChoiceRow(
                        title = "深浅色",
                        value = themeOptions.first { it.first == settings.choice(ChoiceKey.ThemeMode) }.second,
                        onClick = { picker = "theme_mode" to themeOptions },
                    )
                    ChoiceRow(
                        title = "玻璃渲染",
                        value = glassOptions.first { it.first == settings.choice(ChoiceKey.GlassMode) }.second,
                        onClick = { picker = "glass_mode" to glassOptions },
                    )
                    ChoiceRow(
                        title = "性能档位",
                        value = perfOptions.first { it.first == settings.choice(ChoiceKey.Performance) }.second,
                        onClick = { picker = "performance" to perfOptions },
                        showDivider = false,
                    )
                }
            }
            item(key = "motion") {
                GroupGap()
                Column(Modifier.background(c.surface)) {
                    SwitchRow(
                        title = "减少动态效果",
                        summary = "页面转场替换为短淡化，玻璃跟随动画降到最低",
                        checked = settings.bool(BoolKey.ReduceMotion),
                        onCheckedChange = {
                            settings.setBool(BoolKey.ReduceMotion, it)
                            appearance.reduceMotion = it
                        },
                        showDivider = false,
                    )
                }
            }
            item(key = "tab_scale") {
                GroupGap()
                Column(Modifier.background(c.surface)) {
                    IntSliderRow(
                        title = "底栏大小",
                        value = settings.int(IntKey.TabBarScale),
                        min = IntKey.TabBarScale.min,
                        max = IntKey.TabBarScale.max,
                        onValueChange = { settings.setInt(IntKey.TabBarScale, it) },
                    )
                }
            }
            item(key = "tail") { GroupGap(24.dp) }
        }

        // 选择面板（底部弹出，与 miuix 的 ModalBottomSheet 观感一致）。
        val current = picker
        if (current != null) {
            ChoiceSheet(
                title = when (current.first) {
                    "theme_mode" -> "深浅色"
                    "glass_mode" -> "玻璃渲染"
                    else -> "性能档位"
                },
                options = current.second,
                selected = settings.choice(
                    when (current.first) {
                        "theme_mode" -> ChoiceKey.ThemeMode
                        "glass_mode" -> ChoiceKey.GlassMode
                        else -> ChoiceKey.Performance
                    },
                ),
                onDismiss = { picker = null },
                onPick = { value ->
                    when (current.first) {
                        "theme_mode" -> {
                            settings.setChoice(ChoiceKey.ThemeMode, value)
                            // ★ 即时生效：直接改写运行时的 appearance 实例。
                            appearance.themeMode = when (value) {
                                "Light" -> SuchatThemeMode.Light
                                "Dark" -> SuchatThemeMode.Dark
                                else -> SuchatThemeMode.System
                            }
                        }
                        "glass_mode" -> {
                            settings.setChoice(ChoiceKey.GlassMode, value)
                            appearance.glassMode = value
                        }
                        else -> {
                            settings.setChoice(ChoiceKey.Performance, value)
                            appearance.performance = value
                        }
                    }
                    picker = null
                },
            )
        }
    }
}

/** 底部弹出的单选面板（带遮罩）。 */
@Composable
private fun ChoiceSheet(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val c = MiuixTheme.colorScheme
    Box(Modifier.fillMaxSize()) {
        // 遮罩。
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(c.surface),
        ) {
            Spacer(Modifier.height(16.dp))
            MiuixText(
                text = title,
                fontSize = 13.sp,
                color = c.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(8.dp))
            options.forEachIndexed { index, (value, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(value) }
                        .padding(horizontal = 20.dp)
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiuixText(label, fontSize = 16.sp, color = c.onSurface, modifier = Modifier.weight(1f))
                    if (value == selected) {
                        MiuixIcon(
                            imageVector = SuchatIcons.Ok,
                            contentDescription = "已选择",
                            tint = c.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                if (index != options.lastIndex) {
                    Box(
                        Modifier
                            .padding(start = 20.dp)
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(c.outline.copy(alpha = 0.35f)),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.surfaceContainerHigh)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                MiuixText("取消", fontSize = 16.sp, color = c.onSurface)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ============================================================================
// 存储空间 / 字体大小 / 关于 / 帮助
// ============================================================================

@Composable
fun StorageScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    // 分类占用（示意数据；真实产品来自 File 遍历统计）。
    val categories = listOf(
        Triple("聊天记录", 128.4f, c.primary),
        Triple("图片与视频", 86.2f, c.tertiaryContainer),
        Triple("缓存", 42.7f, c.secondary),
        Triple("其它", 12.1f, c.surfaceContainerHigh),
    )
    val total = categories.sumOf { it.second.toDouble() }.toFloat()

    SuchatScaffold(title = "存储空间", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "total") {
                GroupGap()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .padding(20.dp),
                ) {
                    MiuixText("共占用", fontSize = 13.sp, color = c.onSurfaceSecondary)
                    Spacer(Modifier.height(4.dp))
                    MiuixText(
                        "%.1f MB".format(total),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = c.onSurface,
                    )
                    Spacer(Modifier.height(16.dp))
                    // 占比条（用 Row + weight 表达，无需 Canvas）。
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                    ) {
                        categories.forEach { (_, size, color) ->
                            Box(
                                Modifier
                                    .weight(size / total.coerceAtLeast(0.1f))
                                    .fillMaxSize()
                                    .background(color),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    categories.forEach { (name, size, color) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color),
                            )
                            MiuixText(
                                name,
                                fontSize = 14.sp,
                                color = c.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp),
                            )
                            MiuixText(
                                "%.1f MB".format(size),
                                fontSize = 13.sp,
                                color = c.onSurfaceSecondary,
                            )
                        }
                    }
                }
            }
            item(key = "actions") {
                Spacer(Modifier.height(8.dp))
                Column(Modifier.background(c.surface)) {
                    SuchatEntryRow(
                        title = "清理缓存",
                        icon = SuchatIcons.Clear,
                        trailingText = "42.7 MB",
                        onClick = { },
                    )
                    SuchatEntryRow(
                        title = "管理聊天记录",
                        icon = SuchatIcons.Messages,
                        onClick = { },
                        showDivider = false,
                    )
                }
            }
            item(key = "tail") { GroupGap(24.dp) }
        }
    }
}

@Composable
fun FontSizeScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    val settings = LocalSuchatSettings.current ?: return
    val options = listOf(
        "小" to 14.sp,
        "标准" to 16.sp,
        "大" to 18.sp,
        "超大" to 21.sp,
    )
    val current = settings.choice(ChoiceKey.FontScale)

    SuchatScaffold(title = "字体大小", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "preview") {
                GroupGap()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .padding(20.dp),
                ) {
                    MiuixText("预览", fontSize = 12.sp, color = c.onSurfaceVariantSummary)
                    Spacer(Modifier.height(10.dp))
                    // 预览文字随当前档位实时变化（改一下立刻看到大小）。
                    val size = options.first { it.first == current }.second
                    MiuixText(
                        "这是一段示例文字，用于预览当前字体大小在实际聊天中的效果。",
                        fontSize = size,
                        color = c.onSurface,
                    )
                }
            }
            item(key = "list") {
                Spacer(Modifier.height(8.dp))
                Column(Modifier.background(c.surface)) {
                    options.forEachIndexed { index, (label, size) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { settings.setChoice(ChoiceKey.FontScale, label) }
                                .padding(horizontal = 20.dp)
                                .height(64.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MiuixText(
                                label,
                                fontSize = 16.sp,
                                color = c.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            // 右侧用该档位字号显示「微信」二字，直观对比。
                            MiuixText(
                                "Suchat",
                                fontSize = size,
                                color = c.onSurfaceSecondary,
                            )
                            if (label == current) {
                                MiuixIcon(
                                    imageVector = SuchatIcons.Ok,
                                    contentDescription = "已选择",
                                    tint = c.primary,
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .size(20.dp),
                                )
                            }
                        }
                        if (index != options.lastIndex) {
                            Box(
                                Modifier
                                    .padding(start = 20.dp)
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(c.outline.copy(alpha = 0.35f)),
                            )
                        }
                    }
                }
            }
            item(key = "tail") { GroupGap(24.dp) }
        }
    }
}

@Composable
fun AboutScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme

    SuchatScaffold(title = "关于 Suchat", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "logo") {
                GroupGap()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.surface)
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 品牌字标（主题色方块 + 白字，不用 emoji）。
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(c.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        MiuixText(
                            "S",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = c.onPrimary,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    MiuixText(
                        "Suchat",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.onSurface,
                    )
                    Spacer(Modifier.height(6.dp))
                    MiuixText(
                        "版本 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）",
                        fontSize = 13.sp,
                        color = c.onSurfaceSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    MiuixText(
                        "构建 ${BuildConfig.GIT_HASH}",
                        fontSize = 11.sp,
                        color = c.onSurfaceVariantSummary,
                    )
                }
            }
            item(key = "entries") {
                GroupGap()
                Column(Modifier.background(c.surface)) {
                    SuchatEntryRow(
                        title = "功能介绍",
                        icon = SuchatIcons.Help,
                        onClick = { },
                    )
                    SuchatEntryRow(
                        title = "隐私保护指引",
                        icon = SuchatIcons.Lock,
                        onClick = { },
                    )
                    SuchatEntryRow(
                        title = "用户协议",
                        icon = SuchatIcons.File,
                        onClick = { },
                    )
                    SuchatEntryRow(
                        title = "帮助与反馈",
                        icon = SuchatIcons.Help,
                        onClick = { nav.push(SuchatPage.SetHelp) },
                        showDivider = false,
                    )
                }
            }
            item(key = "tail") { GroupGap(24.dp) }
        }
    }
}

@Composable
fun HelpScreen(nav: SuchatNavigator, bottomInset: Dp = 0.dp) {
    val c = MiuixTheme.colorScheme
    val faqs = listOf(
        "如何备份聊天记录？" to "进入「设置 → 聊天 → 聊天记录迁移与备份」，按引导完成。",
        "收不到新消息通知？" to "请检查「设置 → 新消息通知」，并确认系统未限制 Suchat 的后台运行。",
        "如何切换账号？" to "在「我 → 设置 → 账号与安全 → 登录设备管理」中可退出其它设备。",
        "液态玻璃效果不显示？" to "在「设置 → 通用 → 外观 → 玻璃渲染」中确认选择了「液态玻璃」。",
    )
    var expanded by remember { mutableStateOf<Int?>(null) }

    SuchatScaffold(title = "帮助与反馈", onBack = { nav.pop() }, bottomInset = bottomInset) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = LocalTopBarInset.current,
                bottom = pad.calculateBottomPadding(),
            ),
        ) {
            item(key = "hint") {
                GroupGap()
                SectionHint("常见问题")
            }
            item(key = "faq") {
                Column(Modifier.background(c.surface)) {
                    faqs.forEachIndexed { index, (q, a) ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { expanded = if (expanded == index) null else index },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .height(56.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MiuixText(
                                    q,
                                    fontSize = 15.sp,
                                    color = c.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                MiuixIcon(
                                    imageVector = if (expanded == index) {
                                        SuchatIcons.ExpandLess
                                    } else {
                                        SuchatIcons.ExpandMore
                                    },
                                    contentDescription = null,
                                    tint = c.onSurfaceSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            if (expanded == index) {
                                MiuixText(
                                    a,
                                    fontSize = 13.sp,
                                    color = c.onSurfaceSecondary,
                                    modifier = Modifier.padding(
                                        start = 20.dp,
                                        end = 20.dp,
                                        bottom = 16.dp,
                                    ),
                                )
                            }
                        }
                        if (index != faqs.lastIndex) {
                            Box(
                                Modifier
                                    .padding(start = 20.dp)
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(c.outline.copy(alpha = 0.35f)),
                            )
                        }
                    }
                }
            }
            item(key = "feedback") {
                GroupGap()
                Column(Modifier.background(c.surface)) {
                    SuchatEntryRow(
                        title = "意见反馈",
                        icon = SuchatIcons.Send,
                        onClick = { },
                    )
                    SuchatEntryRow(
                        title = "在线客服",
                        icon = SuchatIcons.Messages,
                        onClick = { },
                        showDivider = false,
                    )
                }
            }
            item(key = "tail") { GroupGap(24.dp) }
        }
    }
}
