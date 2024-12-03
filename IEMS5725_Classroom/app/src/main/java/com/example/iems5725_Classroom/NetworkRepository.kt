package com.example.iems5725_Classroom

import android.util.Log
import com.google.firebase.messaging.Constants.MessageNotificationKeys.TAG
import io.ktor.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.InternalAPI
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.Json

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

    @OptIn(InternalAPI::class)
    suspend fun postForCreateChat(message: Any): JsonObject {
        val response: JsonObject = client.post("${BASE_URL}create_chat"){
            contentType(io.ktor.http.ContentType.Application.Json)
            body = message
        }.body()
        client.close()
        return response
    }
}