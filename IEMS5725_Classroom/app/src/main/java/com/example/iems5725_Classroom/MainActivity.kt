package com.example.iems5725_Classroom

import MainViewModel
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.FullyDrawnReporterOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.iems5725_Classroom.ui.theme.ContrastAwareReplyTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import io.ktor.client.*
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val BASE_URL = "https://chat.lamitt.com/"


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        FirebaseApp.initializeApp(this)
        createNotificationChannel()
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "DefaultUser")
        val networkRepository = NetworkRepository()
        val viewModelFactory = MainViewModelFactory(networkRepository)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)
        viewModel.fetchTabData(0, "Courses", username.toString())
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
            ContrastAwareReplyTheme {
                ScaffoldUI(username.toString())
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

//{"room_code":"G01","room_name":"A New Group","username":"test"}
@Serializable
data class ChatGroup(val room_code: String,
                     val room_name: String,
                     val username: String)

@Serializable
data class TokenMessage(val token: String,
                        val user_id: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldUI(userName: String) {
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

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
                actions = {
                    CreateRoomButton(userName)
                }
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
                                viewModel.fetchTabData(0, "Courses", userName)
                                selectedTab = 0
                                      },
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Localized description",
                                tint = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Black,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        IconButton(
                            onClick = { viewModel.fetchTabData(1, "Chat Groups", userName)
                                selectedTab = 1},
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Localized description",
                                tint = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Black,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        IconButton(
                            onClick = { viewModel.fetchTabData(2, "My Profile", userName)
                                selectedTab = 2},
                            modifier = Modifier
                                .size(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Localized description",
                                tint = if (selectedTab == 2) MaterialTheme.colorScheme.scrim else Color.Black,
                                modifier = Modifier.fillMaxSize()
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
            if (viewModel.isLoading.value)
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
fun CourseItem(courseName: String, courseCode: String, instructor: String, students: List<String>) {
//    val context = LocalContext.current
    val isExpanded = remember { mutableStateOf(false) }
    val density = LocalDensity.current
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Button(
            onClick = {
                isExpanded.value = !isExpanded.value
            },
            shape = MaterialTheme.shapes.extraSmall
        ) {
            // Column 用于垂直排列文本
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp), // 为 Column 添加内边距
                horizontalAlignment = Alignment.CenterHorizontally // 内容居中
            ) {
                Text(
                    text = courseName,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp)) // 添加一些间隔

                Text(
                    text = "Code: $courseCode",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(8.dp)) // 添加一些间隔

                Text(
                    text = "Instructor: $instructor",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        AnimatedVisibility(
            visible = isExpanded.value,
            enter = slideInVertically {
                // Slide in from 40 dp from the top.
                with(density) { -40.dp.roundToPx() }
            } + expandVertically(
                // Expand from the top.
                expandFrom = Alignment.Top
            ) + fadeIn(
                // Fade in with the initial alpha of 0.3f.
                initialAlpha = 0.3f
            ),
            exit = slideOutVertically() + shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.LightGray
            ) {
                CourseOption(courseName, courseCode)
            }
        }
    }

}
@Composable
fun ChatRoomItem(roomId: String, roomName: String, owner: String){
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(context, ChatGroupActivity::class.java).apply {
                putExtra("room_code", roomId)
                putExtra("room_name", roomName)
            }
            context.startActivity(intent)
        },
        modifier = Modifier
            .fillMaxWidth() // 使按钮占满宽度
            .padding(8.dp) // 为按钮添加内边距
    ) {
        // Column 用于垂直排列文本
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // 为 Column 添加内边距
            horizontalAlignment = Alignment.CenterHorizontally // 内容居中
        ) {
            Text(text = roomName,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
                )
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

@Composable
fun CourseOption(courseName: String, courseCode: String){
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally // 内容居中
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    val intent = Intent(context, CourseActivity::class.java).apply {
                        putExtra("course_code", courseCode)
                        putExtra("section", "announcement")
                        putExtra("course_name", courseName)
                    }
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(ZeroCornerSize),
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(text = "Announcement", style = MaterialTheme.typography.bodyLarge)
            }
            Surface(
                color = Color.White,
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
            ) {  }
            Button(
                onClick = {

                },
                shape = RoundedCornerShape(ZeroCornerSize),
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(text = "Assignment", style = MaterialTheme.typography.bodyLarge)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                onClick = {

                },
                shape = RoundedCornerShape(ZeroCornerSize),
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(text = "Content", style = MaterialTheme.typography.bodyLarge)
            }
            Surface(
                color = Color.White,
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
            ) {  }
            Button(
                onClick = {

                },
                shape = RoundedCornerShape(ZeroCornerSize),
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(text = "Chat Group", style = MaterialTheme.typography.bodyLarge)
            }

        }
    }
}

@Composable
fun CreateRoomButton(userName: String) {
    var isDialogOpen by remember { mutableStateOf(false) }
    val viewModel: MainViewModel = viewModel()
    CreateRoomDialog(
        isDialogOpen = isDialogOpen,
        onCreate = { roomCode, roomName ->
            val request = ChatGroup(room_code = roomCode, room_name = roomName, username = userName)
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    viewModel.postForCreateChat(request)
                    viewModel.fetchTabData(1, "Chat Groups", userName)
                    isDialogOpen = false
                } catch (e: Exception) {
                    Log.e("CreateRoom", "Error creating room: ${e.message}")
                }
            }
        },
        onCancel = {
            isDialogOpen = false
        }
    )

    IconButton(
        onClick = {
            when (viewModel.selectedTab.value) {
                1 -> {
                    isDialogOpen = true
                }
            }
        }
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(30.dp)
        )
    }

}

@Composable
fun CreateRoomDialog(
    onCreate: (String, String) -> Unit,
    onCancel: () -> Unit,
    isDialogOpen: Boolean,
) {
    var roomCode by remember { mutableStateOf("") }
    var roomName by remember { mutableStateOf("") }
    if (isDialogOpen) {
        AlertDialog(
            onDismissRequest = { onCancel() },
            title = { Text("Create a New Room") },
            text = {
                Column {
                    TextField(
                        value = roomCode,
                        onValueChange = { roomCode = it },
                        label = { Text("Room Code") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    TextField(
                        value = roomName,
                        onValueChange = { roomName = it },
                        label = { Text("Room Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreate(roomCode, roomName)
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { onCancel() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview
@Composable
fun PreviewCourseItem() {
    MaterialTheme {
        CourseItem(courseName = "Intro to Compose", courseCode = "CS101", instructor = "John Doe", students = listOf("Item1", "Item2", "Item3"))
    }
}