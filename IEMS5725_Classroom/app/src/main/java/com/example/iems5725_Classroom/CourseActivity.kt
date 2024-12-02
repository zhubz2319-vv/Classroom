package com.example.iems5725_Classroom

import MainViewModel
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.iems5725_Classroom.ui.theme.ContrastAwareReplyTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.*

class CourseActivity : ComponentActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val networkRepository = NetworkRepository()
        val viewModelFactory = MainViewModelFactory(networkRepository)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)
        setContent {
            ContrastAwareReplyTheme {
                val courseCode = intent.getStringExtra("course_code")
                val section = intent.getStringExtra("section")
                val courseName = intent.getStringExtra("course_name")
                CourseUI(courseCode.toString(), section.toString(), courseName.toString())
            }
        }
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
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CourseUI(cCode: String, sec: String, courseName: String) {
        val viewModel: MainViewModel = viewModel()
        var selectedTab by remember { mutableStateOf(sec) }
        val context = LocalContext.current

        Scaffold (
            topBar = {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(courseName)
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            finish()
                        } ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            },
            /*bottomBar = {
                BottomAppBar(
                    actions = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.fetchTabCourseData(cCode, "announcement")
                                    selectedTab = "announcement"},
                                modifier = Modifier.size(50.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.announcement),
                                    contentDescription = "Localized description",
                                    tint = if (selectedTab == "announcement") MaterialTheme.colorScheme.primary else Color.Black,
                                    modifier = Modifier.clip(RectangleShape)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.fetchTabCourseData(cCode, "assignment")
                                    selectedTab = "assignment"},
                                modifier = Modifier.size(50.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.assignment),
                                    contentDescription = "Localized description",
                                    tint = if (selectedTab == "assignment") MaterialTheme.colorScheme.primary else Color.Black,
                                    modifier = Modifier.clip(RectangleShape)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.fetchTabCourseData(cCode, "content")
                                    selectedTab = "content"},
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.content),
                                    contentDescription = "Localized description",
                                    tint = if (selectedTab == "content") MaterialTheme.colorScheme.scrim else Color.Black,
                                    modifier = Modifier.clip(RectangleShape)
                                )
                            }
                        }
                    },

                    )
            },*/
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            if (viewModel.isLoading2.value){
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding) // 设置外部间距
                ) {
//        {
//            "course_code": "A01",
//            "section": "announcement",
//            "by": "tony",
//            "time": "2024-11-29 00:00:12",
//            "title": "Important!",
//            "body": "Remember to bring your homework tomorrow.",
//            "file_id": ""
//        }
                    item {
                        val dataArray = viewModel.courseData.value["infos"]?.jsonArray
                        dataArray?.forEach { element ->
                            val dataObject = element.jsonObject
                            val courseCode =
                                dataObject["course_code"]?.jsonPrimitive?.content.toString()
                            val section = dataObject["section"]?.jsonPrimitive?.content.toString()
                            val by = dataObject["by"]?.jsonPrimitive?.content.toString()
                            val time = dataObject["time"]?.jsonPrimitive?.content.toString()
                            val title = dataObject["title"]?.jsonPrimitive?.content.toString()
                            val body = dataObject["body"]?.jsonPrimitive?.content.toString()
                            val fileId = dataObject["file_id"]?.jsonPrimitive?.content.toString()
                            val notification = Notification(
                                courseCode = courseCode,
                                section = section,
                                by = by,
                                time = time,
                                title = title,
                                body = body,
                                fileId = fileId
                            )
                            NotificationCard(notification = notification)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                    }
                }
            }
        }
    }

    @Composable
    fun NotificationCard(notification: Notification) {
        // 解析时间格式
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = dateFormat.parse(notification.time)
        val formattedTime = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date ?: Date())

        // 卡片布局
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color.White)
            ) {
                // 课程代码和类型
                Text(
                    text = "Course: ${notification.courseCode} | ${notification.section}",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 标题
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 发送者和时间
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "By: ${notification.by}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // 通知正文
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 文件附件（如果有）
                if (notification.fileId.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Attachment: ${notification.fileId}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

}

data class Notification(
    val courseCode: String,
    val section: String,
    val by: String,
    val time: String,
    val title: String,
    val body: String,
    val fileId: String
)



/*
@Preview(showBackground = true)
@Composable
fun NotificationCardPreview() {
    NotificationCard(
        notification = Notification(
            courseCode = "A01",
            section = "announcement",
            by = "Tony",
            time = "2024-11-29 00:00:12",
            title = "Important!",
            body = "Remember to bring your homework tomorrow.",
            fileId = "file123"
        )
    )
}
 */