package com.example.iems5725_Classroom

import MainViewModel
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.FullyDrawnReporterOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomAppBar
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.example.iems5725_Classroom.ui.theme.IEMS5725_ClassTheme
import com.google.firebase.messaging.Constants.MessageNotificationKeys.TAG
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


const val MY_USER_ID = 1155229615
const val MY_USER_NAME = "test"
const val BASE_URL = "https://chat.lamitt.com/"


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        FirebaseApp.initializeApp(this)
        createNotificationChannel()
        val networkRepository = NetworkRepository()
        val viewModelFactory = MainViewModelFactory(networkRepository)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)

//        CoroutineScope(Dispatchers.IO).launch {
//            val result = getResultFromApi(BASE_URL + "check_token/?user_id=" + MY_USER_ID)
//            if (result["status"]?.jsonPrimitive?.content == "ERROR") {
//                val token = getToken()
//                if (token != "nothing") {
//                    val message = TokenMessage(token, MY_USER_ID)
//                    postInfoToApi (message, BASE_URL+"post_token/")
//                }
//            }
//        }
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
fun ScaffoldUI() {
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
//    LaunchedEffect(Unit) {
//        rooms = withContext(Dispatchers.IO) {
//            getResultFromApi(BASE_URL + "get_chatrooms/")
//        }
//    }
    // 当 selectedTab 改变时，启动网络请求
    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(viewModel.tabName.value)
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
        bottomBar = {
            BottomAppBar(
                actions = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        IconButton(
                            onClick = {
                                viewModel.fetchTabData(0, "Courses")
                                selectedTab = 0
                                      },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.courses_us),
                                contentDescription = "Localized description",
                                tint = if (selectedTab == 0) Color.Blue else Color.Black
                            )
                        }
                        IconButton(
                            onClick = { viewModel.fetchTabData(1, "Chat Groups")
                                selectedTab = 1},
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.chatgroup_us),
                                contentDescription = "Localized description",
                                tint = if (selectedTab == 1) Color.Blue else Color.Black
                            )
                        }
                        IconButton(
                            onClick = { viewModel.fetchTabData(2, "My Profile")
                                selectedTab = 2},
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.myinfo_us),
                                contentDescription = "Localized description",
                                tint = if (selectedTab == 2) Color.Blue else Color.Black
                            )
                        }
                    }
                },

            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (viewModel.selectedTab.value != selectedTab)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            else {
                LazyColumn {
                    item {
                        when (viewModel.selectedTab.value) {
                            0 -> {
                                val dataArray = viewModel.content.value["courses"]?.jsonArray
                                dataArray?.forEach { element ->
                                    val dataObject = element.jsonObject
                                    val courseName =
                                        dataObject["course_name"]?.jsonPrimitive?.content
                                    val courseCode =
                                        dataObject["course_code"]?.jsonPrimitive?.content
                                    val instructor =
                                        dataObject["instructor"]?.jsonPrimitive?.content
                                    val studentsArray = dataObject["students"]?.jsonArray
                                    val student: List<String> = studentsArray?.map {
                                        Json.decodeFromJsonElement<String>(it)
                                    } ?: emptyList()
                                    Log.d(TAG, "students list: ${student}")
                                    CourseItem(
                                        courseName.toString(),
                                        courseCode.toString(),
                                        instructor.toString(),
                                        student
                                    )
                                }
                            }

                            1 -> {
                                val dataArray = viewModel.content.value["rooms"]?.jsonArray
                                dataArray?.forEach { element ->
                                    val dataObject = element.jsonObject
                                    val roomId = dataObject["room_id"]?.jsonPrimitive?.content
                                    val roomName = dataObject["room_name"]?.jsonPrimitive?.content
                                    val owner = dataObject["owner"]?.jsonPrimitive?.content
                                    ChatRoomItem(
                                        roomId.toString(),
                                        roomName.toString(),
                                        owner.toString()
                                    )
                                }
                            }

                            2 -> {
                                val nickname =
                                    viewModel.content.value["nickname"]?.jsonPrimitive?.content
                                val role = viewModel.content.value["role"]?.jsonPrimitive?.content
                                UserInfoScreen(nickname.toString(), role.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun CourseItem(courseName: String, courseCode: String, instructor: String, students: List<String>){
    Button(
        onClick = { /* 可以根据需要实现按钮点击事件 */ },
        modifier = Modifier
            .fillMaxWidth() // 使按钮占满宽度
            .padding(8.dp) // 为按钮添加内边距
    ) {
        // Column 用于垂直排列文本
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp), // 为 Column 添加内边距
            horizontalAlignment = Alignment.CenterHorizontally // 内容居中
        ) {
            Text(text = "Course Name: $courseName", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp)) // 添加一些间隔
            Text(text = "Course Code: $courseCode", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp)) // 添加一些间隔
            Text(text = "Instructor: $instructor", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable
fun ChatRoomItem(roomId: String, roomName: String, owner: String){
    Button(
        onClick = { /* 可以根据需要实现按钮点击事件 */ },
        modifier = Modifier
            .fillMaxWidth() // 使按钮占满宽度
            .padding(8.dp) // 为按钮添加内边距
    ) {
        // Column 用于垂直排列文本
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp), // 为 Column 添加内边距
            horizontalAlignment = Alignment.CenterHorizontally // 内容居中
        ) {
            Text(text = "Course Name: $roomId", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp)) // 添加一些间隔
            Text(text = "Course Code: $roomName", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp)) // 添加一些间隔
            Text(text = "Instructor: $owner", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable
fun UserInfoScreen(nickname: String, role: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp), // 设置边距
        verticalArrangement = Arrangement.Top, // 垂直居中
        horizontalAlignment = Alignment.Start // 水平居中
    ) {
        // 显示用户昵称
        Text(
            text = "Nickname: ${nickname}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(16.dp)) // 添加间距
        // 显示用户角色
        Text(
            text = "Role: ${role}",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}