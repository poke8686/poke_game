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
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val msgType: String = "TEXT", // "TEXT" or "EMOTICON"
    val timestamp: String,
    val isSystem: Boolean = false
)

data class ChatRoomInfo(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0
)

class ChatWsClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    private val _messages = MutableSharedFlow<List<ChatMessage>>()
    val messages: SharedFlow<List<ChatMessage>> = _messages.asSharedFlow()

    private val _newMessage = MutableSharedFlow<ChatMessage>()
    val newMessage: SharedFlow<ChatMessage> = _newMessage.asSharedFlow()

    private val _roomList = MutableSharedFlow<List<ChatRoomInfo>>()
    val roomList: SharedFlow<List<ChatRoomInfo>> = _roomList.asSharedFlow()

    private val _connectionEvent = MutableSharedFlow<Boolean>()
    val connectionEvent: SharedFlow<Boolean> = _connectionEvent.asSharedFlow()

    fun connect(scope: CoroutineScope, url: String = "wss://game.poke86.com/chat/ws") {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                scope.launch { _connectionEvent.emit(true) }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                scope.launch {
                    try {
                        val json = JSONObject(text)
                        val type = json.optString("type")
                        
                        when (type) {
                            "ROOM_LIST" -> {
                                val rooms = parseRooms(json.optJSONArray("rooms"))
                                _roomList.emit(rooms)
                            }
                            "HISTORY" -> {
                                val history = parseMessages(json.optJSONArray("messages"))
                                _messages.emit(history)
                            }
                            "MESSAGE" -> {
                                val msg = parseMessage(json)
                                _newMessage.emit(msg)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                scope.launch { _connectionEvent.emit(false) }
            }
        })
    }

    private fun parseRooms(array: JSONArray?): List<ChatRoomInfo> {
        val list = mutableListOf<ChatRoomInfo>()
        if (array == null) return list
        for (i in 0 until array.length()) {
            val json = array.getJSONObject(i)
            list.add(
                ChatRoomInfo(
                    id = json.optString("id"),
                    name = json.optString("name"),
                    lastMessage = json.optString("lastMessage"),
                    time = json.optString("time"),
                    unreadCount = json.optInt("unreadCount", 0)
                )
            )
        }
        return list
    }

    private fun parseMessages(array: JSONArray?): List<ChatMessage> {
        val list = mutableListOf<ChatMessage>()
        if (array == null) return list
        for (i in 0 until array.length()) {
            list.add(parseMessage(array.getJSONObject(i)))
        }
        return list
    }

    private fun parseMessage(json: JSONObject): ChatMessage {
        return ChatMessage(
            id = json.optString("id"),
            senderId = json.optString("senderId"),
            senderName = json.optString("senderName"),
            content = json.optString("content"),
            msgType = json.optString("msgType", "TEXT"),
            timestamp = json.optString("timestamp"),
            isSystem = json.optBoolean("isSystem", false)
        )
    }

    fun getRooms() {
        send("GET_ROOMS")
    }

    fun joinRoom(roomId: String, userName: String) {
        send("JOIN", "roomId" to roomId, "userName" to userName)
    }

    fun sendMessage(roomId: String, content: String) {
        send("MESSAGE", "roomId" to roomId, "content" to content)
    }

    fun sendEmoticon(roomId: String, emoticonUrl: String) {
        send("EMOTICON", "roomId" to roomId, "emoticonUrl" to emoticonUrl)
    }

    private fun send(type: String, vararg params: Pair<String, Any>) {
        val json = JSONObject().apply {
            put("type", type)
            params.forEach { (k, v) -> put(k, v) }
        }
        webSocket?.send(json.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, null)
    }
}
