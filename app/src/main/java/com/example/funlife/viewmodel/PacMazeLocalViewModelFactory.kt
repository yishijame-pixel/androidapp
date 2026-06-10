package com.example.funlife.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.repository.PacMazeProgressRepository

class PacMazeLocalViewModelFactory(
    private val userId: Long,
    private val database: AppDatabase,
    private val appContext: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PacMazeLocalViewModel::class.java)) {
            return PacMazeLocalViewModel(
                currentUserId = userId,
                progressRepository = PacMazeProgressRepository(database.pacMazeProgressDao()),
                appContext = appContext,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
