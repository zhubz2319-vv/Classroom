package com.example.iems5725_Classroom

import MainViewModel
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iems5725_Classroom.network.CourseInfo
import com.example.iems5725_Classroom.network.CourseInfoResponse
import com.example.iems5725_Classroom.network.FileNameResponse
import com.example.iems5725_Classroom.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CourseActivity : ComponentActivity() {
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
    }

    private suspend fun doGetCourseInfo(courseCode: String, section: String): CourseInfoResponse {
        val api = RetrofitClient.apiService
        return api.getCourseInfo(courseCode, section)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CourseUI(cCode: String, sec: String, courseName: String) {
        val viewModel: MainViewModel = viewModel()
        var selectedTab by remember { mutableStateOf(sec) }
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            viewModel.fetchTabCourseData(cCode,sec)
        }
        Scaffold(
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
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
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
                                    viewModel.fetchTabCourseData(cCode, "announcement")
                                    selectedTab = "announcement"
                                },
                                modifier = Modifier.size(50.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = "Localized description",
                                    tint = if (selectedTab == "announcement") MaterialTheme.colorScheme.primary else Color.Black,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.fetchTabCourseData(cCode, "assignment")
                                    selectedTab = "assignment"
                                },
                                modifier = Modifier.size(50.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Localized description",
                                    tint = if (selectedTab == "assignment") MaterialTheme.colorScheme.primary else Color.Black,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.fetchTabCourseData(cCode, "content")
                                    selectedTab = "content"
                                },
                                modifier = Modifier.size(50.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DateRange,
                                    contentDescription = "Localized description",
                                    tint = if (selectedTab == "content") MaterialTheme.colorScheme.primary else Color.Black,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    },

                    )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            if (viewModel.isLoading2.value) {
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
                    items(viewModel.courseInfo.value?.infos ?: emptyList()) { courseInfo ->
                        NotificationCard(courseInfo = courseInfo)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun NotificationCard(courseInfo: CourseInfo) {
        // 解析时间格式
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = dateFormat.parse(courseInfo.time)
        val formattedTime =
            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(date ?: Date())
        var isFile by remember { mutableStateOf(false) }
        var fileName by remember { mutableStateOf("") }

        LaunchedEffect(courseInfo.file_id) {
            if (courseInfo.file_id?.isNotEmpty()!!) {
                lifecycleScope.launch {
                    val response = doFile(courseInfo.file_id)
                    if (response.status == "success") {
                        fileName = response.file_name
                        isFile = true
                    }
                }
            }
        }

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
                    //.background(Color.White)
            ) {
                // 标题
                Text(
                    text = courseInfo.title,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Spacer(modifier = Modifier.size(8.dp))

                // 发送者和时间
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = courseInfo.by,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.size(8.dp))

                // 通知正文
                Text(
                    text = courseInfo.body,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isFile) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Attachment: $fileName",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = {
                                val downloadUrl = "https://chat.lamitt.com/download_file?file_id=${courseInfo.file_id}"
                                openLink(this@CourseActivity, downloadUrl)
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = "Download Icon",
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun doFile(fileID: String): FileNameResponse {
        val api = RetrofitClient.apiService
        return api.getFileName(fileID)
    }

    private fun openLink(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

}

