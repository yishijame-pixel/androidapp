package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PacMazeOnlineViewModelFactory(
    private val app: Application,
    private val userId: Long,
    private val roomId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PacMazeOnlineViewModel::class.java)) {
            return PacMazeOnlineViewModel(app, userId, roomId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
