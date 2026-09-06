package io.github.sxd91.suchat.data.remote

/** Realtime client contract. Android has no usable platform WebSocket API at minSdk 28 without a dependency. */
interface SuchatWebSocket {
    fun connect(accessToken: String, listener: Listener)
    fun send(text: String): Boolean
    fun close(code: Int = 1000, reason: String? = null)
    interface Listener {
        fun onOpen()
        fun onMessage(text: String)
        fun onClosed(code: Int, reason: String?)
        fun onFailure(error: Throwable)
    }
}

class UnsupportedSuchatWebSocket(private val endpoint: ServerEndpoint) : SuchatWebSocket {
    override fun connect(accessToken: String, listener: SuchatWebSocket.Listener) {
        listener.onFailure(UnsupportedOperationException("WebSocket requires an Android-specific implementation for ${endpoint.webSocketUrl}"))
    }
    override fun send(text: String) = false
    override fun close(code: Int, reason: String?) = Unit
}
