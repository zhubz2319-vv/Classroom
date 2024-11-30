package com.example.iems5725_Classroom

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ChatViewModelFactory(
    private val networkRepository: NetworkRepository,
    private val roomCode: String,
    private val application: Application

) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(networkRepository, roomCode, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}