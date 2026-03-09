// MoodViewModel.kt
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.MoodEntry
import com.example.funlife.repository.MoodRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class MoodViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MoodRepository
    val moods: StateFlow<List<MoodEntry>>
    
    init {
        val database = AppDatabase.getDatabase(application)
        repository = MoodRepository(database.moodDao())
        moods = repository.allMoods.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
    
    fun addMood(mood: String, level: Int, note: String) {
        viewModelScope.launch {
            val entry = MoodEntry(
                date = LocalDate.now().toString(),
                mood = mood,
                moodLevel = level,
                note = note,
                timestamp = LocalDateTime.now().toString()
            )
            repository.insertMood(entry)
        }
    }
    
    fun deleteMood(mood: MoodEntry) {
        viewModelScope.launch {
            repository.deleteMood(mood)
        }
    }
}
