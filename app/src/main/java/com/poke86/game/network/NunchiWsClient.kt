package com.poke86.game.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NunchiWsClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)  // keep-alive: prevents 60s proxy timeout
        .build()

    private var webSocket: WebSocket? = null

    private val _events = MutableSharedFlow<Map<String, Any?>>()
    val events: SharedFlow<Map<String, Any?>> = _events.asSharedFlow()

    // SharedFlow (not StateFlow) — always emits even if value unchanged (false→false)
    // true = connected, false = disconnected/failed
    private val _connectionEvent = MutableSharedFlow<Boolean>()
    val connectionEvent: SharedFlow<Boolean> = _connectionEvent.asSharedFlow()

    // Last failure reason (populated before emitting false)
    var lastError: String = ""
        private set

    fun connect(scope: CoroutineScope, url: String = SERVER_URL) {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                scope.launch { _connectionEvent.emit(true) }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                scope.launch {
                    try {
                        val json = JSONObject(text)
                        val map = mutableMapOf<String, Any?>()
                        for (key in json.keys()) map[key] = json.opt(key)
                        _events.emit(map)
                    } catch (_: Exception) {}
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                lastError = t.message ?: t.javaClass.simpleName
                scope.launch { _connectionEvent.emit(false) }
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                lastError = "closed($code)"
                scope.launch { _connectionEvent.emit(false) }
            }
        })
    }

    fun send(type: String, vararg params: Pair<String, Any>) {
        val json = JSONObject().apply {
            put("type", type)
            params.forEach { (k, v) -> put(k, v) }
        }
        webSocket?.send(json.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, null)
        webSocket = null
    }

    companion object {
        const val SERVER_URL = "wss://game.poke86.com/nunchi/ws"
    }
}
