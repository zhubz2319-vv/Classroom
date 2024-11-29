package com.example.virtualchatroom_1155229616

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.example.iems5725_Classroom.ui.theme.IEMS5725_ClassTheme

const val BASE_URL = "http://34.150.37.253:55722/"
const val MY_USER_ID = 1155229615
const val MY_USER_NAME = "Louis"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        FirebaseApp.initializeApp(this)
        createNotificationChannel()

        CoroutineScope(Dispatchers.IO).launch {
            val result = getResultFromApi(BASE_URL + "check_token/?user_id=" + MY_USER_ID)
            if (result["status"]?.jsonPrimitive?.content == "ERROR") {
                val token = getToken()
                if (token != "nothing") {
                    val message = TokenMessage(token, MY_USER_ID)
                    postInfoToApi (message, BASE_URL+"post_token/")
                }
            }
        }
        setContent {
            IEMS5725_ClassTheme {
                ScaffoldUI()
            }
        }
    }
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
            val channel = NotificationChannel("MyNotification","MyNotification",
                NotificationManager.IMPORTANCE_DEFAULT)
            val manager = ContextCompat.getSystemService(this,NotificationManager::class.java) as
                    NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotificationChannel() {
        val channelId = "default_channel_id"
        val channelName = "Default Channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}

suspend fun getToken(): String = suspendCancellableCoroutine { continuation ->
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            continuation.resumeWithException(
                task.exception ?: Exception("Token fetch failed")
            )
            return@addOnCompleteListener
        }
        continuation.resume (task.result ?: "nothing")
    }
}

suspend fun getResultFromApi(url: String): JsonObject{
    val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }
    val response: JsonObject = client.get(url)
    client.close()
    return response
}
suspend fun postInfoToApi(message: Any, url: String): JsonObject {
    val client = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }
    val response: JsonObject = client.post(url){
        contentType(io.ktor.http.ContentType.Application.Json)
        body = message
    }
    client.close()
    return response
}

@Serializable
data class ChatMessage(val chatroom_id: Int,
                       val user_id: Int,
                       val name: String,
                       val message: String)

@Serializable
data class TokenMessage(val token: String,
                        val user_id: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ScaffoldUI(
) {
    val context = LocalContext.current
    var rooms by remember { mutableStateOf(buildJsonObject { })}
    LaunchedEffect(Unit) {
        rooms = withContext(Dispatchers.IO) {
            getResultFromApi(BASE_URL + "get_chatrooms/")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("IEMS5722")
                },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Localized description"
                        )
                    }
                },
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp)
        ){
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // if "rooms" is empty, a loading indicator will be shown
                if (rooms.values.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ){
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    // get all rooms from "rooms" and show them as button
                } else {
                    val dataArray = rooms["data"]?.jsonArray
                    dataArray?.forEach { element ->
                        val dataObject = element.jsonObject
                        val id = dataObject["id"]?.jsonPrimitive?.int
                        val name = dataObject["name"]?.jsonPrimitive?.content
                        Button(
                            modifier = Modifier
                                .padding()
                                .fillMaxWidth(0.5f),
                            onClick = {

                            }
                        ) {
                            Text(text = name.toString())
                        }
                    }
                }
                Button(
                    onClick = {
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                Toast.makeText(context,"no token",Toast.LENGTH_SHORT).show()
                                return@addOnCompleteListener
                            }

                            val token = task.result
                            Toast.makeText(context,"FCM Token: $token",Toast.LENGTH_SHORT).show()
                            Log.d("FCM Token", token)
                        }
                    }
                ) {
                    Text(text = "test")
                }

                Button(
                    onClick = {
                        // 删除现有令牌
                        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                FirebaseMessaging.getInstance().token.addOnCompleteListener { newTokenTask ->
                                    if (newTokenTask.isSuccessful) {
                                        val newToken = newTokenTask.result
                                        Log.d("New FCM Token", newToken)
                                        // 你可以将新令牌发送到服务器或进行其他操作
                                    } else {
                                        Log.e("Token Error", "Failed to get new token")
                                    }
                                }
                            } else {
                                Log.e("Token Error", "Failed to delete token")
                            }
                        }

                    }
                ) {
                    Text(text = "update")
                }

            }
        }
    }
}
