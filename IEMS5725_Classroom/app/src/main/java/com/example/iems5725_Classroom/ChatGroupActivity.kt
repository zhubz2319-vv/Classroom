package com.example.iems5725_Classroom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iems5725_Classroom.ui.theme.ContrastAwareReplyTheme
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.example.iems5725_Classroom.network.*
import kotlinx.coroutines.launch


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
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var isDialogOpen by remember { mutableStateOf(false) }
        val getImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = uri
            }
        }
        LaunchedEffect(messages.size) {
            listState.animateScrollToItem(messages.size + messagesLength)
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
                    actions ={
                        IconButton(
                            onClick = {
                                isDialogOpen = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Invite user."
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
                                    viewModel.sendMessage(sender = userName, message = inputs)
                                    inputs = ""
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
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth(),
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
    }

    @Composable
    fun MessageBox(message: String, sender: String, timestamp: String, isUser: Boolean, fileID: String?) {
        val backgroundColor = if (isUser) Color(0xFFD1F5FF) else Color(0xFFF1F1F1)
        val alignment = if (isUser) Arrangement.End else Arrangement.Start
        var fileName by remember { mutableStateOf("") }

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
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(
                        onClick = {
                            val downloadUrl = "https://chat.lamitt.com/download_file?file_id=$fileID"
                            openLink(context, downloadUrl)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Download Icon"
                        )
                    }
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

}

