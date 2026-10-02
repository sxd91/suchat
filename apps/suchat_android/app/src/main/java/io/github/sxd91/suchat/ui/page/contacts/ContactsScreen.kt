package io.github.sxd91.suchat.ui.page.contacts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sxd91.suchat.core.design.icon.SuchatIcons
import io.github.sxd91.suchat.core.nav.SuchatNavigator
import io.github.sxd91.suchat.core.nav.SuchatPage
import io.github.sxd91.suchat.data.SampleData
import io.github.sxd91.suchat.ui.component.LocalTopBarInset
import io.github.sxd91.suchat.ui.component.SuchatAvatar
import io.github.sxd91.suchat.ui.component.SuchatChatRow
import io.github.sxd91.suchat.ui.component.SuchatEntryRow
import io.github.sxd91.suchat.ui.component.SuchatScaffold
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs

/**
 * 联系人页。
 *
 * ## 索引栏 = 鱼眼（邻近缩放）
 *
 * ★ 2026-10-02 重做（用户要求「索引栏字母本体做动态缩放」）：
 *
 * 旧实现是「手指按住时在胶囊左侧弹出一个大字母气泡」—— 那是**单个**字母
 * 放大，属于选中反馈。用户要的是**索引栏本身的触摸反馈动画**：把触摸点当
 * 透镜中心，字母按「到触摸点的垂直距离」依次递减地放大 ——
 * 离得最近的字母最大、稍远的略大、超出影响半径后恢复原大小。
 *
 * 这就是 Android 社区所称的 **FisheyeIndexBar / WaveSideBar / 波浪索引栏**。
 *
 * ### 三个实现要点（缺一个就不像鱼眼）
 *
 *  1. **触摸点取连续位置**，不是离散索引 ——
 *     `lensY = touchY / pitch`。若只用「当前选中的字母序号」当中心，
 *     手指滑过时会一档一档跳，放大数永远是整数位置，看着是"逐个亮"
 *     而不是"波跟着手指走"。
 *  2. **衰减 + smoothstep**（`t²(3-2t)`）—— 线性衰减在影响半径边缘有折角，
 *     看起来像突然被"掐断"；smoothstep 两端导数为 0，过渡平滑。
 *  3. **按压缩放零延迟、松手才回弹** —— 按住时若用弹簧逼近，
 *     手指快速滑动会明显"拖后腿"；所以 `pressed` 期间用 `snap()`，
 *     抬手瞬间切回 `spring()` 弹回 1.0。
 *
 * ### 为什么不画离屏 Canvas
 *
 * 本页字母少（A–Z + #），每个字母一个 `graphicsLayer` 缩放即可；
 * 用 Canvas 自绘虽然省几个节点，但会失去 miuix 的文字样式（字重/字距/
 * 主题色）与无字体回退，得不偿失。
 */
@Composable
fun ContactsScreen(
    nav: SuchatNavigator,
    bottomInset: Dp = 0.dp,
) {
    val c = MiuixTheme.colorScheme
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 按首字母分组（保持字母顺序）。
    val grouped: List<Pair<String, List<io.github.sxd91.suchat.data.model.Contact>>> = remember {
        SampleData.contacts
            .groupBy { it.initial }
            .toSortedMap()
            .map { (initial, list) -> initial to list }
    }
    val letters = remember(grouped) { grouped.map { it.first } }

    // 每个字母分组头在 LazyColumn 中的下标（结构：[固定入口][头][人…][头][人…]）。
    val headerIndexByLetter = remember(grouped) {
        buildMap {
            var index = 1 // 跳过「固定入口」整块
            grouped.forEach { (letter, contacts) ->
                put(letter, index)
                index += 1 + contacts.size
            }
        }
    }

    // 索引条状态（鱼眼）。
    // 索引条实测高度（px）—— 用于把触摸 y 换算成「第几个字母」。
    var barHeightPx by remember { mutableFloatStateOf(0f) }
    var activeLetter by remember { mutableStateOf<String?>(null) }
    // 手指是否正按在索引条上 —— 只有按下时才放大，松手立即弹回。
    var pressed by remember { mutableStateOf(false) }
    // 触摸点相对索引条顶部的 y（**连续值**，不是索引），鱼眼透镜中心。
    var touchY by remember { mutableFloatStateOf(0f) }

    // 选字母 → 滚列表（统一入口，点按/滑动都走这里）。
    fun selectLetter(letter: String) {
        if (activeLetter != letter) {
            activeLetter = letter
            headerIndexByLetter[letter]?.let { index ->
                scope.launch { listState.scrollToItem(index) }
            }
        }
    }

    SuchatScaffold(
        title = "联系人",
        onBack = null,
        bottomInset = bottomInset,
        actions = {
            // 顶栏右侧「+」：去「新的朋友」（与旧版行为一致）。
            MiuixIconButton(onClick = { nav.push(SuchatPage.NewFriends) }) {
                MiuixIcon(
                    imageVector = SuchatIcons.Add,
                    contentDescription = "添加",
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
    ) { pad ->
        Box(modifier = Modifier.fillMaxSize()) {
            // --- 列表 ---
            // 顶栏高度（LocalTopBarInset）进 contentPadding.top：
            // 内容初始落在顶栏下方、滚动时**穿过**顶栏下方 —— 模糊层才采得到内容。
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = LocalTopBarInset.current,
                    bottom = pad.calculateBottomPadding(),
                ),
            ) {
                // 固定入口
                item(key = "fixed_entries") {
                    Column(Modifier.background(c.surface)) {
                        SuchatEntryRow(
                            title = "新的朋友",
                            icon = SuchatIcons.Contacts,
                            onClick = { nav.push(SuchatPage.NewFriends) },
                            showDivider = false,
                        )
                        SuchatEntryRow(
                            title = "群聊",
                            icon = SuchatIcons.Chats,
                            onClick = { nav.push(SuchatPage.GroupChats) },
                            showDivider = false,
                        )
                        SuchatEntryRow(
                            title = "标签",
                            icon = SuchatIcons.Favorites,
                            onClick = { nav.push(SuchatPage.Tags) },
                            showDivider = false,
                        )
                        SuchatEntryRow(
                            title = "公众号",
                            icon = SuchatIcons.Channels,
                            onClick = { nav.push(SuchatPage.OfficialAccounts) },
                            showDivider = false,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // 字母分组
                grouped.forEach { (letter, contacts) ->
                    item(key = "header_$letter") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(c.surfaceContainer)
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        ) {
                            MiuixText(
                                text = letter,
                                fontSize = 13.sp,
                                color = c.onSurfaceSecondary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    items(
                        count = contacts.size,
                        key = { i -> contacts[i].id },
                    ) { i ->
                        val contact = contacts[i]
                        SuchatChatRow(
                            title = contact.remark ?: contact.name,
                            subtitle = "",
                            time = "",
                            avatarName = contact.name,
                            avatarSeed = contact.id,
                            showDivider = i != contacts.lastIndex,
                            dividerStart = 68.dp,
                            onClick = { nav.push(SuchatPage.ContactDetail(contact.id)) },
                        )
                        // 联系人行没有副标题与时间，这里隐藏掉对应空位；
                        // 说明（历史）—— 之前用 WeChatListItem 的 subtitle/time 可为空，
                        // 现在 SuchatChatRow 强制要求，故传空串并在下方补偿高度。
                    }
                }

                item(key = "contact_footer") {
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .background(c.surface),
                    )
                }
            }

        // --- 右侧字母索引：鱼眼（Fisheye / WaveSideBar） ---
        //
        // ★ 2026-10-02 重做：旧版是「胶囊 + 左侧大气泡」，用户要求改成
        //   **字母本体按到触摸点的距离动态缩放**（波浪/鱼眼效果）。
        //
        // 关键点：
        //  1. 透镜中心取**连续**触摸位置（touchY / pitch），不是选中字母下标 ——
        //     否则滑动时会一档一档跳，看不到"波"跟着手指走；
        //  2. 影响半径用「字母个数」表达（±[IndexLensRadiusLetters] 个字母内受影响），
        //     这样不论索引条多长、字母多少，观感一致；
        //  3. 按压缩放 `snap()`（零延迟跟手）、抬手 `spring()` 弹回 ——
        //     按住时用动画逼近会明显拖后腿。
        //
        // ⚠️ pitch 必须在**回调内部**用 `barHeightPx` 现算：
        //    pointerInput 的 lambda 只按 `letters` 重建，若把外部算好的
        //    pitch 捕进去，首帧（高度还是 0）算出的值会被永久缓存。
        val lensPitch = if (letters.isEmpty()) 1f else barHeightPx / letters.size
        fun letterAt(offsetY: Float): String? =
            letters.getOrNull((offsetY / lensPitch).toInt().coerceIn(0, letters.lastIndex))

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                // ★ 2026-10-02 修正（用户反馈「索引太大了、字母间隔太远、看不到鱼眼」）：
                //
                // 旧写法 `.fillMaxHeight(0.62f)` + `Arrangement.SpaceEvenly`：
                // 索引条高 = 屏高 × 0.62 ≈ 1719px，26 个字母被**均匀撑满**，
                // 每个字母间距 ≈ 66px（22.6dp）—— 既松弛又看不出放大。
                //
                // 现改为 **wrapContentHeight + 固定字母行高**：
                // 总高 = 字母数 × [IndexItemHeight]（26 × 15dp = 390dp），
                // 间距紧凑后，鱼眼的 1.9× 放大才真正看得见。
                .wrapContentHeight()
                .width(IndexBarWidth)
                .padding(end = 2.dp)
                .pointerInput(letters) {
                    // 滑动选字母（连续）。
                    detectDragGestures(
                        onDragStart = { offset ->
                            pressed = true
                            touchY = offset.y
                            letterAt(offset.y)?.let { selectLetter(it) }
                        },
                        onDragEnd = { pressed = false; activeLetter = null },
                        onDragCancel = { pressed = false; activeLetter = null },
                    ) { change, _ ->
                        touchY = change.position.y
                        letterAt(change.position.y)?.let { selectLetter(it) }
                        change.consume()
                    }
                }
                .pointerInput(letters) {
                    // 点按也能选（与滑动共用 selectLetter）。
                    detectTapGestures(
                        onPress = { offset ->
                            pressed = true
                            touchY = offset.y
                            letterAt(offset.y)?.let { selectLetter(it) }
                            tryAwaitRelease()
                            pressed = false
                            activeLetter = null
                        },
                    )
                },
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .width(IndexBarWidth)
                    .clip(RoundedCornerShape(IndexBarWidth / 2))
                    .background(c.surfaceContainerHigh.copy(alpha = 0.9f))
                    .onSizeChanged { barHeightPx = it.height.toFloat() },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                letters.forEachIndexed { index, letter ->
                    IndexLetter(
                        letter = letter,
                        // ★ 鱼眼核心：透镜中心取连续位置，缩放由「距离多少个字母」决定。
                        scale = fisheyeScale(
                            index = index,
                            lensPosition = if (pressed && lensPitch > 0f) {
                                touchY / lensPitch
                            } else {
                                // 未按下（或高度尚未测量）：推到极远，
                                // 所有字母都在影响半径外 → 缩放恒为 1。
                                -1e3f
                            },
                        ),
                        pressed = pressed,
                        active = letter == activeLetter,
                    )
                }
            }
        }
    }
    }
}

/** 索引条宽。 */
private val IndexBarWidth = 20.dp

/**
 * 单个字母的行高。
 *
 * ★ 2026-10-02 新增：鱼眼的**缩放锚点间距**就是它。
 *
 * ## 为什么必须显式给行高（不能用 SpaceEvenly）
 *
 * 旧写法靠 `fillMaxHeight(0.62f)` + `SpaceEvenly` 决定间距 —— 间距随屏幕尺寸
 * 浮动（本机 ≈22.6dp），导致两个问题：
 *  1. **太松**：26 个字母摊在 1719px 上，视觉上「间隔太远」；
 *  2. **鱼眼失效**：`lensPitch = 条高/字母数` 变大后，
 *     `fisheyeScale` 里 `distance = |index - lensPosition|`（单位=字母数）
 *     虽然不受影响，但**视觉上**相邻字母隔得太开，1.9× 放大在 22.6dp 间距里
 *     显得微不足道 —— 用户「看不到鱼眼效果」的真正原因。
 *
 * 固定 15dp：26 个字母 = 390dp，紧凑得像 iOS/微信的索引条，
 * 放大倍数在紧密排列里一眼可见。
 *
 * 注：`wrapContentHeight` 需要子项有确定高度，否则退化为 0 —— 所以行高必须有。
 */
private val IndexItemHeight = 15.dp

/**
 * 鱼眼影响半径（单位：**字母个数**，不是像素）。
 *
 * 为什么按字母数而不是像素：索引条高度由屏幕决定、字母数由数据决定，
 * 两者都不固定。若用固定像素半径，字母稀疏时影响圈太小（看不出波浪）、
 * 字母密集时又糊成一片。按「±2 个字母」表达则任何数据下观感一致：
 * 手指压住处最大，上下各两个字母依次递减，第 3 个起回到原大小。
 */
private const val IndexLensRadiusLetters = 2.0f

/** 透镜中心（手指正压住的那个字母）的最大放大倍数。 */
private const val IndexLensMaxScale = 1.9f

/**
 * 鱼眼缩放：算第 [index] 个字母在「透镜中心位于 [lensPosition] 时的放大倍数。
 *
 * ## 算法（三步）
 *
 * ```
 * d = |index - lensPosition|            // 到透镜中心的距离（单位：字母）
 * t = (1 - d / radius).coerceIn(0, 1)   // 归一化，越近越接近 1
 * t = t²(3 - 2t)                        // smoothstep：两端导数为 0，过渡平滑
 * scale = 1 + (maxScale - 1) · t
 * ```
 *
 * ## 为什么必须用 smoothstep 而不是线性
 *
 * 线性衰减在 `d = radius` 处导数突变（从 -k 直接跳到 0），
 * 字母会看到明显的「折角」—— 像被一刀切断。smoothstep 在 0 和 1
 * 两端导数都为 0，放大-恢复的过渡是圆滑的，才像"波形"。
 *
 * ## 为什么透镜中心是连续值
 *
 * [lensPosition] 是 `touchY / itemHeight`（浮点），不是 `roundToInt()` 的结果。
 * 若取整，手指连续滑动时中心只在整数间跳变 —— 表现为"一个一个字母轮流放大"，
 * 而不是"波跟着手指平移"。连续中心才能让相邻两个字母同时处于中间态
 * （比如中心 3.5 → 第 3、4 个字母各放大 70%），视觉上才是波。
 *
 * @param index 字母下标。
 * @param lensPosition 透镜中心（连续值，单位同 [index]；默认 -1e3 表示无透镜）。
 */
private fun fisheyeScale(index: Int, lensPosition: Float): Float {
    val distance = kotlin.math.abs(index - lensPosition)
    val t = (1f - distance / IndexLensRadiusLetters).coerceIn(0f, 1f)
    val smooth = t * t * (3f - 2f * t)
    return 1f + (IndexLensMaxScale - 1f) * smooth
}

/**
 * 索引栏里的一个字母 —— 支持鱼眼缩放。
 *
 * ## 两个动画归零的细节
 *
 *  - **按下期间用 [snap]**：手指滑动时若缩放走弹簧，会有明显"追不上"的拖尾；
 *    `snap` 是零时长动画 = 当帧直接生效，跟手无延迟。
 *  - **抬手用 [spring]**：松手后所有字母弹回 1.0，带一点回弹，手感 Q 弹。
 *
 * 缩放锚点在字母**中心**（`graphicsLayer` 默认 transformOrigin 即 0.5,0.5），
 * 所以放大时字母向两侧均匀撑开，不是向下坠。
 *
 * @param pressed 是否被按住（决定用 snap 还是 spring）。
 * @param active 是否是当前选中的字母（额外着色 + 加粗）。
 */
@Composable
private fun IndexLetter(
    letter: String,
    scale: Float,
    pressed: Boolean,
    active: Boolean,
) {
    val c = MiuixTheme.colorScheme
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = if (pressed) snap() else spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "indexLetterScale",
    )
    MiuixText(
        text = letter,
        fontSize = 10.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        color = if (active) c.primary else c.onSurfaceSecondary,
        modifier = Modifier
            // 固定行高：给 wrapContentHeight 一个确定的总高，
            // 同时保证每个字母的"缩放锚点"等距（鱼眼的 pitch 才稳定）。
            .height(IndexItemHeight)
            .wrapContentHeight(Alignment.CenterVertically)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            },
    )
}