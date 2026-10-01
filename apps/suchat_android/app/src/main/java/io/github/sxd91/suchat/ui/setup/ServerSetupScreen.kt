package io.github.sxd91.suchat.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.sxd91.suchat.ui.components.SuchatSegmentPosition
import io.github.sxd91.suchat.ui.components.SuchatBaseWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 服务器连接页（既有流程保留）。
 *
 * 这是 Suchat 的启动入口：输入本地服务地址 → 健康检查 → 连接成功进入主界面；
 * 也可以直接「仅预览前端」使用占位数据体验完整界面（甲方要求：没有的数据用占位示例）。
 *
 * 健康检查走 `GET {endpoint}/api/v1/health`，用 `HttpURLConnection` 实现
 * （与既有 `SuchatApiClient` 同源，不引额外网络库）。
 */
@Composable
fun ServerSetupScreen(
    onPreview: () -> Unit,
    onConnected: (String) -> Unit,
) {
    var endpoint by remember { mutableStateOf("http://10.0.2.2:8787") }
    var status by remember { mutableStateOf("尚未检测") }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.height(36.dp))
            Text(
                "Suchat",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text("跨时代的连接", color = MaterialTheme.colorScheme.onSurfaceVariant)

            // --- 连接服务器 ---
            SegmentHeader("连接到服务器")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("服务器地址") },
                    supportingText = { Text("真机填写电脑 IPv4；模拟器使用 10.0.2.2") },
                )
                SuchatBaseWidget(
                    title = "连接状态",
                    description = status,
                    position = SuchatSegmentPosition.Single,
                    showChevron = false,
                )
            }

            Button(
                enabled = !checking,
                onClick = {
                    checking = true
                    status = "正在测试连接…"
                    scope.launch {
                        val result = checkServer(endpoint)
                        status = result
                        checking = false
                        if (result.startsWith("已连接")) {
                            onConnected(normalizeAddress(endpoint))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (checking) "正在连接" else "测试连接")
            }

            // --- 登录 / 注册（占位：待接入服务端鉴权） ---
            SegmentHeader("开始使用")
            SuchatBaseWidget(
                title = "登录",
                description = "连接成功后登录 Suchat 账号",
                position = SuchatSegmentPosition.Top,
            )
            SuchatBaseWidget(
                title = "注册",
                description = "在当前服务器创建新账号",
                position = SuchatSegmentPosition.Bottom,
            )

            // --- 本地演示 ---
            SegmentHeader("本地演示")
            SuchatBaseWidget(
                title = "仅预览前端",
                description = "使用占位数据体验 Suchat 界面",
                position = SuchatSegmentPosition.Single,
                onClick = onPreview,
            )
        }
    }
}

@Composable
private fun SegmentHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 补全协议头并去掉尾斜杠。 */
private fun normalizeAddress(raw: String): String {
    val value = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "http://$raw"
    return value.trimEnd('/')
}

/** 健康检查：`GET {endpoint}/api/v1/health`。 */
private suspend fun checkServer(raw: String): String = withContext(Dispatchers.IO) {
    try {
        val connection = URL(normalizeAddress(raw) + "/api/v1/health").openConnection()
            as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        if (connection.responseCode == 200) {
            "已连接 Suchat Local Server"
        } else {
            "服务返回 HTTP ${connection.responseCode}"
        }
    } catch (error: Exception) {
        "连接失败：${error.message ?: "检查地址、服务或防火墙"}"
    }
}