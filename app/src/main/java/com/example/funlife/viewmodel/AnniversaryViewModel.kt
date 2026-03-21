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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AnniversaryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val repository: AnniversaryRepository
    
    // 筛选和排序状态
    private val _selectedType = MutableStateFlow<String?>(null)
    val selectedType: StateFlow<String?> = _selectedType
    
    private val _sortOrder = MutableStateFlow(SortOrder.DEFAULT)
    val sortOrder: StateFlow<SortOrder> = _sortOrder
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
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
        
        // 组合筛选、搜索和排序
        anniversaries = combine(
            _selectedType,
            _sortOrder,
            _searchQuery
        ) { type, sort, query ->
            Triple(type, sort, query)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Triple(null, SortOrder.DEFAULT, "")
        ).let { filterState ->
            combine(
                filterState,
                repository.getAllAnniversaries(getCurrentUserId())
            ) { (type, sort, query), allAnniversaries ->
                var filtered = allAnniversaries
                
                // 类型筛选
                if (type != null) {
                    filtered = filtered.filter { it.type == type }
                }
                
                // 搜索
                if (query.isNotBlank()) {
                    filtered = filtered.filter { 
                        it.name.contains(query, ignoreCase = true) || 
                        it.note?.contains(query, ignoreCase = true) == true
                    }
                }
                
                // 排序
                when (sort) {
                    SortOrder.DEFAULT -> filtered.sortedWith(
                        compareByDescending<Anniversary> { it.isPinned }
                            .thenBy { it.getDaysRemaining() }
                    )
                    SortOrder.NEAREST -> filtered.sortedBy { it.getDaysRemaining() }
                    SortOrder.FARTHEST -> filtered.sortedByDescending { it.getDaysRemaining() }
                    SortOrder.IMPORTANCE -> filtered.sortedWith(
                        compareByDescending<Anniversary> { it.importance }
                            .thenBy { it.getDaysRemaining() }
                    )
                    SortOrder.NAME -> filtered.sortedBy { it.name }
                    SortOrder.CUSTOM -> filtered.sortedBy { it.customOrder }
                }
            }.catch { e ->
                android.util.Log.e("AnniversaryViewModel", "Error loading anniversaries", e)
                emit(emptyList())
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
        }
        
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
        importance: Int = 3,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d("AnniversaryViewModel", "=== 开始添加纪念日 ===")
                android.util.Log.d("AnniversaryViewModel", "name: $name")
                android.util.Log.d("AnniversaryViewModel", "date: $date")
                android.util.Log.d("AnniversaryViewModel", "type: $type")
                android.util.Log.d("AnniversaryViewModel", "imageUri: $imageUri")
                android.util.Log.d("AnniversaryViewModel", "isYearly: $isYearly")
                android.util.Log.d("AnniversaryViewModel", "note: $note")
                android.util.Log.d("AnniversaryViewModel", "importance: $importance")
                
                // 检查用户ID
                val userId = getCurrentUserId()
                android.util.Log.d("AnniversaryViewModel", "userId: $userId")
                
                if (userId == 0L) {
                    val error = "用户ID为0，无法添加纪念日。请先登录。"
                    android.util.Log.e("AnniversaryViewModel", "❌ $error")
                    onError(error)
                    return@launch
                }
                
                // 输入验证
                if (name.isBlank()) {
                    val error = "纪念日名称不能为空"
                    android.util.Log.w("AnniversaryViewModel", "❌ $error")
                    onError(error)
                    return@launch
                }
                
                val nameValidation = com.example.funlife.utils.ValidationUtils.validateAnniversaryName(name)
                if (nameValidation is com.example.funlife.utils.ValidationResult.Error) {
                    android.util.Log.w("AnniversaryViewModel", "❌ 验证失败: ${nameValidation.message}")
                    onError(nameValidation.message)
                    return@launch
                }
                
                // 复制图片到应用私有目录
                val savedImageUri = if (imageUri != null) {
                    android.util.Log.d("AnniversaryViewModel", "复制图片到应用目录...")
                    val uri = android.net.Uri.parse(imageUri)
                    com.example.funlife.utils.ImageHelper.copyImageToAppStorage(context, uri)
                } else {
                    null
                }
                
                android.util.Log.d("AnniversaryViewModel", "保存的图片URI: $savedImageUri")
                
                val anniversary = Anniversary(
                    userId = userId,
                    name = name, 
                    date = date, 
                    imageUri = savedImageUri,
                    type = type,
                    isYearly = isYearly,
                    note = note,
                    importance = importance
                )
                
                android.util.Log.d("AnniversaryViewModel", "准备插入数据库: $anniversary")
                repository.insert(anniversary)
                android.util.Log.d("AnniversaryViewModel", "✅ 纪念日添加成功")
                
                // 延迟一下确保数据库操作完成
                kotlinx.coroutines.delay(100)
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("AnniversaryViewModel", "❌ 添加纪念日失败", e)
                android.util.Log.e("AnniversaryViewModel", "错误详情: ${e.message}")
                android.util.Log.e("AnniversaryViewModel", "错误堆栈: ${e.stackTraceToString()}")
                onError("添加失败: ${e.message}")
            }
        }
    }
    
    // 删除纪念日
    fun deleteAnniversary(anniversary: Anniversary) {
        viewModelScope.launch {
            // 删除关联的图片
            if (anniversary.imageUri != null) {
                android.util.Log.d("AnniversaryViewModel", "删除纪念日图片: ${anniversary.imageUri}")
                com.example.funlife.utils.ImageHelper.deleteImage(anniversary.imageUri)
            }
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
    
    // 更新纪念日
    fun updateAnniversary(
        anniversary: Anniversary,
        name: String,
        date: String,
        imageUri: String? = null,
        type: String = "CUSTOM",
        isYearly: Boolean = true,
        note: String? = null,
        importance: Int = 3
    ) {
        viewModelScope.launch {
            // 输入验证
            val nameValidation = com.example.funlife.utils.ValidationUtils.validateAnniversaryName(name)
            if (nameValidation is com.example.funlife.utils.ValidationResult.Error) {
                android.util.Log.w("AnniversaryViewModel", "Invalid anniversary name: ${nameValidation.message}")
                return@launch
            }
            
            // 处理图片：如果是新图片URI（content://），需要复制到应用目录
            val savedImageUri = if (imageUri != null && imageUri.startsWith("content://")) {
                android.util.Log.d("AnniversaryViewModel", "检测到新图片，复制到应用目录...")
                val uri = android.net.Uri.parse(imageUri)
                com.example.funlife.utils.ImageHelper.copyImageToAppStorage(context, uri)
            } else {
                // 如果是已保存的file://或null，直接使用
                imageUri
            }
            
            // 如果旧图片存在且与新图片不同，删除旧图片
            if (anniversary.imageUri != null && anniversary.imageUri != savedImageUri) {
                android.util.Log.d("AnniversaryViewModel", "删除旧图片: ${anniversary.imageUri}")
                com.example.funlife.utils.ImageHelper.deleteImage(anniversary.imageUri)
            }
            
            // 确保 userId 不变，保持数据隔离
            val updatedAnniversary = anniversary.copy(
                name = name,
                date = date,
                imageUri = savedImageUri,
                type = type,
                isYearly = isYearly,
                note = note,
                importance = importance
            )
            repository.update(updatedAnniversary)
        }
    }
    
    // 筛选和排序方法
    fun setTypeFilter(type: String?) {
        _selectedType.value = type
    }
    
    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun clearFilters() {
        _selectedType.value = null
        _sortOrder.value = SortOrder.DEFAULT
        _searchQuery.value = ""
    }
    
    // 统计分析方法
    fun getMonthlyStatistics(): StateFlow<Map<String, Int>> {
        return anniversaries.map { list ->
            val currentMonth = java.time.LocalDate.now().monthValue
            list.filter { anniversary ->
                val date = java.time.LocalDate.parse(anniversary.date)
                date.monthValue == currentMonth
            }.groupBy { it.type }
                .mapValues { it.value.size }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    }
    
    fun getYearlyStatistics(): StateFlow<Map<String, Int>> {
        return anniversaries.map { list ->
            list.groupBy { it.type }
                .mapValues { it.value.size }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    }
    
    fun getTopImportantAnniversaries(limit: Int = 5): StateFlow<List<Anniversary>> {
        return anniversaries.map { list ->
            list.sortedWith(
                compareByDescending<Anniversary> { it.importance }
                    .thenBy { it.getDaysRemaining() }
            ).take(limit)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
    
    fun getUpcomingAnniversaries(days: Int = 30): StateFlow<List<Anniversary>> {
        return anniversaries.map { list ->
            list.filter { it.getDaysRemaining() in 0..days.toLong() }
                .sortedBy { it.getDaysRemaining() }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
    
    // 更新排序
    fun updateAnniversariesOrder(reorderedList: List<Anniversary>) {
        viewModelScope.launch {
            val updatedList = reorderedList.mapIndexed { index, anniversary ->
                anniversary.copy(customOrder = index)
            }
            repository.updateAnniversariesOrder(updatedList)
        }
    }
}

// 排序选项
enum class SortOrder(val displayName: String) {
    DEFAULT("默认"),
    NEAREST("最近"),
    FARTHEST("最远"),
    IMPORTANCE("重要程度"),
    NAME("名称"),
    CUSTOM("自定义排序")
}
