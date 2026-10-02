package io.github.sxd91.suchat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.sxd91.suchat.core.design.icon.SuchatIcons
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Suchat 模糊顶栏脚手架 —— 对齐「逆向系老挂」同款实现。
 *
 * ## 来源
 *
 * 移植自 `cn.apixiaoyuan.app` 的 `AppScaffold.kt`（同一位开发者维护的姊妹项目），
 * 保留其全部关键设计决策：
 *
 *  1. **顶栏本体是 miuix 的 [SmallTopAppBar]**（读 MiuixTheme 配色/字号/高度），
 *     不是 Material3 的 CenterAlignedTopAppBar；
 *  2. **渐变模糊**（[progressiveTextureBlur]）：贴顶一侧模糊最强、向下渐清 ——
 *     用户要求"逆向系老挂同款模糊顶部"指的就是这条链路；
 *  3. **采样源是页面内容自己的 backdrop**，只记录内容、不含顶栏自身（避免自采样）；
 *  4. **顶栏高度进滚动容器的 contentPadding，不进容器 padding** ——
 *     这是关键：内容初始落在顶栏下方，滚动时**穿过**顶栏下面，
 *     模糊层才真正有东西可采（否则模糊看着跟实色没区别）；
 *  5. [BlurOverhang] 让模糊层向下多盖一段，抹平"满强度模糊 → 完全清晰"的硬边；
 *     同时把 Scaffold 的 innerPadding.top 减回去 —— **内容不下移**。
 *
 * ## 适用范围（用户指定）
 *
 * 「除主页、视频号和聊天页外的所有页面」—— 主页用 WeKit 三段式头部、
 * 视频号是全屏内容、聊天页用固定顶栏 + 输入栏，这三者不走本骨架。
 */

/** 顶栏渐变模糊向下额外覆盖的高度（抹平硬边；不是内容下移）。 */
private val BlurOverhang = 28.dp

/**
 * 滚动容器应追加的底部限位（由 [SuchatScaffold] 下发）。
 *
 * ## 语义纪律（勿改成容器 padding）
 *
 * 这个值**只用于滚动容器的底部限位**（`LazyColumn.contentPadding.bottom`）：
 * 悬浮底栏是**浮层**，内容本就该能滑到它**下面**（玻璃折射成立的前提）；
 * 若加进容器 padding，等于把整个页面顶上去，玻璃下方永远空着 —— 设计意图落空。
 */
val LocalScrollBottomLimit = staticCompositionLocalOf { 0.dp }

/** 顶栏高度（供滚动容器作为 contentPadding.top）。 */
val LocalTopBarInset = staticCompositionLocalOf { 0.dp }

/** 导航层下发的底栏限位（由 MainActivity 提供）。 */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

/**
 * 模糊顶栏页面骨架（通用滚动内容）。
 *
 * @param title 顶栏标题。
 * @param onBack 返回动作；null 表示不显示返回键。
 * @param bottomInset 内容底部额外留白。
 * @param actions 顶栏右侧操作区（RowScope，可放 IconButton）。
 */
@Composable
fun SuchatScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 24.dp,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    // 采样层：先铺一层与容器同色的打底，再记录页面内容。
    // 打底色必须与容器色一致，否则顶栏区域会看出"另一块背景"。
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    // ★ 2026-10-02 事故复盘（删除 Kyant 采样层）：
    //
    // 曾在此处给「内容层」加 `rememberKyantLayerBackdrop` 并 provide 给子树，
    // 结果**真机 native crash（RenderThread 栈溢出）**：
    // ```
    // signal 11 (SIGSEGV) ... likely due to stack overflow
    // RenderNode::prepareTreeImpl → prepareListAndChildren → prepareTreeImpl → …512 帧
    // ```
    // 真因：RenderNode 树成环 —— 内容层被记录成 layer，而内容里的玻璃控件
    // （LiquidToggle）绘制时又去引用这个 layer → layer 包含"引用它的节点"，
    // prepareTreeImpl 递归无终止。
    // 上游 demo 无此问题：它的玻璃控件与采样层是**兄弟**（Image 的记录层），
    // 不是"被记录层"的子节点。
    //
    // 因此：**永远不要把"包含玻璃控件自身"的容器记录成 layer**。
    // 控件侧由 [LocalKyantBackdrop] 为 null 时自动回退纯色 canvas 采样
    // （对开关/滑块这类"面板上的元件"，这也正是 iOS 的设计语义：
    //   折射面板色，而非穿透面板看内容）。

    androidx.compose.material3.Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = surfaceColor,
        topBar = { BlurredTopBar(backdrop = backdrop, title = title, onBack = onBack, actions = actions) },
    ) { innerPadding ->
        val blurSupported = isRuntimeShaderSupported()
        // 顶栏 Box 因 BlurOverhang 变高，innerPadding.top 也随之变大；
        // 这里减回去，保证**内容初始位置不变**（只有模糊多盖一段）。
        val topBarInset = (
            innerPadding.calculateTopPadding() - if (blurSupported) BlurOverhang else 0.dp
            ).coerceAtLeast(0.dp)

        CompositionLocalProvider(
            LocalTopBarInset provides topBarInset,
            LocalScrollBottomLimit provides LocalBottomBarInset.current,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    // 内容层被记录进 backdrop，供顶栏采样做渐变模糊。
                    // （注意：**只有 miuix 这一层**，Kyant 层已在上面事故复盘中移除。）
                    .layerBackdrop(backdrop),
            ) {
                content(
                    PaddingValues(
                        top = 0.dp,
                        bottom = innerPadding.calculateBottomPadding() + bottomInset,
                    )
                )
            }
        }
    }
}

/**
 * 列表形态的模糊顶栏骨架（LazyColumn）。
 */
@Composable
fun SuchatListScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 24.dp,
    content: LazyListScope.() -> Unit,
) {
    SuchatScaffold(title = title, onBack = onBack, modifier = modifier, bottomInset = bottomInset) { pad ->
        val topInset = LocalTopBarInset.current
        val scrollLimit = LocalScrollBottomLimit.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topInset,
                bottom = pad.calculateBottomPadding() + scrollLimit,
            ),
            content = content,
        )
    }
}

/**
 * miuix 渐变模糊顶栏。
 *
 * 结构：Box 内先铺 `matchParentSize` 的 [progressiveTextureBlur]（纯背景），
 * 再把 [SmallTopAppBar] 叠上去并设 `color = Color.Transparent`，
 * 这样顶栏自身的实心背景不会盖住模糊层。
 */
@Composable
private fun BlurredTopBar(
    backdrop: LayerBackdrop,
    title: String,
    onBack: (() -> Unit)?,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val blurSupported = isRuntimeShaderSupported()
    val colors = BlurDefaults.blurColors(
        blendColors = listOf(
            BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(alpha = 0.30f)),
        ),
    )
    Box {
        if (blurSupported) {
            Box(
                Modifier
                    .matchParentSize()
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        blurRadius = 10f,
                        gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                        colors = colors,
                    ),
            )
        }
        Column {
            SmallTopAppBar(
                title = title,
                modifier = Modifier.fillMaxWidth(),
                // 支持模糊时透明让下层透出；不支持时退回实色（避免标题与内容叠字）。
                color = if (blurSupported) Color.Transparent else MiuixTheme.colorScheme.surface,
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = SuchatIcons.Back,
                                contentDescription = "返回",
                            )
                        }
                    }
                },
                actions = {
                    if (actions != null) {
                        actions()
                    }
                },
            )
            if (blurSupported) {
                Spacer(Modifier.height(BlurOverhang))
            }
        }
    }
}