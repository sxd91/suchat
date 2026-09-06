package io.github.sxd91.suchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sxd91.suchat.ui.theme.SuchatAppearance
import io.github.sxd91.suchat.ui.theme.SuchatTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SuchatTheme(SuchatAppearance()) { SuchatLauncher() } }
    }
}

private sealed interface AppSession { data object Preview : AppSession; data class Connected(val endpoint: String) : AppSession }
private enum class HomeTab(val label: String) { Messages("消息"), Contacts("联系人"), Discover("发现"), Me("我的") }

@Composable
private fun SuchatLauncher() {
    var session by remember { mutableStateOf<AppSession?>(null) }
    when (val current = session) {
        null -> ServerSetupScreen({ session = AppSession.Preview }, { session = AppSession.Connected(it) })
        is AppSession.Preview -> SuchatRootShell("前端预览 · 未连接服务器") { session = null }
        is AppSession.Connected -> SuchatRootShell("已连接 " + current.endpoint) { session = null }
    }
}

@Composable
private fun ServerSetupScreen(onPreview: () -> Unit, onConnected: (String) -> Unit) {
    var endpoint by remember { mutableStateOf("http://10.0.2.2:8787") }
    var status by remember { mutableStateOf("尚未检测") }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Column(Modifier.padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.height(36.dp))
            Text("Suchat", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("跨时代的连接", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SegmentGroup("连接到服务器") {
                OutlinedTextField(endpoint, { endpoint = it }, Modifier.fillMaxWidth(), label = { Text("服务器地址") }, supportingText = { Text("真机填写电脑 IPv4；模拟器使用 10.0.2.2") })
                SegmentRow("连接状态", status) {}
            }
            Button(enabled = !checking, onClick = { checking = true; status = "正在测试连接…"; scope.launch { status = checkServer(endpoint); checking = false; if (status.startsWith("已连接")) onConnected(normalizeAddress(endpoint)) } }, modifier = Modifier.fillMaxWidth()) { Text(if (checking) "正在连接" else "测试连接") }
            SegmentGroup("开始使用") { SegmentRow("登录", "连接成功后登录 Suchat 账号") {}; SegmentRow("注册", "在当前服务器创建新账号") {} }
            SegmentGroup("本地演示") { SegmentRow("仅预览前端", "使用占位数据体验 Suchat 界面", onPreview) }
        }
    }
}

@Composable
private fun SuchatRootShell(status: String, onExit: () -> Unit) {
    var tab by remember { mutableStateOf(HomeTab.Messages) }
    var drawer by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(if (drawer) 1f else 0f, label = "drawer")
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        WeKitPanel(Modifier.fillMaxSize().fillMaxWidth(.666f).alpha(progress))
        Box(Modifier.fillMaxSize().graphicsLayer { scaleX = 1f - .05f * progress; scaleY = 1f - .05f * progress; translationX = 7.dp.toPx() * progress; translationY = 8.dp.toPx() * progress }.clip(RoundedCornerShape(28.dp * progress)).background(MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) { HomeHeader(status, { drawer = true }, {}, { menu = !menu }); HomeContent(tab); Spacer(Modifier.weight(1f)); LiquidTabs(tab) { tab = it } }
            if (drawer) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .28f * progress)).clickable { drawer = false })
            if (menu) QuickMenu(Modifier.align(Alignment.TopEnd).padding(top = 84.dp, end = 16.dp))
        }
    }
}

@Composable
private fun HomeHeader(status: String, onProfile: () -> Unit, onSearch: () -> Unit, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(52.dp).clickable { onProfile() }) { Box(contentAlignment = Alignment.Center) { Text("S", fontWeight = FontWeight.Bold) } }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).clickable { onProfile() }) { Text("Sxd91", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("⌕", Modifier.padding(12.dp).clickable { onSearch() }, style = MaterialTheme.typography.titleLarge)
        Text("＋", Modifier.padding(12.dp).clickable { onAdd() }, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun HomeContent(tab: HomeTab) {
    val items = when (tab) {
        HomeTab.Messages -> listOf("林星河" to "晚上一起看电影吗？ · 12:30", "旅行计划" to "小澈：我已经订好民宿了 · 3 条新消息", "Suchat 团队" to "欢迎来到 Suchat")
        HomeTab.Contacts -> listOf("新的朋友" to "2 个好友申请", "群聊" to "你加入的 8 个群组", "我的设备" to "Android · Windows · Web")
        HomeTab.Discover -> listOf("朋友圈" to "与朋友分享此刻 · 12 条新动态", "漂流瓶" to "把一句心事交给未知的远方")
        HomeTab.Me -> listOf("主题与 Monet 取色" to "跟随系统", "液态玻璃" to "LiquidGlass · Full", "页面转场" to "Shared Element · 预测性返回")
    }
    SegmentGroup(tab.label) { items.forEach { SegmentRow(it.first, it.second) {} } }
}

@Composable
private fun WeKitPanel(modifier: Modifier) {
    Column(modifier.padding(horizontal = 18.dp, vertical = 54.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(58.dp)) { Box(contentAlignment = Alignment.Center) { Text("S", fontWeight = FontWeight.Bold) } }; Spacer(Modifier.width(8.dp)); Column { Text("Sxd91", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Text("前端预览模式", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        PanelCard("2026 / 09 / 06", "星期日 · 19:32")
        PanelCard("今日回忆", "三年前，你创建了旅行计划。")
        PanelCard("漂流瓶海域", "3 个瓶子正等待开启")
        PanelCard("新建聊天   创建群聊   扫一扫", "快捷操作")
        PanelCard("今日一句", "愿你在每个陌生的地方，都能遇见温柔。")
        PanelCard("朋友圈 · 收藏 · 设置", "更多功能")
    }
}

@Composable private fun PanelCard(title: String, summary: String) { Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } } }
@Composable private fun LiquidTabs(tab: HomeTab, select: (HomeTab) -> Unit) { Surface(shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .78f), shadowElevation = 10.dp, modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) { Row(Modifier.padding(6.dp)) { HomeTab.entries.forEach { item -> Text(item.label, Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(if (item == tab) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable { select(item) }.padding(vertical = 12.dp), color = if (item == tab) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (item == tab) FontWeight.Bold else FontWeight.Normal) } } } }
@Composable private fun QuickMenu(modifier: Modifier) { Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceBright, shadowElevation = 12.dp, modifier = modifier.width(190.dp)) { Column { listOf("发起群聊", "添加朋友", "扫一扫").forEach { Text(it, Modifier.fillMaxWidth().clickable {}.padding(18.dp)) } } } }
@Composable private fun SegmentGroup(title: String, body: @Composable ColumnScope.() -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceBright, modifier = Modifier.fillMaxWidth()) { Column(content = body) } } }
@Composable private fun SegmentRow(title: String, summary: String, onClick: () -> Unit) { ListItem(headlineContent = { Text(title) }, supportingContent = { Text(summary) }, trailingContent = { Text("›") }, modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)) }
private fun normalizeAddress(raw: String): String { val value = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "http://" + raw; return value.trimEnd('/') }
private suspend fun checkServer(raw: String): String = withContext(Dispatchers.IO) { try { val connection = URL(normalizeAddress(raw) + "/api/v1/health").openConnection() as HttpURLConnection; connection.connectTimeout = 5000; connection.readTimeout = 5000; if (connection.responseCode == 200) "已连接 Suchat Local Server" else "服务返回 HTTP " + connection.responseCode } catch (error: Exception) { "连接失败：" + (error.message ?: "检查地址、服务或防火墙") } }
