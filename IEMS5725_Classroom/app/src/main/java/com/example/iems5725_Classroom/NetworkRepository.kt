package com.example.iems5725_Classroom

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

class NetworkRepository {

    suspend fun fetchDataForTab(tabId: Int, userName: String): JsonObject {
        val client = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
        var response = buildJsonObject { }
        when (tabId) {
            0 -> {
                response = client.get("${BASE_URL}get_courses/")
            }
            1 -> {
                response = client.get("${BASE_URL}get_chats?username=${userName}")
            }
            2 -> {
                response = client.get("${BASE_URL}get_info?username=${userName}")
            }
            else -> {}
        }
        client.close()
        return response
    }

    suspend fun fetchMessageByRoomCode(roomCode: String): JsonObject {
        val client = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
        var response = buildJsonObject { }
        response = client.get("${BASE_URL}get_messages?room_code=${roomCode}")
        client.close()
        return response
    }
    suspend fun fetchDataForTab(cCode: String, sec: String): JsonObject{
        val client = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
        val response: JsonObject = client.get("${BASE_URL}get_courseinfo?course_code=${cCode}&section=${sec}")
        client.close()
        return response
    }
    suspend fun postForCreateChat(message: Any): JsonObject {
        val client = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
        Log.d("CreateChat","request: ${message}")
        val response: JsonObject = client.post("${BASE_URL}create_chat"){
            contentType(io.ktor.http.ContentType.Application.Json)
            body = message
        }
        client.close()
        return response
    }
}