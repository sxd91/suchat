package io.github.sxd91.suchat.data.remote

data class ServerEndpoint private constructor(val baseUrl: String) {
    val apiUrl: String get() = "$baseUrl/api/v1"
    val webSocketUrl: String get() = baseUrl.replaceFirst("http", "ws") + "/ws"
    fun apiPath(path: String): String = "$apiUrl/${path.trimStart('/')}"

    companion object {
        fun from(value: String): ServerEndpoint {
            val trimmed = value.trim().trimEnd('/')
            require(trimmed.isNotEmpty()) { "Server endpoint is required" }
            val withScheme = if ("://" in trimmed) trimmed else "http://$trimmed"
            require(withScheme.startsWith("http://") || withScheme.startsWith("https://")) { "Server endpoint must use http or https" }
            return ServerEndpoint(withScheme.removeSuffix("/api/v1").trimEnd('/'))
        }
    }
}
