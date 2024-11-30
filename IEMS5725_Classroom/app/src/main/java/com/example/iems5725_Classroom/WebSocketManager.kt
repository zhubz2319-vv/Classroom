package com.example.iems5725_Classroom

import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class WebSocketManager(private val roomCode: String) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    var onMessageReceived: ((JsonObject) -> Unit)? = null

    fun connect() {
        val request = Request.Builder()
            .url("wss://chat.lamitt.com/ws/$roomCode")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d("WebSocket", "Receive a message: ${text}")
                    val message = Json.parseToJsonElement(text).jsonObject
                    onMessageReceived?.invoke(message)
                } catch (e: Exception) {
                    Log.e("WebSocket", "Failed to decode message: ${e.message}")
                }
            }
        })
    }

    fun sendMessage(sender: String, message: String) {
        val messageJson = """
            {
                "sender": "$sender",
                "message": "$message"
            }
        """
        try {
            webSocket?.send(messageJson)
        } catch (e: Exception) {
            Log.e("WebSocket","Sending fail: ${e.message}")
        }
    }

    fun close() {
        webSocket?.close(1000, "Closing connection")
    }
}