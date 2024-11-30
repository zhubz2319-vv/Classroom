package com.example.iems5725_Classroom

import MainViewModel
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iems5725_Classroom.ui.theme.ContrastAwareReplyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class ChatGroupActivity : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val username = sharedPref.getString("username", "DefaultUser")
        val networkRepository = NetworkRepository()
        val viewModelFactory = MainViewModelFactory(networkRepository)
        val viewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)

        setContent {
            ContrastAwareReplyTheme {
                val roomCode = intent.getStringExtra("room_code")
                val roomName = intent.getStringExtra("room_name")
                viewModel.fetchMessage(roomCode.toString())
                ChatRoomUI(username, roomCode.toString(), roomName.toString())
            }
        }


    }
}


class ChatActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ContrastAwareReplyTheme {
                val id = intent.getStringExtra("id")
                val chatRoomName = intent.getStringExtra("name")
                if (id != null) {
                    ChatRoomUI(id.toInt(), chatRoomName.toString(),viewModel)
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        val id = intent.getStringExtra("id")?.toIntOrNull()
        if (id != null) {
            viewModel.loadMessages(id)
        }
    }

}

class ChatViewModel : ViewModel() {
    var messages by mutableStateOf(buildJsonObject { })
        private set

    fun loadMessages(chatRoomId: Int) {
        viewModelScope.launch {
            messages = withContext(Dispatchers.IO) {
                getResultFromApi(BASE_URL + "get_messages/?chatroom_id=${chatRoomId}")
            }
        }
    }
}


data class MessageItem(val sender:String,
                       val message:String,
                       val time: String,
                       val fileId: Int)

@SuppressLint("SimpleDateFormat")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomUI(userName: String, id: String, chatRoomName: String){

    val context = LocalContext.current
    var inputs by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<MessageItem>()) }
    val coroutineScope = rememberCoroutineScope()
    val viewModel: MainViewModel = viewModel()
    val myUserId = MY_USER_ID
    val myUserName = MY_USER_NAME

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
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                    } ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to MainPage."
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
                    var sendingState by remember { mutableStateOf(buildJsonObject { })}
                    IconButton(
                        onClick = {
                            if(inputs.isNotBlank()){
                                sendingState = buildJsonObject {
                                    put("status", "Not responding")
                                }
                                val message = ChatMessage(id, myUserId, myUserName, inputs)
                                val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())
                                items = items + MessageItem(myUserName, inputs, currentTime, myUserId)
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO){
                                        sendingState = postInfoToApi(message,BASE_URL + "send_message/")
                                    }
                                }
                                inputs = ""
                            }
                        },
                        modifier = Modifier
                            .padding(5.dp)
                            .weight(1f)
                            .align(Alignment.CenterVertically)
                    ) {
                        if (sendingState.values.isNotEmpty()){
                            val status = sendingState["status"]?.jsonPrimitive?.content.toString()
                            if(status == "Not responding"){
                                CircularProgressIndicator(
                                    color = Color.Black,
                                )
                                sendingState = buildJsonObject { }
                            }
                            else if (status == "OK"){
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp)
                                )
                                sendingState = buildJsonObject { }
                            } else {
                                AlertDialog.Builder(context)
                                    .setTitle("Network Error")
                                    .setMessage("Error message: $status")
                                    .setPositiveButton("OK") { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .show()
                                inputs = items.last().message
                                if (items.isNotEmpty()) {
                                    items = items.toMutableList().apply { removeLast() }
                                }
                                sendingState = buildJsonObject { }
                            }
                        }
                        else{
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(30.dp)
                            )
                            sendingState = buildJsonObject { }
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
        ) {
            item {
                if (viewModel.isLoadingMessage.value) {
                    CircularProgressIndicator()
                } else {
                    val dataArray = viewModel.message.value["messages"]?.jsonArray
                    dataArray?.forEach { element ->
                        val dataObject = element.jsonObject
                        val sender = dataObject["sender"]?.jsonPrimitive?.content
                        val message = dataObject["message"]?.jsonPrimitive?.content
                        val time = dataObject["time"]?.jsonPrimitive?.content
                        val fileIdString = dataObject["file_id"]?.jsonPrimitive?.content
                        val fileId: Int? = fileIdString?.toIntOrNull()
                        MessageBox(sender.toString(), message.toString(),time.toString(),userName == sender)
                    }
                }
            }
            items(items) { (userName, messageText, sendingTime, userId) ->
                MessageBox(userName, messageText, sendingTime, userId == myUserId)
            }
        }
    }
}
@Composable
fun MessageBox(userName:String, messageText:String, sendingTime: String, isMine: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = if(isMine) Arrangement.End else Arrangement.Start
    ) {
        Box()
        {
            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd),
                shape = RoundedCornerShape(8.dp),
                color = if(isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = " User: $userName",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.TopStart),
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = messageText,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterStart)
                )
                Text(
                    text = "$sendingTime ",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(start = 10.dp),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

