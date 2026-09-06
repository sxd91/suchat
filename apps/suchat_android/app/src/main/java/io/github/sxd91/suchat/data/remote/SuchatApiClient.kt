package io.github.sxd91.suchat.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/** Coroutine-friendly Suchat API client backed solely by HttpURLConnection. */
class SuchatApiClient(
    val endpoint: ServerEndpoint,
    private val tokenProvider: () -> String? = { null },
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 20_000,
) {
    suspend fun healthCheck() = get("health") { HealthStatus(it.string("status"), it.string("service"), it.string("version"), it.string("time"), it.string("database")) }
    suspend fun register(request: RegisterRequest) = post("auth/register", JSONObject().apply { put("handle", request.handle); put("displayName", request.displayName); put("password", request.password) }) { it.authResponse() }
    suspend fun login(request: AuthRequest) = post("auth/login", JSONObject().apply { put("handle", request.handle); put("password", request.password) }) { it.authResponse() }
    suspend fun getAppearance() = get("me/appearance") { it.appearance() }
    suspend fun updateAppearance(value: Appearance) = put("me/appearance", JSONObject().apply {
        put("glassMode", value.glassMode); put("performanceProfile", value.performanceProfile); put("pageTransition", value.pageTransition); put("themeMode", value.themeMode); put("reducedMotion", value.reducedMotion)
    }) { it.appearance() }
    suspend fun listConversations(): List<Conversation> = get("conversations") { it.getJSONArray("items").mapObjects { item -> item.conversation() } }
    suspend fun createConversation(request: CreateConversationRequest): Conversation = post("conversations", JSONObject().apply {
        put("memberIds", JSONArray(request.memberIds)); put("kind", request.kind); request.title?.let { title -> put("title", title) }
    }) { it.conversation() }
    suspend fun listMessages(conversationId: String): List<Message> = get("conversations/${conversationId.segment()}/messages") { it.getJSONArray("items").mapObjects { item -> item.message() } }
    suspend fun sendMessage(conversationId: String, request: SendMessageRequest): Message = post("conversations/${conversationId.segment()}/messages", JSONObject().apply {
        put("content", request.content); put("contentType", request.contentType); request.attachmentId?.let { id -> put("attachmentId", id) }
    }) { it.message() }

    private suspend fun <T> get(path: String, decode: (JSONObject) -> T) = request("GET", path, null, decode)
    private suspend fun <T> post(path: String, body: JSONObject, decode: (JSONObject) -> T) = request("POST", path, body, decode)
    private suspend fun <T> put(path: String, body: JSONObject, decode: (JSONObject) -> T) = request("PUT", path, body, decode)
    private suspend fun <T> request(method: String, path: String, body: JSONObject?, decode: (JSONObject) -> T): T = withContext(Dispatchers.IO) {
        val connection = URL(endpoint.apiPath(path)).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method; connection.connectTimeout = connectTimeoutMs; connection.readTimeout = readTimeoutMs
            connection.setRequestProperty("Accept", "application/json")
            tokenProvider()?.takeIf { token -> token.isNotBlank() }?.let { token -> connection.setRequestProperty("Authorization", "Bearer $token") }
            if (body != null) { connection.doOutput = true; connection.setRequestProperty("Content-Type", "application/json; charset=utf-8"); OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer -> writer.write(body.toString()) } }
            val status = connection.responseCode
            val raw = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader(Charsets.UTF_8)?.use { reader -> reader.readText() }.orEmpty()
            if (status !in 200..299) throw ApiError(status, runCatching { JSONObject(raw).optString("error").takeIf { code -> code.isNotBlank() } }.getOrNull(), raw)
            decode(JSONObject(raw))
        } finally { connection.disconnect() }
    }
}

private fun JSONObject.string(name: String) = getString(name)
private fun JSONObject.nullableString(name: String): String? = if (isNull(name)) null else optString(name, null)
private fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> = List(length()) { index -> mapper(getJSONObject(index)) }
private fun JSONObject.user() = UserProfile(string("id"), string("handle"), string("displayName"), nullableString("avatarUrl"), nullableString("bio"), string("status"), nullableString("createdAt"), nullableString("lastSeenAt"))
private fun JSONObject.authResponse() = AuthResponse(getJSONObject("user").user(), AuthTokens(getJSONObject("tokens").string("accessToken")))
private fun JSONObject.appearance() = Appearance(string("glassMode"), string("performanceProfile"), string("pageTransition"), string("themeMode"), getBoolean("reducedMotion"))
private fun JSONObject.conversation() = Conversation(string("id"), string("kind"), nullableString("title"), nullableString("createdAt"), nullableString("updatedAt"), nullableString("lastMessage"))
private fun JSONObject.message() = Message(string("id"), string("conversationId"), string("senderId"), string("content"), string("contentType"), nullableString("attachmentId"), string("createdAt"), nullableString("editedAt"))
private fun String.segment() = java.net.URLEncoder.encode(this, Charsets.UTF_8.name())
