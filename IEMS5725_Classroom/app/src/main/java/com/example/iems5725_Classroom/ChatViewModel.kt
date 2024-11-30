package com.example.iems5725_Classroom

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.Constants.MessageNotificationKeys.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

class ChatViewModel(private val networkRepository: NetworkRepository, roomCode: String) : ViewModel() {
    private val _messages = MutableLiveData<List<MessageItem>>()
    val messages: LiveData<List<MessageItem>> get() = _messages

    private val webSocketManager = WebSocketManager(roomCode)

    private val _isLoadingMessage = mutableStateOf(false)
    val isLoadingMessage: State<Boolean> = _isLoadingMessage

    private val _messageHistory = mutableStateOf(buildJsonObject { })
    val messageHistory: MutableState<JsonObject> = _messageHistory

    init {

        webSocketManager.connect()

        webSocketManager.onMessageReceived = { message ->
            viewModelScope.launch(Dispatchers.Main) {
                val currentMessages = _messages.value.orEmpty().toMutableList()
                currentMessages.add(message)
                _messages.value = currentMessages
            }
        }
    }

    fun sendMessage(sender: String, message: String) {
        webSocketManager.sendMessage(sender, message)
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.close()
    }
    fun fetchMessage(roomCode: String){
        viewModelScope.launch {
            _isLoadingMessage.value = true
            val data = networkRepository.fetchMessageByRoomCode(roomCode)
            Log.d(TAG, "Fetched date: ${data}")
            _messageHistory.value = data
            _isLoadingMessage.value = false
        }
    }
}