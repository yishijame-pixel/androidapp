// AnniversaryViewModel.kt - 纪念日视图模型
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.Anniversary
import com.example.funlife.repository.AnniversaryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnniversaryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: AnniversaryRepository
    val anniversaries: StateFlow<List<Anniversary>>
    val pinnedAnniversary: StateFlow<Anniversary?>
    
    init {
        val database = AppDatabase.getDatabase(application)
        val anniversaryDao = database.anniversaryDao()
        repository = AnniversaryRepository(anniversaryDao)
        
        anniversaries = repository.allAnniversaries.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        pinnedAnniversary = repository.pinnedAnniversary.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    }
    
    // 添加纪念日
    fun addAnniversary(
        name: String, 
        date: String, 
        imageUri: String? = null,
        type: String = "CUSTOM",
        isYearly: Boolean = true,
        note: String? = null,
        importance: Int = 3
    ) {
        viewModelScope.launch {
            val anniversary = Anniversary(
                name = name, 
                date = date, 
                imageUri = imageUri,
                type = type,
                isYearly = isYearly,
                note = note,
                importance = importance
            )
            repository.insert(anniversary)
        }
    }
    
    // 删除纪念日
    fun deleteAnniversary(anniversary: Anniversary) {
        viewModelScope.launch {
            repository.delete(anniversary)
        }
    }
    
    // 置顶纪念日
    fun pinAnniversary(anniversary: Anniversary) {
        viewModelScope.launch {
            repository.pinAnniversary(anniversary)
        }
    }
    
    // 取消置顶
    fun unpinAnniversary(anniversary: Anniversary) {
        viewModelScope.launch {
            repository.unpinAnniversary(anniversary)
        }
    }
}
