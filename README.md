# Suchat

**跨时代的连接** —— 跨平台聊天软件（Web / Android / Windows）。

[![Android CI](https://github.com/sxd91/suchat/actions/workflows/android-ci.yml/badge.svg)](https://github.com/sxd91/suchat/actions/workflows/android-ci.yml)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 仓库结构

```
suchat/
├── apps/
│   ├── suchat_android/   # Android 客户端（Kotlin + Compose + miuix）
│   └── suchat_windows/   # Windows 客户端（Flutter）
├── docs/
│   └── android-experience.md   # ★ Android 体验契约（改代码前必读）
└── scripts/
```

## Android 客户端

### 技术基座

| 维度 | 选型 |
|---|---|
| UI 参照 | **微信**（布局/色板/列表规范）+ **WeKit**（液态玻璃/莫奈取色/miuix） |
| 框架 | Jetpack Compose · BOM 2026.08.00 |
| 主题 | miuix 0.9.4-rc01 + material-kolor 5.0.0 |
| 构建 | AGP 9.3.1 / Kotlin 2.4.10 / JDK 21 |
| SDK | compileSdk 37 / minSdk 33 / targetSdk 37 |

### 已实现（前端先行阶段）

**四大主 Tab** —— 命名与内容均对齐 [`docs/android-experience.md`](docs/android-experience.md)：

| Tab | 内容 |
|---|---|
| **消息** | 会话列表（头像/摘要/时间/未读红点/免打扰灰点）、搜索条、`+` 菜单 |
| **联系人** | 新的朋友 / 仅聊天的朋友 / 群聊 / 标签 / 公众号 + A–Z 字母分组 + 右侧索引可点击跳转 |
| **发现** | 朋友圈、**漂流瓶**、视频号、直播、扫一扫、摇一摇、看一看、搜一搜、购物、游戏、小程序 |
| **我的** | 个人卡片、服务、收藏、朋友圈、卡包、表情、设置 |

**聊天界面**（核心）：
- 气泡：自己发出 = 绿色 `#95EC69` + 头像靠右；对方 = 白色 + 头像靠左
- 群聊：对方消息气泡上方显示发送者名字
- 消息类型：文本 / 语音（时长）/ 图片（占位块）
- 输入栏：语音键 + 输入框 + 表情键 + 加号；**有文字时表情键自动变「发送」**
- 表情面板：8 列网格 · 96 个 emoji · 点击插入
- 扩展面板：相册 / 拍摄 / 视频通话 / 位置 / 红包 / 转账 / 语音输入 / 收藏
- 新消息自动滚到底部

**17 个二级页面**：聊天详情、搜索、新的朋友、群聊、标签、公众号、联系人详情、
朋友圈（九宫格 + 点赞 + 评论）、漂流瓶、视频号、扫一扫、看一看、搜一搜、
小程序、服务、收藏、卡包、表情、设置、个人信息。

> 数据层当前使用**占位示例数据**（`data/SampleData.kt`）；
> 后端接入时替换该文件即可，UI 层无需改动。

### 关键架构决策

#### 1. 双轨主题（静态锚点 + 莫奈动态）

```
SuchatRootTheme(appearance)
  ├── MaterialExpressiveTheme → MaterialTheme.colorScheme（莫奈动态色）
  ├── MiuixTheme              → MiuixTheme.colorScheme（miuix 组件，与莫奈同源）
  └── LocalSuchatTokens       → SuchatTokens（静态锚点：气泡绿/顶栏灰…）
```

聊天气泡、顶栏这类**观感锚点**若被动态取色覆盖会立刻失去产品辨识度，
因此单独抽出静态色板；三层共用同一个种子色（品牌绿 `#07C160`），色彩不打架。

#### 2. 液态玻璃底栏（WeKit 模型）

契约要求底栏走 WeKit 的液态玻璃交互模型，由 `core/design/glass/` 完整实现：

```
内容层 .layerBackdrop(backdrop)  →  录成可采样纹理
底栏 drawBackdrop(backdrop)      →  vibrancy() → blur() → lens(refraction)
选中指示器                        →  lens(depthEffect, chromaticAberration)
```

含：pill 几何 · 弹性可拖拽指示器（`DampedDragAnimation`）· 按压缩放 ·
无障碍 tab 语义 · 按下高光（`InteractiveHighlight`）。

**约束**：内容区不做底部 `padding` 抬高，只抬高**滚动终点** ——
内容能滑到玻璃下方，玻璃才有东西可折射。这是折射成立的前提。

#### 3. 轻量导航（不用 navigation-compose）

微信导航模型 = 四常驻 Tab + 单条二级页栈。自研状态机（`core/nav/`）更贴手：

- 切 Tab 走 `HorizontalPager.animateScrollToPage`（整屏平移）
- 二级页右滑入 / 右滑出，全屏覆盖、不显示底栏
- 栈全局共享（从「我的」进设置 → 切走 → 切回，设置仍在）
- `rememberSaveable` 跨配置变更存活

### 构建与运行

```bash
cd apps/suchat_android
./gradlew assembleDebug        # 或在 CI 下载产物
```

要求 JDK 21 + Android SDK（compileSdk 37）。

**APK 构建走 GitHub Actions**：
- 推 `main` 触发 `.github/workflows/android-ci.yml`（JDK 21 + `assembleDebug`）
- 打 `android-v*` tag 触发 release job

> ⚠️ 本机（aarch64 proot）只能做 `compileDebugKotlin` 静态验证：
> aapt2 / llvm-strip 是 x86-64 二进制，跑不了资源编译与符号剥离。
> 完整打包必须在 x86-64 环境（即 CI）。

## 文档

| 文件 | 内容 |
|---|---|
| [`docs/android-experience.md`](docs/android-experience.md) | **Android 体验契约** —— 外观三档、导航结构、底栏行为、转场规则 |

## 许可

[MIT](LICENSE)
