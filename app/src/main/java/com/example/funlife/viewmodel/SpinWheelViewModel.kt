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
import kotlin.random.Random

class SpinWheelViewModel(application: Application) : AndroidViewModel(application) {
    
    private val templateRepository: SpinWheelTemplateRepository
    private val historyRepository: SpinWheelHistoryRepository
    private val coinRepository: CoinRepository
    private val guaranteeCounterDao: com.example.funlife.data.dao.GuaranteeCounterDao
    private val customModeRepository: com.example.funlife.repository.CustomSpinModeRepository
    
    init {
        val database = AppDatabase.getDatabase(application)
        templateRepository = SpinWheelTemplateRepository(database.spinWheelTemplateDao())
        historyRepository = SpinWheelHistoryRepository(database.spinWheelHistoryDao())
        coinRepository = CoinRepository(database.coinDao())
        guaranteeCounterDao = database.guaranteeCounterDao()
        customModeRepository = com.example.funlife.repository.CustomSpinModeRepository(database.customSpinModeDao())
    }
    
    // 所有可用模式
    val allModes: StateFlow<List<com.example.funlife.data.model.CustomSpinMode>> = 
        customModeRepository.getAllActiveModes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 当前选中的自定义模式
    private val _currentCustomMode = MutableStateFlow<com.example.funlife.data.model.CustomSpinMode?>(null)
    val currentCustomMode: StateFlow<com.example.funlife.data.model.CustomSpinMode?> = _currentCustomMode.asStateFlow()
    
    // 保底设置
    private val _guaranteeSettings = MutableStateFlow(com.example.funlife.data.model.GuaranteeSettings())
    val guaranteeSettings: StateFlow<com.example.funlife.data.model.GuaranteeSettings> = _guaranteeSettings.asStateFlow()
    
    // 保底计数器
    val guaranteeCounters: StateFlow<List<com.example.funlife.data.model.GuaranteeCounter>> = 
        guaranteeCounterDao.getAllEnabledCounters()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 所有模板
    val allTemplates: StateFlow<List<SpinWheelTemplate>> = templateRepository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 最近历史记录
    val recentHistory: StateFlow<List<SpinWheelHistory>> = historyRepository.getRecentHistory(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 筛选后的历史记录
    private val _filteredHistory = MutableStateFlow<List<SpinWheelHistory>>(emptyList())
    val filteredHistory: StateFlow<List<SpinWheelHistory>> = _filteredHistory.asStateFlow()
    
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
    val userCoins: StateFlow<Int> = coinRepository.userCoins
        .map { it?.coins ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    // 设置：启用音效
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()
    
    // 设置：启用震动
    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()
    
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
            // 初始化金币（如果还没有记录）
            coinRepository.initializeCoins()
            
            // 给新用户一些初始金币（100金币）
            val currentCoins = coinRepository.getCoinsAmount()
            if (currentCoins == 0) {
                coinRepository.addCoins(100)
            }
            
            createDefaultTemplatesIfNeeded()
            customModeRepository.initializeDefaultModes()
            
            // 设置默认模式为第一个可用模式
            allModes.first().firstOrNull()?.let { mode ->
                _currentCustomMode.value = mode
            }
            
            updateHistoryCount()
            updateStatistics()
        }
    }
    
    // 更新历史记录数量
    private suspend fun updateHistoryCount() {
        _historyCount.value = historyRepository.getCount()
    }
    
    // 更新统计数据
    private suspend fun updateStatistics() {
        _totalSpins.value = historyRepository.getCount()
        _totalCoinsSpent.value = historyRepository.getTotalCoinCost()
        _totalCoinsEarned.value = historyRepository.getTotalCoinReward()
    }
    
    // 切换转盘模式（使用自定义模式）
    fun setCustomMode(mode: com.example.funlife.data.model.CustomSpinMode) {
        _currentCustomMode.value = mode
        viewModelScope.launch {
            customModeRepository.incrementUsageCount(mode.id)
        }
    }
    
    // 切换转盘模式（使用基础模式）
    fun setMode(mode: SpinWheelMode) {
        _currentMode.value = mode
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
        val coins = userCoins.value
        
        // 检查金币
        if (!mode.canAfford(coins)) {
            return false
        }
        
        // 扣除金币
        if (mode.costPerSpin > 0) {
            coinRepository.spendCoins(mode.costPerSpin)
        }
        
        return true
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
        
        // 发放奖励
        if (coinReward > 0) {
            coinRepository.addCoins(coinReward)
        }
        
        // 保存历史
        saveResultToHistory(selectedResult, mode, coinReward)
        
        return SpinResult.Success(selectedResult, coinReward)
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
            coinRepository.spendCoins(mode.costPerSpin)
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
            coinRepository.addCoins(coinReward)
        }
        
        // 保存历史
        saveResultToHistory(result, mode, coinReward)
        
        return SpinResult.Success(result, coinReward)
    }
    
    // 根据权重选择选项（带保底机制）
    private suspend fun selectOptionByWeight(): String {
        val options = _currentOptions.value.filter { !it.isExcluded }
        if (options.isEmpty()) return "无可用选项"
        
        // 检查保底机制
        if (_guaranteeSettings.value.enabled) {
            for (option in options) {
                val counter = guaranteeCounterDao.getCounterByOption(option.text)
                if (counter != null && counter.currentCount >= counter.guaranteeThreshold) {
                    // 触发保底
                    guaranteeCounterDao.resetCounter(option.text)
                    // 重置其他计数器
                    options.filter { it.text != option.text }.forEach {
                        guaranteeCounterDao.incrementCounter(it.text)
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
                    guaranteeCounterDao.resetCounter(option.text)
                } else {
                    guaranteeCounterDao.incrementCounter(option.text)
                }
            }
        }
        
        return result
    }
    
    // 计算幸运奖励
    private fun calculateLuckyReward(): Int {
        val chance = Random.nextInt(100)
        return when {
            chance < 5 -> 100  // 5% 概率获得100金币
            chance < 20 -> 50  // 15% 概率获得50金币
            chance < 50 -> 20  // 30% 概率获得20金币
            else -> 0          // 50% 概率无奖励
        }
    }
    
    // 保存模板
    fun saveTemplate(name: String, options: List<String>, weights: List<Int> = emptyList(), category: String = "custom") {
        viewModelScope.launch {
            val template = SpinWheelTemplate(
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
        
        // 增加使用次数
        viewModelScope.launch {
            templateRepository.update(template.copy(usageCount = template.usageCount + 1))
        }
    }
    
    // 更新当前选项
    fun updateOptions(options: List<WheelOption>) {
        _currentOptions.value = options
    }
    
    // 切换选项排除状态
    fun toggleOptionExclusion(optionText: String) {
        _currentOptions.value = _currentOptions.value.map {
            if (it.text == optionText) it.copy(isExcluded = !it.isExcluded) else it
        }
    }
    
    // 设置选项权重
    fun setOptionWeight(optionText: String, weight: Int) {
        _currentOptions.value = _currentOptions.value.map {
            if (it.text == optionText) it.copy(weight = weight.coerceIn(1, 10)) else it
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
            historyRepository.deleteAll()
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
        val templates = templateRepository.allTemplates.first()
        if (templates.isEmpty()) {
            // 美食模板
            templateRepository.insert(
                SpinWheelTemplate(
                    name = "今天吃什么",
                    options = "火锅,烧烤,日料,川菜,粤菜,西餐,快餐,面食",
                    category = "food",
                    isDefault = true
                )
            )
            
            // 娱乐模板
            templateRepository.insert(
                SpinWheelTemplate(
                    name = "周末娱乐",
                    options = "看电影,打游戏,运动健身,逛街购物,郊游,K歌,读书,睡觉",
                    category = "game",
                    isDefault = true
                )
            )
            
            // 决策模板
            templateRepository.insert(
                SpinWheelTemplate(
                    name = "做决定",
                    options = "去做,不去做,再想想,问朋友",
                    category = "decision",
                    isDefault = true
                )
            )
        }
    }
    
    private fun getDefaultWheelOptions(): List<WheelOption> {
        return listOf("吃火锅", "看电影", "打游戏", "去旅行", "读书", "运动").map {
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
        viewModelScope.launch {
            val mode = _filterMode.value
            val dateRange = _filterDateRange.value
            val query = _searchQuery.value
            
            val flow = when {
                // 有日期范围和模式
                dateRange != null && mode != null -> {
                    historyRepository.getHistoryByDateRangeAndMode(
                        dateRange.first, dateRange.second, mode
                    )
                }
                // 只有日期范围
                dateRange != null -> {
                    historyRepository.getHistoryByDateRange(
                        dateRange.first, dateRange.second
                    )
                }
                // 只有模式
                mode != null -> {
                    historyRepository.getHistoryByMode(mode)
                }
                // 只有搜索关键词
                query.isNotEmpty() -> {
                    historyRepository.searchHistoryByResult(query)
                }
                // 无筛选条件
                else -> {
                    historyRepository.getAllHistory()
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
        val coins = userCoins.value
        
        // 检查金币
        if (coins < totalCost) {
            return MultiSpinResult.InsufficientCoins
        }
        
        _isMultiSpinning.value = true
        
        // 扣除金币
        coinRepository.spendCoins(totalCost)
        
        // 执行多次抽取
        val results = mutableListOf<String>()
        var totalReward = 0
        
        for (i in 0 until count) {
            val result = selectOptionByWeight()
            results.add(result)
            
            // 计算奖励（仅幸运模式）
            if (_currentMode.value == SpinWheelMode.LUCKY) {
                val reward = calculateLuckyReward()
                totalReward += reward
            }
            
            // 保存每次的历史记录
            saveResultToHistory(result, _currentMode.value, 0)
        }
        
        // 发放总奖励
        if (totalReward > 0) {
            coinRepository.addCoins(totalReward)
        }
        
        _multiSpinResults.value = results
        _isMultiSpinning.value = false
        
        return MultiSpinResult.Success(results, totalReward)
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
        options.forEach { optionText ->
            val existing = guaranteeCounterDao.getCounterByOption(optionText)
            if (existing == null) {
                guaranteeCounterDao.insert(
                    com.example.funlife.data.model.GuaranteeCounter(
                        optionText = optionText,
                        guaranteeThreshold = _guaranteeSettings.value.defaultThreshold
                    )
                )
            }
        }
    }
    
    // 更新特定选项的保底阈值
    suspend fun updateGuaranteeThreshold(optionText: String, threshold: Int) {
        val counter = guaranteeCounterDao.getCounterByOption(optionText)
        if (counter != null) {
            guaranteeCounterDao.update(counter.copy(guaranteeThreshold = threshold))
        }
    }
    
    // 重置所有保底计数器
    suspend fun resetAllGuaranteeCounters() {
        guaranteeCounterDao.resetAllCounters()
    }
    
    // ========== 统计分析功能 ==========
    
    // 获取选项统计数据
    suspend fun getOptionStatistics(): Map<String, Int> {
        val history = historyRepository.getAllHistory().first()
        return history.groupingBy { it.result }.eachCount()
    }
    
    // 获取金币收支趋势（按天）
    suspend fun getCoinTrendByDay(days: Int = 7): List<Pair<Long, Int>> {
        val history = historyRepository.getAllHistory().first()
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
        val history = historyRepository.getAllHistory().first()
        
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
    data class Success(val result: String, val coinReward: Int) : SpinResult()
    object InsufficientCoins : SpinResult()
}

// 连抽结果
sealed class MultiSpinResult {
    data class Success(val results: List<String>, val totalReward: Int) : MultiSpinResult()
    object InsufficientCoins : MultiSpinResult()
    data class Error(val message: String) : MultiSpinResult()
}
