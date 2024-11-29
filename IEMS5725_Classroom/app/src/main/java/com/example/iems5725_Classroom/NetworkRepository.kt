package com.example.iems5725_Classroom

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.get
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

class NetworkRepository {
    private val testUser = MY_USER_NAME

    // 模拟一个网络请求
    suspend fun fetchDataForTab(tabId: Int): JsonObject {
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
                response = client.get("${BASE_URL}get_chats?username=${testUser}")
            }
            2 -> {
                response = client.get("${BASE_URL}get_info?username=${testUser}")
            }
            else -> {}
        }
        client.close()
        return response
    }

    // 你可以添加更多的网络请求函数，根据需要添加
    suspend fun fetchAdditionalData(): String {

        return "Additional Network Data"
    }
}