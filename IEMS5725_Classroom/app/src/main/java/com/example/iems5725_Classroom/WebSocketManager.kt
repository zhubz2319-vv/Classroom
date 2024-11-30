package com.example.iems5725_Classroom

import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class WebSocketManager(private val roomCode: String) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    // 回调，用于接收消息
    var onMessageReceived: ((MessageItem) -> Unit)? = null

    // 建立 WebSocket 连接
    fun connect() {
        val request = Request.Builder()
            .url("ws://your_backend_url/ws/$roomCode")  // 替换为 FastAPI WebSocket 的 URL
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                // 假设你有一个 Message 数据类
                val message = Json.decodeFromString<MessageItem>(text)
                onMessageReceived?.invoke(message)  // 触发回调，将消息传递给 ViewModel
            }

            // 其他 WebSocket 事件处理...
        })
    }

    // 发送消息
    fun sendMessage(sender: String, message: String) {
        val messageJson = """
            {
                "sender": "$sender",
                "message": "$message"
            }
        """
        webSocket?.send(messageJson)
    }

    // 关闭 WebSocket 连接
    fun close() {
        webSocket?.close(1000, "Closing connection")
    }
}