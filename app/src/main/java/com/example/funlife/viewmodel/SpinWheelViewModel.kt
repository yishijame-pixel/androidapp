// SpinWheelViewModel.kt - 转盘视图模型
package com.example.funlife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.SpinWheelHistory
import com.example.funlife.data.model.SpinWheelTemplate
import com.example.funlife.data.model.SpinWheelMode
import com.example.funlife.data.model.WheelOption
import com.example.funlife.repository.SpinWheelHistoryRepository
import com.example.funlife.repository.SpinWheelTemplateRepository
import com.example.funlife.repository.CoinRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlin.random.Random

class SpinWheelViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val templateRepository: SpinWheelTemplateRepository
    private val historyRepository: SpinWheelHistoryRepository
    private val coinRepository: CoinRepository
    private val guaranteeCounterDao: com.example.funlife.data.dao.GuaranteeCounterDao
    private val customModeRepository: com.example.funlife.repository.CustomSpinModeRepository
    private val preferencesRepository: com.example.funlife.repository.UserPreferencesRepository  // 🔥 新增
    
    // 🔥 获取当前用户ID
    private fun getCurrentUserId(): Long {
        val sessionManager = com.example.funlife.utils.UserSessionManager(context)
        return sessionManager.getCurrentUserId().takeIf { it > 0 } ?: 0L
    }
    
    init {
        val database = AppDatabase.getDatabase(application)
        templateRepository = SpinWheelTemplateRepository(database.spinWheelTemplateDao())
        historyRepository = SpinWheelHistoryRepository(database.spinWheelHistoryDao())
        coinRepository = CoinRepository(database.coinDao(), application.applicationContext)
        guaranteeCounterDao = database.guaranteeCounterDao()
        customModeRepository = com.example.funlife.repository.CustomSpinModeRepository(database.customSpinModeDao())
        preferencesRepository = com.example.funlife.repository.UserPreferencesRepository(database.userPreferencesDao())  // 🔥 新增
    }
    
    // 所有可用模式
    val allModes: StateFlow<List<com.example.funlife.data.model.CustomSpinMode>> = 
        customModeRepository.getAllActiveModes(getCurrentUserId())
            .catch { e ->
                android.util.Log.e("SpinWheelViewModel", "Error loading custom modes", e)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 当前选中的自定义模式
    private val _currentCustomMode = MutableStateFlow<com.example.funlife.data.model.CustomSpinMode?>(null)
    val currentCustomMode: StateFlow<com.example.funlife.data.model.CustomSpinMode?> = _currentCustomMode.asStateFlow()
    
    // 保底设置
    private val _guaranteeSettings = MutableStateFlow(com.example.funlife.data.model.GuaranteeSettings())
    val guaranteeSettings: StateFlow<com.example.funlife.data.model.GuaranteeSettings> = _guaranteeSettings.asStateFlow()
    
    // 保底计数器
    val guaranteeCounters: StateFlow<List<com.example.funlife.data.model.GuaranteeCounter>> = 
        guaranteeCounterDao.getAllEnabledCounters(getCurrentUserId())
            .catch { e ->
                android.util.Log.e("SpinWheelViewModel", "Error loading guarantee counters", e)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 所有模板
    val allTemplates: StateFlow<List<SpinWheelTemplate>> = templateRepository.getAllTemplates(getCurrentUserId())
        .catch { e ->
            android.util.Log.e("SpinWheelViewModel", "Error loading templates", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 最近历史记录
    val recentHistory: StateFlow<List<SpinWheelHistory>> = historyRepository.getRecentHistory(getCurrentUserId(), 20)
        .catch { e ->
            android.util.Log.e("SpinWheelViewModel", "Error loading recent history", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 筛选后的历史记录
    private val _filteredHistory = MutableStateFlow<List<SpinWheelHistory>>(emptyList())
    val filteredHistory: StateFlow<List<SpinWheelHistory>> = _filteredHistory.asStateFlow()
    
    // 🔥 新增：Flow 收集任务管理
    private var filterJob: Job? = null
    
    // 筛选条件
    private val _filterMode = MutableStateFlow<String?>(null)
    val filterMode: StateFlow<String?> = _filterMode.asStateFlow()
    
    private val _filterDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val filterDateRange: StateFlow<Pair<Long, Long>?> = _filterDateRange.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // 历史记录总数（使用 MutableStateFlow 手动更新）
    private val _historyCount = MutableStateFlow(0)
    val historyCount: StateFlow<Int> = _historyCount.asStateFlow()
    
    // 当前选中的模板
    private val _currentTemplate = MutableStateFlow<SpinWheelTemplate?>(null)
    val currentTemplate: StateFlow<SpinWheelTemplate?> = _currentTemplate.asStateFlow()
    
    // 当前选项列表（带权重）
    private val _currentOptions = MutableStateFlow(getDefaultWheelOptions())
    val currentOptions: StateFlow<List<WheelOption>> = _currentOptions.asStateFlow()
    
    // 当前转盘模式
    private val _currentMode = MutableStateFlow(SpinWheelMode.NORMAL)
    val currentMode: StateFlow<SpinWheelMode> = _currentMode.asStateFlow()
    
    // 用户金币
    val userCoins: StateFlow<Int> = coinRepository.getUserCoins(getCurrentUserId())
        .map { it?.coins ?: 0 }
        .catch { e ->
            android.util.Log.e("SpinWheelViewModel", "Error loading user coins", e)
            emit(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    // 设置：启用音效
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()
    
    // 设置：启用震动
    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()
    
    // 🔥 新增：保存状态提示
    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()
    
    fun clearSaveMessage() {
        _saveMessage.value = null
    }
    
    // 当前主题
    private val _currentTheme = MutableStateFlow(com.example.funlife.data.model.WheelThemes.Default)
    val currentTheme: StateFlow<com.example.funlife.data.model.WheelTheme> = _currentTheme.asStateFlow()
    
    // 权重可视化开关
    private val _showWeightVisualization = MutableStateFlow(false)
    val showWeightVisualization: StateFlow<Boolean> = _showWeightVisualization.asStateFlow()
    
    // 动画设置
    private val _particleEffectEnabled = MutableStateFlow(true)
    val particleEffectEnabled: StateFlow<Boolean> = _particleEffectEnabled.asStateFlow()
    
    private val _fireworksEnabled = MutableStateFlow(true)
    val fireworksEnabled: StateFlow<Boolean> = _fireworksEnabled.asStateFlow()
    
    private val _coinAnimationEnabled = MutableStateFlow(true)
    val coinAnimationEnabled: StateFlow<Boolean> = _coinAnimationEnabled.asStateFlow()
    
    // 统计数据
    private val _totalSpins = MutableStateFlow(0)
    val totalSpins: StateFlow<Int> = _totalSpins.asStateFlow()
    
    private val _totalCoinsSpent = MutableStateFlow(0)
    val totalCoinsSpent: StateFlow<Int> = _totalCoinsSpent.asStateFlow()
    
    private val _totalCoinsEarned = MutableStateFlow(0)
    val totalCoinsEarned: StateFlow<Int> = _totalCoinsEarned.asStateFlow()
    
    // 连抽相关状态
    private val _multiSpinCount = MutableStateFlow(1) // 1, 3, 5, 10
    val multiSpinCount: StateFlow<Int> = _multiSpinCount.asStateFlow()
    
    private val _multiSpinResults = MutableStateFlow<List<String>>(emptyList())
    val multiSpinResults: StateFlow<List<String>> = _multiSpinResults.asStateFlow()
    
    private val _isMultiSpinning = MutableStateFlow(false)
    val isMultiSpinning: StateFlow<Boolean> = _isMultiSpinning.asStateFlow()
    
    // 连抽模式开关
    private val _multiSpinMode = MutableStateFlow(false)
    val multiSpinMode: StateFlow<Boolean> = _multiSpinMode.asStateFlow()
    
    // 当前连抽进度
    private val _currentMultiSpinProgress = MutableStateFlow(0)
    val currentMultiSpinProgress: StateFlow<Int> = _currentMultiSpinProgress.asStateFlow()
    
    init {
        // 初始化默认模板和金币
        viewModelScope.launch {
            val userId = getCurrentUserId()
            android.util.Log.d("SpinWheelViewModel", "=== ViewModel Initialization Started, userId=$userId ===")
            
            // 🔥 步骤1：初始化或获取用户偏好
            val prefs = preferencesRepository.getOrCreatePreferences(userId)
            android.util.Log.d("SpinWheelViewModel", "User preferences loaded: lastTemplateId=${prefs.lastTemplateId}, lastCustomOptions length=${prefs.lastCustomOptions.length}")
            
            // 🔥 步骤2：从偏好中恢复设置
            _soundEnabled.value = prefs.enableSound
            _vibrationEnabled.value = prefs.enableVibration
            _showWeightVisualization.value = prefs.showWeightVisualization
            _particleEffectEnabled.value = prefs.particleEffectEnabled
            _fireworksEnabled.value = prefs.fireworksEnabled
            _coinAnimationEnabled.value = prefs.coinAnimationEnabled
            
            // 恢复主题
            val theme = com.example.funlife.data.model.WheelThemes.fromString(prefs.wheelTheme)
            _currentTheme.value = theme
            
            // 恢复转盘模式
            try {
                _currentMode.value = SpinWheelMode.valueOf(prefs.lastSpinMode)
            } catch (e: Exception) {
                _currentMode.value = SpinWheelMode.NORMAL
            }
            
            android.util.Log.d("SpinWheelViewModel", "Current mode restored: ${_currentMode.value.name}")
            
            // 初始化金币（如果还没有记录），但不再赠送任何初始金币
            coinRepository.initializeCoins(userId)
            
            createDefaultTemplatesIfNeeded()
            customModeRepository.initializeDefaultModes(userId)
            
            // 🔥 步骤2.5：恢复最后使用的自定义模式
            prefs.lastCustomModeId?.let { modeId ->
                allModes.first().find { it.id == modeId }?.let { mode ->
                    _currentCustomMode.value = mode
                    android.util.Log.d("SpinWheelViewModel", "Custom mode restored: ${mode.name}")
                }
            } ?: run {
                // 设置默认模式为第一个可用模式
                allModes.first().firstOrNull()?.let { mode ->
                    _currentCustomMode.value = mode
                    android.util.Log.d("SpinWheelViewModel", "Default mode set: ${mode.name}")
                }
            }
            
            // 🔥 步骤3：加载当前模式的选项
            android.util.Log.d("SpinWheelViewModel", "=== Step 3: Loading Options for Current Mode ===")
            loadOptionsForMode(userId, _currentMode.value)
            
            updateHistoryCount()
            updateStatistics()
            
            android.util.Log.d("SpinWheelViewModel", "=== ViewModel Initialization Completed ===")
        }
    }
    
    // 更新历史记录数量
    private suspend fun updateHistoryCount() {
        _historyCount.value = historyRepository.getCount(getCurrentUserId())
    }
    
    // 更新统计数据
    private suspend fun updateStatistics() {
        _totalSpins.value = historyRepository.getCount(getCurrentUserId())
        _totalCoinsSpent.value = historyRepository.getTotalCoinCost(getCurrentUserId())
        _totalCoinsEarned.value = historyRepository.getTotalCoinReward(getCurrentUserId())
    }
    
    // 切换转盘模式（使用自定义模式）
    fun setCustomMode(mode: com.example.funlife.data.model.CustomSpinMode) {
        _currentCustomMode.value = mode
        
        // 🔥 同步更新基础模式，确保UI显示正确
        _currentMode.value = when {
            mode.name.contains("普通", ignoreCase = true) -> SpinWheelMode.NORMAL
            mode.name.contains("进阶", ignoreCase = true) -> SpinWheelMode.ADVANCED
            mode.name.contains("幸运", ignoreCase = true) -> SpinWheelMode.LUCKY
            mode.hasReward -> SpinWheelMode.LUCKY
            mode.costPerSpin > 10 -> SpinWheelMode.ADVANCED
            mode.costPerSpin > 0 -> SpinWheelMode.ADVANCED
            else -> SpinWheelMode.NORMAL
        }
        
        android.util.Log.d("SpinWheelViewModel", "Custom mode set: ${mode.name}, base mode: ${_currentMode.value.name}")
        
        // 🔥 保存到用户偏好
        viewModelScope.launch {
            customModeRepository.incrementUsageCount(getCurrentUserId(), mode.id)
            preferencesRepository.updateLastCustomModeId(getCurrentUserId(), mode.id)
            preferencesRepository.updateLastSpinMode(getCurrentUserId(), _currentMode.value.name)
            android.util.Log.d("SpinWheelViewModel", "Mode preferences saved: customModeId=${mode.id}, spinMode=${_currentMode.value.name}")
        }
    }
    
    // 切换转盘模式（使用基础模式）
    fun setMode(mode: SpinWheelMode) {
        // 🔥 切换模式时，先保存当前模式的选项
        viewModelScope.launch {
            val userId = getCurrentUserId()
            val currentOptions = _currentOptions.value
            val oldMode = _currentMode.value  // 🔥 保存旧模式
            
            // 保存旧模式的选项到对应字段
            saveOptionsForMode(userId, oldMode, currentOptions)
            android.util.Log.d("SpinWheelViewModel", "Saved options for old mode: ${oldMode.name}")
            
            // 切换到新模式
            _currentMode.value = mode
            
            // 加载新模式的选项
            loadOptionsForMode(userId, mode)
            
            // 保存模式切换到用户偏好
            preferencesRepository.updateLastSpinMode(userId, mode.name)
            android.util.Log.d("SpinWheelViewModel", "Mode switched to: ${mode.name}, options loaded")
        }
    }
    
    // 保存指定模式的选项
    private suspend fun saveOptionsForMode(userId: Long, mode: SpinWheelMode, options: List<WheelOption>) {
        val optionsJson = optionsToJson(options)
        when (mode) {
            SpinWheelMode.NORMAL -> {
                preferencesRepository.updateNormalModeOptions(userId, optionsJson)
                android.util.Log.d("SpinWheelViewModel", "Saved NORMAL mode options: $optionsJson")
            }
            SpinWheelMode.ADVANCED -> {
                preferencesRepository.updateAdvancedModeOptions(userId, optionsJson)
                android.util.Log.d("SpinWheelViewModel", "Saved ADVANCED mode options: $optionsJson")
            }
            SpinWheelMode.LUCKY -> {
                preferencesRepository.updateLuckyModeOptions(userId, optionsJson)
                android.util.Log.d("SpinWheelViewModel", "Saved LUCKY mode options: $optionsJson")
            }
        }
    }
    
    // 加载指定模式的选项
    private suspend fun loadOptionsForMode(userId: Long, mode: SpinWheelMode) {
        val prefs = preferencesRepository.getOrCreatePreferences(userId)
        val optionsJson = when (mode) {
            SpinWheelMode.NORMAL -> prefs.normalModeOptions
            SpinWheelMode.ADVANCED -> prefs.advancedModeOptions
            SpinWheelMode.LUCKY -> prefs.luckyModeOptions
        }
        
        if (optionsJson.isNotEmpty()) {
            // 从保存的JSON加载选项
            val options = parseOptionsFromJson(optionsJson)
            _currentOptions.value = options
            android.util.Log.d("SpinWheelViewModel", "Loaded ${mode.name} mode options from saved: ${options.size} items")
        } else {
            // 首次使用该模式，加载默认选项
            val defaultOptions = getDefaultOptionsForMode(mode)
            _currentOptions.value = defaultOptions
            // 立即保存默认选项
            saveOptionsForMode(userId, mode, defaultOptions)
            android.util.Log.d("SpinWheelViewModel", "Loaded ${mode.name} mode default options: ${defaultOptions.size} items")
        }
    }
    
    // 获取指定模式的默认选项
    private fun getDefaultOptionsForMode(mode: SpinWheelMode): List<WheelOption> {
        return when (mode) {
            SpinWheelMode.NORMAL -> {
                // 普通模式：6个选项
                listOf("吃火锅", "看电影", "打游戏", "去旅行", "读书", "运动").map {
                    WheelOption(text = it, weight = 1)
                }
            }
            SpinWheelMode.ADVANCED -> {
                // 进阶模式：8个选项
                listOf("火锅", "烧烤", "日料", "川菜", "粤菜", "西餐", "快餐", "面食").map {
                    WheelOption(text = it, weight = 1)
                }
            }
            SpinWheelMode.LUCKY -> {
                // 幸运模式：8个选项
                // 🔥 根据实际转盘图片上的文字位置（从顶部开始顺时针）：
                // index=0（顶部）：双倍奖励
                // index=1（右上）：幸运星
                // index=2（右侧）：神秘礼物
                // index=3（右下）：再来一次
                // index=4（底部）：大奖
                // index=5（左下）：安慰奖
                // index=6（左侧）：小奖
                // index=7（左上）：中奖
                listOf("双倍奖励", "幸运星", "神秘礼物", "再来一次", "大奖", "安慰奖", "小奖", "中奖").map {
                    WheelOption(text = it, weight = 1)
                }
            }
        }
    }
    
    // 检查是否有足够金币（使用自定义模式）
    fun canAffordSpinCustom(): Boolean {
        val mode = _currentCustomMode.value ?: return false
        return mode.canAfford(userCoins.value)
    }
    
    // 检查是否有足够金币（使用基础模式）
    fun canAffordSpin(): Boolean {
        val mode = _currentMode.value
        return mode.canAfford(userCoins.value)
    }
    
    // 检查并扣除金币（开始旋转时调用）
    suspend fun checkAndDeductCoins(): Boolean {
        val mode = _currentMode.value
        val cost = mode.costPerSpin
        if (cost <= 0) return true
        // 🔒 直接走原子扣费：返回值 = true 表示真的扣到了，否则余额不足。
        //    去掉了之前的"先看缓存余额再扣"两步操作，避免缓存与 DB 不一致时
        //    判断通过但实际扣费失败仍然返回 true → 用户能零成本转盘。
        return coinRepository.spendCoins(getCurrentUserId(), cost)
    }
    
    // 🔥 连抽模式一次性扣除金币（🔒 仅依赖原子扣费返回值，缓存余额不可信）
    suspend fun deductCoinsForMultiSpin(totalCost: Int): Boolean {
        if (totalCost <= 0) return true
        return coinRepository.spendCoins(getCurrentUserId(), totalCost)
    }
    
    // 处理旋转结果（旋转完成后调用）
    suspend fun processSpinResult(selectedResult: String): SpinResult {
        val mode = _currentMode.value
        
        // 计算金币奖励（仅幸运模式）
        val coinReward = if (mode == SpinWheelMode.LUCKY) {
            calculateLuckyReward()
        } else {
            0
        }
        
        // 🎯 积分掉落（仅 LUCKY / ADVANCED 这两种"扣金币"的模式，小概率）
        val shopPointsReward = rollShopPointsReward(mode)
        
        // 发放奖励
        val uid = getCurrentUserId()
        if (coinReward > 0) {
            coinRepository.addCoins(uid, coinReward, reason = "spin_lucky")
        }
        if (shopPointsReward > 0) {
            coinRepository.addShopPoints(uid, shopPointsReward, reason = "spin_${mode.name.lowercase()}_drop")
        }
        
        // 保存历史
        saveResultToHistory(selectedResult, mode, coinReward)
        
        return SpinResult.Success(selectedResult, coinReward, shopPointsReward)
    }
    
    // 执行转盘（带权重和金币逻辑）- 保留用于兼容
    @Deprecated("使用 checkAndDeductCoins 和 processSpinResult 替代")
    suspend fun performSpin(selectedResult: String): SpinResult {
        val mode = _currentMode.value
        val coins = userCoins.value
        
        // 检查金币
        if (!mode.canAfford(coins)) {
            return SpinResult.InsufficientCoins
        }
        
        // 扣除金币
        if (mode.costPerSpin > 0) {
            coinRepository.spendCoins(getCurrentUserId(), mode.costPerSpin)
        }
        
        // 使用传入的结果，而不是重新计算
        val result = selectedResult
        
        // 计算金币奖励（仅幸运模式）
        val coinReward = if (mode == SpinWheelMode.LUCKY) {
            calculateLuckyReward()
        } else {
            0
        }
        
        // 发放奖励
        if (coinReward > 0) {
            coinRepository.addCoins(getCurrentUserId(), coinReward)
        }
        
        // 保存历史
        saveResultToHistory(result, mode, coinReward)
        
        return SpinResult.Success(result, coinReward)
    }
    
    // 根据权重选择选项（带保底机制）
    private suspend fun selectOptionByWeight(): String {
        val userId = getCurrentUserId()
        val options = _currentOptions.value.filter { !it.isExcluded }
        if (options.isEmpty()) return "无可用选项"
        
        // 检查保底机制
        if (_guaranteeSettings.value.enabled) {
            for (option in options) {
                val counter = guaranteeCounterDao.getCounterByOption(userId, option.text)
                if (counter != null && counter.currentCount >= counter.guaranteeThreshold) {
                    // 触发保底
                    guaranteeCounterDao.resetCounter(userId, option.text)
                    // 重置其他计数器
                    options.filter { it.text != option.text }.forEach {
                        guaranteeCounterDao.incrementCounter(userId, it.text)
                    }
                    return option.text
                }
            }
        }
        
        // 正常权重抽取
        val totalWeight = options.sumOf { it.weight }
        var random = Random.nextInt(totalWeight)
        
        var selectedOption: String? = null
        for (option in options) {
            random -= option.weight
            if (random < 0) {
                selectedOption = option.text
                break
            }
        }
        
        val result = selectedOption ?: options.last().text
        
        // 更新保底计数器
        if (_guaranteeSettings.value.enabled) {
            options.forEach { option ->
                if (option.text == result) {
                    guaranteeCounterDao.resetCounter(userId, option.text)
                } else {
                    guaranteeCounterDao.incrementCounter(userId, option.text)
                }
            }
        }
        
        return result
    }
    
    // 🔒 转盘不再发金币（原期望奖励 18.5 金币/spin > 成本 10 金币，可被无限刷）
    private fun calculateLuckyReward(): Int = 0

    /**
     * 🎯 积分掉落算法（反作弊）
     *
     * 触发条件：仅 LUCKY / ADVANCED 这两种扣金币的模式
     * 触发概率：3%（约 33 次旋转触发一次）
     * 奖励分布（加权随机）：
     *   1 分 40% · 2 分 25% · 3 分 15% · 5 分 12% · 8 分 5% · 10 分 3%
     *   期望值 ≈ 2.65 积分/触发 → 单次旋转期望 ≈ 0.08 积分
     *   按转盘成本 10 金币/spin 折算：每积分约 125 金币
     *   远高于商店"花 50 金币购买送 5 积分"= 10 金币/积分的兑换比，
     *   不构成无限刷积分的套利路径。
     */
    private fun rollShopPointsReward(mode: SpinWheelMode): Int {
        if (mode != SpinWheelMode.LUCKY && mode != SpinWheelMode.ADVANCED) return 0
        if (Random.nextInt(100) >= 3) return 0  // 3% 触发概率
        val r = Random.nextInt(100)
        return when {
            r < 40 -> 1
            r < 65 -> 2
            r < 80 -> 3
            r < 92 -> 5
            r < 97 -> 8
            else   -> 10
        }
    }
    
    // 保存模板
    fun saveTemplate(name: String, options: List<String>, weights: List<Int> = emptyList(), category: String = "custom") {
        viewModelScope.launch {
            // 🔥 新增：输入验证
            if (name.isBlank() || name.length > 30) {
                android.util.Log.w("SpinWheelViewModel", "Invalid template name")
                return@launch
            }
            
            // 验证选项
            for (option in options) {
                val optionValidation = com.example.funlife.utils.ValidationUtils.validateWheelOption(option)
                if (optionValidation is com.example.funlife.utils.ValidationResult.Error) {
                    android.util.Log.w("SpinWheelViewModel", "Invalid wheel option: ${optionValidation.message}")
                    return@launch
                }
            }
            
            // 验证权重
            for (weight in weights) {
                val weightValidation = com.example.funlife.utils.ValidationUtils.validateWeight(weight)
                if (weightValidation is com.example.funlife.utils.ValidationResult.Error) {
                    android.util.Log.w("SpinWheelViewModel", "Invalid weight: ${weightValidation.message}")
                    return@launch
                }
            }
            
            val template = SpinWheelTemplate(
                userId = getCurrentUserId(),
                name = name,
                options = SpinWheelTemplate.createOptionsString(options),
                weights = if (weights.isNotEmpty()) SpinWheelTemplate.createWeightsString(weights) else "",
                category = category,
                createdAt = System.currentTimeMillis().toString()
            )
            templateRepository.insert(template)
        }
    }
    
    // 加载模板
    fun loadTemplate(template: SpinWheelTemplate) {
        _currentTemplate.value = template
        _currentOptions.value = template.getWheelOptions()
        
        // 增加使用次数并保存模板ID
        viewModelScope.launch {
            templateRepository.update(template.copy(usageCount = template.usageCount + 1))
            
            // 🔥 保存最后使用的模板ID，并清除自定义选项
            preferencesRepository.updateLastTemplate(getCurrentUserId(), template.id)
            preferencesRepository.updateLastCustomOptions(getCurrentUserId(), "")
            android.util.Log.d("SpinWheelViewModel", "Template loaded and saved: ${template.name}, id=${template.id}")
        }
    }
    
    // 更新当前选项
    fun updateOptions(options: List<WheelOption>) = viewModelScope.launch {
        android.util.Log.d("SpinWheelViewModel", "=== updateOptions called with ${options.size} options ===")
        
        // 🔥 验证：至少需要2个选项
        if (options.size < 2) {
            android.util.Log.w("SpinWheelViewModel", "Cannot update options: need at least 2 options, got ${options.size}")
            _saveMessage.value = "至少需要2个选项"
            return@launch
        }
        
        // 🔥 验证：去除重复选项
        val uniqueOptions = options.distinctBy { it.text }
        if (uniqueOptions.size < options.size) {
            android.util.Log.w("SpinWheelViewModel", "Removed ${options.size - uniqueOptions.size} duplicate options")
        }
        
        // 打印每个选项的详细信息
        uniqueOptions.forEachIndexed { index, option ->
            android.util.Log.d("SpinWheelViewModel", "Option $index: text='${option.text}', weight=${option.weight}, excluded=${option.isExcluded}")
        }
        
        // 🔥 立即更新UI
        _currentOptions.value = uniqueOptions
        
        // 🔥 清除当前模板状态
        _currentTemplate.value = null
        android.util.Log.d("SpinWheelViewModel", "Template cleared")
        
        // 🔥 保存到当前模式的选项配置
        try {
            val userId = getCurrentUserId()
            val currentMode = _currentMode.value
            android.util.Log.d("SpinWheelViewModel", "Saving options for mode: ${currentMode.name}, userId: $userId")
            
            // 保存到对应模式的字段
            saveOptionsForMode(userId, currentMode, uniqueOptions)
            
            android.util.Log.d("SpinWheelViewModel", "✓ Options saved successfully to ${currentMode.name} mode")
            _saveMessage.value = "选项已保存 (${uniqueOptions.size}个)"
        } catch (e: Exception) {
            android.util.Log.e("SpinWheelViewModel", "✗ Failed to save options", e)
            _saveMessage.value = "保存失败，请重试"
        }
    }
    
    // 切换选项排除状态
    fun toggleOptionExclusion(optionText: String) {
        _currentOptions.value = _currentOptions.value.map {
            if (it.text == optionText) it.copy(isExcluded = !it.isExcluded) else it
        }
        
        // 🔥 清除当前模板状态
        _currentTemplate.value = null
        
        // 🔥 自动保存到当前模式
        viewModelScope.launch {
            val userId = getCurrentUserId()
            val currentMode = _currentMode.value
            android.util.Log.d("SpinWheelViewModel", "Toggled exclusion for '$optionText', saving to ${currentMode.name} mode")
            saveOptionsForMode(userId, currentMode, _currentOptions.value)
            android.util.Log.d("SpinWheelViewModel", "Exclusion state saved")
        }
    }
    
    // 设置选项权重
    fun setOptionWeight(optionText: String, weight: Int) {
        _currentOptions.value = _currentOptions.value.map {
            if (it.text == optionText) it.copy(weight = weight.coerceIn(1, 10)) else it
        }
        
        // 🔥 清除当前模板状态
        _currentTemplate.value = null
        
        // 🔥 自动保存到当前模式
        viewModelScope.launch {
            val userId = getCurrentUserId()
            val currentMode = _currentMode.value
            android.util.Log.d("SpinWheelViewModel", "Set weight for '$optionText' to $weight, saving to ${currentMode.name} mode")
            saveOptionsForMode(userId, currentMode, _currentOptions.value)
            android.util.Log.d("SpinWheelViewModel", "Weight saved")
        }
    }
    
    // 🔥 新增：将选项转换为JSON
    private fun optionsToJson(options: List<WheelOption>): String {
        return try {
            val items = options.joinToString(",") { option ->
                // 🔥 转义特殊字符
                val escapedText = option.text.replace("\"", "\\\"").replace("\n", "\\n")
                """{"text":"$escapedText","weight":${option.weight},"isExcluded":${option.isExcluded}}"""
            }
            val json = "[$items]"
            android.util.Log.d("SpinWheelViewModel", "optionsToJson: $json")
            json
        } catch (e: Exception) {
            android.util.Log.e("SpinWheelViewModel", "Failed to convert options to JSON", e)
            ""
        }
    }
    
    // 🔥 新增：从JSON解析选项
    private fun parseOptionsFromJson(json: String): List<WheelOption> {
        return try {
            android.util.Log.d("SpinWheelViewModel", "parseOptionsFromJson input: $json")
            
            if (json.isBlank() || json == "[]") {
                android.util.Log.w("SpinWheelViewModel", "Empty JSON, returning empty list")
                return emptyList()
            }
            
            // 简单的JSON解析：格式为 [{"text":"选项1","weight":1,"isExcluded":false},...]
            val options = mutableListOf<WheelOption>()
            val items = json.trim().removeSurrounding("[", "]").split("},")
            
            android.util.Log.d("SpinWheelViewModel", "Parsing ${items.size} items")
            
            for ((index, item) in items.withIndex()) {
                try {
                    val cleanItem = item.trim().removeSurrounding("{", "}").replace("}", "")
                    
                    // 🔥 使用正则表达式解析 JSON 字段（支持中文和特殊字符）
                    var text = ""
                    var weight = 1
                    var isExcluded = false
                    
                    // 提取 text 字段
                    val textMatch = Regex(""""text":"([^"]*?)"""").find(cleanItem)
                    if (textMatch != null) {
                        text = textMatch.groupValues[1].replace("\\\"", "\"").replace("\\n", "\n")
                    }
                    
                    // 提取 weight 字段
                    val weightMatch = Regex(""""weight":(\d+)""").find(cleanItem)
                    if (weightMatch != null) {
                        weight = weightMatch.groupValues[1].toIntOrNull() ?: 1
                    }
                    
                    // 提取 isExcluded 字段
                    val excludedMatch = Regex(""""isExcluded":(true|false)""").find(cleanItem)
                    if (excludedMatch != null) {
                        isExcluded = excludedMatch.groupValues[1].toBoolean()
                    }
                    
                    if (text.isNotEmpty()) {
                        options.add(WheelOption(text, weight, isExcluded))
                        android.util.Log.d("SpinWheelViewModel", "✓ Parsed option $index: text='$text', weight=$weight, excluded=$isExcluded")
                    } else {
                        android.util.Log.w("SpinWheelViewModel", "✗ Skipping empty option at index $index")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SpinWheelViewModel", "✗ Failed to parse option at index $index", e)
                }
            }
            
            android.util.Log.d("SpinWheelViewModel", "Successfully parsed ${options.size} options")
            options
        } catch (e: Exception) {
            android.util.Log.e("SpinWheelViewModel", "Failed to parse options JSON", e)
            emptyList()
        }
    }
    
    // 保存转盘结果到历史
    private suspend fun saveResultToHistory(result: String, mode: SpinWheelMode, coinReward: Int) {
        val history = SpinWheelHistory(
            templateId = _currentTemplate.value?.id,
            templateName = _currentTemplate.value?.name ?: "自定义",
            result = result,
            allOptions = _currentOptions.value.joinToString(",") { it.text },
            mode = mode.name,
            coinCost = mode.costPerSpin,
            coinReward = coinReward
        )
        historyRepository.insert(history)
        
        // 🔥 新增：定期清理旧记录（保留最近1000条）
        historyRepository.cleanOldHistory(getCurrentUserId(), 1000)
        
        updateHistoryCount()
        updateStatistics()
    }
    
    // 删除模板
    fun deleteTemplate(template: SpinWheelTemplate) {
        viewModelScope.launch {
            templateRepository.delete(template)
        }
    }
    
    // 删除历史记录
    fun deleteHistory(history: SpinWheelHistory) {
        viewModelScope.launch {
            historyRepository.delete(history)
            updateHistoryCount()
            updateStatistics()
        }
    }
    
    // 清空所有历史
    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.deleteAll(getCurrentUserId())
            updateHistoryCount()
            updateStatistics()
        }
    }
    
    // 切换音效
    fun toggleSound() {
        _soundEnabled.value = !_soundEnabled.value
    }
    
    // 切换震动
    fun toggleVibration() {
        _vibrationEnabled.value = !_vibrationEnabled.value
    }
    
    // ========== 主题和动画设置 ==========
    
    // 设置主题
    fun setTheme(theme: com.example.funlife.data.model.WheelTheme) {
        _currentTheme.value = theme
    }
    
    // 切换权重可视化
    fun toggleWeightVisualization() {
        _showWeightVisualization.value = !_showWeightVisualization.value
    }
    
    // 切换粒子效果
    fun toggleParticleEffect() {
        _particleEffectEnabled.value = !_particleEffectEnabled.value
    }
    
    // 切换烟花效果
    fun toggleFireworks() {
        _fireworksEnabled.value = !_fireworksEnabled.value
    }
    
    // 切换金币动画
    fun toggleCoinAnimation() {
        _coinAnimationEnabled.value = !_coinAnimationEnabled.value
    }
    
    // 创建默认模板
    private suspend fun createDefaultTemplatesIfNeeded() {
        val userId = getCurrentUserId()
        val templates = allTemplates.first()
        if (templates.isEmpty()) {
            // 🎯 普通模式模板（6个选项）
            templateRepository.insert(
                SpinWheelTemplate(
                    userId = userId,
                    name = "普通模式 - 今天吃什么",
                    options = "火锅,烧烤,日料,川菜,粤菜,西餐",
                    category = "food",
                    isDefault = true
                )
            )
            
            templateRepository.insert(
                SpinWheelTemplate(
                    userId = userId,
                    name = "普通模式 - 周末活动",
                    options = "看电影,打游戏,运动,逛街,读书,睡觉",
                    category = "game",
                    isDefault = true
                )
            )
            
            templateRepository.insert(
                SpinWheelTemplate(
                    userId = userId,
                    name = "普通模式 - 做决定",
                    options = "去做,不去做,再想想,问朋友,明天再说,随便吧",
                    category = "decision",
                    isDefault = true
                )
            )
            
            // ⚙️ 进阶模式模板（8个选项）
            templateRepository.insert(
                SpinWheelTemplate(
                    userId = userId,
                    name = "进阶模式 - 美食探索",
                    options = "火锅,烧烤,日料,川菜,粤菜,西餐,快餐,面食",
                    category = "food",
                    isDefault = true
                )
            )
            
            templateRepository.insert(
                SpinWheelTemplate(
                    userId = userId,
                    name = "进阶模式 - 娱乐活动",
                    options = "看电影,打游戏,运动健身,逛街购物,郊游,K歌,读书,睡觉",
                    category = "game",
                    isDefault = true
                )
            )
            
            templateRepository.insert(
                SpinWheelTemplate(
                    userId = userId,
                    name = "进阶模式 - 学习计划",
                    options = "英语,数学,编程,阅读,写作,画画,音乐,运动",
                    category = "study",
                    isDefault = true
                )
            )
            
            // 🍀 幸运模式模板（8个选项）
            templateRepository.insert(
                SpinWheelTemplate(
                    userId = userId,
                    name = "幸运模式 - 幸运美食",
                    options = "豪华火锅,高级烧烤,精致日料,特色川菜,经典粤菜,浪漫西餐,网红快餐,特色面食",
                    category = "food",
                    isDefault = true
                )
            )
            
            templateRepository.insert(
                SpinWheelTemplate(
                    userId = userId,
                    name = "幸运模式 - 幸运活动",
                    options = "电影院,游戏厅,健身房,购物中心,旅游景点,KTV,图书馆,温泉SPA",
                    category = "game",
                    isDefault = true
                )
            )
            
            templateRepository.insert(
                SpinWheelTemplate(
                    userId = userId,
                    name = "幸运模式 - 幸运奖励",
                    options = "大奖,中奖,小奖,安慰奖,再来一次,双倍奖励,神秘礼物,幸运星",
                    category = "reward",
                    isDefault = true
                )
            )
        }
    }
    
    private fun getDefaultWheelOptions(): List<WheelOption> {
        // 默认6个选项（普通模式）
        // 注意：进阶和幸运模式需要8个选项，用户需要手动添加或使用模板
        return listOf(
            "吃火锅", 
            "看电影", 
            "打游戏", 
            "去旅行", 
            "读书", 
            "运动"
        ).map {
            WheelOption(text = it, weight = 1)
        }
    }
    
    // ========== 历史记录筛选功能 ==========
    
    // 设置模式筛选
    fun setFilterMode(mode: String?) {
        _filterMode.value = mode
        applyFilters()
    }
    
    // 设置日期范围筛选
    fun setFilterDateRange(startTime: Long?, endTime: Long?) {
        _filterDateRange.value = if (startTime != null && endTime != null) {
            Pair(startTime, endTime)
        } else {
            null
        }
        applyFilters()
    }
    
    // 设置搜索关键词
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }
    
    // 清除所有筛选
    fun clearFilters() {
        _filterMode.value = null
        _filterDateRange.value = null
        _searchQuery.value = ""
        applyFilters()
    }
    
    // 应用筛选条件
    private fun applyFilters() {
        // 🔥 修复：取消之前的收集任务，防止内存泄漏
        filterJob?.cancel()
        
        filterJob = viewModelScope.launch {
            val userId = getCurrentUserId()
            val mode = _filterMode.value
            val dateRange = _filterDateRange.value
            val query = _searchQuery.value
            
            val flow = when {
                // 有日期范围和模式
                dateRange != null && mode != null -> {
                    historyRepository.getHistoryByDateRangeAndMode(
                        userId, dateRange.first, dateRange.second, mode
                    )
                }
                // 只有日期范围
                dateRange != null -> {
                    historyRepository.getHistoryByDateRange(
                        userId, dateRange.first, dateRange.second
                    )
                }
                // 只有模式
                mode != null -> {
                    historyRepository.getHistoryByMode(userId, mode)
                }
                // 只有搜索关键词
                query.isNotEmpty() -> {
                    historyRepository.searchHistoryByResult(userId, query)
                }
                // 无筛选条件
                else -> {
                    historyRepository.getAllHistory(userId)
                }
            }
            
            flow.collect { history ->
                // 如果有搜索关键词，进一步过滤
                _filteredHistory.value = if (query.isNotEmpty() && (mode != null || dateRange != null)) {
                    history.filter { it.result.contains(query, ignoreCase = true) }
                } else {
                    history
                }
            }
        }
    }
    
    // ========== 连抽功能 ==========
    
    // 设置连抽次数
    fun setMultiSpinCount(count: Int) {
        _multiSpinCount.value = count
    }
    
    // 开启/关闭连抽模式
    fun toggleMultiSpinMode(enabled: Boolean) {
        _multiSpinMode.value = enabled
        // 无论开启还是关闭，都重置进度和结果
        _currentMultiSpinProgress.value = 0
        _multiSpinResults.value = emptyList()
    }
    
    // 检查是否需要继续连抽
    fun shouldContinueMultiSpin(): Boolean {
        return _multiSpinMode.value && _currentMultiSpinProgress.value < _multiSpinCount.value
    }
    
    // 增加连抽进度（在开始旋转时调用）
    fun incrementMultiSpinProgress() {
        _currentMultiSpinProgress.value += 1
    }
    
    // 记录连抽结果（不再增加进度，只记录结果）
    fun recordMultiSpinResult(result: String) {
        _multiSpinResults.value = _multiSpinResults.value + result
    }
    
    // 重置连抽状态
    fun resetMultiSpinState() {
        _multiSpinMode.value = false
        _currentMultiSpinProgress.value = 0
        _multiSpinResults.value = emptyList()
    }
    
    // 获取连抽折扣价格
    fun getMultiSpinCost(): Int {
        val baseCost = _currentMode.value.costPerSpin
        val count = _multiSpinCount.value
        
        return when (count) {
            3 -> (baseCost * 3 * 0.9).toInt()  // 9折
            5 -> (baseCost * 5 * 0.85).toInt() // 85折
            10 -> (baseCost * 10 * 0.8).toInt() // 8折
            else -> baseCost
        }
    }
    
    // 检查是否能进行连抽
    fun canAffordMultiSpin(): Boolean {
        return userCoins.value >= getMultiSpinCost()
    }
    
    // 执行连抽
    suspend fun performMultiSpin(): MultiSpinResult {
        val count = _multiSpinCount.value
        if (count == 1) {
            return MultiSpinResult.Error("请使用单次旋转")
        }
        
        val totalCost = getMultiSpinCost()

        // 🔒 直接走原子扣费，不再相信缓存余额（防止扣费失败仍执行抽奖）
        if (totalCost > 0) {
            val ok = coinRepository.spendCoins(getCurrentUserId(), totalCost)
            if (!ok) return MultiSpinResult.InsufficientCoins
        }
        _isMultiSpinning.value = true
        
        // 执行多次抽取
        val results = mutableListOf<String>()
        var totalReward = 0
        var totalShopPoints = 0
        
        for (i in 0 until count) {
            val result = selectOptionByWeight()
            results.add(result)
            
            // 计算金币奖励（仅幸运模式）
            if (_currentMode.value == SpinWheelMode.LUCKY) {
                val reward = calculateLuckyReward()
                totalReward += reward
            }
            // 🎯 每次都独立 roll 积分掉落（仅 LUCKY/ADVANCED 模式）
            totalShopPoints += rollShopPointsReward(_currentMode.value)
            
            // 保存每次的历史记录
            saveResultToHistory(result, _currentMode.value, 0)
        }
        
        // 发放总奖励
        val uid = getCurrentUserId()
        if (totalReward > 0) {
            coinRepository.addCoins(uid, totalReward, reason = "spin_lucky_multi")
        }
        if (totalShopPoints > 0) {
            coinRepository.addShopPoints(uid, totalShopPoints, reason = "spin_${_currentMode.value.name.lowercase()}_drop_multi")
        }
        
        _multiSpinResults.value = results
        _isMultiSpinning.value = false
        
        return MultiSpinResult.Success(results, totalReward, totalShopPoints)
    }
    
    // 清除连抽结果
    fun clearMultiSpinResults() {
        _multiSpinResults.value = emptyList()
    }
    
    // ========== 保底机制功能 ==========
    
    // 启用/禁用保底系统
    fun setGuaranteeEnabled(enabled: Boolean) {
        _guaranteeSettings.value = _guaranteeSettings.value.copy(enabled = enabled)
    }
    
    // 设置默认保底次数
    fun setDefaultGuaranteeThreshold(threshold: Int) {
        _guaranteeSettings.value = _guaranteeSettings.value.copy(defaultThreshold = threshold)
    }
    
    // 初始化选项的保底计数器
    suspend fun initializeGuaranteeCounters(options: List<String>) {
        val userId = getCurrentUserId()
        options.forEach { optionText ->
            val existing = guaranteeCounterDao.getCounterByOption(userId, optionText)
            if (existing == null) {
                guaranteeCounterDao.insert(
                    com.example.funlife.data.model.GuaranteeCounter(
                        userId = userId,
                        optionText = optionText,
                        guaranteeThreshold = _guaranteeSettings.value.defaultThreshold
                    )
                )
            }
        }
    }
    
    // 更新特定选项的保底阈值
    suspend fun updateGuaranteeThreshold(optionText: String, threshold: Int) {
        val userId = getCurrentUserId()
        val counter = guaranteeCounterDao.getCounterByOption(userId, optionText)
        if (counter != null) {
            guaranteeCounterDao.update(counter.copy(guaranteeThreshold = threshold))
        }
    }
    
    // 重置所有保底计数器
    suspend fun resetAllGuaranteeCounters() {
        guaranteeCounterDao.resetAllCounters(getCurrentUserId())
    }
    
    // ========== 统计分析功能 ==========
    
    // 获取选项统计数据
    suspend fun getOptionStatistics(): Map<String, Int> {
        val history = historyRepository.getAllHistory(getCurrentUserId()).first()
        return history.groupingBy { it.result }.eachCount()
    }
    
    // 获取金币收支趋势（按天）
    suspend fun getCoinTrendByDay(days: Int = 7): List<Pair<Long, Int>> {
        val history = historyRepository.getAllHistory(getCurrentUserId()).first()
        val now = System.currentTimeMillis()
        val startTime = now - (days * 24 * 60 * 60 * 1000L)
        
        return history
            .filter { it.timestamp >= startTime }
            .groupBy { 
                // 按天分组
                val dayStart = it.timestamp / (24 * 60 * 60 * 1000L)
                dayStart * (24 * 60 * 60 * 1000L)
            }
            .map { (day, records) ->
                val netCoins = records.sumOf { it.coinReward - it.coinCost }
                day to netCoins
            }
            .sortedBy { it.first }
    }
    
    // 分析最幸运时间段（按小时）
    suspend fun getLuckyHourAnalysis(): Map<Int, LuckyStats> {
        val history = historyRepository.getAllHistory(getCurrentUserId()).first()
        
        return history.groupBy { 
            // 按小时分组
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = it.timestamp
            calendar.get(java.util.Calendar.HOUR_OF_DAY)
        }.mapValues { (_, records) ->
            val totalSpins = records.size
            val totalReward = records.sumOf { it.coinReward }
            val totalCost = records.sumOf { it.coinCost }
            val netProfit = totalReward - totalCost
            val avgProfit = if (totalSpins > 0) netProfit.toFloat() / totalSpins else 0f
            
            LuckyStats(
                hour = 0, // 会在外层设置
                totalSpins = totalSpins,
                totalReward = totalReward,
                totalCost = totalCost,
                netProfit = netProfit,
                avgProfit = avgProfit
            )
        }.mapKeys { (hour, stats) ->
            hour
        }.mapValues { (hour, stats) ->
            stats.copy(hour = hour)
        }
    }
    
    // 导出历史记录为 CSV
    fun exportHistoryToCsv(): String {
        val history = recentHistory.value
        val csv = StringBuilder()
        
        // CSV 头部
        csv.append("时间,模板名称,结果,模式,消耗金币,获得金币,净收益\n")
        
        // 数据行
        history.forEach { record ->
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(record.timestamp))
            val netProfit = record.coinReward - record.coinCost
            
            csv.append("$date,")
            csv.append("${record.templateName},")
            csv.append("${record.result},")
            csv.append("${record.mode},")
            csv.append("${record.coinCost},")
            csv.append("${record.coinReward},")
            csv.append("$netProfit\n")
        }
        
        return csv.toString()
    }
    
    // ========== 商品转盘功能 ==========
    
    // 商品转盘奖品定义
    data class ProductPrize(
        val name: String,
        val icon: String,
        val type: String,  // "coins", "item"
        val value: Int,    // 金币数量或商品value
        val weight: Int = 1  // 权重
    )
    
    // 商品转盘奖品列表（8个奖品）
    val productPrizes = listOf(
        ProductPrize("10金币", "💰", "coins", 10, weight = 30),
        ProductPrize("补卡卡片", "🎫", "item", 1, weight = 15),
        ProductPrize("50金币", "💰", "coins", 50, weight = 12),
        ProductPrize("宠物食物", "🍖", "item", 1, weight = 18),
        ProductPrize("20金币", "💰", "coins", 20, weight = 20),
        ProductPrize("幸运符", "🍀", "item", 1, weight = 5),
        ProductPrize("宠物玩具", "🎾", "item", 1, weight = 15),
        ProductPrize("100金币", "💰", "coins", 100, weight = 3)
    )
    
    // 用户积分
    val userShopPoints: StateFlow<Int> = coinRepository.getUserCoins(getCurrentUserId())
        .map { it?.shopPoints ?: 0 }
        .catch { e ->
            android.util.Log.e("SpinWheelViewModel", "Error loading shop points", e)
            emit(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    // 商品转盘结果消息
    private val _productSpinMessage = MutableStateFlow<String?>(null)
    val productSpinMessage: StateFlow<String?> = _productSpinMessage.asStateFlow()
    
    fun clearProductSpinMessage() {
        _productSpinMessage.value = null
    }
    
    // 检查积分是否足够（10积分一次）
    fun canAffordProductSpin(): Boolean {
        return userShopPoints.value >= 10
    }
    
    // 执行商品转盘抽取
    suspend fun performProductSpin(): ProductSpinResult {
        val userId = getCurrentUserId()
        val points = userShopPoints.value
        
        if (points < 10) {
            return ProductSpinResult.InsufficientPoints
        }
        
        // 🔒 先校验奖池非空再扣积分，避免扣后退款的非原子中间态
        val totalWeight = productPrizes.sumOf { it.weight }
        if (productPrizes.isEmpty() || totalWeight <= 0) {
            return ProductSpinResult.InsufficientPoints
        }
        // 扣除 10 积分
        val success = coinRepository.spendShopPoints(userId, 10, reason = "product_spin")
        if (!success) {
            return ProductSpinResult.InsufficientPoints
        }
        var random = Random.nextInt(totalWeight)
        var selectedPrize = productPrizes.last()
        
        for (prize in productPrizes) {
            random -= prize.weight
            if (random < 0) {
                selectedPrize = prize
                break
            }
        }
        
        // 发放奖励（金币奖项已取消）
        when (selectedPrize.type) {
            "coins" -> {
                // 🔒 不再发放金币，避免商品转盘被刷
            }
            "item" -> {
                // 添加到背包
                val database = com.example.funlife.data.database.AppDatabase.getDatabase(getApplication())
                val inventoryDao = database.inventoryDao()
                
                val itemTypeMap = mapOf(
                    "补卡卡片" to Pair("makeup_card_spin", com.example.funlife.data.model.InventoryItemType.CONSUMABLE),
                    "宠物食物" to Pair("pet_food_spin", com.example.funlife.data.model.InventoryItemType.CONSUMABLE),
                    "幸运符" to Pair("lucky_charm_spin", com.example.funlife.data.model.InventoryItemType.CONSUMABLE),
                    "宠物玩具" to Pair("pet_toy_spin", com.example.funlife.data.model.InventoryItemType.CONSUMABLE)
                )
                
                val (itemId, itemType) = itemTypeMap[selectedPrize.name] ?: Pair("unknown_spin", com.example.funlife.data.model.InventoryItemType.CONSUMABLE)
                
                // 检查是否已有该物品，有则增加数量
                val existingItem = inventoryDao.getItemByItemId(userId, itemId)
                if (existingItem != null) {
                    inventoryDao.increaseQuantity(existingItem.id, 1)
                } else {
                    val rarityMap = mapOf(
                        "补卡卡片" to com.example.funlife.data.model.ItemRarity.COMMON,
                        "宠物食物" to com.example.funlife.data.model.ItemRarity.COMMON,
                        "宠物玩具" to com.example.funlife.data.model.ItemRarity.COMMON,
                        "幸运符" to com.example.funlife.data.model.ItemRarity.RARE
                    )
                    val inventoryItem = com.example.funlife.data.model.InventoryItem(
                        userId = userId,
                        itemId = itemId,
                        itemName = selectedPrize.name,
                        itemType = itemType,
                        itemRarity = rarityMap[selectedPrize.name] ?: com.example.funlife.data.model.ItemRarity.COMMON,
                        iconEmoji = selectedPrize.icon,
                        description = "商品转盘抽取获得",
                        quantity = 1,
                        purchasePrice = 0,
                        obtainedTime = System.currentTimeMillis()
                    )
                    inventoryDao.insertItem(inventoryItem)
                }
            }
        }
        
        android.util.Log.d("SpinWheelViewModel", "商品转盘结果: ${selectedPrize.name}")
        return ProductSpinResult.Success(selectedPrize)
    }
    
    // 🧪 测试用：商品转盘演练 — 不扣积分、不发奖励，仅随机选个奖品供 UI 演示
    suspend fun performTestProductSpin(): ProductSpinResult {
        val totalWeight = productPrizes.sumOf { it.weight }
        if (totalWeight <= 0 || productPrizes.isEmpty()) {
            return ProductSpinResult.InsufficientPoints
        }
        var random = Random.nextInt(totalWeight)
        var selectedPrize = productPrizes.last()
        for (prize in productPrizes) {
            random -= prize.weight
            if (random < 0) {
                selectedPrize = prize
                break
            }
        }
        android.util.Log.d("SpinWheelViewModel", "🧪 测试商品转盘结果: ${selectedPrize.name}")
        return ProductSpinResult.Success(selectedPrize)
    }

    // 导出历史记录为 JSON
    fun exportHistoryToJson(): String {
        val history = recentHistory.value
        val json = StringBuilder()
        
        json.append("[\n")
        history.forEachIndexed { index, record ->
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(record.timestamp))
            
            json.append("  {\n")
            json.append("    \"timestamp\": \"$date\",\n")
            json.append("    \"templateName\": \"${record.templateName}\",\n")
            json.append("    \"result\": \"${record.result}\",\n")
            json.append("    \"mode\": \"${record.mode}\",\n")
            json.append("    \"coinCost\": ${record.coinCost},\n")
            json.append("    \"coinReward\": ${record.coinReward},\n")
            json.append("    \"netProfit\": ${record.coinReward - record.coinCost}\n")
            json.append("  }")
            
            if (index < history.size - 1) {
                json.append(",")
            }
            json.append("\n")
        }
        json.append("]")
        
        return json.toString()
    }
}

// 幸运时段统计
data class LuckyStats(
    val hour: Int,
    val totalSpins: Int,
    val totalReward: Int,
    val totalCost: Int,
    val netProfit: Int,
    val avgProfit: Float
)

// 转盘结果
sealed class SpinResult {
    data class Success(val result: String, val coinReward: Int, val shopPointsReward: Int = 0) : SpinResult()
    object InsufficientCoins : SpinResult()
}

// 连抽结果
sealed class MultiSpinResult {
    data class Success(val results: List<String>, val totalReward: Int, val totalShopPointsReward: Int = 0) : MultiSpinResult()
    object InsufficientCoins : MultiSpinResult()
    data class Error(val message: String) : MultiSpinResult()
}

// 商品转盘结果
sealed class ProductSpinResult {
    data class Success(val prize: SpinWheelViewModel.ProductPrize) : ProductSpinResult()
    object InsufficientPoints : ProductSpinResult()
}
