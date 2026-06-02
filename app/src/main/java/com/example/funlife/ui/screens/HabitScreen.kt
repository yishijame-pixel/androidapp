// HabitScreen.kt - 完善版习惯打卡屏幕
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeomSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.data.model.Habit
import com.example.funlife.data.model.HabitRecord
import com.example.funlife.viewmodel.HabitViewModel
import com.example.funlife.viewmodel.HabitWithStats
import com.example.funlife.viewmodel.CheckInResult
import com.example.funlife.ui.components.EnhancedTopBar
import com.example.funlife.ui.components.TopBarAction
import com.example.funlife.ui.components.TopBarGradients
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    viewModel: HabitViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val habitsWithStats by viewModel.habitsWithStats.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedHabit by remember { mutableStateOf<HabitWithStats?>(null) }
    var showCoinReward by remember { mutableStateOf(false) }
    var coinRewardAmount by remember { mutableStateOf(0) }
    var bonusMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // 🔥 视图模式切换（按 userId 隔离持久化，§1.5）
    val currentUserId = remember {
        com.example.funlife.utils.UserSessionManager(context).getCurrentUserId()
    }
    val viewPrefs = remember(currentUserId) {
        context.getSharedPreferences("habit_view_prefs", android.content.Context.MODE_PRIVATE)
    }
    val viewModeKey = "u${currentUserId}_habit_view_mode"
    val autoSwitchedKey = "u${currentUserId}_habit_auto_switched"
    var compactMode by remember(currentUserId) {
        val saved = viewPrefs.getString(viewModeKey, null)
        mutableStateOf(saved == "compact")
    }
    // 🔥 习惯数 > 2（即 ≥ 3）时自动切到紧凑视图 + 一次性提示
    LaunchedEffect(habitsWithStats.size, currentUserId) {
        if (currentUserId > 0 && habitsWithStats.size > 2) {
            val alreadyAuto = viewPrefs.getBoolean(autoSwitchedKey, false)
            val savedMode = viewPrefs.getString(viewModeKey, null)
            // 用户未手动设置 + 未自动切换过 → 自动切 + Toast 提示
            if (!alreadyAuto && savedMode == null) {
                compactMode = true
                viewPrefs.edit()
                    .putString(viewModeKey, "compact")
                    .putBoolean(autoSwitchedKey, true)
                    .apply()
                android.widget.Toast.makeText(
                    context,
                    "习惯数已超过 2 个，已为你切换为紧凑视图 ✨",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    // 加载背景图片
    val backgroundBitmap = remember {
        com.example.funlife.utils.ImageCache.loadImage(context, "login/xiguan.png", sampleSize = 2)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 背景图片
        backgroundBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "习惯背景",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        // 内容层
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部返回按钮（简洁版，不使用 EnhancedTopBar）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 🔥 视图模式切换按钮（仅当 ≥ 2 个习惯时显示）
                    if (habitsWithStats.size >= 2) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                .clickable {
                                    compactMode = !compactMode
                                    if (currentUserId > 0) {
                                        viewPrefs.edit()
                                            .putString(viewModeKey, if (compactMode) "compact" else "detailed")
                                            .apply()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (compactMode) Icons.Default.ViewAgenda else Icons.Default.ViewList,
                                contentDescription = if (compactMode) "切换为详细视图" else "切换为紧凑视图",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "添加习惯",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { /* 查看统计 */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = "统计",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // 添加顶部间距，避免挡住标题
            Spacer(modifier = Modifier.height(120.dp))
            
            // 主内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                if (habitsWithStats.isEmpty()) {
                    EmptyHabitState(Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = if (compactMode) 20.dp else 32.dp,
                            end = if (compactMode) 20.dp else 32.dp,
                            // 🔥 沉浸式：内容从状态栏下面开始；底部 = Tab 90 + 系统导航 + 30
                            top = 16.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                            bottom = 90.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 30.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(if (compactMode) 10.dp else 16.dp)
                    ) {
                        // 顶部状态栏：习惯数量 + 当前视图模式提示（紧凑模式下显示）
                        if (compactMode) {
                            item(key = "_header_") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val doneToday = habitsWithStats.count { it.todayChecked }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.22f))
                                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            "📋 共 ${habitsWithStats.size} 个 · 今日已完成 $doneToday",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        "紧凑视图",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        // 习惯列表
                        items(habitsWithStats, key = { it.habit.id }) { habitStats ->
                            val onCheckInClick: () -> Unit = {
                                coroutineScope.launch {
                                    viewModel.toggleCheckIn(habitStats.habit.id, habitStats.todayChecked).collect { result ->
                                        when (result) {
                                            is CheckInResult.Success -> {
                                                coinRewardAmount = result.coins
                                                bonusMessage = if (result.hasBonus) "连续7天额外奖励！" else ""
                                                showCoinReward = true
                                            }
                                            is CheckInResult.Cancelled -> {}
                                            is CheckInResult.Failed -> {}
                                        }
                                    }
                                }
                            }
                            if (compactMode) {
                                CompactHabitCard(
                                    habitStats = habitStats,
                                    onCheckIn = onCheckInClick,
                                    onDelete = { viewModel.deleteHabit(habitStats.habit) },
                                    onClick = { selectedHabit = habitStats }
                                )
                            } else {
                                EnhancedHabitCard(
                                    habitStats = habitStats,
                                    onCheckIn = onCheckInClick,
                                    onDelete = { viewModel.deleteHabit(habitStats.habit) },
                                    onClick = { selectedHabit = habitStats }
                                )
                            }
                        }
                    }
                }
                
                // 金币奖励动画移到下方用 Dialog 包装（真·全屏遮罩）
            }
        }
    }
    
    if (showDialog) {
        AddHabitDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, icon, color ->
                viewModel.addHabit(name, icon, color)
                showDialog = false
            }
        )
    }
    
    selectedHabit?.let { selectedHabitStats ->
        // 实时获取最新的habitStats
        val currentHabitStats = habitsWithStats.find { it.habit.id == selectedHabitStats.habit.id }
        
        if (currentHabitStats != null) {
            HabitDetailDialog(
                habitStats = currentHabitStats,
                viewModel = viewModel,
                onDismiss = { selectedHabit = null }
            )
        }
    }

    // 🔥 金币动画 — 全屏 Dialog 形式，覆盖状态栏 + 导航栏
    if (showCoinReward) {
        Dialog(
            onDismissRequest = { showCoinReward = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false,
                dismissOnBackPress = true,
                decorFitsSystemWindows = false
            )
        ) {
            // 让 Dialog 真正延伸到状态栏 & 导航栏
            val view = androidx.compose.ui.platform.LocalView.current
            if (!view.isInEditMode) {
                androidx.compose.runtime.SideEffect {
                    val w = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
                    w?.let {
                        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(it, false)
                        @Suppress("DEPRECATION")
                        it.statusBarColor = android.graphics.Color.TRANSPARENT
                        @Suppress("DEPRECATION")
                        it.navigationBarColor = android.graphics.Color.TRANSPARENT
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                CoinRewardAnimation(
                    amount = coinRewardAmount,
                    bonusMessage = bonusMessage,
                    onDismiss = { showCoinReward = false }
                )
            }
        }
    }
}

@Composable
fun EmptyHabitState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // 可爱的浮动提示 - 位于顶部偏下，不遮挡背景
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 180.dp)  // 增加到180dp，更靠下
        ) {
            // 跳动的小图标
            var offsetY by remember { mutableStateOf(0f) }
            LaunchedEffect(Unit) {
                while (true) {
                    animate(0f, -15f, animationSpec = tween(800, easing = FastOutSlowInEasing)) { value, _ -> 
                        offsetY = value 
                    }
                    animate(-15f, 0f, animationSpec = tween(800, easing = FastOutSlowInEasing)) { value, _ -> 
                        offsetY = value 
                    }
                }
            }
            
            // 可爱的小卡片提示 - 纯透明无阴影
            Box(
                modifier = Modifier
                    .graphicsLayer { translationY = offsetY }
                    .background(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌸", fontSize = 24.sp)  // 可爱的樱花图标
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "还没有习惯哦",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6B9D),  // 可爱的粉红色
                            fontSize = 16.sp
                        )
                        Text(
                            "点击右上角 + 开始添加",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFB6C1),  // 柔和的浅粉色
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HabitOverviewCard(habitsWithStats: List<HabitWithStats>) {
    val totalHabits = habitsWithStats.size
    val todayCompleted = habitsWithStats.count { it.todayChecked }
    val avgStreak = if (habitsWithStats.isNotEmpty()) {
        habitsWithStats.map { it.currentStreak }.average().toInt()
    } else 0
    val totalDays = habitsWithStats.sumOf { it.totalDays }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📊", fontSize = 24.sp)
                Text(
                    "今日概览",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                HabitStatItem("✅", "今日完成", "$todayCompleted/$totalHabits")
                Divider(
                    modifier = Modifier
                        .height(50.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                HabitStatItem("🔥", "平均连续", "$avgStreak 天")
                Divider(
                    modifier = Modifier
                        .height(50.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                HabitStatItem("📈", "总打卡", "$totalDays 次")
            }
        }
    }
}

@Composable
fun HabitStatItem(icon: String, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(icon, fontSize = 24.sp)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun EnhancedHabitCard(
    habitStats: HabitWithStats,
    onCheckIn: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val habit = habitStats.habit
    val color = Color(android.graphics.Color.parseColor(habit.color))
    val checked = habitStats.todayChecked
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 装饰动画
    val infinite = rememberInfiniteTransition(label = "habitCard_${habit.id}")
    val ringRot by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "ringRot"
    )
    val iconBob by infinite.animateFloat(
        initialValue = -2f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "iconBob"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val shimmer by infinite.animateFloat(
        initialValue = -0.3f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "shimmer"
    )

    // 卡片整体外层（彩色描边光晕 + 阴影）
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = color.copy(alpha = 0.45f),
                spotColor = color.copy(alpha = 0.55f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White,
                        color.copy(alpha = 0.06f),
                        Color.White
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.25f), color.copy(alpha = 0.5f))
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // 🎀 左侧装饰彩条
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.55f), color)
                    )
                )
        )

        // 🌟 右上角小光点装饰
        Canvas(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(60.dp)
                .padding(8.dp)
        ) {
            drawCircle(
                color = color.copy(alpha = 0.10f),
                radius = size.minDimension * 0.45f,
                center = Offset(size.width * 0.75f, size.height * 0.25f)
            )
            drawCircle(
                color = color.copy(alpha = 0.18f),
                radius = size.minDimension * 0.15f,
                center = Offset(size.width * 0.4f, size.height * 0.55f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ════════════════ 顶部：图标 + 名称 + 删除按钮 ════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 🎯 图标圆盘（旋转光环 + emoji 摆动 + 已打卡金对勾叠加）
                Box(
                    modifier = Modifier.size(58.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 旋转光环（仅未打卡显示）
                    if (!checked) {
                        Canvas(
                            modifier = Modifier
                                .size(58.dp)
                                .graphicsLayer { rotationZ = ringRot }
                        ) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        color.copy(alpha = 0f),
                                        color.copy(alpha = 0.8f),
                                        color.copy(alpha = 0f)
                                    )
                                ),
                                startAngle = 0f, sweepAngle = 240f, useCenter = false,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                    // 主圆
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = if (checked)
                                        listOf(Color(0xFFFFD54F), Color(0xFFFFA000))
                                    else
                                        listOf(color, color.copy(alpha = 0.7f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        com.example.funlife.ui.components.HabitIcon(
                            icon = habit.icon,
                            iconSize = 28.dp,
                            emojiFontSize = 24.sp,
                            tint = Color.White,
                            modifier = Modifier.graphicsLayer { translationY = iconBob }
                        )
                    }
                    // 已打卡：右下角金色对勾徽章
                    if (checked) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF66BB6A), Color(0xFF2E7D32))
                                    )
                                )
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check, null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // 名称 + 补卡徽章
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        habit.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF263238),
                        maxLines = 1,
                        style = TextStyle(
                            shadow = Shadow(
                                color = color.copy(alpha = 0.25f),
                                offset = Offset(0f, 1f),
                                blurRadius = 3f
                            )
                        )
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 状态文字
                        Text(
                            if (checked) "今日已完成" else "今日待完成",
                            fontSize = 11.sp,
                            color = if (checked) Color(0xFF2E7D32) else Color(0xFF757575),
                            fontWeight = FontWeight.Medium
                        )
                        // 补卡卡
                        if (habit.makeupCards > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFFF3CD))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    "🎫×${habit.makeupCards}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB8860B)
                                )
                            }
                        }
                    }
                }

                // 删除按钮
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEBEE))
                ) {
                    Icon(
                        Icons.Default.Delete, "删除",
                        tint = Color(0xFFE57373),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // ════════════════ 中部：3 个统计胶囊（渐变 + 阴影） ════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBadge(
                    icon = "🔥",
                    label = "连续",
                    value = "${habitStats.currentStreak}天",
                    color = Color(0xFFFF6B35),
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    icon = "📊",
                    label = "总计",
                    value = "${habitStats.totalDays}天",
                    color = color,
                    modifier = Modifier.weight(1f)
                )
                StatBadge(
                    icon = "✨",
                    label = "完成率",
                    value = "${(habitStats.completionRate * 100).toInt()}%",
                    color = Color(0xFF4ECDC4),
                    modifier = Modifier.weight(1f)
                )
            }

            // ════════════════ 底部：进度条 + 打卡按钮 ════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 进度区
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "最近30天",
                            fontSize = 11.sp,
                            color = Color(0xFF9E9E9E),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${(habitStats.completionRate * 30).toInt()} / 30",
                            fontSize = 11.sp,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // 进度条 + shimmer 光带
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color.copy(alpha = 0.13f))
                    ) {
                        val ratio = habitStats.completionRate.coerceIn(0f, 1f)
                        if (ratio > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(ratio)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                color.copy(alpha = 0.85f),
                                                color,
                                                color.copy(alpha = 0.75f)
                                            )
                                        )
                                    )
                            )
                            // shimmer 流光
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(ratio)
                                    .clip(RoundedCornerShape(4.dp))
                                    .graphicsLayer { alpha = 0.55f }
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.7f),
                                                Color.Transparent
                                            ),
                                            startX = shimmer * 400f - 60f,
                                            endX = shimmer * 400f + 60f
                                        )
                                    )
                            )
                        }
                    }
                }

                // 打卡按钮（大圆胶囊 + 呼吸脉冲 + 渐变 + 阴影）
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            if (!checked) { scaleX = pulse; scaleY = pulse }
                        }
                        .shadow(
                            elevation = if (checked) 2.dp else 8.dp,
                            shape = RoundedCornerShape(22.dp),
                            ambientColor = if (checked) Color(0xFF66BB6A) else color,
                            spotColor = if (checked) Color(0xFF66BB6A) else color
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (checked)
                                    listOf(Color(0xFFA5D6A7), Color(0xFF66BB6A))
                                else
                                    listOf(color, color.copy(alpha = 0.8f))
                            )
                        )
                        .clickable(enabled = !checked, onClick = onCheckIn)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (checked) Icons.Default.Check else Icons.Default.Add,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            if (checked) "已打卡" else "打卡",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("删除习惯") },
            text = { Text("确定要删除「${habit.name}」吗？所有打卡记录将被永久删除。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 🔥 紧凑型习惯卡片（一屏可显示 6+ 个）
 * 单行横向布局：[侧边条] [图标圆] [名字+副标] [打卡按钮]
 */
@Composable
fun CompactHabitCard(
    habitStats: HabitWithStats,
    onCheckIn: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val habit = habitStats.habit
    val color = Color(android.graphics.Color.parseColor(habit.color))
    val checked = habitStats.todayChecked
    var showDeleteDialog by remember { mutableStateOf(false) }

    val infinite = rememberInfiniteTransition(label = "compactCard_${habit.id}")
    val pulse by infinite.animateFloat(
        initialValue = 0.94f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val shimmer by infinite.animateFloat(
        initialValue = -0.3f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = color.copy(alpha = 0.35f),
                spotColor = color.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White,
                        color.copy(alpha = 0.05f),
                        Color.White
                    )
                )
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // 左侧色条
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.6f), color)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 图标圆盘 42dp
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (checked)
                                    listOf(Color(0xFFFFD54F), Color(0xFFFFA000))
                                else
                                    listOf(color, color.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    com.example.funlife.ui.components.HabitIcon(
                        icon = habit.icon,
                        iconSize = 22.dp,
                        emojiFontSize = 20.sp,
                        tint = Color.White
                    )
                }
                if (checked) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF66BB6A))
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check, null,
                            tint = Color.White,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
            }

            // 名字 + 数据
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    habit.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF263238),
                    maxLines = 1
                )
                // 副标：🔥 X天连续 · ✨ Y% · 📊 N天
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "🔥${habitStats.currentStreak}天",
                        fontSize = 10.sp,
                        color = Color(0xFFFF6B35),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "✨${(habitStats.completionRate * 100).toInt()}%",
                        fontSize = 10.sp,
                        color = Color(0xFF4ECDC4),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "📊${habitStats.totalDays}天",
                        fontSize = 10.sp,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
                // 微型进度条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color.copy(alpha = 0.12f))
                ) {
                    val ratio = habitStats.completionRate.coerceIn(0f, 1f)
                    if (ratio > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(color, color.copy(alpha = 0.7f))
                                    )
                                )
                        )
                        // shimmer 光带
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.6f),
                                            Color.Transparent
                                        ),
                                        startX = shimmer * 400f - 50f,
                                        endX = shimmer * 400f + 50f
                                    )
                                )
                        )
                    }
                }
            }

            // 删除按钮（小图标按钮）
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Delete, "删除",
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(14.dp)
                )
            }

            // 打卡按钮（小胶囊）
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        if (!checked) { scaleX = pulse; scaleY = pulse }
                    }
                    .shadow(
                        elevation = if (checked) 1.dp else 5.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = if (checked) Color(0xFF66BB6A) else color,
                        spotColor = if (checked) Color(0xFF66BB6A) else color
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (checked)
                                listOf(Color(0xFFA5D6A7), Color(0xFF66BB6A))
                            else
                                listOf(color, color.copy(alpha = 0.8f))
                        )
                    )
                    .clickable(enabled = !checked, onClick = onCheckIn)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        if (checked) Icons.Default.Check else Icons.Default.Add,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        if (checked) "已打" else "打卡",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("删除习惯") },
            text = { Text("确定要删除「${habit.name}」吗？所有打卡记录将被永久删除。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun StatBadge(
    icon: String,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.18f),
                        color.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(vertical = 8.dp, horizontal = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // 图标 + 数值 同行（节省竖向空间）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(icon, fontSize = 14.sp)
                Text(
                    value,
                    fontWeight = FontWeight.Black,
                    color = color,
                    fontSize = 14.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = color.copy(alpha = 0.35f),
                            offset = Offset(0f, 1f),
                            blurRadius = 2f
                        )
                    )
                )
            }
            Text(
                label,
                color = Color(0xFF757575),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailDialog(
    habitStats: HabitWithStats,
    viewModel: HabitViewModel,
    onDismiss: () -> Unit
) {
    val records by viewModel.getHabitRecords(habitStats.habit.id).collectAsState(initial = emptyList())
    val recordDates = records.map { it.date }.toSet()
    
    // 当前月份
    var currentMonth by remember { mutableStateOf(LocalDate.now()) }
    
    // 本地打卡状态 - 用于实时更新UI
    var localTodayChecked by remember { mutableStateOf(habitStats.todayChecked) }
    
    // 金币动画状态
    var showCoinReward by remember { mutableStateOf(false) }
    var coinRewardAmount by remember { mutableStateOf(0) }
    var bonusMessage by remember { mutableStateOf("") }
    
    val coroutineScope = rememberCoroutineScope()
    
    val habitColor = Color(android.graphics.Color.parseColor(habitStats.habit.color))

    // 装饰动画
    val infinite = rememberInfiniteTransition(label = "habitDetail")
    val haloRot by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "haloRot"
    )
    val iconBob by infinite.animateFloat(
        initialValue = -2.5f, targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "iconBob"
    )
    val sparkPhase by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "sparkPhase"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // ═══════════ Dialog 主卡片 ═══════════
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 680.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = habitColor.copy(alpha = 0.5f),
                        spotColor = habitColor.copy(alpha = 0.55f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFFFFF8FB))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // ─── ① 顶部彩色 Banner（图标 + 名字 + 打卡按钮）───
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    topStart = 26.dp, topEnd = 26.dp,
                                    bottomStart = 16.dp, bottomEnd = 16.dp
                                )
                            )
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        habitColor,
                                        habitColor.copy(alpha = 0.7f),
                                        habitColor.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        // 背景星粒装饰
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val stars = listOf(
                                Triple(0.10f, 0.20f, 0.0f),
                                Triple(0.92f, 0.15f, 0.3f),
                                Triple(0.88f, 0.78f, 0.6f),
                                Triple(0.18f, 0.85f, 0.8f),
                                Triple(0.55f, 0.45f, 0.5f)
                            )
                            stars.forEach { (x, y, phase) ->
                                val a = sin(((sparkPhase + phase) % 1f) * Math.PI.toFloat()).coerceIn(0f, 1f)
                                drawCircle(
                                    color = Color.White.copy(alpha = a * 0.7f),
                                    radius = (2f + a * 2f),
                                    center = Offset(size.width * x, size.height * y)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 图标圆盘 + 旋转光环
                            Box(
                                modifier = Modifier.size(62.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .graphicsLayer { rotationZ = haloRot }
                                ) {
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0f),
                                                Color.White.copy(alpha = 0.85f),
                                                Color.White.copy(alpha = 0f),
                                                Color.White.copy(alpha = 0.6f),
                                                Color.White.copy(alpha = 0f)
                                            )
                                        ),
                                        startAngle = 0f, sweepAngle = 360f, useCenter = false,
                                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.95f))
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    com.example.funlife.ui.components.HabitIcon(
                                        icon = habitStats.habit.icon,
                                        iconSize = 32.dp,
                                        emojiFontSize = 28.sp,
                                        tint = habitColor,
                                        modifier = Modifier.graphicsLayer { translationY = iconBob }
                                    )
                                }
                            }

                            // 名字 + 副标
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    habitStats.habit.name,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 1,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.28f),
                                            offset = Offset(0f, 2f), blurRadius = 5f
                                        )
                                    )
                                )
                                Text(
                                    "🔥 连续 ${habitStats.currentStreak} 天 · ${habitStats.totalDays} 天累计",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.3.sp
                                )
                            }

                            // 打卡按钮（玻璃态）
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(
                                        if (localTodayChecked) Color.White.copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.92f)
                                    )
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.6f),
                                        RoundedCornerShape(22.dp)
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            viewModel.toggleCheckIn(habitStats.habit.id, localTodayChecked).collect { result ->
                                                when (result) {
                                                    is CheckInResult.Success -> {
                                                        localTodayChecked = !localTodayChecked
                                                        coinRewardAmount = result.coins
                                                        bonusMessage = if (result.hasBonus) "连续7天额外奖励！" else ""
                                                        showCoinReward = true
                                                    }
                                                    is CheckInResult.Cancelled -> {
                                                        localTodayChecked = !localTodayChecked
                                                    }
                                                    is CheckInResult.Failed -> {}
                                                }
                                            }
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        if (localTodayChecked) Icons.Default.Check else Icons.Default.Add,
                                        null,
                                        tint = if (localTodayChecked) Color.White else habitColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        if (localTodayChecked) "已打卡" else "打卡",
                                        color = if (localTodayChecked) Color.White else habitColor,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // ─── ② 3 个统计胶囊（升级版）───
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBadge(
                            icon = "📅",
                            label = "总天数",
                            value = "${habitStats.totalDays}",
                            color = habitColor,
                            modifier = Modifier.weight(1f)
                        )
                        StatBadge(
                            icon = "🔥",
                            label = "连续",
                            value = "${habitStats.currentStreak}",
                            color = Color(0xFFFF6B35),
                            modifier = Modifier.weight(1f)
                        )
                        StatBadge(
                            icon = "🏆",
                            label = "最长",
                            value = "${habitStats.longestStreak}",
                            color = Color(0xFFFFB300),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // ─── ③ 月份导航 + 当月打卡数 ───
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左圆按钮
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(habitColor.copy(alpha = 0.12f))
                                .clickable { currentMonth = currentMonth.minusMonths(1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowLeft, "上个月",
                                tint = habitColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                currentMonth.format(DateTimeFormatter.ofPattern("yyyy 年 M 月")),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF263238)
                            )
                            // 当月已打卡天数
                            val monthChecked = recordDates.count {
                                try {
                                    val d = LocalDate.parse(it)
                                    d.year == currentMonth.year && d.month == currentMonth.month
                                } catch (_: Exception) { false }
                            }
                            Text(
                                "本月已打卡 $monthChecked 天",
                                fontSize = 10.sp,
                                color = habitColor,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        val canForward = currentMonth.isBefore(LocalDate.now().withDayOfMonth(1))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canForward) habitColor.copy(alpha = 0.12f)
                                    else Color(0xFFEEEEEE)
                                )
                                .clickable(enabled = canForward) {
                                    currentMonth = currentMonth.plusMonths(1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowRight, "下个月",
                                tint = if (canForward) habitColor else Color(0xFFBDBDBD),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ─── ④ 星期标题 ───
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("日", "一", "二", "三", "四", "五", "六").forEachIndexed { i, day ->
                            Text(
                                day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (i == 0 || i == 6) habitColor.copy(alpha = 0.8f) else Color(0xFF757575)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // ─── ⑤ 日历网格 ───
                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                        CompactHabitCalendarGrid(
                            currentMonth = currentMonth,
                            recordDates = recordDates,
                            habitColor = habitColor,
                            onDateClick = { date ->
                                val dateStr = date.toString()
                                val isChecked = recordDates.contains(dateStr)
                                viewModel.toggleCheckIn(habitStats.habit.id, isChecked, dateStr)
                            }
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // ─── ⑥ 关闭按钮（habit 渐变胶囊）───
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .height(46.dp)
                            .shadow(8.dp, RoundedCornerShape(23.dp), ambientColor = habitColor, spotColor = habitColor)
                            .clip(RoundedCornerShape(23.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(habitColor, habitColor.copy(alpha = 0.75f))
                                )
                            )
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "关闭",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // 金币奖励动画 - 嵌套 Dialog，实现全屏遮罩（覆盖详情卡 + 状态栏）
            if (showCoinReward) {
                Dialog(
                    onDismissRequest = { showCoinReward = false },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnClickOutside = false,
                        dismissOnBackPress = true,
                        decorFitsSystemWindows = false
                    )
                ) {
                    val v = androidx.compose.ui.platform.LocalView.current
                    if (!v.isInEditMode) {
                        androidx.compose.runtime.SideEffect {
                            val w = (v.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
                            w?.let {
                                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(it, false)
                                @Suppress("DEPRECATION")
                                it.statusBarColor = android.graphics.Color.TRANSPARENT
                                @Suppress("DEPRECATION")
                                it.navigationBarColor = android.graphics.Color.TRANSPARENT
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        CoinRewardAnimation(
                            amount = coinRewardAmount,
                            bonusMessage = bonusMessage,
                            onDismiss = { showCoinReward = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HabitCalendarGrid(
    currentMonth: LocalDate,
    recordDates: Set<String>,
    habitColor: Color,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.withDayOfMonth(1)
    val lastDayOfMonth = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth())
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0=周日
    
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var currentDay = firstDayOfMonth.minusDays(firstDayOfWeek.toLong())
        
        // 生成6周的日历
        repeat(6) { weekIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { dayIndex ->
                    val date = currentDay
                    val isCurrentMonth = date.month == currentMonth.month
                    val isChecked = recordDates.contains(date.toString())
                    val isToday = date == LocalDate.now()
                    val isFuture = date.isAfter(LocalDate.now())
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    !isCurrentMonth -> Color.Transparent
                                    isChecked -> habitColor.copy(alpha = 0.8f)
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            )
                            .clickable(enabled = isCurrentMonth && !isFuture) {
                                onDateClick(date)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrentMonth) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        isChecked -> Color.White
                                        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isChecked) {
                                    Text("✓", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                    
                    currentDay = currentDay.plusDays(1)
                }
            }
            
            // 如果已经超过当前月份，停止生成（用 return@repeat，避免 return@Column
            // 跨越 @Composable lambda 边界破坏 Compose 槽表）
            if (currentDay.isAfter(lastDayOfMonth.plusDays(7))) {
                return@repeat
            }
        }
    }
}

@Composable
fun CompactStatItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CompactHabitCalendarGrid(
    currentMonth: LocalDate,
    recordDates: Set<String>,
    habitColor: Color,
    onDateClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = currentMonth.withDayOfMonth(1)
    val lastDayOfMonth = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth())
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        var currentDay = firstDayOfMonth.minusDays(firstDayOfWeek.toLong())
        
        repeat(6) { weekIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { dayIndex ->
                    val date = currentDay
                    val isCurrentMonth = date.month == currentMonth.month
                    val isChecked = recordDates.contains(date.toString())
                    val isToday = date == LocalDate.now()
                    val isFuture = date.isAfter(LocalDate.now())
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    !isCurrentMonth -> Color.Transparent
                                    isChecked -> habitColor.copy(alpha = 0.8f)
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            )
                            .clickable(enabled = isCurrentMonth && !isFuture) {
                                onDateClick(date)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrentMonth) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = when {
                                        isChecked -> Color.White
                                        isToday -> MaterialTheme.colorScheme.primary
                                        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isToday || isChecked) FontWeight.Bold else FontWeight.Normal
                                )
                                // 显示金币图标：未打卡显示金币，已打卡显示打勾的金币
                                if (!isFuture) {
                                    Text(
                                        if (isChecked) "✅" else "💰",
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    currentDay = currentDay.plusDays(1)
                }
            }
        }
    }
}

@Composable
fun DetailStatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CheckInDayCard(record: HabitRecord) {
    val date = LocalDate.parse(record.date)
    val formatter = DateTimeFormatter.ofPattern("MM/dd")
    
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "✓",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                date.format(formatter),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    // 🔥 §1.2 实时读取当前 userId（不缓存）
    val currentUserId = remember {
        com.example.funlife.utils.UserSessionManager(context).getCurrentUserId()
    }
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("${com.example.funlife.ui.components.MATERIAL_ICON_PREFIX}fitness") }
    var selectedColor by remember { mutableStateOf("#4ECDC4") }
    // 用户自定义上传的图标路径（最多保留1个待选）
    var customIcon by remember { mutableStateOf<String?>(null) }

    // 图库选择器（支持 Android 13+ PhotoPicker，无需权限）
    val pickImageLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val saved = com.example.funlife.ui.components.saveCustomHabitIcon(context, uri, currentUserId)
            if (saved != null) {
                customIcon = saved
                selectedIcon = saved
            } else {
                android.widget.Toast.makeText(context, "图片保存失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🌟 图标库：32 个 Material 矢量图标（保证全机型显示，可应用主题色）
    val icons = remember {
        com.example.funlife.ui.components.MATERIAL_HABIT_ICONS.keys
            .map { "${com.example.funlife.ui.components.MATERIAL_ICON_PREFIX}$it" }
    }
    val colors = listOf(
        "#4ECDC4", "#FF6B9D", "#FFD700", "#9B59B6",
        "#FF6B35", "#3498DB", "#2ECC71", "#E74C3C"
    )
    val curColor = Color(android.graphics.Color.parseColor(selectedColor))

    // 装饰动画
    val infinite = rememberInfiniteTransition(label = "addHabit")
    val previewBob by infinite.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "previewBob"
    )
    val haloRot by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "haloRot"
    )
    val sparkPhase by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "sparkPhase"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .shadow(20.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFFFF5F8))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // ════════════════ ① 顶部渐变 Banner（emoji 实时预览 + 旋转光环 + 闪烁星粒）════════════════
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                topStart = 28.dp, topEnd = 28.dp,
                                bottomStart = 16.dp, bottomEnd = 16.dp
                            )
                        )
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(curColor, curColor.copy(alpha = 0.7f), curColor.copy(alpha = 0.85f))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    // 背景星粒装饰
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val stars = listOf(
                            Triple(0.08f, 0.25f, 0.0f),
                            Triple(0.85f, 0.18f, 0.3f),
                            Triple(0.92f, 0.75f, 0.6f),
                            Triple(0.15f, 0.85f, 0.8f),
                            Triple(0.70f, 0.55f, 0.5f)
                        )
                        stars.forEach { (x, y, phase) ->
                            val a = kotlin.math.sin(((sparkPhase + phase) % 1f) * Math.PI.toFloat()).coerceIn(0f, 1f)
                            drawCircle(
                                color = Color.White.copy(alpha = a * 0.7f),
                                radius = (2f + a * 2f),
                                center = Offset(size.width * x, size.height * y)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 实时预览圆盘
                        Box(
                            modifier = Modifier.size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 旋转光环
                            Canvas(
                                modifier = Modifier
                                    .size(64.dp)
                                    .graphicsLayer { rotationZ = haloRot }
                            ) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0f),
                                            Color.White.copy(alpha = 0.85f),
                                            Color.White.copy(alpha = 0f),
                                            Color.White.copy(alpha = 0.6f),
                                            Color.White.copy(alpha = 0f)
                                        )
                                    ),
                                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            // 内圆
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.95f))
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                com.example.funlife.ui.components.HabitIcon(
                                    icon = selectedIcon,
                                    iconSize = 30.dp,
                                    emojiFontSize = 28.sp,
                                    tint = curColor,
                                    modifier = Modifier.graphicsLayer { translationY = previewBob }
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.AddTask, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    "添加新习惯",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.sp,
                                    style = TextStyle(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.3f),
                                            offset = Offset(0f, 2f), blurRadius = 5f
                                        )
                                    )
                                )
                            }
                            Text(
                                "坚持就是胜利 ✨",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.92f),
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                }

                // ════════════════ ② 内容区（输入 + 图标网格 + 颜色，可滚动） ════════════════
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 20) name = it },
                        label = { Text("习惯名称", color = curColor) },
                        placeholder = { Text("例如：每天喝8杯水", color = Color(0xFF9E9E9E)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        leadingIcon = {
                            com.example.funlife.ui.components.HabitIcon(
                                icon = selectedIcon,
                                iconSize = 22.dp,
                                emojiFontSize = 18.sp,
                                tint = curColor
                            )
                        },
                        trailingIcon = {
                            Text(
                                "${name.length}/20",
                                fontSize = 10.sp,
                                color = Color(0xFF9E9E9E),
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = curColor,
                            unfocusedBorderColor = curColor.copy(alpha = 0.45f),
                            cursorColor = curColor,
                            focusedLabelColor = curColor
                        )
                    )

                    // ════════════════ ③ 图标选择（4×N 网格 + 上传 + 32 emoji + 可选自定义） ════════════════
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(curColor)
                                )
                                Text("选择图标", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424242))
                            }
                            Text(
                                "支持自定义上传 ✨",
                                fontSize = 10.sp,
                                color = curColor.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // 构造完整网格项：[上传+] + [customIcon?（若已上传）] + 32 emoji
                        // 用 sealed 表示三种类型
                        val gridItems: List<Any> = buildList {
                            add("__UPLOAD__")
                            customIcon?.let { add(it) } // 自定义图标显示在第二格
                            addAll(icons)
                        }
                        val rows = (gridItems.size + 3) / 4
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (row in 0 until rows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (col in 0 until 4) {
                                        val idx = row * 4 + col
                                        if (idx < gridItems.size) {
                                            val item = gridItems[idx]
                                            // === 上传按钮 ===
                                            if (item == "__UPLOAD__") {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(52.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(
                                                            brush = Brush.linearGradient(
                                                                colors = listOf(
                                                                    curColor.copy(alpha = 0.15f),
                                                                    curColor.copy(alpha = 0.08f)
                                                                )
                                                            )
                                                        )
                                                        .border(
                                                            width = 1.5.dp,
                                                            brush = Brush.linearGradient(
                                                                colors = listOf(
                                                                    curColor.copy(alpha = 0.6f),
                                                                    curColor.copy(alpha = 0.3f)
                                                                )
                                                            ),
                                                            shape = RoundedCornerShape(14.dp)
                                                        )
                                                        .clickable {
                                                            if (currentUserId <= 0) {
                                                                android.widget.Toast.makeText(
                                                                    context, "请先登录", android.widget.Toast.LENGTH_SHORT
                                                                ).show()
                                                            } else {
                                                                pickImageLauncher.launch(
                                                                    androidx.activity.result.PickVisualMediaRequest(
                                                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                                                    )
                                                                )
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Add, null,
                                                            tint = curColor,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Text(
                                                            "上传",
                                                            fontSize = 9.sp,
                                                            color = curColor,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            } else {
                                                // === emoji 或 自定义图标 ===
                                                val ic = item as String
                                                val selected = ic == selectedIcon
                                                val isCustomImg = com.example.funlife.ui.components.isCustomHabitIcon(ic)
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(52.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .then(
                                                            if (selected) Modifier
                                                                .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = curColor, spotColor = curColor)
                                                                .background(
                                                                    brush = Brush.linearGradient(
                                                                        colors = listOf(curColor, curColor.copy(alpha = 0.7f))
                                                                    )
                                                                )
                                                            else Modifier
                                                                .background(Color(0xFFF5F5F5))
                                                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(14.dp))
                                                        )
                                                        .clickable { selectedIcon = ic },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    com.example.funlife.ui.components.HabitIcon(
                                                        icon = ic,
                                                        iconSize = if (selected) 28.dp else 26.dp,
                                                        emojiFontSize = if (selected) 24.sp else 22.sp,
                                                        tint = if (selected) Color.White else curColor.copy(alpha = 0.85f)
                                                    )
                                                    if (selected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .align(Alignment.TopEnd)
                                                                .offset(x = (-3).dp, y = 3.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.White),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Check, null,
                                                                tint = curColor,
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                        }
                                                    }
                                                    if (isCustomImg) {
                                                        // 左下角加"我的"小角标
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomStart)
                                                                .offset(x = 3.dp, y = (-3).dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color(0xFFFF6B35))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text("我的", fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Box(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ════════════════ ④ 颜色选择（4x2 圆点 + 选中白圈 + 阴影） ════════════════
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(curColor)
                            )
                            Text("选择颜色", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF424242))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colors.forEach { c ->
                                val cc = Color(android.graphics.Color.parseColor(c))
                                val selected = c == selectedColor
                                Box(
                                    modifier = Modifier.size(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        // 外圈白色光环
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .shadow(6.dp, CircleShape, ambientColor = cc, spotColor = cc)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(if (selected) 30.dp else 32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(cc, cc.copy(alpha = 0.75f))
                                                )
                                            )
                                            .clickable { selectedColor = c },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selected) {
                                            Icon(
                                                Icons.Default.Check, null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ════════════════ ⑤ 底部按钮（渐变 + 阴影 胶囊） ════════════════
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 取消
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFF5F5F5))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "取消",
                            color = Color(0xFF757575),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    // 确定
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = curColor, spotColor = curColor)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(curColor, curColor.copy(alpha = 0.75f))
                                )
                            )
                            .clickable {
                                if (name.isBlank()) {
                                    android.widget.Toast.makeText(
                                        context, "请输入习惯名称", android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    onConfirm(name, selectedIcon, selectedColor)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(
                                "创建习惯",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun CoinRewardAnimation(
    amount: Int,
    bonusMessage: String,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }

    // ─── 全屏入场/退场 ───
    val cardScale = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(0f) }
    val bgAlpha = remember { Animatable(0f) }
    var coinRotation by remember { mutableStateOf(0f) }
    var coinScale by remember { mutableStateOf(0f) }
    var displayAmount by remember { mutableStateOf(0) }

    // ─── 持续动画（光线旋转 + 脉冲）───
    val infinite = rememberInfiniteTransition(label = "coinReward")
    val rayRot by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "rayRot"
    )
    val coinFloat by infinite.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "coinFloat"
    )
    val coinShine by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "coinShine"
    )
    val sparkle by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "sparkle"
    )

    // ─── 入场动画序列 ───
    LaunchedEffect(Unit) {
        // 1. 背景蒙层快速淡入
        bgAlpha.animateTo(1f, tween(220))
    }
    LaunchedEffect(Unit) {
        // 2. 卡片弹性放大 + 透明度
        kotlinx.coroutines.delay(80)
        cardAlpha.animateTo(1f, tween(180))
        cardScale.animateTo(
            1f,
            spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow)
        )
    }
    LaunchedEffect(Unit) {
        // 3. 主金币 emoji 弹出 + 旋转
        kotlinx.coroutines.delay(200)
        animate(
            0f, 1.3f,
            animationSpec = tween(380, easing = FastOutSlowInEasing)
        ) { v, _ -> coinScale = v }
        animate(
            1.3f, 1f,
            animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)
        ) { v, _ -> coinScale = v }
    }
    LaunchedEffect(Unit) {
        // 4. 金币 360° 翻滚一圈
        kotlinx.coroutines.delay(250)
        animate(0f, 360f, animationSpec = tween(700, easing = FastOutSlowInEasing)) { v, _ -> coinRotation = v }
    }
    LaunchedEffect(amount) {
        // 5. 数字从 0 滚动到 amount（800ms 内）
        kotlinx.coroutines.delay(450)
        val steps = 24
        val stepDur = 800L / steps
        for (i in 1..steps) {
            displayAmount = (amount * i / steps)
            kotlinx.coroutines.delay(stepDur)
        }
        displayAmount = amount
    }
    LaunchedEffect(Unit) {
        // 6. 总展示时长 2.4s 后退出
        kotlinx.coroutines.delay(2400)
        cardAlpha.animateTo(0f, tween(380))
        bgAlpha.animateTo(0f, tween(300))
        visible = false
        onDismiss()
    }

    // ─── 金币粒子（8 个，从中心向四周飞）───
    val particleCount = 10
    val particles = remember {
        List(particleCount) {
            ParticleSpec(
                angle = (360f / particleCount) * it + Random.nextFloat() * 15f,
                distance = 110f + Random.nextFloat() * 60f,
                delay = Random.nextInt(0, 200),
                size = 22 + Random.nextInt(0, 14),
                emoji = listOf("💰", "🪙", "✨", "⭐", "💎", "🌟").random()
            )
        }
    }

    // ─── 五彩纸片（15 片从顶部下落）───
    val confetti = remember {
        List(20) {
            ConfettiSpec(
                xRatio = Random.nextFloat(),
                size = 6 + Random.nextInt(0, 8),
                speed = 0.7f + Random.nextFloat() * 1.2f,
                color = listOf(
                    Color(0xFFFFD700), Color(0xFFFF6B9D), Color(0xFF4ECDC4),
                    Color(0xFFFF6B35), Color(0xFF9B59B6), Color(0xFF3498DB),
                    Color(0xFF2ECC71), Color(0xFFFF80AB)
                ).random(),
                startDelay = Random.nextFloat()
            )
        }
    }

    if (!visible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A0F00).copy(alpha = 0.65f * bgAlpha.value),
                        Color.Black.copy(alpha = 0.85f * bgAlpha.value)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // ═══════ ① 背景放射光线（旋转金色）═══════
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rayRot; alpha = cardAlpha.value * 0.6f }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension * 0.7f
            for (i in 0 until 16) {
                val a = (i * 22.5f) * (Math.PI / 180f).toFloat()
                val ex = cx + cos(a) * radius
                val ey = cy + sin(a) * radius
                drawLine(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = radius
                    ),
                    start = Offset(cx, cy),
                    end = Offset(ex, ey),
                    strokeWidth = if (i % 2 == 0) 30f else 18f,
                    cap = StrokeCap.Round
                )
            }
        }

        // ═══════ ② 五彩纸片飘落（覆盖整屏）═══════
        confetti.forEachIndexed { i, c ->
            val fall by infinite.animateFloat(
                initialValue = -0.1f + c.startDelay * 0.3f, targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    tween((3000 / c.speed).toInt(), easing = LinearEasing)
                ),
                label = "fall_$i"
            )
            val sway by infinite.animateFloat(
                initialValue = 0f, targetValue = 6.28f,
                animationSpec = infiniteRepeatable(
                    tween((2400 / c.speed).toInt(), easing = LinearEasing)
                ),
                label = "sway_$i"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = cardAlpha.value }
            ) {
                Box(
                    modifier = Modifier
                        .size(c.size.dp, (c.size * 1.4f).dp)
                        .graphicsLayer {
                            translationX = c.xRatio * 1080f + sin(sway.toDouble()).toFloat() * 30f
                            translationY = fall * 2400f
                            rotationZ = sway * 60f
                        }
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.color)
                )
            }
        }

        // ═══════ ③ 主金币（旋转翻滚 + 浮动 + 光晕）═══════
        Box(
            modifier = Modifier
                .size(180.dp)
                .graphicsLayer {
                    scaleX = coinScale; scaleY = coinScale
                    alpha = cardAlpha.value
                    translationY = coinFloat
                },
            contentAlignment = Alignment.Center
        ) {
            // 金币光晕
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD700).copy(alpha = 0.65f),
                            Color(0xFFFFD700).copy(alpha = 0.0f)
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension / 2f
                    ),
                    radius = size.minDimension / 2f
                )
            }
            // 主 emoji 金币
            Text(
                "🪙",
                fontSize = 92.sp,
                modifier = Modifier.graphicsLayer { rotationY = coinRotation }
            )
            // 旋转高光
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = coinShine }
            ) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0f),
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0f)
                        )
                    ),
                    startAngle = 0f, sweepAngle = 80f, useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(size.width * 0.18f, size.height * 0.18f),
                    size = GeomSize(size.width * 0.64f, size.height * 0.64f)
                )
            }
        }

        // ═══════ ④ 金币四射粒子 ═══════
        particles.forEachIndexed { i, p ->
            val anim = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(300L + p.delay)
                anim.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            }
            val rad = (p.angle * Math.PI / 180f).toFloat()
            val tx = cos(rad) * p.distance * anim.value
            val ty = sin(rad) * p.distance * anim.value
            Text(
                p.emoji,
                fontSize = p.size.sp,
                modifier = Modifier
                    .graphicsLayer {
                        translationX = tx * density
                        translationY = ty * density
                        alpha = (1f - anim.value) * cardAlpha.value
                        scaleX = 0.5f + anim.value * 0.5f
                        scaleY = 0.5f + anim.value * 0.5f
                        rotationZ = anim.value * 360f
                    }
            )
        }

        // ═══════ ⑤ 主卡片（数字 + 文案）═══════
        Column(
            modifier = Modifier
                .padding(32.dp)
                .graphicsLayer {
                    scaleX = cardScale.value
                    scaleY = cardScale.value
                    alpha = cardAlpha.value
                    translationY = 130f  // 让卡片显示在金币下方
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 数字滚动（+N 金币）—— 渐变金色 + 阴影
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "+",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFEB3B),
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xFFFF6F00),
                            offset = Offset(0f, 3f),
                            blurRadius = 10f
                        )
                    )
                )
                Text(
                    "$displayAmount",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFEB3B),
                    letterSpacing = 1.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color(0xFFFF6F00),
                            offset = Offset(0f, 4f),
                            blurRadius = 14f
                        )
                    )
                )
                Text(
                    "金币",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // 打卡成功标签（渐变胶囊）
            Box(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(50), ambientColor = Color(0xFFFFD700), spotColor = Color(0xFFFFD700))
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFFD54F), Color(0xFFFFB74D), Color(0xFFFF9800)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🎉", fontSize = 16.sp)
                    Text(
                        "打卡成功",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0xFFE65100),
                                offset = Offset(0f, 2f),
                                blurRadius = 4f
                            )
                        )
                    )
                    Text("🎉", fontSize = 16.sp)
                }
            }

            // 7 天连续奖励
            if (bonusMessage.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = Color(0xFFE91E63), spotColor = Color(0xFFE91E63))
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF80AB), Color(0xFFE040FB), Color(0xFFAB47BC))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🏆", fontSize = 14.sp)
                        Text(
                            bonusMessage,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

// 粒子数据
private data class ParticleSpec(
    val angle: Float,
    val distance: Float,
    val delay: Int,
    val size: Int,
    val emoji: String
)

private data class ConfettiSpec(
    val xRatio: Float,
    val size: Int,
    val speed: Float,
    val color: Color,
    val startDelay: Float
)
