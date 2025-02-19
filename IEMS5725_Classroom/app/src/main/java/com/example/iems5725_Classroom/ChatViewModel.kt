package com.example.iems5725_Classroom

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.iems5725_Classroom.network.MessagesResponse
import com.example.iems5725_Classroom.network.RetrofitClient
import com.google.firebase.messaging.Constants.MessageNotificationKeys.TAG
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class ChatViewModel(roomCode: String, application: Application) : AndroidViewModel(application) {
    private val api = RetrofitClient.apiService
    private val _messages = MutableLiveData<List<JsonObject>>()
    val messages: LiveData<List<JsonObject>> get() = _messages

    private val webSocketManager = WebSocketManager(roomCode)

    private val _isLoadingMessage = mutableStateOf(false)
    val isLoadingMessage: State<Boolean> = _isLoadingMessage

    private val _isMessageSent = MutableLiveData<Boolean>()
    val isMessageSent: LiveData<Boolean> get() = _isMessageSent

    private val _messageHistory = MutableLiveData<MessagesResponse>()
    val messageHistory: LiveData<MessagesResponse> get() = _messageHistory

    init {
        val sharedPref = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("username", "DefaultUser") ?: "DefaultUser"
        webSocketManager.onMessageReceived = { jsonObject ->
            val currentMessages = _messages.value.orEmpty()
            val updatedMessages = currentMessages + jsonObject
            _messages.postValue(updatedMessages)
            val sender = jsonObject["sender"]?.jsonPrimitive?.content
            if (sender == currentUser) {
                _isMessageSent.postValue(true)
            }
        }
        webSocketManager.connect()
    }

    fun sendMessage(sender: String, message: String) {
        _isMessageSent.value = false
        webSocketManager.sendMessage(sender, message)
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.close()
    }

    fun fetchMessage(roomCode: String){
        viewModelScope.launch {
            _isLoadingMessage.value = true
            delay(1000)
            val data = api.getMessages(roomCode)
            Log.d(TAG, "Fetched date: $data")
            _messageHistory.postValue(data)
            _isLoadingMessage.value = false
        }
    }
}