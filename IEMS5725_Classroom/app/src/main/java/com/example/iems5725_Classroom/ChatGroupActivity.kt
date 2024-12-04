package com.example.iems5725_Classroom

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.iems5725_Classroom.ui.theme.ContrastAwareReplyTheme
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.example.iems5725_Classroom.network.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File


class ChatGroupActivity : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "DefaultUser")
        val networkRepository = NetworkRepository()

        setContent {
            ContrastAwareReplyTheme {
                val roomCode = intent.getStringExtra("room_code")
                val roomName = intent.getStringExtra("room_name")
                val viewModelFactory = ChatViewModelFactory(networkRepository, roomCode.toString(), application)
                val viewModel = ViewModelProvider(this, viewModelFactory).get(ChatViewModel::class.java)
                viewModel.fetchMessage(roomCode.toString())
                ChatRoomUI(username.toString(), roomCode.toString(), roomName.toString())
            }
        }
    }
    /*
    override fun onResume() {
        super.onResume()
        val roomCode = intent.getStringExtra("room_code")
        val networkRepository = NetworkRepository()
        val viewModelFactory = ChatViewModelFactory(networkRepository,roomCode.toString(), application)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(ChatViewModel::class.java)
        if (roomCode != null) {
            viewModel.fetchMessage(roomCode.toString())
        }
    }
    */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ChatRoomUI(userName: String, id: String, chatRoomName: String){
        val listState = rememberLazyListState()
        val context = LocalContext.current
        var inputs by remember { mutableStateOf("") }
        val viewModel: ChatViewModel = viewModel()
        val isMessageSent = viewModel.isMessageSent.observeAsState(initial = true).value
        val messages by viewModel.messages.observeAsState(emptyList())
        Log.d("Messages", "Messages updated: $messages")
        val messagesLength = viewModel.messageHistory.value["messages"]?.jsonArray?.size ?: 0
        var expanded by remember { mutableStateOf(false) }
        var isDialogOpen by remember { mutableStateOf(false) }
        var showUploadDialog by remember { mutableStateOf(false) }
        var sendFileId by remember { mutableStateOf("") }
        var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
        var isUploading by remember { mutableStateOf(false) }

        LaunchedEffect(messages.size) {
            listState.animateScrollToItem(messages.size + messagesLength)
        }

        val getFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                selectedFileUri = uri
            }
        }

        fun getFileNameFromUri(): String? {
            var fileName: String? = null
            val cursor = context.contentResolver.query(selectedFileUri!!, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst()) {
                    fileName = it.getString(nameIndex)
                }
            }
            return fileName
        }

        fun getFileFromUri(): File? {
            val fileName = getFileNameFromUri() ?: "Default_Name"
            val inputStream = context.contentResolver.openInputStream(selectedFileUri!!)
            val tempFile = File(context.cacheDir, fileName)

            inputStream?.let { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            return if (tempFile.exists()) tempFile else null
        }

        fun createMultipartBodyPart(file: File, parameterName: String): MultipartBody.Part {
            val requestBody: RequestBody = file
                .asRequestBody("application/octet-stream".toMediaTypeOrNull())

            return MultipartBody.Part.createFormData(parameterName, file.name, requestBody)
        }

        fun prepareFileForUpload(): MultipartBody.Part? {
            val file = getFileFromUri()
            return file?.let {
                createMultipartBodyPart(it, "file")
            }
        }

        fun doUploadFile() {
            val multipartBodyPart = prepareFileForUpload()

            if (multipartBodyPart != null) {
                lifecycleScope.launch {
                    isUploading = true
                    val api = RetrofitClient.apiService
                    val response = api.uploadFile(multipartBodyPart)
                    if (response.status == "success") {
                        selectedFileUri = null
                        sendFileId = response.file_id
                        Toast.makeText(context, "Upload successfully", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        println("FILE UPLOAD UNSUCCESSFULLY!")
                    }
                    isUploading = false
                }
            }
        }

        if (selectedFileUri != null && !isUploading) {
            doUploadFile()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    colors = topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(chatRoomName)
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            finish()
                        } ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to MainPage."
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                expanded = !expanded // 切换菜单显示状态
                            }
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }, // 关闭菜单

                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    // 处理点击事件
                                    expanded = false
                                    isDialogOpen = true
                                },
                                text = {
                                    Row {
                                        Icon(
                                            painter = painterResource(R.drawable.person_add),
                                            contentDescription = "Invite user."
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Invite user",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold, // 加粗文本
                                                color = MaterialTheme.colorScheme.onSurface // 文本颜色
                                            )
                                        )
                                    }
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp), // 控制分隔线的边距
                                thickness = 1.dp, // 分隔线的厚度
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) // 分隔线的颜色
                            )
                            DropdownMenuItem(
                                onClick = {
                                    // 处理点击事件
                                    expanded = false
                                    Toast.makeText(context, "Option 1 clicked", Toast.LENGTH_SHORT).show()
                                },
                                text = {
                                    Text(
                                        text = "Option 1",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold, // 加粗文本
                                            color = MaterialTheme.colorScheme.onSurface // 文本颜色
                                        )
                                    )
                                }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {

                    Row (
                        modifier = Modifier
                            .fillMaxWidth()

                    ){
                        TextField(
                            value = inputs,
                            onValueChange = {
                                inputs = it
                            },
                            singleLine = false,
                            placeholder = {Text(text = "Message...")},
                            shape = CircleShape,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = Color.White,
                                disabledContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                            ),
                            modifier = Modifier
                                .padding(5.dp)
                                .weight(5f)
                        )
                        IconButton(
                            onClick = {
                                if(inputs.isNotBlank()){
                                    if (sendFileId.isNotEmpty()) {
                                        lifecycleScope.launch {
                                            val response = doSendFile(id, userName, inputs, sendFileId)
                                            if (response.status == "success") {
                                                inputs = ""
                                                sendFileId = ""
                                            }
                                            else {
                                                Toast.makeText(
                                                    context,
                                                    "Something wrong. Please try again later.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    } else {
                                        viewModel.sendMessage(sender = userName, message = inputs)
                                        inputs = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .padding(5.dp)
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                        ) {
                            if (isMessageSent) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp)
                                )
                            } else {
                                CircularProgressIndicator()
                            }
                        }
                        IconButton(
                            onClick = {
                                getFile.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier
                                .padding(5.dp)
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.upload),
                                contentDescription = "Upload File",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            },


        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize()
                    .padding(innerPadding)
            ){

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 5.dp, end = 5.dp),
                    state = listState,
                ) {
                    item {
                        if (viewModel.isLoadingMessage.value) {
                            CircularProgressIndicator()
                        } else {
                            val dataArray = viewModel.messageHistory.value["messages"]?.jsonArray
                            dataArray?.forEach { element ->
                                val dataObject = element.jsonObject
                                val sender = dataObject["sender"]?.jsonPrimitive?.content ?: "Unknown"
                                val message = dataObject["message"]?.jsonPrimitive?.content ?: ""
                                val time = dataObject["time"]?.jsonPrimitive?.content ?: "0000-00-00"
                                val fileIdString = dataObject["file_id"]?.jsonPrimitive?.content
                                MessageBox(message, sender, time, sender == userName, fileIdString)
                            }
                        }
                    }
                    items(messages) { item ->
                        val sender = item["sender"]?.jsonPrimitive?.content ?: "Unknown"
                        val message = item["message"]?.jsonPrimitive?.content ?: ""
                        val time = item["time"]?.jsonPrimitive?.content ?: "0000-00-00"
                        val fileIdString = item["file_id"]?.jsonPrimitive?.content
                        Log.d("TAG", "items updated: ${sender}:${message}:${time}")
                        MessageBox(message, sender, time, sender == userName, fileIdString)
                    }
                }
            }


        }

        if (isDialogOpen) {
            var newUser by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = {
                    isDialogOpen = false
                },
                title = { Text("Invite new member") },
                text = {
                    Column {
                        TextField(
                            value = newUser,
                            onValueChange = { newUser = it },
                            label = { Text("Username") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            lifecycleScope.launch {
                                val response = doInvite(id, newUser)
                                if (response.status == "success") {
                                    Toast.makeText(
                                        context,
                                        "$newUser joined the group",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isDialogOpen = false
                                } else {
                                    Toast.makeText(
                                        context,
                                        response.message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    ) {
                        Text("Submit")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            isDialogOpen = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showUploadDialog) {
            AlertDialog(
                onDismissRequest = {
                    showUploadDialog = false
                },
                title = { Text("Attach File") },
                text = {
                    Column {
                        TextField(
                            value = sendFileId,
                            onValueChange = { sendFileId = it },
                            label = { Text("File ID") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showUploadDialog = false
                        }
                    ) {
                        Text("Submit")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showUploadDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    fun MessageBox(message: String, sender: String, timestamp: String, isUser: Boolean, fileID: String?) {
        val backgroundColor = if (isUser) Color(0xFFD1F5FF) else Color(0xFFF1F1F1)
        val alignment = if (isUser) Arrangement.End else Arrangement.Start
        var fileName by remember { mutableStateOf("") }
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        val getImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = uri
            }
        }
        val profilePicUrl by remember { mutableStateOf(sharedPref.getString("profile_pic_url", "")!!) }

        LaunchedEffect(fileID) {
            if (fileID != null && fileID != "null") {
                lifecycleScope.launch {
                    val response = doFile(fileID)
                    Log.d("FILE", "RESPONSE HERE: $response")
                    if (response.status == "success") {
                        fileName = response.file_name
                    }
                    else {
                        Log.d("FILE", "RETRIEVE FILE NAME ERROR")
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = alignment
        ) {

            if (!isUser){
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePicUrl.isEmpty()) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.White
                        )
                    }
                    else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .background(backgroundColor, shape = MaterialTheme.shapes.medium)
                    .padding(12.dp)
                    .widthIn(max = 250.dp)
            ) {
                Text(text = message)

                if (fileName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FilePreview(fileID!!, fileName)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = sender,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            if (isUser){
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                ) {
                    if (profilePicUrl.isEmpty()) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.White
                        )
                    } else {
                        GlideImage(
                            model = profilePicUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun FilePreview(fileID: String, fileName: String) {
        val context = LocalContext.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = {
                        val downloadUrl = "https://chat.lamitt.com/download_file?file_id=$fileID"
                        openLink(context, downloadUrl)
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

    private fun openLink(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    private suspend fun doFile(fileID: String): FileNameResponse {
        val api = RetrofitClient.apiService
        return api.getFileName(fileID)
    }

    private suspend fun doInvite(roomCode: String, userName: String): StandardResponse {
        val api = RetrofitClient.apiService
        return api.inviteUser(InviteRequest(roomCode, userName, "add"))
    }

    private suspend fun doSendFile(roomCode: String, userName: String, message: String, fileID: String): StandardResponse {
        val api = RetrofitClient.apiService
        return api.sendMessage(MessageRequest(roomCode, userName, message, fileID))
    }

}

