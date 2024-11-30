package com.example.iems5725_Classroom

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
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
import kotlinx.serialization.json.jsonPrimitive

class ChatViewModel(private val networkRepository: NetworkRepository, roomCode: String, application: Application) : AndroidViewModel(application) {
    private val _messages = MutableLiveData<List<JsonObject>>()
    val messages: LiveData<List<JsonObject>> get() = _messages

    private val webSocketManager = WebSocketManager(roomCode)

    private val _isLoadingMessage = mutableStateOf(false)
    val isLoadingMessage: State<Boolean> = _isLoadingMessage

    private val _isMessageSent = MutableLiveData<Boolean>()
    val isMessageSent: LiveData<Boolean> get() = _isMessageSent

    private val _messageHistory = mutableStateOf(buildJsonObject { })
    val messageHistory: MutableState<JsonObject> = _messageHistory

    init {
        val sharedPref = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentUser = sharedPref.getString("username", "DefaultUser") ?: "DefaultUser"
        webSocketManager.onMessageReceived = { jsonObject ->
            _messages.postValue(listOf(jsonObject))
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
            val data = networkRepository.fetchMessageByRoomCode(roomCode)
            Log.d(TAG, "Fetched date: ${data}")
            _messageHistory.value = data
            _isLoadingMessage.value = false
        }
    }
}