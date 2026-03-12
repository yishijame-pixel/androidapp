// EnhancedSpinWheelScreen.kt - 增强版转盘屏幕（简化版）
package com.example.funlife.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.SpinWheelMode
import com.example.funlife.data.model.SpinWheelTemplate
import com.example.funlife.data.model.WheelOption
import com.example.funlife.ui.components.SpinWheel
import com.example.funlife.viewmodel.SpinWheelViewModel
import com.example.funlife.viewmodel.SpinResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Suppress("DEPRECATION")
@Composable
fun EnhancedSpinWheelScreen(
    viewModel: SpinWheelViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val currentMode by viewModel.currentMode.collectAsState()
    val currentOptions by viewModel.currentOptions.collectAsState()
    val userCoins by viewModel.userCoins.collectAsState()
    val totalSpins by viewModel.totalSpins.collectAsState()
    val showWeightVisualization by viewModel.showWeightVisualization.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val multiSpinMode by viewModel.multiSpinMode.collectAsState()
    val multiSpinCount by viewModel.multiSpinCount.collectAsState()
    val currentMultiSpinProgress by viewModel.currentMultiSpinProgress.collectAsState()
    
    // 调试日志
    LaunchedEffect(userCoins, currentMode, multiSpinMode, multiSpinCount, currentOptions) {
        android.util.Log.d("EnhancedSpinWheel", "=== State Changed ===")
        android.util.Log.d("EnhancedSpinWheel", "userCoins: $userCoins")
        android.util.Log.d("EnhancedSpinWheel", "currentMode: ${currentMode.displayName} (cost: ${currentMode.costPerSpin})")
        android.util.Log.d("EnhancedSpinWheel", "multiSpinMode: $multiSpinMode")
        android.util.Log.d("EnhancedSpinWheel", "multiSpinCount: $multiSpinCount")
        android.util.Log.d("EnhancedSpinWheel", "currentOptions: ${currentOptions.size} items")
        android.util.Log.d("EnhancedSpinWheel", "currentOptions excluded: ${currentOptions.filter { !it.isExcluded }.size} active")
    }
    
    var showModeDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showEditOptionsDialog by remember { mutableStateOf(false) }
    var showTemplatesDialog by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showResultSnackbar by remember { mutableStateOf(false) }
    var showResultAnimation by remember { mutableStateOf(false) }
    var showMultiSpinResultAnimation by remember { mutableStateOf(false) }
    var animationResult by remember { mutableStateOf("") }
    var multiSpinAnimationResult by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    var showHistoryFilterDialog by remember { mutableStateOf(false) }
    var showMultiSpinDialog by remember { mutableStateOf(false) }
    var showGuaranteeDialog by remember { mutableStateOf(false) }
    var showModeManagementDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAnimationDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    
    // 幸运值系统状态
    var luckyValue by remember { mutableStateOf(0) }
    var selectedTargetOption by remember { mutableStateOf<WheelOption?>(null) }
    var showTargetSelectionDialog by remember { mutableStateOf(false) }
    
    val allTemplates by viewModel.allTemplates.collectAsState()
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 监听连抽模式变化，显示提示
    LaunchedEffect(multiSpinMode, multiSpinCount) {
        if (multiSpinMode && multiSpinCount > 1) {
            snackbarHostState.showSnackbar("✨ 已开启 ${multiSpinCount}连抽模式！点击开始旋转即可连续抽取")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("幸运转盘", fontWeight = FontWeight.Bold)
                        Text(
                            "${currentMode.emoji} ${currentMode.displayName}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 设置按钮
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                    
                    // 金币显示
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("💰", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "$userCoins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        snackbarHost = { 
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 140.dp, start = 16.dp, end = 16.dp)
                )
            }
        },
        floatingActionButton = {
            // 模式切换按钮
            ExtendedFloatingActionButton(
                onClick = { showModeDialog = true },
                icon = { Text(currentMode.emoji) },
                text = { Text(currentMode.displayName) }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = currentTheme.backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                
                // 连抽模式提示（紧凑版）
                if (multiSpinMode && currentMultiSpinProgress == 0) {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎲", fontSize = 24.sp)
                                Text(
                                    "${multiSpinCount}连抽已就绪",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(
                                onClick = { viewModel.toggleMultiSpinMode(false) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("取消", fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                // 幸运值系统 - 紧凑版
                com.example.funlife.ui.components.LuckyValueSystem(
                    luckyValue = luckyValue,
                    onLuckyValueChange = { value ->
                        luckyValue = value
                        android.util.Log.d("EnhancedSpinWheel", "Lucky value changed: $value")
                    }
                )
                
                // 目标选项选择按钮 - 优化布局
                if (selectedTargetOption != null) {
                    // 紧凑单行版本
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        shadowElevation = 4.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFFF6B6B),
                                            Color(0xFFFFD93D)
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🎯", fontSize = 20.sp)
                                    Text(
                                        selectedTargetOption!!.text,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    Surface(
                                        color = Color.White.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        val baseProb = 100f / currentOptions.filter { !it.isExcluded }.size
                                        val maxProb = 50f
                                        val totalProb = (baseProb + (maxProb - baseProb) * (luckyValue / 100f)).toInt()
                                        Text(
                                            "${totalProb}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                
                                IconButton(
                                    onClick = { selectedTargetOption = null },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        "取消",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showTargetSelectionDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF6B6B),
                                    Color(0xFFFFD93D)
                                )
                            )
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            "选择目标",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFF6B6B)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "选择目标",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
                
                // 转盘 - 使用独立的key确保权重可视化生效
                var currentSpinIndex by remember { mutableStateOf(0) }
                var isPreparingToSpin by remember { mutableStateOf(false) }
                var currentForceResult by remember { mutableStateOf<String?>(null) }
                var triggerSpin by remember { mutableStateOf(0) }
                
                // 调试日志 - 权重可视化
                LaunchedEffect(showWeightVisualization) {
                    android.util.Log.d("EnhancedSpinWheel", "=== Weight Visualization Changed ===")
                    android.util.Log.d("EnhancedSpinWheel", "showWeightVisualization: $showWeightVisualization")
                    android.util.Log.d("EnhancedSpinWheel", "currentOptions: ${currentOptions.map { "${it.text}:${it.weight}" }}")
                }
                
                // 计算是否可以旋转（响应式）
                val canSpin = remember(userCoins, currentMode, multiSpinMode, multiSpinCount, currentSpinIndex, isPreparingToSpin) {
                    if (isPreparingToSpin) return@remember false
                    
                    val result = if (multiSpinMode && currentSpinIndex == 0) {
                        val totalCost = currentMode.costPerSpin * multiSpinCount
                        userCoins >= totalCost
                    } else if (multiSpinMode && currentSpinIndex > 0) {
                        true
                    } else {
                        userCoins >= currentMode.costPerSpin
                    }
                    result
                }
                
                // 使用key强制重组SpinWheel，确保权重可视化生效
                key(currentOptions.hashCode(), currentMode, currentTheme, multiSpinMode, showWeightVisualization, triggerSpin) {
                    SpinWheel(
                        options = currentOptions.filter { !it.isExcluded }.map { it.text },
                        mode = currentMode.name,
                        canSpin = true,
                        weights = currentOptions.filter { !it.isExcluded }.map { it.weight },
                        showWeightVisualization = showWeightVisualization,
                        theme = null,
                        multiSpinMode = multiSpinMode,
                        autoSpinTrigger = triggerSpin,
                        forceResult = currentForceResult,
                        showButton = false,
                        onSpinStart = {
                            // 在旋转开始前计算forceResult
                            android.util.Log.d("EnhancedSpinWheel", "=== onSpinStart (inside SpinWheel) ===")
                            android.util.Log.d("EnhancedSpinWheel", "currentForceResult at onSpinStart: $currentForceResult")
                        },
                        onAutoSpinStart = {
                            // 不再使用
                        },
                        onResult = { result ->
                            scope.launch {
                                android.util.Log.d("EnhancedSpinWheel", "=== onResult called ===")
                                android.util.Log.d("EnhancedSpinWheel", "result: $result")
                                android.util.Log.d("EnhancedSpinWheel", "multiSpinMode: $multiSpinMode")
                                android.util.Log.d("EnhancedSpinWheel", "currentSpinIndex: $currentSpinIndex")
                                android.util.Log.d("EnhancedSpinWheel", "multiSpinCount: $multiSpinCount")
                                
                                // 重置forceResult
                                currentForceResult = null
                                
                                // 清零幸运值（仅单次模式）
                                if (!multiSpinMode && selectedTargetOption != null) {
                                    luckyValue = 0
                                    selectedTargetOption = null
                                }
                                
                                val spinResult = viewModel.processSpinResult(result)
                                
                                if (multiSpinMode) {
                                    viewModel.recordMultiSpinResult(result)
                                    
                                    android.util.Log.d("EnhancedSpinWheel", "Recorded result, currentSpinIndex: $currentSpinIndex")
                                    
                                    if (currentSpinIndex >= multiSpinCount) {
                                        // 连抽完成
                                        android.util.Log.d("EnhancedSpinWheel", "=== Multi-spin COMPLETE ===")
                                        kotlinx.coroutines.delay(500)
                                        val results = viewModel.multiSpinResults.value
                                        val summary = results.groupingBy { it }.eachCount()
                                            .entries.joinToString(", ") { "${it.key}×${it.value}" }
                                        
                                        multiSpinAnimationResult = summary
                                        showMultiSpinResultAnimation = true
                                        
                                        viewModel.resetMultiSpinState()
                                        currentSpinIndex = 0
                                        
                                        // 清零幸运值
                                        if (selectedTargetOption != null) {
                                            luckyValue = 0
                                            selectedTargetOption = null
                                        }
                                    } else {
                                        // 继续下一次抽取 - 减少延迟提升流畅度
                                        android.util.Log.d("EnhancedSpinWheel", "=== Continuing multi-spin ===")
                                        android.util.Log.d("EnhancedSpinWheel", "Waiting 100ms before next spin...")
                                        kotlinx.coroutines.delay(100)
                                        currentSpinIndex++
                                        viewModel.incrementMultiSpinProgress()
                                        
                                        // 🔥 关键修复：自动触发下一次旋转
                                        triggerSpin++
                                        android.util.Log.d("EnhancedSpinWheel", "Auto-triggered next spin, triggerSpin: $triggerSpin, currentSpinIndex: $currentSpinIndex")
                                    }
                                }
                            }
                        },
                        onMultiSpinComplete = {
                            // 不再使用
                        },
                        onShowResult = { result ->
                            // 显示结果动画（仅单次模式）
                            if (!multiSpinMode) {
                                animationResult = result
                                showResultAnimation = true
                            }
                        }
                    )
                    
                    // 自定义旋转按钮 - 放在转盘下方
                    Button(
                        onClick = {
                            scope.launch {
                                isPreparingToSpin = true
                                
                                android.util.Log.d("EnhancedSpinWheel", "=== Custom Button Clicked ===")
                                android.util.Log.d("EnhancedSpinWheel", "selectedTargetOption: ${selectedTargetOption?.text}")
                                android.util.Log.d("EnhancedSpinWheel", "luckyValue: $luckyValue")
                                
                                // 计算是否命中目标 - 50%概率
                                if (selectedTargetOption != null && luckyValue > 0) {
                                    val hitProbability = (luckyValue * 0.5f).toInt()
                                    val random = kotlin.random.Random.nextInt(100)
                                    val hit = random < hitProbability
                                    
                                    android.util.Log.d("EnhancedSpinWheel", "hitProbability: $hitProbability%, random: $random, hit: $hit")
                                    
                                    // 根据概率决定是否命中
                                    currentForceResult = if (hit) selectedTargetOption?.text else null
                                    android.util.Log.d("EnhancedSpinWheel", "currentForceResult set to: $currentForceResult")
                                } else {
                                    currentForceResult = null
                                    android.util.Log.d("EnhancedSpinWheel", "No target or lucky value is 0")
                                }
                                
                                // 扣除金币
                                if (multiSpinMode) {
                                    if (currentSpinIndex == 0) {
                                        val totalCost = currentMode.costPerSpin * multiSpinCount
                                        if (viewModel.userCoins.value < totalCost) {
                                            snackbarHostState.showSnackbar("❌ 金币不足！")
                                            currentForceResult = null
                                            isPreparingToSpin = false
                                            return@launch
                                        }
                                        repeat(multiSpinCount) {
                                            viewModel.checkAndDeductCoins()
                                        }
                                        currentSpinIndex = 1
                                        viewModel.incrementMultiSpinProgress()
                                    }
                                } else {
                                    if (!viewModel.checkAndDeductCoins()) {
                                        snackbarHostState.showSnackbar("❌ 金币不足！")
                                        currentForceResult = null
                                        isPreparingToSpin = false
                                        return@launch
                                    }
                                }
                                
                                isPreparingToSpin = false
                                
                                // 触发旋转
                                triggerSpin++
                                android.util.Log.d("EnhancedSpinWheel", "triggerSpin incremented to: $triggerSpin")
                            }
                        },
                        enabled = !isPreparingToSpin && canSpin && currentOptions.filter { !it.isExcluded }.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (currentMode.name) {
                                "LUCKY" -> Color(0xFFFFD700)
                                "ADVANCED" -> Color(0xFF9C27B0)
                                else -> Color(0xFF2196F3)
                            },
                            disabledContainerColor = Color(0xFFBDBDBD)
                        ),
                        shape = RoundedCornerShape(30.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 10.dp
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isPreparingToSpin) "⏳" else "🎯", fontSize = 24.sp)
                            Text(
                                if (isPreparingToSpin) "准备中..." else "开始旋转",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // 连抽进度条和圈中可视化
                    if (multiSpinMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 进度条
                            if (currentMultiSpinProgress > 0) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "连抽进度",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "$currentMultiSpinProgress / $multiSpinCount",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        LinearProgressIndicator(
                                            progress = currentMultiSpinProgress.toFloat() / multiSpinCount.toFloat(),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            // 圈中可视化 - 显示已抽中的结果
                            val multiSpinResults by viewModel.multiSpinResults.collectAsState()
                            if (multiSpinResults.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            "🎯 已抽中",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        // 使用FlowRow显示结果标签
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            multiSpinResults.forEach { result ->
                                                AssistChip(
                                                    onClick = { },
                                                    label = { Text(result) },
                                                    leadingIcon = {
                                                        Text("✨", fontSize = 14.sp)
                                                    },
                                                    colors = AssistChipDefaults.assistChipColors(
                                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                )
                                            }
                                        }
                                        
                                        // 统计信息
                                        val resultCounts = multiSpinResults.groupingBy { it }.eachCount()
                                        if (resultCounts.size > 1) {
                                            Divider()
                                            Text(
                                                "统计",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                            )
                                            resultCounts.entries.sortedByDescending { it.value }.forEach { (result, count) ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        result,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        "×$count",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    
    // 获得动画（覆盖在整个屏幕上）- 保留
    if (showResultAnimation) {
        com.example.funlife.ui.components.ResultAnimation(
            result = animationResult,
            onDismiss = { showResultAnimation = false }
        )
    }
    
    // 连抽结算动画
    if (showMultiSpinResultAnimation) {
        com.example.funlife.ui.components.MultiSpinResultAnimation(
            results = multiSpinAnimationResult,
            onDismiss = { showMultiSpinResultAnimation = false }
        )
    }
    
    // 模式选择对话框
    if (showModeDialog) {
        ModeSelectionDialog(
            currentMode = currentMode,
            userCoins = userCoins,
            onModeSelected = { mode ->
                viewModel.setMode(mode)
                showModeDialog = false
            },
            onDismiss = { showModeDialog = false }
        )
    }
    
    // 目标选项选择对话框
    if (showTargetSelectionDialog) {
        TargetSelectionDialog(
            options = currentOptions,
            onSelect = { option ->
                selectedTargetOption = option
                showTargetSelectionDialog = false
            },
            onDismiss = { showTargetSelectionDialog = false }
        )
    }
    
    // 统计对话框
    if (showStatsDialog) {
        StatisticsDialog(
            viewModel = viewModel,
            onDismiss = { showStatsDialog = false }
        )
    }
    
    // 选项管理对话框
    if (showOptionsDialog) {
        OptionsManagementDialog(
            options = currentOptions,
            onOptionsUpdated = { updatedOptions ->
                viewModel.updateOptions(updatedOptions)
            },
            onDismiss = { showOptionsDialog = false }
        )
    }
    
    // 编辑选项对话框
    if (showEditOptionsDialog) {
        EditOptionsDialog(
            currentOptions = currentOptions,
            onOptionsUpdated = { updatedOptions ->
                viewModel.updateOptions(updatedOptions)
                showEditOptionsDialog = false
            },
            onDismiss = { showEditOptionsDialog = false }
        )
    }
    
    // 模板管理对话框
    if (showTemplatesDialog) {
        TemplatesDialog(
            templates = allTemplates,
            onSelectTemplate = { template ->
                viewModel.loadTemplate(template)
                showTemplatesDialog = false
            },
            onSaveTemplate = {
                showTemplatesDialog = false
                showSaveTemplateDialog = true
            },
            onDeleteTemplate = { template ->
                viewModel.deleteTemplate(template)
            },
            onDismiss = { showTemplatesDialog = false }
        )
    }
    
    // 保存模板对话框
    if (showSaveTemplateDialog) {
        SaveTemplateDialog(
            currentOptions = currentOptions,
            onSave = { name, category ->
                viewModel.saveTemplate(
                    name = name,
                    options = currentOptions.map { it.text },
                    weights = currentOptions.map { it.weight },
                    category = category
                )
                showSaveTemplateDialog = false
            },
            onDismiss = { showSaveTemplateDialog = false }
        )
    }
    
    // 历史筛选对话框
    if (showHistoryFilterDialog) {
        HistoryFilterDialog(
            viewModel = viewModel,
            onDismiss = { showHistoryFilterDialog = false }
        )
    }
    
    // 连抽对话框
    if (showMultiSpinDialog) {
        MultiSpinDialog(
            viewModel = viewModel,
            onDismiss = { showMultiSpinDialog = false }
        )
    }
    
    // 保底设置对话框
    if (showGuaranteeDialog) {
        GuaranteeSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showGuaranteeDialog = false }
        )
    }
    
    // 模式管理对话框
    if (showModeManagementDialog) {
        CustomModeManagementDialog(
            viewModel = viewModel,
            onDismiss = { showModeManagementDialog = false }
        )
    }
    
    // 主题选择对话框
    if (showThemeDialog) {
        ThemeSelectionDialog(
            viewModel = viewModel,
            onDismiss = { showThemeDialog = false }
        )
    }
    
    // 动画设置对话框
    if (showAnimationDialog) {
        AnimationSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showAnimationDialog = false }
        )
    }
    
    // 设置底部表单
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text("🎡", fontSize = 28.sp)
                    Text(
                        "转盘设置",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 基础设置分组
                SettingsGroupHeader(title = "基础设置", emoji = "⚙️")
                
                SettingsItem(
                    title = "模式管理",
                    description = "切换和管理转盘模式",
                    icon = Icons.Default.Category,
                    iconColor = Color(0xFFFF6B9D),
                    onClick = { showModeManagementDialog = true }
                )
                
                SettingsItem(
                    title = "模板管理",
                    description = "保存和加载转盘模板",
                    icon = Icons.Default.Folder,
                    iconColor = Color(0xFFFFD93D),
                    onClick = { showTemplatesDialog = true }
                )
                
                SettingsItem(
                    title = "编辑选项",
                    description = "自定义转盘选项和权重",
                    icon = Icons.Default.Edit,
                    iconColor = Color(0xFF4CAF50),
                    onClick = { showEditOptionsDialog = true }
                )
                
                // 显示设置分组
                SettingsGroupHeader(title = "显示设置", emoji = "👁️")
                
                // 权重可视化 - 特殊卡片
                WeightVisualizationSettingsCard(
                    checked = showWeightVisualization,
                    onCheckedChange = { viewModel.toggleWeightVisualization() }
                )
                
                // 抽取功能分组
                SettingsGroupHeader(title = "抽取功能", emoji = "🎲")
                
                SettingsItem(
                    title = "连抽功能",
                    description = "3/5/10连抽，享受折扣",
                    icon = Icons.Default.Repeat,
                    iconColor = Color(0xFF2196F3),
                    onClick = { showMultiSpinDialog = true }
                )
                
                SettingsItem(
                    title = "保底设置",
                    description = "设置选项保底次数",
                    icon = Icons.Default.Shield,
                    iconColor = Color(0xFFAB47BC),
                    onClick = { showGuaranteeDialog = true }
                )
                
                // 外观设置分组
                SettingsGroupHeader(title = "外观设置", emoji = "🎨")
                
                SettingsItem(
                    title = "主题设置",
                    description = "选择转盘主题颜色",
                    icon = Icons.Default.Palette,
                    iconColor = Color(0xFFFF9800),
                    onClick = { showThemeDialog = true }
                )
                
                SettingsItem(
                    title = "动画设置",
                    description = "开关各种动画效果",
                    icon = Icons.Default.Animation,
                    iconColor = Color(0xFF00BCD4),
                    onClick = { showAnimationDialog = true }
                )
                
                // 数据管理分组
                SettingsGroupHeader(title = "数据管理", emoji = "📊")
                
                SettingsItem(
                    title = "历史筛选",
                    description = "筛选和导出历史记录",
                    icon = Icons.Default.FilterList,
                    iconColor = Color(0xFF9C27B0),
                    onClick = { showHistoryFilterDialog = true }
                )
                
                SettingsItem(
                    title = "统计数据",
                    description = "查看详细统计图表",
                    icon = Icons.Default.BarChart,
                    iconColor = Color(0xFFFF5722),
                    onClick = { showStatsDialog = true }
                )
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ModeSelectionDialog(
    currentMode: SpinWheelMode,
    userCoins: Int,
    onModeSelected: (SpinWheelMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择转盘模式", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SpinWheelMode.values().forEach { mode ->
                    val canAfford = mode.canAfford(userCoins)
                    val isSelected = mode == currentMode
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = canAfford) { 
                                if (canAfford) onModeSelected(mode) 
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                !canAfford -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(mode.emoji, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        mode.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    mode.description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(8.dp))
                                mode.features.forEach { feature ->
                                    Text(
                                        "• $feature",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    "已选择",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else if (!canAfford) {
                                Icon(
                                    Icons.Default.Lock,
                                    "金币不足",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun StatisticsDialog(
    viewModel: SpinWheelViewModel,
    onDismiss: () -> Unit
) {
    val totalSpins by viewModel.totalSpins.collectAsState()
    val totalCoinsSpent by viewModel.totalCoinsSpent.collectAsState()
    val totalCoinsEarned by viewModel.totalCoinsEarned.collectAsState()
    val recentHistory by viewModel.recentHistory.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var optionStats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var coinTrend by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var luckyHours by remember { mutableStateOf<Map<Int, com.example.funlife.viewmodel.LuckyStats>>(emptyMap()) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // 加载统计数据
    LaunchedEffect(Unit) {
        scope.launch {
            optionStats = viewModel.getOptionStatistics()
            
            val trend = viewModel.getCoinTrendByDay(7)
            coinTrend = trend.map { (timestamp, coins) ->
                val date = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
                    .format(java.util.Date(timestamp))
                date to coins
            }
            
            luckyHours = viewModel.getLuckyHourAnalysis()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📊 转盘统计", fontWeight = FontWeight.Bold)
                IconButton(onClick = { showExportDialog = true }) {
                    Icon(Icons.Default.Share, "导出")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标签页
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("总览") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("选项分布") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("金币趋势") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("幸运时段") }
                    )
                }
                
                when (selectedTab) {
                    0 -> {
                        // 总览
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatRow("🎯 总转数", "$totalSpins 次")
                                    StatRow("💸 消耗金币", "$totalCoinsSpent 枚")
                                    StatRow("💰 获得金币", "$totalCoinsEarned 枚")
                                    val netCoins = totalCoinsEarned - totalCoinsSpent
                                    StatRow(
                                        "📈 净收益",
                                        "$netCoins 枚",
                                        color = if (netCoins >= 0) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            
                            // 最近记录
                            if (recentHistory.isNotEmpty()) {
                                Text(
                                    "最近记录",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                recentHistory.take(5).forEach { history ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                history.result,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (history.coinReward > 0) {
                                                Text(
                                                    "+${history.coinReward}💰",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    1 -> {
                        // 选项分布饼图
                        if (optionStats.isNotEmpty()) {
                            com.example.funlife.ui.components.PieChart(
                                data = optionStats,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    2 -> {
                        // 金币趋势折线图
                        if (coinTrend.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "近7天金币收支",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                com.example.funlife.ui.components.LineChart(
                                    data = coinTrend,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    
                    3 -> {
                        // 幸运时段分析
                        if (luckyHours.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    "各时段平均收益",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                com.example.funlife.ui.components.BarChart(
                                    data = luckyHours.mapValues { it.value.avgProfit },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                // 最幸运和最倒霉时段
                                val bestHour = luckyHours.maxByOrNull { it.value.avgProfit }
                                val worstHour = luckyHours.minByOrNull { it.value.avgProfit }
                                
                                if (bestHour != null && worstHour != null) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("🍀 最幸运时段")
                                                Text(
                                                    "${bestHour.key}:00 - ${bestHour.key + 1}:00",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("😢 最倒霉时段")
                                                Text(
                                                    "${worstHour.key}:00 - ${worstHour.key + 1}:00",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
    
    // 导出对话框
    if (showExportDialog) {
        ExportDialog(
            viewModel = viewModel,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun OptionsManagementDialog(
    options: List<WheelOption>,
    onOptionsUpdated: (List<WheelOption>) -> Unit,
    onDismiss: () -> Unit
) {
    var editedOptions by remember { mutableStateOf(options) }
    val totalWeight = editedOptions.filter { !it.isExcluded }.sumOf { it.weight }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚙️ 选项管理", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 说明文字
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        "调整权重可以改变选项被抽中的概率。权重越高，被抽中的概率越大。",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // 选项列表
                editedOptions.forEachIndexed { index, option ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (option.isExcluded) 
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 选项名称和排除开关
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    option.text,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (option.isExcluded) 
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        if (option.isExcluded) "已排除" else "启用",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Switch(
                                        checked = !option.isExcluded,
                                        onCheckedChange = { enabled ->
                                            editedOptions = editedOptions.toMutableList().apply {
                                                this[index] = option.copy(isExcluded = !enabled)
                                            }
                                        }
                                    )
                                }
                            }
                            
                            // 权重调整（仅未排除的选项）
                            if (!option.isExcluded) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "权重: ${option.weight}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "概率: ${String.format("%.1f", option.getProbability(totalWeight))}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Slider(
                                        value = option.weight.toFloat(),
                                        onValueChange = { newWeight ->
                                            editedOptions = editedOptions.toMutableList().apply {
                                                this[index] = option.copy(weight = newWeight.toInt())
                                            }
                                        },
                                        valueRange = 1f..10f,
                                        steps = 8,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOptionsUpdated(editedOptions)
                    onDismiss()
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun EditOptionsDialog(
    currentOptions: List<WheelOption>,
    onOptionsUpdated: (List<WheelOption>) -> Unit,
    onDismiss: () -> Unit
) {
    var optionsText by remember { 
        mutableStateOf(currentOptions.joinToString("\n") { it.text }) 
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "✏️ 编辑转盘选项",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 说明卡片
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        "💡 每行输入一个选项，建议 4-8 个选项效果最佳",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 输入框
                OutlinedTextField(
                    value = optionsText,
                    onValueChange = { optionsText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .verticalScroll(rememberScrollState()),
                    placeholder = { 
                        Text(
                            "吃火锅\n看电影\n打游戏\n去旅行",
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                
                // 选项计数
                val optionCount = optionsText.split("\n").filter { it.isNotBlank() }.size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "当前选项数：$optionCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (optionCount in 2..12) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    
                    if (optionCount < 2) {
                        Text(
                            "至少需要2个选项",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (optionCount > 12) {
                        Text(
                            "最多12个选项",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newOptionTexts = optionsText
                        .split("\n")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    
                    if (newOptionTexts.size in 2..12) {
                        // 保留现有选项的权重和排除状态（如果存在）
                        val updatedOptions = newOptionTexts.map { text ->
                            val existingOption = currentOptions.find { it.text == text }
                            existingOption?.copy(text = text) ?: WheelOption(text = text, weight = 1)
                        }
                        onOptionsUpdated(updatedOptions)
                    }
                },
                enabled = optionsText.split("\n").filter { it.isNotBlank() }.size in 2..12
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun TemplatesDialog(
    templates: List<SpinWheelTemplate>,
    onSelectTemplate: (SpinWheelTemplate) -> Unit,
    onSaveTemplate: () -> Unit,
    onDeleteTemplate: (SpinWheelTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📁 转盘模板", fontWeight = FontWeight.Bold)
                IconButton(onClick = onSaveTemplate) {
                    Icon(Icons.Default.Add, "保存当前")
                }
            }
        },
        text = {
            if (templates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📝", style = MaterialTheme.typography.displayMedium)
                        Text(
                            "暂无模板",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "点击右上角 + 保存当前选项为模板",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    templates.forEach { template ->
                        TemplateCard(
                            template = template,
                            onClick = { onSelectTemplate(template) },
                            onDelete = { onDeleteTemplate(template) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun TemplateCard(
    template: SpinWheelTemplate,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        getCategoryEmoji(template.category),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${template.getOptionsList().size} 个选项 · 使用 ${template.usageCount} 次",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!template.isDefault) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveTemplateDialog(
    currentOptions: List<WheelOption>,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("custom") }
    
    val categories = listOf(
        "custom" to "自定义",
        "food" to "美食",
        "game" to "娱乐",
        "sport" to "运动",
        "decision" to "决策",
        "study" to "学习"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💾 保存为模板", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 模板名称
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("模板名称") },
                    placeholder = { Text("例如：今天吃什么") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 分类选择
                Text(
                    "选择分类",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedCategory == key,
                                    onClick = { selectedCategory = key },
                                    label = { Text(label) },
                                    leadingIcon = {
                                        Text(getCategoryEmoji(key))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // 填充空白
                            repeat(3 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                
                // 预览
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "预览",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            currentOptions.joinToString(", ") { it.text },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (templateName.isNotBlank()) {
                        onSave(templateName, selectedCategory)
                    }
                },
                enabled = templateName.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun getCategoryEmoji(category: String): String {
    return when (category) {
        "food" -> "🍜"
        "game" -> "🎮"
        "sport" -> "⚽"
        "decision" -> "🤔"
        "study" -> "📚"
        else -> "📝"
    }
}

// ========== 新增功能组件 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterDialog(
    viewModel: SpinWheelViewModel,
    onDismiss: () -> Unit
) {
    val filteredHistory by viewModel.filteredHistory.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var selectedMode by remember { mutableStateOf(filterMode) }
    var searchText by remember { mutableStateOf(searchQuery) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // 初始化时应用筛选
    LaunchedEffect(Unit) {
        // viewModel.applyFilters() // 私有方法，不需要手动调用
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text("🔍 历史记录筛选", fontWeight = FontWeight.Bold) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 搜索框
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { 
                        searchText = it
                        viewModel.setSearchQuery(it)
                    },
                    label = { Text("搜索结果") },
                    placeholder = { Text("输入关键词...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 模式筛选
                Text(
                    "按模式筛选",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMode == null,
                        onClick = { 
                            selectedMode = null
                            viewModel.setFilterMode(null)
                        },
                        label = { Text("全部") }
                    )
                    
                    SpinWheelMode.values().forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode.name,
                            onClick = { 
                                selectedMode = mode.name
                                viewModel.setFilterMode(mode.name)
                            },
                            label = { Text("${mode.emoji} ${mode.displayName}") }
                        )
                    }
                }
                
                // 日期筛选按钮
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, null)
                    Spacer(Modifier.width(8.dp))
                    Text("按日期筛选")
                }
                
                // 清除筛选按钮
                if (selectedMode != null || searchText.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            selectedMode = null
                            searchText = ""
                            viewModel.clearFilters()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Clear, null)
                        Spacer(Modifier.width(8.dp))
                        Text("清除所有筛选")
                    }
                }
                
                Divider()
                
                // 筛选结果
                Text(
                    "筛选结果 (${filteredHistory.size} 条)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🔍", style = MaterialTheme.typography.displayMedium)
                            Text(
                                "没有找到匹配的记录",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredHistory.take(10).forEach { history ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                SpinWheelMode.valueOf(history.mode).emoji,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                history.result,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            history.templateName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    if (history.coinReward > 0) {
                                        Text(
                                            "+${history.coinReward}💰",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (filteredHistory.size > 10) {
                            Text(
                                "还有 ${filteredHistory.size - 10} 条记录...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun MultiSpinDialog(
    viewModel: SpinWheelViewModel,
    onDismiss: () -> Unit
) {
    val currentMode by viewModel.currentMode.collectAsState()
    val userCoins by viewModel.userCoins.collectAsState()
    val multiSpinMode by viewModel.multiSpinMode.collectAsState()
    
    var selectedCount by remember { mutableStateOf(3) }
    
    // 计算价格
    val baseCost = currentMode.costPerSpin
    val totalCost = when (selectedCount) {
        3 -> (baseCost * 3 * 0.9).toInt()
        5 -> (baseCost * 5 * 0.85).toInt()
        10 -> (baseCost * 10 * 0.8).toInt()
        else -> baseCost
    }
    val discount = when (selectedCount) {
        3 -> "9折"
        5 -> "85折"
        10 -> "8折"
        else -> "无折扣"
    }
    val savedCoins = (baseCost * selectedCount) - totalCost
    val canAfford = userCoins >= totalCost
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text("🎲 连续抽取", fontWeight = FontWeight.Bold) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 选择连抽次数
                Text(
                    "选择抽取次数",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3, 5, 10).forEach { count ->
                        val cost = when (count) {
                            3 -> (baseCost * 3 * 0.9).toInt()
                            5 -> (baseCost * 5 * 0.85).toInt()
                            10 -> (baseCost * 10 * 0.8).toInt()
                            else -> baseCost * count
                        }
                        val affordable = userCoins >= cost
                        
                        FilterChip(
                            selected = selectedCount == count,
                            onClick = { selectedCount = count },
                            label = { 
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("${count}连抽")
                                    Text(
                                        "$cost 💰",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            enabled = affordable,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // 价格信息卡片
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("原价")
                            Text(
                                "${baseCost * selectedCount} 💰",
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("折扣")
                            Text(
                                discount,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Divider()
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "实付",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$totalCost 💰",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        if (savedCoins > 0) {
                            Text(
                                "💰 节省 $savedCoins 金币",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // 当前金币
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("当前金币")
                    Text(
                        "$userCoins 💰",
                        fontWeight = FontWeight.Bold,
                        color = if (canAfford) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
                
                if (!canAfford) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            "❌ 金币不足，还需要 ${totalCost - userCoins} 金币",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.setMultiSpinCount(selectedCount)
                    viewModel.toggleMultiSpinMode(true)
                    onDismiss()
                },
                enabled = canAfford
            ) {
                Text("开始连抽")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 导出对话框
@Composable
fun ExportDialog(
    viewModel: SpinWheelViewModel,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf("CSV") }
    var exportContent by remember { mutableStateOf("") }
    var showContent by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📤 导出历史记录", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!showContent) {
                    // 选择格式
                    Text(
                        "选择导出格式",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFormat == "CSV",
                            onClick = { selectedFormat = "CSV" },
                            label = { Text("CSV") },
                            modifier = Modifier.weight(1f)
                        )
                        
                        FilterChip(
                            selected = selectedFormat == "JSON",
                            onClick = { selectedFormat = "JSON" },
                            label = { Text("JSON") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // 说明
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "💡 导出说明",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "• CSV 格式适合用 Excel 打开",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "• JSON 格式适合程序处理",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "• 导出内容可复制或分享",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    // 显示导出内容
                    Text(
                        "导出内容",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(
                                    exportContent,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                    
                    // 操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // 复制到剪贴板
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) 
                                    as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("转盘历史", exportContent)
                                clipboard.setPrimaryClip(clip)
                                
                                android.widget.Toast.makeText(
                                    context,
                                    "已复制到剪贴板",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("复制")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                // 分享
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, exportContent)
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "转盘历史记录")
                                }
                                context.startActivity(
                                    android.content.Intent.createChooser(intent, "分享历史记录")
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("分享")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!showContent) {
                Button(
                    onClick = {
                        exportContent = when (selectedFormat) {
                            "CSV" -> viewModel.exportHistoryToCsv()
                            "JSON" -> viewModel.exportHistoryToJson()
                            else -> ""
                        }
                        showContent = true
                    }
                ) {
                    Text("生成")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("完成")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}


// 保底设置对话框
@Composable
fun GuaranteeSettingsDialog(
    viewModel: SpinWheelViewModel,
    onDismiss: () -> Unit
) {
    val guaranteeSettings by viewModel.guaranteeSettings.collectAsState()
    val guaranteeCounters by viewModel.guaranteeCounters.collectAsState()
    val currentOptions by viewModel.currentOptions.collectAsState()
    
    var enabled by remember { mutableStateOf(guaranteeSettings.enabled) }
    var defaultThreshold by remember { mutableIntStateOf(guaranteeSettings.defaultThreshold) }
    
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🍀 保底设置", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 启用开关
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "启用保底机制",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "连续未中特定选项时必中",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it }
                        )
                    }
                }
                
                if (enabled) {
                    // 默认保底次数
                    Text(
                        "默认保底次数",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${defaultThreshold} 次")
                        Slider(
                            value = defaultThreshold.toFloat(),
                            onValueChange = { defaultThreshold = it.toInt() },
                            valueRange = 5f..20f,
                            steps = 14,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // 当前选项保底状态
                    Text(
                        "当前选项保底状态",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (guaranteeCounters.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                "💡 开始转盘后将自动创建保底计数器",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            guaranteeCounters.forEach { counter ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (counter.currentCount >= counter.guaranteeThreshold) 
                                            MaterialTheme.colorScheme.errorContainer
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                counter.optionText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "保底: ${counter.guaranteeThreshold} 次",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "${counter.currentCount} / ${counter.guaranteeThreshold}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (counter.currentCount >= counter.guaranteeThreshold)
                                                    MaterialTheme.colorScheme.error
                                                else
                                                    MaterialTheme.colorScheme.primary
                                            )
                                            if (counter.currentCount >= counter.guaranteeThreshold) {
                                                Text(
                                                    "下次必中！",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // 重置按钮
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                viewModel.resetAllGuaranteeCounters()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("重置所有计数器")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.setGuaranteeEnabled(enabled)
                    viewModel.setDefaultGuaranteeThreshold(defaultThreshold)
                    
                    if (enabled) {
                        scope.launch {
                            viewModel.initializeGuaranteeCounters(
                                currentOptions.map { it.text }
                            )
                        }
                    }
                    
                    onDismiss()
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}


// 自定义模式管理对话框
@Composable
fun CustomModeManagementDialog(
    viewModel: SpinWheelViewModel,
    onDismiss: () -> Unit
) {
    val allModes by viewModel.allModes.collectAsState()
    val currentMode by viewModel.currentCustomMode.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingMode by remember { mutableStateOf<com.example.funlife.data.model.CustomSpinMode?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎨 模式管理", fontWeight = FontWeight.Bold)
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, "创建模式")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (allModes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🎲", style = MaterialTheme.typography.displayMedium)
                            Text("暂无模式", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else {
                    // 默认模式
                    val defaultModes = allModes.filter { it.isDefault }
                    if (defaultModes.isNotEmpty()) {
                        Text(
                            "预设模式",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        defaultModes.forEach { mode ->
                            ModeCard(
                                mode = mode,
                                isSelected = mode.id == currentMode?.id,
                                onSelect = { viewModel.setCustomMode(mode) },
                                onEdit = null, // 预设模式不可编辑
                                onDelete = null
                            )
                        }
                    }
                    
                    // 自定义模式
                    val customModes = allModes.filter { !it.isDefault }
                    if (customModes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "自定义模式",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        customModes.forEach { mode ->
                            ModeCard(
                                mode = mode,
                                isSelected = mode.id == currentMode?.id,
                                onSelect = { viewModel.setCustomMode(mode) },
                                onEdit = {
                                    editingMode = mode
                                    showEditDialog = true
                                },
                                onDelete = {
                                    // TODO: 实现删除功能
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
    
    // 创建模式对话框
    if (showCreateDialog) {
        CreateModeDialog(
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false }
        )
    }
    
    // 编辑模式对话框
    if (showEditDialog && editingMode != null) {
        EditModeDialog(
            viewModel = viewModel,
            mode = editingMode!!,
            onDismiss = { 
                showEditDialog = false
                editingMode = null
            }
        )
    }
}

@Composable
fun ModeCard(
    mode: com.example.funlife.data.model.CustomSpinMode,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(mode.emoji, style = MaterialTheme.typography.titleLarge)
                    Text(
                        mode.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(8.dp))
                
                mode.getFeatures().forEach { feature ->
                    Text(
                        "• $feature",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (mode.usageCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "使用 ${mode.usageCount} 次",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            "编辑",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        "已选择",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateModeDialog(
    viewModel: SpinWheelViewModel,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🎲") }
    var description by remember { mutableStateOf("") }
    var costPerSpin by remember { mutableIntStateOf(0) }
    var hasReward by remember { mutableStateOf(false) }
    var rewardMultiplier by remember { mutableFloatStateOf(1.0f) }
    
    val emojis = listOf("🎲", "🎯", "⚡", "💰", "🍀", "✨", "🎪", "🎨", "🎭", "🎰")
    
    val scope = rememberCoroutineScope()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✨ 创建自定义模式", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 模式名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("模式名称") },
                    placeholder = { Text("例如：超级幸运模式") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // 图标选择
                Text(
                    "选择图标",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojis.forEach { e ->
                        FilterChip(
                            selected = emoji == e,
                            onClick = { emoji = e },
                            label = { Text(e, fontSize = 24.sp) }
                        )
                    }
                }
                
                // 描述
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述") },
                    placeholder = { Text("简单描述这个模式的特点") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                
                // 金币消耗
                Text(
                    "每次消耗金币",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$costPerSpin 💰")
                    Slider(
                        value = costPerSpin.toFloat(),
                        onValueChange = { costPerSpin = it.toInt() },
                        valueRange = 0f..50f,
                        steps = 49,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // 是否有奖励
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("有机会获得金币奖励")
                        Switch(
                            checked = hasReward,
                            onCheckedChange = { hasReward = it }
                        )
                    }
                }
                
                // 奖励倍率
                if (hasReward) {
                    Text(
                        "奖励倍率",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${String.format("%.1f", rewardMultiplier)}x")
                        Slider(
                            value = rewardMultiplier,
                            onValueChange = { rewardMultiplier = it },
                            valueRange = 1.0f..3.0f,
                            steps = 19,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        // TODO: 实现创建模式
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun EditModeDialog(
    viewModel: SpinWheelViewModel,
    mode: com.example.funlife.data.model.CustomSpinMode,
    onDismiss: () -> Unit
) {
    // 类似 CreateModeDialog，但预填充现有数据
    // 为了简洁，这里省略实现
    onDismiss()
}


// 主题选择对话框
@Composable
fun ThemeSelectionDialog(
    viewModel: SpinWheelViewModel,
    onDismiss: () -> Unit
) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    val allThemes = com.example.funlife.data.model.WheelThemes.getAllThemes()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎨 选择主题", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 默认主题
                Text(
                    "默认主题",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                allThemes.filter { it.isDefault }.forEach { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = theme.id == currentTheme.id,
                        onClick = { viewModel.setTheme(theme) }
                    )
                }
                
                // 季节主题
                val seasonalThemes = allThemes.filter { it.isSeasonalTheme }
                if (seasonalThemes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "季节主题",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    seasonalThemes.forEach { theme ->
                        ThemeCard(
                            theme = theme,
                            isSelected = theme.id == currentTheme.id,
                            onClick = { viewModel.setTheme(theme) }
                        )
                    }
                }
                
                // 其他主题
                val otherThemes = allThemes.filter { !it.isDefault && !it.isSeasonalTheme }
                if (otherThemes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "更多主题",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    otherThemes.forEach { theme ->
                        ThemeCard(
                            theme = theme,
                            isSelected = theme.id == currentTheme.id,
                            onClick = { viewModel.setTheme(theme) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun ThemeCard(
    theme: com.example.funlife.data.model.WheelTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(theme.emoji, style = MaterialTheme.typography.titleLarge)
                    Text(
                        theme.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(8.dp))
                
                // 颜色预览
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    theme.wheelColors.take(6).forEach { (color1, color2) ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(color1, color2)
                                    ),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                }
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    "已选择",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// 动画设置对话框
@Composable
fun AnimationSettingsDialog(
    viewModel: SpinWheelViewModel,
    onDismiss: () -> Unit
) {
    val particleEnabled by viewModel.particleEffectEnabled.collectAsState()
    val fireworksEnabled by viewModel.fireworksEnabled.collectAsState()
    val coinAnimationEnabled by viewModel.coinAnimationEnabled.collectAsState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✨ 动画设置", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        "💡 关闭动画可以提升性能和节省电量",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // 粒子效果
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "✨ 粒子效果",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "转盘旋转时的粒子特效",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = particleEnabled,
                            onCheckedChange = { viewModel.toggleParticleEffect() }
                        )
                    }
                }
                
                // 烟花效果
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🎆 烟花动画",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "抽中结果时的烟花特效",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = fireworksEnabled,
                            onCheckedChange = { viewModel.toggleFireworks() }
                        )
                    }
                }
                
                // 金币动画
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "💰 金币动画",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "获得金币时的飘动效果",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = coinAnimationEnabled,
                            onCheckedChange = { viewModel.toggleCoinAnimation() }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}


// 设置分组标题
@Composable
private fun SettingsGroupHeader(
    title: String,
    emoji: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// 权重可视化设置卡片
@Composable
private fun WeightVisualizationSettingsCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1.02f else 1f,
        animationSpec = tween(300)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (checked) 4.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (checked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 图标容器
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (checked) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFD93D),
                                        Color(0xFFFF9800)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PieChart,
                        contentDescription = null,
                        tint = if (checked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        "权重可视化",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (checked) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        if (checked) "已开启 - 显示权重" else "显示选项权重大小",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (checked) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFFD93D),
                    checkedTrackColor = Color(0xFFFFD93D).copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 彩色图标容器
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                iconColor.copy(alpha = 0.8f),
                                iconColor
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 箭头图标
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


// 目标选项选择对话框
@Composable
private fun TargetSelectionDialog(
    options: List<WheelOption>,
    onSelect: (WheelOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎯 选择目标选项") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "选择一个目标选项，幸运值越高，转到该选项的概率越大！",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                
                options.filter { !it.isExcluded }.forEach { option ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onSelect(option) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                option.text,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.Star, "选择")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
