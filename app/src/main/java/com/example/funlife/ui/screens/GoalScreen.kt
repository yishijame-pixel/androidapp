// GoalScreen.kt - 完善版目标管理屏幕
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.data.model.Countdown
import com.example.funlife.viewmodel.GoalViewModel
import com.example.funlife.ui.components.PageHeader
import com.example.funlife.ui.components.PageHeaderGradients
import com.example.funlife.ui.components.ArtisticText
import com.example.funlife.ui.components.ArtisticTextStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(
    viewModel: GoalViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val countdowns by viewModel.countdowns.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var showMoreDialog by remember { mutableStateOf(false) }  // 查看更多对话框
    
    // 不再自动删除过期目标，保留作为历史记录
    
    // 分类目标：进行中的和已完成的
    val activeCountdowns = countdowns.filter { it.getDaysRemaining() > 0 }
    val completedCountdowns = countdowns.filter { it.getDaysRemaining() <= 0 }
    
    // 加载背景图片
    val backgroundBitmap = remember {
        try {
            context.assets.open("login/mubiao.png").use { inputStream ->
                android.graphics.BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            android.util.Log.e("GoalScreen", "Failed to load background image", e)
            null
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 背景图片铺满
        backgroundBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "背景",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
            )
        }
        
        // 内容层 - 显示在背景图片上面
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp),
            floatingActionButton = {
                // 添加按钮 - 右下角，避开底部装饰
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = Color(0xFFFFB8D5),
                    contentColor = Color.White,
                    modifier = Modifier
                        .padding(bottom = 100.dp, end = 8.dp)
                        .size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "添加目标",
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 顶部导航栏 - 返回按钮和查看更多按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                ) {
                    // 左侧返回按钮
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = Color(0xFF8B4513),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // 右侧查看更多按钮
                    TextButton(
                        onClick = { showMoreDialog = true },
                        modifier = Modifier.align(Alignment.CenterEnd),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF8B4513)
                        )
                    ) {
                        Text(
                            "查看更多",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // 内容面板区域 - 精确对齐背景图片的面板（最多显示4个进行中的目标）
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 140.dp, bottom = 120.dp),
                    userScrollEnabled = false // 禁用滚动
                ) {
                    // 只显示前4个进行中的目标
                    val displayCountdowns = activeCountdowns.take(4)
                    
                    items(4) { index ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 60.dp,
                                    end = 60.dp,
                                    top = if (index == 0) 0.dp else 38.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index < displayCountdowns.size) {
                                // 显示目标卡片
                                EnhancedCountdownCard(
                                    countdown = displayCountdowns[index],
                                    onDelete = { viewModel.deleteCountdown(displayCountdowns[index]) },
                                    isRightAligned = index % 2 == 1
                                )
                            } else {
                                // 显示空状态面板
                                EmptyGoalPanel(
                                    index = index,
                                    isRightAligned = index % 2 == 1,
                                    onClick = { showDialog = true }
                                )
                            }
                        }
                    }
                }
            }
        }
    
        // 添加目标对话框
        if (showDialog) {
            AddCountdownDialog(
                onDismiss = { showDialog = false },
                onConfirm = { title, date, icon, color, note ->
                    viewModel.addCountdown(title, date, "生活", icon, color, note)
                    showDialog = false
                }
            )
        }
        
        // 查看更多对话框
        if (showMoreDialog) {
            GoalMoreDialog(
                activeCountdowns = activeCountdowns,
                completedCountdowns = completedCountdowns,
                onDismiss = { showMoreDialog = false },
                onDelete = { countdown -> viewModel.deleteCountdown(countdown) }
            )
        }
    }
}

@Composable
fun EmptyGoalPanel(
    index: Int,
    isRightAligned: Boolean,
    onClick: () -> Unit
) {
    // 可爱的提示文字和图标
    val emptyMessages = listOf(
        "🎯" to "快来编辑我们的目标吧！",
        "✨" to "添加一个新目标吧~",
        "💫" to "这里还空着呢！",
        "🌟" to "来设定一个小目标！"
    )
    
    val (icon, message) = emptyMessages[index % emptyMessages.size]
    
    // 根据index设置不同的顶部padding，第三、第四个卡片往上移
    val topPadding = when (index) {
        0, 1 -> 25.dp  // 第一、第二个卡片
        2, 3 -> 15.dp  // 第三、第四个卡片往上移
        else -> 25.dp
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (isRightAligned) 0.dp else 60.dp,
                    end = if (isRightAligned) 60.dp else 0.dp,
                    top = topPadding  // 使用动态的顶部padding
                ),
            horizontalAlignment = if (isRightAligned) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFE4B5).copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    icon,
                    fontSize = 24.sp
                )
            }
            
            // 提示文字
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD2691E).copy(alpha = 0.6f),
                fontSize = 11.sp,
                textAlign = if (isRightAligned) TextAlign.End else TextAlign.Start,
                maxLines = 2
            )
        }
    }
}

@Composable
fun EmptyGoalState(modifier: Modifier = Modifier) {
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
                Text("🎯", fontSize = (60 * scale).sp)
            }
            
            Text(
                "还没有倒数日",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "点击下方按钮添加重要日期\n记录生活中的重要时刻",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GoalOverviewCard(countdowns: List<Countdown>) {
    val totalCountdowns = countdowns.size
    val upcomingCountdowns = countdowns.count { it.getDaysRemaining() >= 0 }
    val closestCountdown = countdowns
        .filter { it.getDaysRemaining() >= 0 }
        .minByOrNull { it.getDaysRemaining() }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9F66)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 装饰圆圈 - 右上角
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-30).dp)
                    .size(120.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // 装饰圆圈 - 左下角
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-30).dp, y = 30.dp)
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
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
                        "目标概览",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    GoalStatItem("📝", "总目标", "$totalCountdowns 个", Color.White)
                    Divider(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp),
                        color = Color.White.copy(alpha = 0.3f)
                    )
                    GoalStatItem("⏰", "进行中", "$upcomingCountdowns 个", Color.White)
                    Divider(
                        modifier = Modifier
                            .height(50.dp)
                            .width(1.dp),
                        color = Color.White.copy(alpha = 0.3f)
                    )
                    GoalStatItem(
                        "🔥",
                        "最近目标",
                        closestCountdown?.let { "${it.getDaysRemaining()}天" } ?: "无",
                        Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun GoalStatItem(icon: String, label: String, value: String, textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(icon, fontSize = 24.sp)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = textColor.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun EnhancedCountdownCard(countdown: Countdown, onDelete: () -> Unit, isRightAligned: Boolean = false) {
    val daysRemaining = countdown.getDaysRemaining()
    val color = Color(android.graphics.Color.parseColor(countdown.color))
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }
    
    // 计算进度
    val totalDays = 100
    val progress = if (daysRemaining > 0) {
        ((totalDays - daysRemaining).toFloat() / totalDays).coerceIn(0f, 1f)
    } else {
        1f
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { showDetailDialog = true }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        // 竖直排列：标题+备注 → 进度条+天数
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (isRightAligned) 0.dp else 60.dp,
                    end = if (isRightAligned) 60.dp else 0.dp
                ),
            horizontalAlignment = if (isRightAligned) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)  // 增加间距
        ) {
            // 标题和备注（更大的文字）
            Column(
                horizontalAlignment = if (isRightAligned) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 标题
                Text(
                    countdown.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD2691E),
                    fontSize = 16.sp,  // 从12sp增加到16sp
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                // 备注（如果有）
                if (countdown.note.isNotBlank()) {
                    Text(
                        countdown.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD2691E).copy(alpha = 0.7f),
                        fontSize = 13.sp,  // 备注稍小一点
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            // 进度条和天数（横向排列）
            Row(
                modifier = Modifier.fillMaxWidth(0.75f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 进度条
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)  // 稍微增加高度
                ) {
                    val density = LocalDensity.current
                    val maxWidthPx = with(density) { maxWidth.toPx() }
                    val sliderSizePx = with(density) { 14.dp.toPx() }
                    val sliderOffset = with(density) { ((progress * (maxWidthPx - sliderSizePx))).toDp() }
                    
                    // 背景轨道
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFE4B5))
                    )
                    
                    // 进度填充
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFB347))
                    )
                    
                    // 圆形滑块
                    if (progress > 0.02f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = sliderOffset)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .drawBehind {
                                    drawCircle(
                                        color = Color.Black.copy(alpha = 0.1f),
                                        radius = size.width * 0.5f,
                                        center = center.copy(y = center.y + 1f)
                                    )
                                }
                        )
                    }
                }
                
                // 天数显示（在进度条后面）
                Text(
                    when {
                        daysRemaining > 0 -> "${daysRemaining}天"
                        daysRemaining == 0L -> "今天"
                        else -> "${-daysRemaining}天"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9966),
                    fontSize = 14.sp  // 从10sp增加到14sp
                )
            }
        }
        
        // 删除按钮 - 右上角
        IconButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .offset(x = 8.dp, y = (-8).dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                tint = Color(0xFFFFB8B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Text("🗑️", fontSize = 48.sp)
            },
            title = {
                Text(
                    "确认删除",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "确定要删除「${countdown.title}」吗？\n删除后将无法恢复。",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B6B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("确定删除")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    // 可爱的详情面板
    if (showDetailDialog) {
        GoalDetailDialog(
            countdown = countdown,
            color = color,
            onDismiss = { showDetailDialog = false }
        )
    }
}

@Composable
fun GoalDetailDialog(
    countdown: Countdown,
    color: Color,
    onDismiss: () -> Unit
) {
    val daysRemaining = countdown.getDaysRemaining()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFFFFFBF5))
                .drawBehind {
                    // 外层光晕
                    drawRoundRect(
                        color = color.copy(alpha = 0.1f),
                        topLeft = Offset(-10f, -10f),
                        size = Size(size.width + 20f, size.height + 20f),
                        cornerRadius = CornerRadius(32.dp.toPx())
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 顶部装饰星星
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("✨", fontSize = 24.sp)
                    Text("✨", fontSize = 24.sp)
                }
                
                // 大图标
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    val context = LocalContext.current
                    val iconBitmap = remember(countdown.icon) {
                        try {
                            context.assets.open("login/${countdown.icon}.png").use { inputStream ->
                                android.graphics.BitmapFactory.decodeStream(inputStream)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        color.copy(alpha = 0.3f),
                                        color.copy(alpha = 0.15f)
                                    )
                                )
                            )
                            .drawBehind {
                                // 装饰圆点
                                val dotSize = size.width * 0.08f
                                for (i in 0..7) {
                                    val angle = (i * 45f) * (Math.PI / 180f)
                                    val radius = size.width * 0.45f
                                    drawCircle(
                                        color = color.copy(alpha = 0.25f),
                                        radius = dotSize,
                                        center = Offset(
                                            (size.width / 2 + radius * Math.cos(angle)).toFloat(),
                                            (size.height / 2 + radius * Math.sin(angle)).toFloat()
                                        )
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        iconBitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = countdown.icon,
                                modifier = Modifier.size(100.dp)
                            )
                        }
                    }
                }
                
                // 标题
                Text(
                    countdown.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037),
                    textAlign = TextAlign.Center
                )
                
                // 日期信息卡片
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = color.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📅", fontSize = 20.sp)
                            Text(
                                countdown.targetDate,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF5D4037)
                            )
                        }
                        
                        Divider(
                            modifier = Modifier.fillMaxWidth(0.6f),
                            color = color.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )
                        
                        Text(
                            when {
                                daysRemaining > 0 -> "还有 $daysRemaining 天"
                                daysRemaining == 0L -> "就是今天！"
                                else -> "已过去 ${-daysRemaining} 天"
                            },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
                
                // 备注内容
                if (countdown.note.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📝", fontSize = 20.sp)
                                Text(
                                    "我的备注",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5D4037)
                                )
                            }
                            
                            Text(
                                countdown.note,
                                fontSize = 15.sp,
                                color = Color(0xFF5D4037).copy(alpha = 0.8f),
                                lineHeight = 22.sp
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("💭", fontSize = 32.sp)
                            Text(
                                "还没有添加备注哦~",
                                fontSize = 14.sp,
                                color = Color(0xFF5D4037).copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                
                // 关闭按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color.copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        "知道了 ✓",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CountdownCard(countdown: Countdown, onDelete: () -> Unit) {
    val daysRemaining = countdown.getDaysRemaining()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(countdown.color)).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(countdown.color))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(countdown.icon, fontSize = 24.sp)
                }
                
                Column {
                    Text(
                        countdown.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            daysRemaining > 0 -> "还有 $daysRemaining 天"
                            daysRemaining == 0L -> "就是今天！"
                            else -> "已过去 ${-daysRemaining} 天"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCountdownDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit  // 添加 note 参数
) {
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }  // 新增备注状态
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedIcon by remember { mutableStateOf("mb_1") }
    var selectedColor by remember { mutableStateOf("#4ECDC4") }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val iconOptions = listOf("mb_1", "mb_2", "mb_3", "mb_4", "mb_5", "mb_6", "mb_7", "mb_8", "mb_9", "mb_10")
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
                    "添加倒数日",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    placeholder = { Text("例如：毕业典礼") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // 备注输入框
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    placeholder = { Text("记录一些想说的话...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 5
                )
                
                // 日期选择按钮
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        selectedDate?.let {
                            val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
                            it.format(formatter)
                        } ?: "选择日期"
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "选择图标",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(iconOptions) { iconPath ->
                            val isSelected = selectedIcon == iconPath
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 56.dp else 48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) 
                                            Color(android.graphics.Color.parseColor(selectedColor)).copy(alpha = 0.3f)
                                        else 
                                            Color.LightGray.copy(alpha = 0.2f)
                                    )
                                    .clickable { selectedIcon = iconPath },
                                contentAlignment = Alignment.Center
                            ) {
                                val bitmap = remember(iconPath) {
                                    try {
                                        context.assets.open("login/$iconPath.png").use { inputStream ->
                                            android.graphics.BitmapFactory.decodeStream(inputStream)
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                bitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = iconPath,
                                        modifier = Modifier.size(if (isSelected) 40.dp else 32.dp)
                                    )
                                }
                            }
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
                        onClick = {
                            if (title.isNotBlank() && selectedDate != null) {
                                onConfirm(title, selectedDate.toString(), selectedIcon, selectedColor, note)
                            }
                        },
                        enabled = title.isNotBlank() && selectedDate != null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
    
    // 日期选择器对话框
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                ?: System.currentTimeMillis()
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}




// 查看更多对话框 - 显示超出4个的目标和已完成的历史目标
@Composable
fun GoalMoreDialog(
    activeCountdowns: List<Countdown>,
    completedCountdowns: List<Countdown>,
    onDismiss: () -> Unit,
    onDelete: (Countdown) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }  // 0: 更多目标, 1: 历史目标
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF8F0)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 顶部标题栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFB8D5),
                                    Color(0xFFFFD4A3)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        "目标管理",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White
                        )
                    }
                }
                
                // Tab切换
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFFF9966)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "更多目标",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                                if (activeCountdowns.size > 4) {
                                    Text(
                                        "(${activeCountdowns.size - 4})",
                                        fontSize = 12.sp,
                                        color = Color(0xFFFF9966)
                                    )
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "历史目标",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                                if (completedCountdowns.isNotEmpty()) {
                                    Text(
                                        "(${completedCountdowns.size})",
                                        fontSize = 12.sp,
                                        color = Color(0xFF999999)
                                    )
                                }
                            }
                        }
                    )
                }
                
                // 内容区域
                when (selectedTab) {
                    0 -> {
                        // 更多目标（第5个及以后的进行中目标）
                        val moreCountdowns = activeCountdowns.drop(4)
                        if (moreCountdowns.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text("🎯", fontSize = 48.sp)
                                    Text(
                                        "暂无更多目标",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(moreCountdowns) { countdown ->
                                    CompactCountdownCard(
                                        countdown = countdown,
                                        onDelete = { onDelete(countdown) }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // 历史目标（已完成的目标）
                        if (completedCountdowns.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text("📜", fontSize = 48.sp)
                                    Text(
                                        "暂无历史目标",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(completedCountdowns) { countdown ->
                                    CompactCountdownCard(
                                        countdown = countdown,
                                        onDelete = { onDelete(countdown) },
                                        isCompleted = true
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

// 紧凑型目标卡片 - 用于更多目标和历史目标列表
@Composable
fun CompactCountdownCard(
    countdown: Countdown,
    onDelete: () -> Unit,
    isCompleted: Boolean = false
) {
    val daysRemaining = countdown.getDaysRemaining()
    val color = Color(android.graphics.Color.parseColor(countdown.color))
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val iconBitmap = remember(countdown.icon) {
        try {
            context.assets.open("login/${countdown.icon}.png").use { inputStream ->
                android.graphics.BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFF5F5F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 图标
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    iconBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = countdown.icon,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // 标题和时间
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        countdown.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color.Gray else Color(0xFF5D4037),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            countdown.targetDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        
                        Text(
                            when {
                                daysRemaining > 0 -> "还有 $daysRemaining 天"
                                daysRemaining == 0L -> "今天"
                                else -> "已过去 ${-daysRemaining} 天"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isCompleted) Color.Gray else color
                        )
                    }
                }
            }
            
            // 删除按钮
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color(0xFFFFB8B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Text("🗑️", fontSize = 48.sp)
            },
            title = {
                Text(
                    "确认删除",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    "确定要删除「${countdown.title}」吗？\n删除后将无法恢复。",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B6B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("确定删除")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("取消")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}
