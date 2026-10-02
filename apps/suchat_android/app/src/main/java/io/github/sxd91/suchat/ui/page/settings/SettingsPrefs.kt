package io.github.sxd91.suchat.ui.page.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * 设置页的本地持久化 + 可观察状态。
 *
 * ## 为什么用 SharedPreferences 而不是 DataStore
 *
 * 项目里已有 [io.github.sxd91.suchat.data.local.SessionStore]（DataStore），
 * 但本文件是**同步读**：设置页一进来就要立刻拿到当前值渲染开关，
 * 走 DataStore 的 Flow 会先闪一帧默认值再跳成真实值（视觉抖动）。
 *
 * 设置项是「少量、低频写、启动即读」的典型场景 —— 正是 SharedPreferences
 * 的设计目标。写入仍在 apply() 异步落盘，不卡主线程。
 *
 * ## 为什么做成可观察
 *
 * 用户要求「外观」这类设置**改完立刻生效**，不要重进页面：
 *  - [SuchatSettings] 用 `mutableStateOf` 持有每个值 → 写入即触发重组；
 *  - 主题层（[io.github.sxd91.suchat.core.design.theme.SuchatRootTheme]）
 *    直接读同一份状态 → 主题/玻璃档/转场风格全应用实时跟随。
 *
 * [SuchatSettings] 通过 [LocalSuchatSettings] 下发，全树共享同一个实例。
 */
class SuchatSettings internal constructor(private val prefs: android.content.SharedPreferences) {

    /** 各开关项的当前值（key 见 [BoolKey]）。 */
    private val bools: SnapshotStateMap<String, Boolean> = mutableStateMapOf()

    /** 选项值（key 见 [ChoiceKey]）。 */
    private val choices: SnapshotStateMap<String, String> = mutableStateMapOf()

    /** 数值设置值（key 见 [IntKey]）。 */
    private val ints: SnapshotStateMap<String, Int> = mutableStateMapOf()

    init {
        // 首次构造把落盘值全量读进内存状态（同步、一次性）。
        BoolKey.entries.forEach { key ->
            bools[key.id] = prefs.getBoolean(key.id, key.default)
        }
        ChoiceKey.entries.forEach { key ->
            choices[key.id] = prefs.getString(key.id, key.default) ?: key.default
        }
        IntKey.entries.forEach { key ->
            ints[key.id] = prefs.getInt(key.id, key.default).coerceIn(key.min, key.max)
        }
    }

    /** 读开关。 */
    fun bool(key: BoolKey): Boolean = bools[key.id] ?: key.default

    /** 写开关（立即生效 + 异步落盘）。 */
    fun setBool(key: BoolKey, value: Boolean) {
        bools[key.id] = value
        prefs.edit().putBoolean(key.id, value).apply()
    }

    /** 读选项。 */
    fun choice(key: ChoiceKey): String = choices[key.id] ?: key.default

    /** 写选项。 */
    fun setChoice(key: ChoiceKey, value: String) {
        choices[key.id] = value
        prefs.edit().putString(key.id, value).apply()
    }

    /** 读数值项（自动夹到 `min..max`）。 */
    fun int(key: IntKey): Int = (ints[key.id] ?: key.default).coerceIn(key.min, key.max)

    /** 写数值项（夹紧后立即生效 + 异步落盘）。 */
    fun setInt(key: IntKey, value: Int) {
        val v = value.coerceIn(key.min, key.max)
        ints[key.id] = v
        prefs.edit().putInt(key.id, v).apply()
    }

    /** 是否所有设置都在默认值（用于「恢复默认」按钮文案）。 */
    fun isDefault(): Boolean =
        BoolKey.entries.all { bool(it) == it.default } &&
            ChoiceKey.entries.all { choice(it) == it.default } &&
            IntKey.entries.all { int(it) == it.default }

    /** 全部恢复默认。 */
    fun resetAll() {
        BoolKey.entries.forEach { setBool(it, it.default) }
        ChoiceKey.entries.forEach { setChoice(it, it.default) }
        IntKey.entries.forEach { setInt(it, it.default) }
    }
}

/**
 * 布尔设置项。
 *
 * `default` 是「出厂默认值」，必须与微信的设置默认态一致
 * （例如「新消息通知」默认开、「声音」默认开、「输入时显示字数」默认关）。
 */
enum class BoolKey(val id: String, val default: Boolean) {
    // --- 新消息通知 ---
    NotifyMessage("set_notify_message", true),
    NotifySound("set_notify_sound", true),
    NotifyVibrate("set_notify_vibrate", true),
    NotifyDetail("set_notify_detail", true),

    // --- 聊天 ---
    ChatTypingIndicator("set_chat_typing", true),
    ChatSendByEnter("set_chat_send_enter", false),
    ChatHistorySearch("set_chat_history_search", true),
    ChatWeRunRank("set_chat_werun", true),
    ChatClearCache("set_chat_clear_cache", false),

    // --- 隐私 ---
    PrivacyAddMeVerify("set_privacy_add_verify", true),
    PrivacyPhoneSearch("set_privacy_phone_search", true),
    PrivacyIdSearch("set_privacy_id_search", true),
    PrivacyMomentsRange("set_privacy_moments_range", false),
    PrivacyReadReceipt("set_privacy_read_receipt", true),

    // --- 通用 ---
    GeneralSearchHistory("set_general_search_history", true),
    GeneralAutoDownload("set_general_auto_download", true),
    GeneralHaptics("set_general_haptics", true),
    GeneralLandscapeFollow("set_general_landscape", true),

    // --- 青少年模式 ---
    TeenModeEnabled("set_teen_enabled", false),

    // --- 关怀模式 ---
    CareModeEnabled("set_care_enabled", false),
    CareModeLargeText("set_care_large_text", false),

    // --- 账号与安全 ---
    SecurityLoginProtect("set_security_login_protect", true),
    SecurityDeviceLock("set_security_device_lock", false),
    SecuritySecurityCenter("set_security_center", true),

    // --- 外观 / 性能 ---
    ReduceMotion("set_reduce_motion", false),
    DynamicColor("set_dynamic_color", true),
}
/** 多选一设置项。 */
enum class ChoiceKey(val id: String, val default: String) {
    /** 主题模式：System / Light / Dark。 */
    ThemeMode("set_theme_mode", "System"),

    /** 玻璃渲染档位：LiquidGlass(默认) / Blur / None。 */
    GlassMode("set_glass_mode", "LiquidGlass"),

    /** 性能档位：Full(默认) / Balanced / Battery。 */
    Performance("set_performance", "Full"),

    /** 页面转场风格：Shared Element / Miuix / AOSP / Fade。 */
    Transition("set_transition", "Shared Element"),

    /** 聊天记录保留时长文案。 */
    ChatHistoryKeep("set_chat_history_keep", "永久"),

    /** 字体大小档。 */
    FontScale("set_font_scale", "标准"),

    // --- 外观（2026-10-02 新增：颜色来源 / 调色风格） ---

    /**
     * 颜色来源：`Wallpaper`（背景取色·默认）/ `Monet`（莫奈取色）/ `Custom`（自定义）。
     *
     * ⚠️ 默认必须是 **Wallpaper**：这是「真·莫奈」——读系统壁纸提取种子色。
     * 上一版默认用了 `Monet` 且走 `ColorSchemeMode.System`（用品牌绿），
     * 导致整套配色退化成固定绿、用户体感「莫奈取色没了」。
     */
    ColorSource("set_color_source", "Wallpaper"),

    /** 调色风格（TonalSpot / Vibrant / Expressive / … 共 9 种）。 */
    PaletteStyle("set_palette_style", "TonalSpot"),
}

/** 整数设置项（带范围）。 */
enum class IntKey(val id: String, val default: Int, val min: Int, val max: Int) {
    /** 底栏缩放（对齐 WeKit 的 `nav_bar_scale`，50–150%，默认 100）。 */
    TabBarScale("set_tab_bar_scale", 100, 50, 150),

    /**
     * 自定义种子色（ARGB Int）。
     *
     * 用 Int 而不是 Compose 的 `Color`：`Color` 是 inline value class，
     * 落盘/读取都不如 Int 直接，且 SharedPreferences 原生只支持基本类型。
     * 默认值 = 品牌绿 `0xFF07C160`。
     */
    SeedColor("set_seed_color", 0xFF07C160.toInt(), Int.MIN_VALUE, Int.MAX_VALUE),
}

/** 设置项 CompositionLocal（全树共享同一实例）。 */
val LocalSuchatSettings = staticCompositionLocalOf<SuchatSettings?> { null }

/**
 * 取得（并首次创建）设置实例。
 *
 * 只在 `SuchatAppShell` 调一次；子页面用 [LocalSuchatSettings] 读同一个实例。
 */
@Composable
fun rememberSuchatSettings(): SuchatSettings {
    val context = LocalContext.current
    return remember(context) {
        val prefs = context.getSharedPreferences("suchat_settings", Context.MODE_PRIVATE)
        SuchatSettings(prefs)
    }
}
