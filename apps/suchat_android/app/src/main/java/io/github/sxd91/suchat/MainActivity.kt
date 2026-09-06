package io.github.sxd91.suchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { SuchatLauncher() } }
    }
}

private sealed interface AppSession {
    data object Preview : AppSession
    data class Connected(val endpoint: String) : AppSession
}

@Composable private fun SuchatLauncher() {
    var session by remember { mutableStateOf<AppSession?>(null) }
    when (val current = session) {
        null -> ServerSetupScreen({ session = AppSession.Preview }, { session = AppSession.Connected(it) })
        is AppSession.Preview -> PreviewHome("前端预览 · 未连接服务器") { session = null }
        is AppSession.Connected -> PreviewHome("已连接 " + current.endpoint) { session = null }
    }
}

@Composable private fun ServerSetupScreen(onPreview: () -> Unit, onConnected: (String) -> Unit) {
    var address by remember { mutableStateOf("http://10.0.2.2:8787") }
    var status by remember { mutableStateOf("填写局域网服务器地址后测试连接") }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0B1224), Color(0xFF214B78), Color(0xFF38234C))))) {
        Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Suchat", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("跨时代的连接", color = Color.White.copy(alpha = .72f))
            Spacer(Modifier.height(36.dp))
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .14f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("连接 Suchat Server", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("服务器地址") }, placeholder = { Text("http://192.168.1.23:8787") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("真机填写电脑局域网 IPv4；模拟器使用 10.0.2.2。", color = Color.White.copy(alpha = .68f), style = MaterialTheme.typography.bodySmall)
                    Button(enabled = !checking, onClick = { checking = true; status = "正在测试连接…"; scope.launch { val result = checkServer(address); checking = false; status = result; if (result.startsWith("已连接")) onConnected(normalizeAddress(address)) } }, modifier = Modifier.fillMaxWidth()) { Text(if (checking) "正在连接" else "测试连接") }
                    Text(status, color = if (status.startsWith("已连接")) Color(0xFF99E5B4) else Color.White.copy(alpha = .78f))
                }
            }
            Spacer(Modifier.height(18.dp)); Text("或", color = Color.White.copy(alpha = .6f)); Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = onPreview, modifier = Modifier.fillMaxWidth()) { Text("没有服务器，仅预览前端") }
        }
    }
}

@Composable private fun PreviewHome(label: String, onExit: () -> Unit) {
    var selected by remember { mutableStateOf("消息") }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF0A152A), Color(0xFF183C66), Color(0xFF452C5E))))) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("Suchat", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(label, color = Color.White.copy(alpha = .7f)) }; Text("退出", modifier = Modifier.clickable { onExit() }.padding(10.dp), color = Color(0xFF9FCBFF)) }
            PreviewList(selected, when(selected) { "消息" -> listOf("林星河 · 晚上一起看电影吗？", "旅行计划 · 3 条新消息", "Suchat 团队 · 欢迎来到 Suchat"); "联系人" -> listOf("新的朋友", "旅行计划群", "我的设备"); "发现" -> listOf("朋友圈 · 与朋友分享此刻", "漂流瓶 · 把一句心事交给未知的远方"); else -> listOf("外观 · LiquidGlass / Full", "主题与 Monet 取色", "隐私与安全") })
            Spacer(Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(32.dp), color = Color(0xFF1E3A5F).copy(alpha = .55f), shadowElevation = 12.dp, modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(6.dp)) { listOf("消息", "联系人", "发现", "我的").forEach { item -> Text(item, modifier = Modifier.weight(1f).clickable { selected = item }.background(if (selected == item) Color(0xFF8FC7FF).copy(alpha = .28f) else Color.Transparent, RoundedCornerShape(24.dp)).padding(vertical = 12.dp), color = Color.White, fontWeight = if (selected == item) FontWeight.Bold else FontWeight.Normal) } } }
        }
    }
}

@Composable private fun PreviewList(title: String, items: List<String>) { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); items.forEach { item -> Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .13f)), modifier = Modifier.fillMaxWidth()) { Text(item, Modifier.padding(18.dp)) } } } }

private fun normalizeAddress(raw: String): String { val address = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "http://" + raw; return address.trimEnd('/') }
private suspend fun checkServer(raw: String): String = withContext(Dispatchers.IO) { try { val c = URL(normalizeAddress(raw) + "/api/v1/health").openConnection() as HttpURLConnection; c.connectTimeout = 5000; c.readTimeout = 5000; c.requestMethod = "GET"; if (c.responseCode == 200) "已连接 Suchat Local Server" else "服务返回 HTTP " + c.responseCode } catch (e: Exception) { "连接失败：" + (e.message ?: "请检查地址、服务和防火墙") } }
