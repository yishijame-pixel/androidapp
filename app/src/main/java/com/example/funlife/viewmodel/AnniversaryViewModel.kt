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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AnniversaryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val repository: AnniversaryRepository
    val anniversaries: StateFlow<List<Anniversary>>
    val pinnedAnniversary: StateFlow<Anniversary?>
    
    // 🔥 获取当前用户ID
    private fun getCurrentUserId(): Long {
        val sessionManager = com.example.funlife.utils.UserSessionManager(context)
        return sessionManager.getCurrentUserId().takeIf { it > 0 } ?: 0L
    }
    
    init {
        val database = AppDatabase.getDatabase(application)
        val anniversaryDao = database.anniversaryDao()
        repository = AnniversaryRepository(anniversaryDao)
        
        anniversaries = repository.getAllAnniversaries(getCurrentUserId())
            .catch { e ->
                android.util.Log.e("AnniversaryViewModel", "Error loading anniversaries", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
        
        pinnedAnniversary = repository.getPinnedAnniversary(getCurrentUserId())
            .catch { e ->
                android.util.Log.e("AnniversaryViewModel", "Error loading pinned anniversary", e)
                emit(null)
            }
            .stateIn(
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
            // 🔥 新增：输入验证
            val nameValidation = com.example.funlife.utils.ValidationUtils.validateAnniversaryName(name)
            if (nameValidation is com.example.funlife.utils.ValidationResult.Error) {
                android.util.Log.w("AnniversaryViewModel", "Invalid anniversary name: ${nameValidation.message}")
                return@launch
            }
            
            val anniversary = Anniversary(
                userId = getCurrentUserId(),
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
            repository.pinAnniversary(getCurrentUserId(), anniversary)
        }
    }
    
    // 取消置顶
    fun unpinAnniversary(anniversary: Anniversary) {
        viewModelScope.launch {
            repository.unpinAnniversary(anniversary)
        }
    }
}
