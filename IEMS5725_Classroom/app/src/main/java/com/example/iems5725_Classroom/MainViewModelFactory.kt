package com.example.iems5725_Classroom

import MainViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainViewModelFactory(private val networkRepository: NetworkRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(networkRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
