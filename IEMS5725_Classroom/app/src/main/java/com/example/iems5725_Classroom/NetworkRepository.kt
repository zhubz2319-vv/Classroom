package com.example.iems5725_Classroom

import android.util.Log
import com.example.iems5725_Classroom.network.CreateChatRequest
import com.example.iems5725_Classroom.network.RetrofitClient
import com.google.firebase.messaging.Constants.MessageNotificationKeys.TAG
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement

class NetworkRepository {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    suspend fun fetchDataForTab(tabId: Int, userName: String): JsonObject {
        return when (tabId) {
            0 -> client.get("${BASE_URL}get_courses/").body()
            1 -> client.get("${BASE_URL}get_chats?username=${userName}").body()
            2 -> client.get("${BASE_URL}get_info?username=${userName}").body()
            else -> buildJsonObject { }
        }
    }

    suspend fun fetchMessageByRoomCode(roomCode: String): JsonObject {
        var response = buildJsonObject { }
        response = client.get("${BASE_URL}get_messages?room_code=${roomCode}").body()
        client.close()
        return response
    }
    suspend fun fetchDataForTab(cCode: String, sec: String): JsonObject{
        val response: JsonObject = client.get("${BASE_URL}get_courseinfo?course_code=${cCode}&section=${sec}").body()
        Log.d(TAG, "Fetched data course: ${response}")
        client.close()
        return response
    }

    suspend fun postForCreateChat(message: ChatGroup): JsonObject {
        val api = RetrofitClient.apiService
        return Json.encodeToJsonElement(
            api.createChat(
                CreateChatRequest(
                    message.username,
                    message.room_code,
                    message.room_name
                )
            )
        ) as JsonObject
    }
}