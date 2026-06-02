// EnhancedSpinWheelScreen.kt - 增强版转盘屏幕（简化版）
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.SpinWheelMode
import com.example.funlife.data.model.SpinWheelTemplate
import com.example.funlife.data.model.WheelOption
import com.example.funlife.ui.components.SpinWheel
import com.example.funlife.ui.components.ImageBasedSpinWheel
import com.example.funlife.viewmodel.SpinWheelViewModel
import kotlinx.coroutines.launch
import com.example.funlife.viewmodel.SpinResult
import kotlinx.coroutines.launch
import com.example.funlife.utils.SoundEffectManager
import com.example.funlife.utils.SoundEffect
import com.example.funlife.ui.components.ProductSpinWheel
import com.example.funlife.viewmodel.ProductSpinResult

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Suppress("DEPRECATION")
@Composable
fun EnhancedSpinWheelScreen(
    viewModel: SpinWheelViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val soundManager = remember { SoundEffectManager.getInstance(context) }
    
    val currentMode by viewModel.currentMode.collectAsState()
    val currentOptions by viewModel.currentOptions.collectAsState()
    val userCoins by viewModel.userCoins.collectAsState()
    val totalSpins by viewModel.totalSpins.collectAsState()
    val showWeightVisualization by viewModel.showWeightVisualization.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val multiSpinMode by viewModel.multiSpinMode.collectAsState()
    val multiSpinCount by viewModel.multiSpinCount.collectAsState()
    val currentMultiSpinProgress by viewModel.currentMultiSpinProgress.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()
    
    // 商品转盘相关状态
    val userShopPoints by viewModel.userShopPoints.collectAsState()
    var isProductWheelMode by remember { mutableStateOf(false) }
    var isProductSpinning by remember { mutableStateOf(false) }
    var productSpinResult by remember { mutableStateOf<SpinWheelViewModel.ProductPrize?>(null) }
    
    // 加载状态 - 显示1秒加载动画
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000) // 显示1秒加载动画
        isLoading = false
    }
    
    // 如果正在加载，显示加载动画
    if (isLoading) {
        com.example.funlife.ui.components.SpinWheelLoadingAnimation()
        return
    }
    
    // 显示保存消息
    LaunchedEffect(saveMessage) {
        saveMessage?.let { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearSaveMessage()
        }
    }
    
    // 🔥 获取用户偏好中的按钮皮肤
    val userPreferencesRepository = remember {
        com.example.funlife.repository.UserPreferencesRepository(
            (context.applicationContext as com.example.funlife.FunLifeApplication).database.userPreferencesDao()
        )
    }
    val authViewModel: com.example.funlife.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val userSession = authViewModel.getCurrentSession()
    val userPreferences by userPreferencesRepository.getPreferences(userSession?.userId ?: 0L)
        .collectAsState(initial = null)
    val buttonSkin = userPreferences?.spinButtonSkin ?: "pf_1"
    
    // 加载按钮皮肤图片（🚀 跨页面缓存）
    val buttonSkinBitmap = remember(buttonSkin) {
        com.example.funlife.utils.ImageCache.loadImage(context, "login/$buttonSkin.png", sampleSize = 2)
    }
    
    // 🔥 按钮皮肤文字位置映射 - 根据每个皮肤的设计调整文字位置
    // 特殊皮肤居中显示：青山绿水(pf_9)、金碧辉煌(pf_10)、银装素裹(pf_11)、霓虹幻彩(pf_12)、烈焰焚天(pf_14)、冰清玉洁(pf_15)
    // 其他皮肤靠右显示
    val buttonSkinTextOffsets = remember {
        mapOf(
            "pf_1" to (110.dp to 20.dp),   // 初心如故 - 靠右
            "pf_2" to (110.dp to 20.dp),   // 粉黛流年 - 靠右
            "pf_3" to (110.dp to 20.dp),   // 碧海青天 - 靠右
            "pf_4" to (110.dp to 20.dp),   // 翠竹凝烟 - 靠右
            "pf_5" to (110.dp to 20.dp),   // 紫气东来 - 靠右
            "pf_6" to (110.dp to 20.dp),   // 橙黄橘绿 - 靠右
            "pf_7" to (110.dp to 20.dp),   // 丹霞映日 - 靠右
            "pf_8" to (110.dp to 20.dp),   // 金风玉露 - 靠右
            "pf_9" to (0.dp to 0.dp),      // 青山绿水 - 居中 ✓
            "pf_10" to (0.dp to 0.dp),     // 金碧辉煌 - 居中 ✓
            "pf_11" to (0.dp to 0.dp),     // 银装素裹 - 居中 ✓
            "pf_12" to (0.dp to 0.dp),     // 霓虹幻彩 - 居中 ✓
            "pf_13" to (110.dp to 20.dp),  // 星河璀璨 - 靠右
            "pf_14" to (0.dp to 0.dp),     // 烈焰焚天 - 居中 ✓
            "pf_15" to (0.dp to 0.dp),     // 冰清玉洁 - 居中 ✓
            "pf_16" to (110.dp to 20.dp),  // 雷霆万钧 - 靠右
            "pf_17" to (110.dp to 20.dp),  // 林深见鹿 - 靠右
            "pf_18" to (110.dp to 20.dp),  // 沧海桑田 - 靠右
            "pf_19" to (110.dp to 20.dp),  // 大漠孤烟 - 靠右
            "pf_20" to (110.dp to 20.dp),  // 极光流转 - 靠右
            "pf_21" to (110.dp to 20.dp),  // 樱花烂漫 - 靠右
            "pf_22" to (110.dp to 20.dp),  // 枫叶如丹 - 靠右
            "pf_23" to (110.dp to 20.dp),  // 雪舞轻扬 - 靠右
            "pf_24" to (110.dp to 20.dp),  // 星辰大海 - 靠右
            "pf_25" to (110.dp to 20.dp),  // 月华如水 - 靠右
            "pf_26" to (110.dp to 20.dp)   // 传世经典 - 靠右
        )
    }
    
    // 获取当前按钮皮肤的文字偏移量
    val (textStartPadding, textEndPadding) = buttonSkinTextOffsets[buttonSkin] ?: (110.dp to 20.dp)
    
    // 🔥 转盘旋转音量状态
    var spinRotationVolume by remember { mutableFloatStateOf(0.7f) }
    
    // 🔥 从用户偏好加载音量
    LaunchedEffect(userPreferences) {
        userPreferences?.let {
            spinRotationVolume = it.spinRotationVolume
            android.util.Log.d("EnhancedSpinWheel", "Loaded volume: ${it.spinRotationVolume}")
        }
    }
    
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
    // 🎯 积分掉落奖励浮层
    var shopPointsRewardAmount by remember { mutableStateOf(0) }
    var showShopPointsReward by remember { mutableStateOf(false) }
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
    
    // 监听连抽模式变化，显示明显的提示（1.5秒后自动消失）
    LaunchedEffect(multiSpinMode, multiSpinCount) {
        if (multiSpinMode && multiSpinCount > 1) {
            snackbarHostState.showSnackbar(
                message = "✨ ${multiSpinCount}连抽模式已开启！点击【开始旋转】按钮启动",
                duration = SnackbarDuration.Short  // 1.5秒后自动消失
            )
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent, // 设置为透明，让背景渐变显示
        topBar = {
            // 自定义头部导航栏 - 透明背景，融入整体渐变
            Surface(
                modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                color = Color.Transparent, // 透明背景
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(end = 8.dp), // 移除所有左侧padding
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回按钮 - 移除默认padding
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.offset(x = (-8).dp) // 向左偏移
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color(0xFF6A1B9A)
                        )
                    }
                    
                    // 幸运转盘图片，文字叠加在图片上 - 向左靠近返回按钮
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.offset(x = (-16).dp) // 向左偏移，靠近返回按钮
                    ) {
                        // 幸运转盘图片
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(
                                id = com.example.funlife.R.drawable.spin_wheel_logo
                            ),
                            contentDescription = "幸运转盘",
                            modifier = Modifier.height(56.dp)
                        )
                        
                        // 模式文字 - 叠加在图片的文字区域，居中偏左
                        Text(
                            text = currentMode.displayName,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .offset(x = (-10).dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 设置按钮
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = Color(0xFF6A1B9A)
                        )
                    }
                    
                    // 金币显示
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("💰", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "$userCoins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6A1B9A)
                            )
                        }
                    }
                }
            }
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE1BEE7), // 顶部浅紫色
                            Color(0xFFF3E5F5), // 中间更浅的紫色
                            Color(0xFFFCE4EC)  // 底部粉色
                        )
                    )
                )
        ) {
            // 加载状态
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "加载中...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                
                // ═══════════════════════════════════════════════════════
                // 🎯 转盘模式切换 - 紧凑胶囊分段控制器（不再占独立大行）
                // ═══════════════════════════════════════════════════════
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 60.dp, vertical = 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 外框容器
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF3E8FF).copy(alpha = 0.8f))
                    ) {
                        // 动画滑动指示器
                        val indicatorFraction by animateFloatAsState(
                            targetValue = if (isProductWheelMode) 1f else 0f,
                            animationSpec = spring(
                                dampingRatio = 0.7f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "tabSlide"
                        )
                        
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val halfWidth = maxWidth / 2
                            val indicatorPadding = 3.dp
                            
                            // 滑动色块
                            Box(
                                modifier = Modifier
                                    .padding(indicatorPadding)
                                    .width(halfWidth - indicatorPadding * 2)
                                    .fillMaxHeight()
                                    .offset(
                                        x = (halfWidth - indicatorPadding) * indicatorFraction
                                    )
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = if (isProductWheelMode) listOf(
                                                Color(0xFFFF6B6B), Color(0xFFFF8E53)
                                            ) else listOf(
                                                Color(0xFF7C3AED), Color(0xFF9333EA)
                                            )
                                        )
                                    )
                                    .shadow(4.dp, RoundedCornerShape(13.dp))
                            )
                        }
                        
                        // 标签文字层
                        Row(modifier = Modifier.fillMaxSize()) {
                            // 幸运转盘
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { isProductWheelMode = false },
                                contentAlignment = Alignment.Center
                            ) {
                                val textColor by animateColorAsState(
                                    targetValue = if (!isProductWheelMode) Color.White else Color(0xFF7C3AED),
                                    animationSpec = tween(300),
                                    label = "luckyColor"
                                )
                                Text(
                                    "🎯 幸运转盘",
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            
                            // 商品转盘
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { isProductWheelMode = true },
                                contentAlignment = Alignment.Center
                            ) {
                                val textColor by animateColorAsState(
                                    targetValue = if (isProductWheelMode) Color.White else Color(0xFFFF6B6B),
                                    animationSpec = tween(300),
                                    label = "productColor"
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        "🎁 商品转盘",
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    if (userShopPoints >= 10) {
                                        Spacer(Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isProductWheelMode) Color.White.copy(alpha = 0.3f) 
                                                    else Color(0xFFFF6B6B).copy(alpha = 0.12f),
                                                    CircleShape
                                                )
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                "${userShopPoints / 10}",
                                                color = if (isProductWheelMode) Color.White else Color(0xFFFF6B6B),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // ═══════════════════════════════════════════════════════
                // 📦 内容区域 - 使用AnimatedContent实现平滑切换
                // ═══════════════════════════════════════════════════════
                AnimatedContent(
                    targetState = isProductWheelMode,
                    transitionSpec = {
                        if (targetState) {
                            // 切换到商品转盘：从右滑入
                            (slideInHorizontally(
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ) { it } + fadeIn(tween(300)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                                    ) { -it } + fadeOut(tween(200))
                                )
                        } else {
                            // 切换到幸运转盘：从左滑入
                            (slideInHorizontally(
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ) { -it } + fadeIn(tween(300)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                                    ) { it } + fadeOut(tween(200))
                                )
                        }
                    },
                    label = "wheelSwitch"
                ) { isProduct ->
                    if (isProduct) {
                        // 商品转盘内容
                        ProductSpinWheel(
                            prizes = viewModel.productPrizes,
                            shopPoints = userShopPoints,
                            userCoins = userCoins,
                            onSpin = {
                                // 🎯 修复：立即把 prize 传给转盘，由转盘根据 prize 索引精确计算
                                // 落点角度并播放 4500ms 旋转动画。结果弹窗在动画结束时才弹出，
                                // 确保指针指向的扇形 = 弹窗显示的奖品。
                                isProductSpinning = true
                                scope.launch {
                                    val result = viewModel.performProductSpin()
                                    when (result) {
                                        is ProductSpinResult.Success -> {
                                            productSpinResult = result.prize
                                            // 等待动画完成后再解锁可旋转状态（防止重复点）
                                            kotlinx.coroutines.delay(4500)
                                            isProductSpinning = false
                                        }
                                        is ProductSpinResult.InsufficientPoints -> {
                                            isProductSpinning = false
                                            snackbarHostState.showSnackbar("积分不足，需要10积分")
                                        }
                                    }
                                }
                            },
                            isSpinning = isProductSpinning,
                            resultPrize = productSpinResult,
                            onResultDismiss = {
                                productSpinResult = null
                            },
                            modifier = Modifier.padding(bottom = 16.dp),
                            // 🧪 测试转盘（不扣积分），仅 Debug 显示按钮
                            onTestSpin = {
                                isProductSpinning = true
                                scope.launch {
                                    val result = viewModel.performTestProductSpin()
                                    when (result) {
                                        is ProductSpinResult.Success -> {
                                            productSpinResult = result.prize
                                            kotlinx.coroutines.delay(4500)
                                            isProductSpinning = false
                                        }
                                        is ProductSpinResult.InsufficientPoints -> {
                                            isProductSpinning = false
                                            snackbarHostState.showSnackbar("奖品列表为空，无法测试")
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                
                // 幸运值进度条 - 使用原型图图片
                com.example.funlife.ui.components.LuckyValueImageBar(
                    currentValue = luckyValue,
                    maxValue = 100,
                    onDiceClick = {
                        val increment = kotlin.random.Random.nextInt(1, 11)
                        luckyValue = (luckyValue + increment).coerceAtMost(100)
                        android.util.Log.d("EnhancedSpinWheel", "Lucky value changed: $luckyValue")
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
                var currentSpinIndex by remember { mutableIntStateOf(0) }
                var isPreparingToSpin by remember { mutableStateOf(false) }
                var currentForceResult by remember { mutableStateOf<String?>(null) }
                var hasUserClicked by remember { mutableStateOf(false) }
                
                // 🔥 将 triggerSpin 移到 key() 外面，避免 key 变化时触发旋转
                var triggerSpin by remember { mutableIntStateOf(0) }
                
                // 调试日志 - 权重可视化
                LaunchedEffect(showWeightVisualization) {
                    android.util.Log.d("EnhancedSpinWheel", "=== Weight Visualization Changed ===")
                    android.util.Log.d("EnhancedSpinWheel", "showWeightVisualization: $showWeightVisualization")
                    android.util.Log.d("EnhancedSpinWheel", "currentOptions: ${currentOptions.map { "${it.text}:${it.weight}" }}")
                }
                
                // 计算是否可以旋转（响应式）
                val canSpin = remember(userCoins, currentMode, multiSpinMode, multiSpinCount, currentSpinIndex, isPreparingToSpin, showResultAnimation, showMultiSpinResultAnimation) {
                    // 如果正在显示结算动画，禁止旋转
                    if (showResultAnimation || showMultiSpinResultAnimation) return@remember false
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
                // 🔥 修复：移除 triggerSpin 避免每次旋转都重新创建组件
                key(currentOptions.hashCode(), currentTheme, multiSpinMode, showWeightVisualization) {
                    // 🔥 强制使用正确的幸运模式选项顺序（不受数据库影响）
                    val displayOptions = if (currentMode == SpinWheelMode.LUCKY) {
                        // 根据转盘图片的实际顺序（从顶部开始顺时针）
                        // 仔细观察图片：顶部=再来一次，右上=大奖，右侧=双倍奖励，右下=幸运星
                        // 底部=系出，左下=神秘礼物，左侧=中奖，左上=小奖
                        listOf("再来一次", "大奖", "双倍奖励", "幸运星", "安慰奖", "神秘礼物", "中奖", "小奖")
                    } else {
                        currentOptions.filter { !it.isExcluded }.map { it.text }
                    }
                    
                    ImageBasedSpinWheel(
                        options = displayOptions,
                        canSpin = true,
                        autoSpinTrigger = triggerSpin,
                        forceResult = currentForceResult,
                        wheelMode = when(currentMode) {
                            SpinWheelMode.NORMAL -> com.example.funlife.ui.components.WheelMode.NORMAL
                            SpinWheelMode.ADVANCED -> com.example.funlife.ui.components.WheelMode.ADVANCED
                            SpinWheelMode.LUCKY -> com.example.funlife.ui.components.WheelMode.LUCKY
                        },
                        onSpinStart = {
                            android.util.Log.d("EnhancedSpinWheel", "=== onSpinStart (ImageBasedSpinWheel) ===")
                            // 播放转盘旋转音效（循环播放）- 使用用户设置的音量
                            soundManager.play(SoundEffect.SPIN_ROTATING, volume = spinRotationVolume, loop = true)
                        },
                        onResult = { result ->
                            scope.launch {
                                android.util.Log.d("EnhancedSpinWheel", "=== onResult called ===")
                                android.util.Log.d("EnhancedSpinWheel", "result: $result")
                                
                                // 停止转盘旋转音效
                                soundManager.stop(SoundEffect.SPIN_ROTATING)
                                
                                // 播放结果音效
                                soundManager.play(SoundEffect.RESULT_NORMAL, volume = 0.8f)
                                
                                // 重置forceResult
                                currentForceResult = null
                                
                                // 清零幸运值（仅单次模式）
                                if (!multiSpinMode && selectedTargetOption != null) {
                                    luckyValue = 0
                                    selectedTargetOption = null
                                }
                                
                                val spinResult = viewModel.processSpinResult(result)
                                // 🎯 积分掉落：拿到积分则触发全屏浮层（凌驾于常规结果动画之上）
                                val ptsReward = (spinResult as? SpinResult.Success)?.shopPointsReward ?: 0
                                if (ptsReward > 0) {
                                    shopPointsRewardAmount = ptsReward
                                    showShopPointsReward = true
                                    soundManager.play(SoundEffect.RESULT_NORMAL, volume = 1.0f)
                                }
                                
                                if (multiSpinMode) {
                                    currentSpinIndex++
                                    viewModel.incrementMultiSpinProgress()
                                    viewModel.recordMultiSpinResult(result)
                                    
                                    if (currentSpinIndex >= multiSpinCount) {
                                        kotlinx.coroutines.delay(500)
                                        val results = viewModel.multiSpinResults.value
                                        val summary = results.groupingBy { it }.eachCount()
                                            .entries.joinToString(", ") { "${it.key}×${it.value}" }
                                        
                                        multiSpinAnimationResult = summary
                                        showMultiSpinResultAnimation = true
                                        
                                        kotlinx.coroutines.delay(2000)
                                        viewModel.resetMultiSpinState()
                                        currentSpinIndex = 0
                                        hasUserClicked = false
                                        
                                        if (selectedTargetOption != null) {
                                            luckyValue = 0
                                            selectedTargetOption = null
                                        }
                                    } else {
                                        kotlinx.coroutines.delay(600)
                                        triggerSpin++
                                    }
                                } else {
                                    hasUserClicked = false
                                }
                                
                                // 显示结果动画
                                if (!multiSpinMode && !showMultiSpinResultAnimation) {
                                    animationResult = result
                                    showResultAnimation = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .padding(vertical = 16.dp)
                    )
                    
                    // 自定义旋转按钮 - 超级加强版设计
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isPreparingToSpin) 0.92f else 1f,
                        animationSpec = tween(200),
                        label = "buttonScale"
                    )
                    
                    // 呼吸动画
                    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 0.6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glowAlpha"
                    )
                    
                    val shimmerOffset by infiniteTransition.animateFloat(
                        initialValue = -1f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "shimmer"
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .height(120.dp)
                            .scale(buttonScale),
                        contentAlignment = Alignment.Center
                    ) {
                        // 🔥 如果有按钮皮肤，直接使用图片作为按钮
                        if (buttonSkinBitmap != null && !isPreparingToSpin) {
                            // 🔥 使用remember状态来手动控制按下效果
                            var isManualPressed by remember { mutableStateOf(false) }
                            
                            // 🔥 调试日志 - 监控状态变化
                            LaunchedEffect(isManualPressed) {
                                android.util.Log.d("ButtonClick", "isManualPressed changed to: $isManualPressed")
                            }
                            
                            // 🔥 增强点击效果：更明显的缩放和透明度变化
                            val pressScale by animateFloatAsState(
                                targetValue = if (isManualPressed) 0.85f else 1f, // 更明显的缩放
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "pressScale"
                            )
                            
                            // 🔥 添加透明度动画
                            val pressAlpha by animateFloatAsState(
                                targetValue = if (isManualPressed) 0.6f else 1f, // 更明显的透明度
                                animationSpec = tween(durationMillis = 150),
                                label = "pressAlpha"
                            )
                            
                            // 🔥 调试日志 - 监控动画值
                            LaunchedEffect(pressScale, pressAlpha) {
                                android.util.Log.d("ButtonClick", "Animation values - pressScale: $pressScale, pressAlpha: $pressAlpha")
                            }
                            
                            Box(
                                modifier = Modifier
                                    .width(280.dp)  // 缩小宽度
                                    .height(90.dp)  // 缩小高度
                                    .align(Alignment.Center)  // 居中对齐
                                    .pointerInput(canSpin) {
                                        detectTapGestures(
                                            onPress = {
                                                android.util.Log.d("ButtonClick", "onPress triggered - setting isManualPressed=true")
                                                isManualPressed = true
                                                tryAwaitRelease()
                                                isManualPressed = false
                                                android.util.Log.d("ButtonClick", "onPress released - setting isManualPressed=false")
                                            },
                                            onTap = {
                                                if (!canSpin || currentOptions.filter { !it.isExcluded }.isEmpty()) {
                                                    android.util.Log.d("ButtonClick", "Tap blocked: canSpin=$canSpin")
                                                    return@detectTapGestures
                                                }
                                                
                                                android.util.Log.d("ButtonClick", "Button tapped!")
                                                scope.launch {
                                                    if (isPreparingToSpin || hasUserClicked) {
                                                        android.util.Log.d("ButtonClick", "Blocked: isPreparingToSpin=$isPreparingToSpin, hasUserClicked=$hasUserClicked")
                                                        return@launch
                                                    }
                                                    hasUserClicked = true
                                                    isPreparingToSpin = true
                                                    
                                                    // 🔥 添加触觉反馈
                                                    try {
                                                        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                                        } else {
                                                            @Suppress("DEPRECATION")
                                                            vibrator?.vibrate(50)
                                                        }
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("EnhancedSpinWheel", "Vibration failed", e)
                                                    }
                                                    
                                                    if (selectedTargetOption != null && luckyValue > 0) {
                                                        val baseProb = 100f / currentOptions.filter { !it.isExcluded }.size
                                                        val maxProb = 50f
                                                        val hitProbability = baseProb + (maxProb - baseProb) * (luckyValue / 100f)
                                                        val random = kotlin.random.Random.nextFloat() * 100f
                                                        currentForceResult = if (random < hitProbability) selectedTargetOption?.text else null
                                                    } else {
                                                        currentForceResult = null
                                                    }
                                                    
                                                    if (multiSpinMode && currentSpinIndex == 0) {
                                                        val totalCost = currentMode.costPerSpin * multiSpinCount
                                                        if (viewModel.userCoins.value < totalCost || !viewModel.deductCoinsForMultiSpin(totalCost)) {
                                                            snackbarHostState.showSnackbar("❌ 金币不足！")
                                                            currentForceResult = null
                                                            isPreparingToSpin = false
                                                            hasUserClicked = false
                                                            return@launch
                                                        }
                                                    } else if (!multiSpinMode && !viewModel.checkAndDeductCoins()) {
                                                        snackbarHostState.showSnackbar("❌ 金币不足！")
                                                        currentForceResult = null
                                                        isPreparingToSpin = false
                                                        hasUserClicked = false
                                                        return@launch
                                                    }
                                                    
                                                    isPreparingToSpin = false
                                                    triggerSpin++
                                                }
                                            }
                                        )
                                    }
                                    .scale(pressScale)
                                    .graphicsLayer(alpha = pressAlpha),
                                contentAlignment = Alignment.Center
                            ) {
                                // 背景图片
                                androidx.compose.foundation.Image(
                                    bitmap = buttonSkinBitmap,
                                    contentDescription = "按钮皮肤",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.FillBounds  // 改为FillBounds，填充整个按钮
                                )
                                
                                // 文字叠加层 - 根据按钮皮肤调整位置
                                // 🔥 关键修复：不要让文字层拦截点击事件
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(start = textStartPadding, end = textEndPadding),  // 🔥 使用动态padding，根据皮肤自动调整
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            if (isPreparingToSpin) "准备中..." else "开始旋转",
                                            fontSize = 16.sp,  // 缩小字体
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                shadow = androidx.compose.ui.graphics.Shadow(
                                                    color = Color.Black.copy(alpha = 0.7f),
                                                    offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                                    blurRadius = 8f
                                                )
                                            )
                                        )
                                        if (!isPreparingToSpin && multiSpinMode && currentSpinIndex == 0) {
                                            Text(
                                                "🎲 ${multiSpinCount}连抽模式",
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    shadow = androidx.compose.ui.graphics.Shadow(
                                                        color = Color.Black.copy(alpha = 0.7f),
                                                        offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                                        blurRadius = 6f
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // 默认按钮样式（无皮肤或准备中状态）
                            // 第一层：最外层脉冲光晕
                            if (!isPreparingToSpin && canSpin) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .scale(1.15f)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    when (currentMode.name) {
                                                        "LUCKY" -> Color(0xFFFFD700).copy(alpha = glowAlpha * 0.5f)
                                                        "ADVANCED" -> Color(0xFF9C27B0).copy(alpha = glowAlpha * 0.5f)
                                                        else -> Color(0xFF2196F3).copy(alpha = glowAlpha * 0.5f)
                                                    },
                                                    Color.Transparent
                                                )
                                            ),
                                            shape = RoundedCornerShape(40.dp)
                                        )
                                )
                            }
                            
                            // 第二层：中层光晕
                            if (!isPreparingToSpin && canSpin) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .scale(1.08f)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    when (currentMode.name) {
                                                        "LUCKY" -> Color(0xFFFFD700).copy(alpha = glowAlpha * 0.7f)
                                                        "ADVANCED" -> Color(0xFF9C27B0).copy(alpha = glowAlpha * 0.7f)
                                                        else -> Color(0xFF2196F3).copy(alpha = glowAlpha * 0.7f)
                                                    },
                                                    Color.Transparent
                                                )
                                            ),
                                            shape = RoundedCornerShape(40.dp)
                                        )
                                )
                            }
                            
                            // 主按钮容器
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(40.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (!isPreparingToSpin && canSpin) 12.dp else 4.dp
                                )
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            // 防止重复点击
                                            if (isPreparingToSpin || hasUserClicked) {
                                                android.util.Log.d("EnhancedSpinWheel", "Button click ignored - already spinning or clicked")
                                                return@launch
                                            }
                                            
                                            hasUserClicked = true
                                            isPreparingToSpin = true
                                            
                                            android.util.Log.d("EnhancedSpinWheel", "=== Custom Button Clicked ===")
                                            android.util.Log.d("EnhancedSpinWheel", "selectedTargetOption: ${selectedTargetOption?.text}")
                                            android.util.Log.d("EnhancedSpinWheel", "luckyValue: $luckyValue")
                                            
                                            // 计算是否命中目标 - 幸运值满时50%概率
                                            if (selectedTargetOption != null && luckyValue > 0) {
                                                // 计算实际命中概率：从基础概率逐渐增加到50%
                                                val baseProb = 100f / currentOptions.filter { !it.isExcluded }.size
                                                val maxProb = 50f
                                                val hitProbability = baseProb + (maxProb - baseProb) * (luckyValue / 100f)
                                                val random = kotlin.random.Random.nextFloat() * 100f
                                                val hit = random < hitProbability
                                                
                                                android.util.Log.d("EnhancedSpinWheel", "baseProb: $baseProb%, maxProb: $maxProb%, luckyValue: $luckyValue")
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
                                                    // 🔥 修复：第一次点击，一次性扣除所有金币
                                                    val totalCost = currentMode.costPerSpin * multiSpinCount
                                                    if (viewModel.userCoins.value < totalCost) {
                                                        snackbarHostState.showSnackbar("❌ 金币不足！")
                                                        currentForceResult = null
                                                        isPreparingToSpin = false
                                                        hasUserClicked = false
                                                        return@launch
                                                    }
                                                    // 一次性扣除总金额，而不是循环扣除
                                                    if (!viewModel.deductCoinsForMultiSpin(totalCost)) {
                                                        snackbarHostState.showSnackbar("❌ 金币扣除失败！")
                                                        currentForceResult = null
                                                        isPreparingToSpin = false
                                                        hasUserClicked = false
                                                        return@launch
                                                    }
                                                    android.util.Log.d("EnhancedSpinWheel", "Deducted $totalCost coins for $multiSpinCount spins")
                                                }
                                            } else {
                                                if (!viewModel.checkAndDeductCoins()) {
                                                    snackbarHostState.showSnackbar("❌ 金币不足！")
                                                    currentForceResult = null
                                                    isPreparingToSpin = false
                                                    hasUserClicked = false
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
                                    modifier = Modifier.fillMaxSize(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(40.dp),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 0.dp,
                                        pressedElevation = 0.dp
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        // 默认背景渐变层
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    brush = if (!isPreparingToSpin && canSpin) {
                                                        when (currentMode.name) {
                                                            "LUCKY" -> Brush.linearGradient(
                                                                colors = listOf(
                                                                    Color(0xFFFFD700),
                                                                    Color(0xFFFFA500),
                                                                    Color(0xFFFF8C00)
                                                                ),
                                                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                                end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                                                            )
                                                            "ADVANCED" -> Brush.linearGradient(
                                                                colors = listOf(
                                                                    Color(0xFF9C27B0),
                                                                    Color(0xFFE91E63),
                                                                    Color(0xFFFF4081)
                                                                ),
                                                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                                end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                                                            )
                                                            else -> Brush.linearGradient(
                                                                colors = listOf(
                                                                    Color(0xFF2196F3),
                                                                    Color(0xFF00BCD4),
                                                                    Color(0xFF00E5FF)
                                                                ),
                                                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                                end = androidx.compose.ui.geometry.Offset(1000f, 1000f)
                                                            )
                                                        }
                                                    } else {
                                                        Brush.linearGradient(
                                                            colors = listOf(
                                                                Color(0xFF9E9E9E),
                                                                Color(0xFF757575)
                                                            )
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(40.dp)
                                                )
                                        )
                                        
                                        // 闪光动画层
                                        if (!isPreparingToSpin && canSpin) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(
                                                                Color.Transparent,
                                                                Color.White.copy(alpha = 0.4f),
                                                                Color.Transparent
                                                            ),
                                                            start = androidx.compose.ui.geometry.Offset(
                                                                shimmerOffset * 1000f,
                                                                shimmerOffset * 1000f
                                                            ),
                                                            end = androidx.compose.ui.geometry.Offset(
                                                                (shimmerOffset + 0.5f) * 1000f,
                                                                (shimmerOffset + 0.5f) * 1000f
                                                            )
                                                        ),
                                                        shape = RoundedCornerShape(40.dp)
                                                    )
                                            )
                                        }
                                        
                                        // 顶部高光
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(25.dp)
                                                .align(Alignment.TopCenter)
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.White.copy(alpha = 0.35f),
                                                            Color.Transparent
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
                                                )
                                        )
                                        
                                        // 按钮内容 - 文字始终显示
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 24.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 文字内容
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    if (isPreparingToSpin) "准备中..." else "开始旋转",
                                                    fontSize = 16.sp,  // 缩小字体
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.headlineSmall.copy(
                                                        shadow = androidx.compose.ui.graphics.Shadow(
                                                            color = Color.Black.copy(alpha = 0.5f),
                                                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                                            blurRadius = 6f
                                                        )
                                                    )
                                                )
                                                if (!isPreparingToSpin && multiSpinMode && currentSpinIndex == 0) {
                                                    Text(
                                                        "🎲 ${multiSpinCount}连抽模式",
                                                        fontSize = 13.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            shadow = androidx.compose.ui.graphics.Shadow(
                                                                color = Color.Black.copy(alpha = 0.5f),
                                                                offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                                                blurRadius = 4f
                                                            )
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        }  // else 结束
                    }  // Box (button container) 结束
                }  // key 结束
                }  // Column (幸运转盘内容) 结束
                }  // else (AnimatedContent) 结束
                }  // AnimatedContent 结束
            }  // Column 结束
            
            // 🔥 浮动的连抽进度条和结果显示（不挤压转盘）
            if (multiSpinMode) {
                val multiSpinResults by viewModel.multiSpinResults.collectAsState()
                
                // 使用 AnimatedVisibility 实现平滑显示/隐藏
                AnimatedVisibility(
                    visible = currentMultiSpinProgress > 0 || multiSpinResults.isNotEmpty(),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = padding.calculateTopPadding() + 16.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 进度条
                        if (currentMultiSpinProgress > 0) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                                            "🎲 连抽进度",
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
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // 已抽中结果展示 - 紧凑横向标签样式（不遮挡转盘）
                        if (multiSpinResults.isNotEmpty()) {
                            // 统计结果
                            val resultCounts = multiSpinResults.groupingBy { it }.eachCount()
                            
                            // 紧凑的横向滚动标签
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.95f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    Color(0xFFFFD700).copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 标题行 - 紧凑设计
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🎁", fontSize = 16.sp)
                                            Text(
                                                "已抽中",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2C3E50),
                                                fontSize = 14.sp
                                            )
                                        }
                                        
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF00BCD4).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "${multiSpinResults.size}/${multiSpinCount}",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00BCD4),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    
                                    // 横向滚动的结果标签
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        resultCounts.entries.sortedByDescending { it.value }.forEach { (result, count) ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFFF8F9FA)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // 图标
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .background(
                                                                brush = Brush.linearGradient(
                                                                    colors = listOf(
                                                                        Color(0xFF4CAF50),
                                                                        Color(0xFF8BC34A)
                                                                    )
                                                                ),
                                                                shape = RoundedCornerShape(8.dp)
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("✨", fontSize = 14.sp)
                                                    }
                                                    
                                                    // 文字
                                                    Text(
                                                        result,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF2C3E50),
                                                        fontSize = 14.sp
                                                    )
                                                    
                                                    // 数量
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                brush = Brush.linearGradient(
                                                                    colors = listOf(
                                                                        Color(0xFFFF6B9D),
                                                                        Color(0xFFFF8E53)
                                                                    )
                                                                ),
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            "×$count",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            fontSize = 13.sp
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
                }
            }
            
            // 🎨 可拖动的浮动模式切换按钮
            var buttonOffsetX by remember { mutableFloatStateOf(0f) }
            var buttonOffsetY by remember { mutableFloatStateOf(0f) }
            
            // 可拖动的浮动按钮
            Box(
                modifier = Modifier
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            buttonOffsetX.toInt(),
                            buttonOffsetY.toInt()
                        )
                    }
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 100.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            buttonOffsetX += dragAmount.x
                            buttonOffsetY += dragAmount.y
                        }
                    }
            ) {
                // 可爱的圆形按钮
                FloatingActionButton(
                    onClick = { showModeDialog = true },
                    modifier = Modifier.size(64.dp),
                    containerColor = when(currentMode) {
                        SpinWheelMode.NORMAL -> Color(0xFFFF6B9D)
                        SpinWheelMode.ADVANCED -> Color(0xFF4ECDC4)
                        SpinWheelMode.LUCKY -> Color(0xFFFFB347)
                    },
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentMode.emoji,
                            fontSize = 24.sp
                        )
                        Text(
                            text = when(currentMode) {
                                SpinWheelMode.NORMAL -> "普通"
                                SpinWheelMode.ADVANCED -> "进阶"
                                SpinWheelMode.LUCKY -> "幸运"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }  // Box (background) 结束
    
    // 获取用户偏好中的结算面板皮肤
    val panelSkin = userPreferences?.spinResultPanelSkin ?: "js_1"
    
    // 获得动画（覆盖在整个屏幕上）- 保留
    if (showResultAnimation) {
        com.example.funlife.ui.components.ResultAnimation(
            result = animationResult,
            mode = currentMode,
            onDismiss = { showResultAnimation = false },
            panelSkin = panelSkin
        )
    }
    
    // 连抽结算动画
    if (showMultiSpinResultAnimation) {
        com.example.funlife.ui.components.MultiSpinResultAnimation(
            results = multiSpinAnimationResult,
            onDismiss = { showMultiSpinResultAnimation = false }
        )
    }

    // 🎯 积分掉落奖励浮层（最高 zIndex，会盖在普通结果动画之上）
    com.example.funlife.ui.components.ShopPointsRewardOverlay(
        visible = showShopPointsReward,
        pointsAmount = shopPointsRewardAmount,
        onDismiss = {
            showShopPointsReward = false
            shopPointsRewardAmount = 0
        }
    )
    
    // 模式选择对话框
    if (showModeDialog) {
        // 🔥 获取VIP状态，进阶/幸运模式需VIP
        val context = androidx.compose.ui.platform.LocalContext.current
        val vipViewModel: com.example.funlife.viewmodel.VipViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        val authViewModel: com.example.funlife.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return com.example.funlife.viewmodel.AuthViewModel(context.applicationContext as android.app.Application) as T
                }
            }
        )
        
        // 🔥 设置用户ID
        val userSession = authViewModel.getCurrentSession()
        LaunchedEffect(userSession) {
            userSession?.let {
                vipViewModel.setUserId(it.userId)
            }
        }
        
        val userVip by vipViewModel.userVip.collectAsState()
        val vipLevel = userVip?.getCurrentVipLevel() ?: com.example.funlife.data.model.VipLevel.NORMAL
        val isVip = vipLevel != com.example.funlife.data.model.VipLevel.NORMAL
        
        ModeSelectionDialog(
            currentMode = currentMode,
            userCoins = userCoins,
            isVip = isVip,
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
        com.example.funlife.ui.components.EnhancedEditOptionsDialog(
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
                
                // 🔥 音效设置分组
                SettingsGroupHeader(title = "音效设置", emoji = "🔊")
                
                // 🔥 转盘旋转音量控制卡片
                VolumeControlSettingsCard(
                    volume = spinRotationVolume,
                    onVolumeChange = { newVolume ->
                        android.util.Log.d("EnhancedSpinWheel", "Volume changed to: $newVolume")
                        spinRotationVolume = newVolume
                        scope.launch {
                            userPreferences?.let { prefs ->
                                userPreferencesRepository.update(
                                    prefs.copy(spinRotationVolume = newVolume)
                                )
                            }
                        }
                    }
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
    isVip: Boolean = false,
    onModeSelected: (SpinWheelMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎡", fontSize = 28.sp)
                Text(
                    "选择转盘模式",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 提示信息
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E5).copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💰", fontSize = 20.sp)
                        Text(
                            "当前金币：$userCoins",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF8C00)
                        )
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                
                // 三个模式卡片
                SpinWheelMode.values().forEach { mode ->
                    val canAfford = mode.canAfford(userCoins)
                    val isSelected = mode == currentMode
                    // 🔥 VIP限制：进阶和幸运模式需要VIP
                    val isVipLocked = !isVip && mode != SpinWheelMode.NORMAL
                    val isEnabled = canAfford && !isVipLocked
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isEnabled) {
                                onModeSelected(mode)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isVipLocked -> Color(0xFFF5F5F5)
                                isSelected -> when(mode) {
                                    SpinWheelMode.NORMAL -> Color(0xFFFFE5E5)
                                    SpinWheelMode.ADVANCED -> Color(0xFFE5F3FF)
                                    SpinWheelMode.LUCKY -> Color(0xFFFFF3E5)
                                }
                                !canAfford -> Color(0xFFF5F5F5)
                                else -> Color.White
                            }
                        ),
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(
                                3.dp,
                                when(mode) {
                                    SpinWheelMode.NORMAL -> Color(0xFFFF6B9D)
                                    SpinWheelMode.ADVANCED -> Color(0xFF4ECDC4)
                                    SpinWheelMode.LUCKY -> Color(0xFFFFB347)
                                }
                            )
                        } else null,
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 8.dp else 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 头部：Emoji + 名称 + 金币消耗
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Emoji
                                    Text(
                                        text = mode.emoji,
                                        fontSize = 32.sp
                                    )
                                    
                                    // 名称
                                    Column {
                                        Text(
                                            text = mode.displayName,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isEnabled) {
                                                when(mode) {
                                                    SpinWheelMode.NORMAL -> Color(0xFFFF1493)
                                                    SpinWheelMode.ADVANCED -> Color(0xFF00CED1)
                                                    SpinWheelMode.LUCKY -> Color(0xFFFF8C00)
                                                }
                                            } else Color.Gray
                                        )
                                        
                                        // 金币消耗
                                        if (mode.costPerSpin > 0) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("💰", fontSize = 12.sp)
                                                Text(
                                                    "${mode.costPerSpin} 金币/次",
                                                    fontSize = 12.sp,
                                                    color = if (canAfford) Color(0xFF666666) else Color.Red,
                                                    fontWeight = if (canAfford) FontWeight.Normal else FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Text(
                                                "完全免费",
                                                fontSize = 12.sp,
                                                color = Color(0xFF4CAF50),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                
                                // 选中标记
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        when(mode) {
                                                            SpinWheelMode.NORMAL -> Color(0xFFFF6B9D)
                                                            SpinWheelMode.ADVANCED -> Color(0xFF4ECDC4)
                                                            SpinWheelMode.LUCKY -> Color(0xFFFFB347)
                                                        },
                                                        when(mode) {
                                                            SpinWheelMode.NORMAL -> Color(0xFFFF1493)
                                                            SpinWheelMode.ADVANCED -> Color(0xFF00CED1)
                                                            SpinWheelMode.LUCKY -> Color(0xFFFF8C00)
                                                        }
                                                    )
                                                ),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "已选择",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            
                            // 描述
                            Text(
                                text = mode.description,
                                fontSize = 13.sp,
                                color = if (canAfford) Color(0xFF666666) else Color.Gray,
                                lineHeight = 18.sp
                            )
                            
                            // 特性列表
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                mode.features.forEach { feature ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    color = if (canAfford) {
                                                        when(mode) {
                                                            SpinWheelMode.NORMAL -> Color(0xFFFF6B9D)
                                                            SpinWheelMode.ADVANCED -> Color(0xFF4ECDC4)
                                                            SpinWheelMode.LUCKY -> Color(0xFFFFB347)
                                                        }
                                                    } else Color.Gray,
                                                    shape = CircleShape
                                                )
                                        )
                                        Text(
                                            text = feature,
                                            fontSize = 12.sp,
                                            color = if (canAfford) Color(0xFF666666) else Color.Gray
                                        )
                                    }
                                }
                            }
                            
                            // 🔥 VIP专属提示
                            if (mode != SpinWheelMode.NORMAL) {
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isVip) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (isVip) "✅" else "🔒",
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            if (isVip) "VIP专属模式，已解锁" else "VIP专属模式，开通任意VIP即可解锁",
                                            fontSize = 11.sp,
                                            color = if (isVip) Color(0xFF2E7D32) else Color(0xFFE65100),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            
                            // 金币不足提示
                            if (!canAfford && !isVipLocked) {
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFEBEE)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⚠️", fontSize = 14.sp)
                                        Text(
                                            "金币不足，还需 ${mode.costPerSpin - userCoins} 金币",
                                            fontSize = 11.sp,
                                            color = Color(0xFFD32F2F),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "关闭",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
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
                        // 🔥 自适应高度：小屏/键盘弹出时可压缩到 160dp，理想 240dp
                        .heightIn(min = 160.dp, max = 240.dp)
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
    val context = androidx.compose.ui.platform.LocalContext.current
    
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
                    
                    // 显示 Toast 提示
                    android.widget.Toast.makeText(
                        context,
                        "✨ ${selectedCount}连抽模式已开启！返回主页点击【开始旋转】按钮启动",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
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
    
    // 🔥 获取VIP状态
    val context = androidx.compose.ui.platform.LocalContext.current
    val vipViewModel: com.example.funlife.viewmodel.VipViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val authViewModel: com.example.funlife.viewmodel.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return com.example.funlife.viewmodel.AuthViewModel(context.applicationContext as android.app.Application) as T
            }
        }
    )
    
    // 🔥 设置用户ID
    val userSession = authViewModel.getCurrentSession()
    LaunchedEffect(userSession) {
        userSession?.let {
            vipViewModel.setUserId(it.userId)
        }
    }
    
    val userVip by vipViewModel.userVip.collectAsState()
    val vipLevel = userVip?.getCurrentVipLevel() ?: com.example.funlife.data.model.VipLevel.NORMAL
    val isVip = vipLevel != com.example.funlife.data.model.VipLevel.NORMAL
    
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
                            // 🔥 检查是否为VIP专属模式
                            val isVipMode = mode.name == "进阶模式" || mode.name == "幸运模式"
                            val isLocked = isVipMode && !isVip
                            
                            ModeCard(
                                mode = mode,
                                isSelected = mode.id == currentMode?.id,
                                isVip = isVip,
                                isLocked = isLocked,
                                onSelect = { 
                                    if (!isLocked) {
                                        viewModel.setCustomMode(mode)
                                    }
                                },
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
                                isVip = isVip,
                                isLocked = false, // 自定义模式不锁定
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
    isVip: Boolean = false,
    isLocked: Boolean = false,
    onSelect: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked, onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isLocked -> Color(0xFFF5F5F5)
                isSelected -> MaterialTheme.colorScheme.primaryContainer
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
                        mode.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isLocked) Color.Gray else Color.Unspecified
                    )
                }
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLocked) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(8.dp))
                
                mode.getFeatures().forEach { feature ->
                    Text(
                        "• $feature",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLocked) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 🔥 VIP锁定提示
                if (isLocked) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFF3E0)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔒", fontSize = 12.sp)
                            Text(
                                "VIP专属模式",
                                fontSize = 10.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (mode.name == "进阶模式" || mode.name == "幸运模式") {
                    // VIP用户显示已解锁
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✅", fontSize = 12.sp)
                            Text(
                                "VIP已解锁",
                                fontSize = 10.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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

// 🔥 音量控制设置卡片
@Composable
private fun VolumeControlSettingsCard(
    volume: Float,
    onVolumeChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 音量图标容器
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00BCD4).copy(alpha = 0.8f),
                                    Color(0xFF00BCD4)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when {
                            volume == 0f -> Icons.Default.VolumeOff
                            volume < 0.5f -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "转盘旋转音量",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${(volume * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // 音量滑块
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                steps = 9,  // 10个档位（0%, 10%, 20%, ..., 100%）
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00BCD4),
                    activeTrackColor = Color(0xFF00BCD4),
                    inactiveTrackColor = Color(0xFF00BCD4).copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
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
