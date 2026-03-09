// HabitScreen.kt - 完善版习惯打卡屏幕
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.data.model.Habit
import com.example.funlife.data.model.HabitRecord
import com.example.funlife.viewmodel.HabitViewModel
import com.example.funlife.viewmodel.HabitWithStats
import com.example.funlife.viewmodel.CheckInResult
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    viewModel: HabitViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val habitsWithStats by viewModel.habitsWithStats.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var selectedHabit by remember { mutableStateOf<HabitWithStats?>(null) }
    var showCoinReward by remember { mutableStateOf(false) }
    var coinRewardAmount by remember { mutableStateOf(0) }
    var bonusMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Default.Add, "添加") },
                text = { Text("添加习惯") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (habitsWithStats.isEmpty()) {
                EmptyHabitState(Modifier.padding(padding))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 顶部统计卡片
                    item {
                        HabitOverviewCard(habitsWithStats)
                    }
                    
                    // 习惯列表
                    items(habitsWithStats, key = { it.habit.id }) { habitStats ->
                        EnhancedHabitCard(
                            habitStats = habitStats,
                            onCheckIn = { 
                                coroutineScope.launch {
                                    viewModel.toggleCheckIn(habitStats.habit.id, habitStats.todayChecked).collect { result ->
                                        when (result) {
                                            is CheckInResult.Success -> {
                                                coinRewardAmount = result.coins
                                                bonusMessage = if (result.hasBonus) "连续7天额外奖励！" else ""
                                                showCoinReward = true
                                            }
                                            is CheckInResult.Cancelled -> {
                                                // 取消打卡，不显示动画
                                            }
                                            is CheckInResult.Failed -> {
                                                // 可以显示错误提示
                                            }
                                        }
                                    }
                                }
                            },
                            onDelete = { viewModel.deleteHabit(habitStats.habit) },
                            onClick = { selectedHabit = habitStats }
                        )
                    }
                }
            }
            
            // 金币奖励动画
            if (showCoinReward) {
                CoinRewardAnimation(
                    amount = coinRewardAmount,
                    bonusMessage = bonusMessage,
                    onDismiss = { showCoinReward = false }
                )
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
}

@Composable
fun EmptyHabitState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // 动画图标
            var scale by remember { mutableStateOf(1f) }
            LaunchedEffect(Unit) {
                while (true) {
                    animate(1f, 1.2f, animationSpec = tween(1000)) { value, _ -> scale = value }
                    animate(1.2f, 1f, animationSpec = tween(1000)) { value, _ -> scale = value }
                }
            }
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("📅", fontSize = (60 * scale).sp)
            }
            
            Text(
                "还没有习惯",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "点击下方按钮添加第一个习惯\n开始养成好习惯的旅程！",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 顶部：图标、名称、统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // 图标
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        color,
                                        color.copy(alpha = 0.7f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(habit.icon, fontSize = 36.sp)
                    }
                    
                    // 名称
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            habit.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // 补卡卡片显示
                        if (habit.makeupCards > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFD700).copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🎫", fontSize = 16.sp)
                                    Text(
                                        "×${habit.makeupCards}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD700)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 右侧按钮组
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 删除按钮
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // 立即打卡按钮 - 缩小版
                    Button(
                        onClick = onCheckIn,
                        modifier = Modifier
                            .height(40.dp)
                            .widthIn(min = 90.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (habitStats.todayChecked) 
                                MaterialTheme.colorScheme.surfaceVariant 
                            else 
                                color,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (habitStats.todayChecked) 0.dp else 4.dp
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (habitStats.todayChecked) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                if (habitStats.todayChecked) "已打卡" else "打卡",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            
            // 统计数据行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 连续天数
                StatBadge(
                    icon = "🔥",
                    label = "连续",
                    value = "${habitStats.currentStreak}天",
                    color = Color(0xFFFF6B35),
                    modifier = Modifier.weight(1f)
                )
                
                // 总天数
                StatBadge(
                    icon = "📊",
                    label = "总计",
                    value = "${habitStats.totalDays}天",
                    color = color,
                    modifier = Modifier.weight(1f)
                )
                
                // 完成率
                StatBadge(
                    icon = "✨",
                    label = "完成率",
                    value = "${(habitStats.completionRate * 100).toInt()}%",
                    color = Color(0xFF4ECDC4),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // 进度条
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "最近30天完成率",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(color.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(habitStats.completionRate)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        color,
                                        color.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )
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

@Composable
fun StatBadge(
    icon: String,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 20.sp)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    
    // 使用Dialog而不是AlertDialog，这样可以更好地控制层级
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dialog内容
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .align(Alignment.Center),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 标题 - 更紧凑
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(habitStats.habit.icon, fontSize = 24.sp)
                            Text(
                                habitStats.habit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // 立即打卡按钮
                        Button(
                            onClick = { 
                                coroutineScope.launch {
                                    viewModel.toggleCheckIn(habitStats.habit.id, localTodayChecked).collect { result ->
                                        when (result) {
                                            is CheckInResult.Success -> {
                                                // 立即更新本地状态
                                                localTodayChecked = !localTodayChecked
                                                coinRewardAmount = result.coins
                                                bonusMessage = if (result.hasBonus) "连续7天额外奖励！" else ""
                                                showCoinReward = true
                                            }
                                            is CheckInResult.Cancelled -> {
                                                // 取消打卡，更新本地状态
                                                localTodayChecked = !localTodayChecked
                                            }
                                            is CheckInResult.Failed -> {}
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (localTodayChecked) 
                                    MaterialTheme.colorScheme.surfaceVariant 
                                else 
                                    MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                if (localTodayChecked) Icons.Default.CheckCircle else Icons.Default.Circle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                if (localTodayChecked) "已打卡" else "打卡",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp
                            )
                        }
                    }
                
                    // 统计数据 - 更紧凑
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        CompactStatItem("${habitStats.totalDays}", "总天数")
                        CompactStatItem("${habitStats.currentStreak}", "连续")
                        CompactStatItem("${habitStats.longestStreak}", "最长")
                    }
                
                    // 月历视图 - 更紧凑
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 月份导航
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { currentMonth = currentMonth.minusMonths(1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowLeft, 
                                    "上个月",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            Text(
                                currentMonth.format(DateTimeFormatter.ofPattern("yyyy年MM月")),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            
                            IconButton(
                                onClick = { currentMonth = currentMonth.plusMonths(1) },
                                enabled = currentMonth.isBefore(LocalDate.now().withDayOfMonth(1)),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowRight, 
                                    "下个月",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        // 星期标题
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                                Text(
                                    day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // 日历网格 - 带金币提示
                        CompactHabitCalendarGrid(
                            currentMonth = currentMonth,
                            recordDates = recordDates,
                            habitColor = Color(android.graphics.Color.parseColor(habitStats.habit.color)),
                            onDateClick = { date ->
                                val dateStr = date.toString()
                                val isChecked = recordDates.contains(dateStr)
                                viewModel.toggleCheckIn(habitStats.habit.id, isChecked, dateStr)
                            }
                        )
                    }
                
                    // 关闭按钮
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("关闭", style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp)
                    }
                }
        }
            
            // 金币奖励动画 - 显示在Dialog内容的最上层
            if (showCoinReward) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
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
            
            // 如果已经超过当前月份，停止生成
            if (currentDay.isAfter(lastDayOfMonth.plusDays(7))) {
                return@Column
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
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("💪") }
    var selectedColor by remember { mutableStateOf("#4ECDC4") }
    
    val icons = listOf("💪", "📚", "🏃", "💧", "🧘", "🎯", "✍️", "🎨", "🍎", "😴", "🚶", "🎵")
    val colors = listOf(
        "#4ECDC4", "#FF6B9D", "#FFD700", "#9B59B6", 
        "#FF6B35", "#3498DB", "#2ECC71", "#E74C3C"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "添加习惯",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("习惯名称") },
                    placeholder = { Text("例如：每天喝8杯水") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "选择图标",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(icons) { icon ->
                            FilterChip(
                                selected = selectedIcon == icon,
                                onClick = { selectedIcon = icon },
                                label = { Text(icon, fontSize = 24.sp) },
                                shape = CircleShape
                            )
                        }
                    }
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "选择颜色",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(colors) { color ->
                            Box(
                                modifier = Modifier
                                    .size(if (selectedColor == color) 48.dp else 40.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = { if (name.isNotBlank()) onConfirm(name, selectedIcon, selectedColor) },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("确定")
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
    var scale by remember { mutableStateOf(0f) }
    var alpha by remember { mutableStateOf(1f) }
    
    LaunchedEffect(Unit) {
        // 放大动画
        animate(0f, 1.2f, animationSpec = tween(300)) { value, _ -> scale = value }
        animate(1.2f, 1f, animationSpec = tween(200)) { value, _ -> scale = value }
        
        // 等待2秒
        kotlinx.coroutines.delay(2000)
        
        // 淡出动画
        animate(1f, 0f, animationSpec = tween(500)) { value, _ -> alpha = value }
        visible = false
        onDismiss()
    }
    
    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * alpha)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = alpha
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("💰", fontSize = 64.sp)
                    
                    Text(
                        "+$amount 金币",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    
                    if (bonusMessage.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                bonusMessage,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    Text(
                        "打卡成功！",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
