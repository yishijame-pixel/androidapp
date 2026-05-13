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
import com.example.funlife.data.model.AnniversaryViewMode

class AnniversaryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val repository: AnniversaryRepository
    
    // 视图模式状态
    private val _viewMode = MutableStateFlow(AnniversaryViewMode.LIST)
    val viewMode: StateFlow<AnniversaryViewMode> = _viewMode
    
    // 筛选和排序状态
    private val _selectedType = MutableStateFlow<String?>(null)
    val selectedType: StateFlow<String?> = _selectedType
    
    private val _sortOrder = MutableStateFlow(SortOrder.DEFAULT)
    val sortOrder: StateFlow<SortOrder> = _sortOrder
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    // 🔥 改用 MutableStateFlow 直接管理数据，完全控制更新时机
    private val _anniversaries = MutableStateFlow<List<Anniversary>>(emptyList())
    val anniversaries: StateFlow<List<Anniversary>> = _anniversaries
    
    private val _pinnedAnniversary = MutableStateFlow<Anniversary?>(null)
    val pinnedAnniversary: StateFlow<Anniversary?> = _pinnedAnniversary
    
    // 🔥 改用实时获取userId，而不是在init时缓存
    private fun getCurrentUserId(): Long {
        val sessionManager = com.example.funlife.utils.UserSessionManager(context)
        val userId = sessionManager.getCurrentUserId().takeIf { it > 0 } ?: 0L
        android.util.Log.d("AnniversaryViewModel", "实时获取userId: $userId")
        return userId
    }
    
    init {
        val database = AppDatabase.getDatabase(application)
        val anniversaryDao = database.anniversaryDao()
        repository = AnniversaryRepository(anniversaryDao)
        
        // 🔥 启动时立即加载数据
        loadAnniversaries()
        
        // 🔥 监听筛选和排序变化，重新加载
        viewModelScope.launch {
            combine(_selectedType, _sortOrder, _searchQuery) { type, sort, query ->
                Triple(type, sort, query)
            }.collect {
                loadAnniversaries()
            }
        }
    }
    
    // 🔥 新增：主动加载纪念日数据的方法 - 每次都实时获取userId
    private fun loadAnniversaries() {
        viewModelScope.launch {
            try {
                // 🔥 每次加载都重新获取userId，确保使用最新的登录用户
                val userId = getCurrentUserId()
                android.util.Log.d("AnniversaryViewModel", "=== 加载纪念日数据 ===")
                android.util.Log.d("AnniversaryViewModel", "当前userId: $userId")
                
                if (userId == 0L) {
                    android.util.Log.w("AnniversaryViewModel", "用户未登录，清空数据")
                    _anniversaries.value = emptyList()
                    _pinnedAnniversary.value = null
                    return@launch
                }
                
                // 🔥 直接从数据库查询最新数据
                repository.getAllAnniversaries(userId).collect { allAnniversaries ->
                    android.util.Log.d("AnniversaryViewModel", "从数据库获取到 ${allAnniversaries.size} 条纪念日")
                    
                    var filtered = allAnniversaries
                    
                    // 类型筛选
                    val type = _selectedType.value
                    if (type != null) {
                        filtered = filtered.filter { it.type == type }
                    }
                    
                    // 搜索
                    val query = _searchQuery.value
                    if (query.isNotBlank()) {
                        filtered = filtered.filter { 
                            it.name.contains(query, ignoreCase = true) || 
                            it.note?.contains(query, ignoreCase = true) == true
                        }
                    }
                    
                    // 排序
                    val sorted = when (_sortOrder.value) {
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
                    
                    android.util.Log.d("AnniversaryViewModel", "过滤排序后 ${sorted.size} 条")
                    sorted.forEach {
                        android.util.Log.d("AnniversaryViewModel", "  - ${it.name} (ID: ${it.id}, userId: ${it.userId})")
                    }
                    
                    // 🔥 直接更新 StateFlow
                    _anniversaries.value = sorted
                    
                    // 更新置顶纪念日
                    _pinnedAnniversary.value = sorted.firstOrNull { it.isPinned }
                }
            } catch (e: Exception) {
                android.util.Log.e("AnniversaryViewModel", "加载纪念日失败", e)
                _anniversaries.value = emptyList()
                _pinnedAnniversary.value = null
            }
        }
    }
    
    // 🔥 新增：公开方法，供UI层在用户切换时调用
    fun refreshForNewUser() {
        android.util.Log.d("AnniversaryViewModel", "用户切换，刷新数据")
        loadAnniversaries()
    }
    
    // 添加纪念日
    fun addAnniversary(
        name: String, 
        date: String, 
        imageUri: String? = null,
        frameId: String = "jinian_card_1",  // 🔥 添加相框ID参数
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
                android.util.Log.d("AnniversaryViewModel", "name: $name, date: $date, type: $type")
                
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
                
                val anniversary = Anniversary(
                    userId = userId,
                    name = name, 
                    date = date, 
                    imageUri = savedImageUri,
                    frameId = frameId,  // 🔥 使用传入的相框ID
                    type = type,
                    isYearly = isYearly,
                    note = note,
                    importance = importance
                )
                
                android.util.Log.d("AnniversaryViewModel", "准备插入数据库: $anniversary")
                
                // 🔥 在IO线程执行数据库操作
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    repository.insert(anniversary)
                }
                
                android.util.Log.d("AnniversaryViewModel", "✅ 数据库插入完成")
                
                // 🔥 等待数据库事务提交
                kotlinx.coroutines.delay(300)
                
                // 🔥 主动重新加载数据 - 这是关键！
                android.util.Log.d("AnniversaryViewModel", "主动重新加载数据...")
                loadAnniversaries()
                
                // 🔥 再等待一下确保UI更新
                kotlinx.coroutines.delay(200)
                
                android.util.Log.d("AnniversaryViewModel", "✅ 纪念日添加成功，当前列表数量: ${_anniversaries.value.size}")
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
            // 🔥 主动重新加载
            loadAnniversaries()
        }
    }
    
    // 置顶纪念日
    fun pinAnniversary(anniversary: Anniversary) {
        viewModelScope.launch {
            repository.pinAnniversary(getCurrentUserId(), anniversary)
            // 🔥 主动重新加载
            loadAnniversaries()
        }
    }
    
    // 取消置顶
    fun unpinAnniversary(anniversary: Anniversary) {
        viewModelScope.launch {
            repository.unpinAnniversary(anniversary)
            // 🔥 主动重新加载
            loadAnniversaries()
        }
    }
    
    // 更新纪念日
    fun updateAnniversary(
        anniversary: Anniversary,
        name: String,
        date: String,
        imageUri: String? = null,
        frameId: String = "jinian_card_1",  // 🔥 添加相框ID参数
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
                frameId = frameId,  // 🔥 更新相框ID
                type = type,
                isYearly = isYearly,
                note = note,
                importance = importance
            )
            repository.update(updatedAnniversary)
            // 🔥 主动重新加载
            loadAnniversaries()
        }
    }
    
    // 筛选和排序方法
    fun setTypeFilter(type: String?) {
        _selectedType.value = type
        // 🔥 筛选变化会自动触发loadAnniversaries（在init中监听）
    }
    
    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        // 🔥 排序变化会自动触发loadAnniversaries（在init中监听）
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        // 🔥 搜索变化会自动触发loadAnniversaries（在init中监听）
    }
    
    fun clearFilters() {
        _selectedType.value = null
        _sortOrder.value = SortOrder.DEFAULT
        _searchQuery.value = ""
        // 🔥 会自动触发loadAnniversaries
    }
    
    // 统计分析方法
    fun getMonthlyStatistics(): StateFlow<Map<String, Int>> {
        return _anniversaries.map { list ->
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
        return _anniversaries.map { list ->
            list.groupBy { it.type }
                .mapValues { it.value.size }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    }
    
    fun getTopImportantAnniversaries(limit: Int = 5): StateFlow<List<Anniversary>> {
        return _anniversaries.map { list ->
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
        return _anniversaries.map { list ->
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
            // 🔥 主动重新加载
            loadAnniversaries()
        }
    }
    
    // 切换视图模式
    fun setViewMode(mode: AnniversaryViewMode) {
        _viewMode.value = mode
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
