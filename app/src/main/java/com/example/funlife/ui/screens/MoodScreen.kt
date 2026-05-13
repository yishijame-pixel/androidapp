// MoodScreen.kt - 完善版心情日记屏幕
package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.data.model.MoodEntry
import com.example.funlife.viewmodel.MoodViewModel
import com.example.funlife.ui.components.PageHeader
import com.example.funlife.ui.components.PageHeaderGradients
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodScreen(
    viewModel: MoodViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val moods by viewModel.moods.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val buttonSize = with(density) { 90.dp.toPx() }
    val paddingPx = with(density) { 16.dp.toPx() }
    val bottomPaddingPx = with(density) { 96.dp.toPx() }
    
    // 可拖动的3D按钮状态 - 初始位置在右下角
    var offsetX by remember { mutableStateOf(screenWidthPx - buttonSize - paddingPx) }
    var offsetY by remember { mutableStateOf(screenHeightPx - buttonSize - bottomPaddingPx) }
    var isPressed by remember { mutableStateOf(false) }
    
    // 加载背景图片
    val backgroundBitmap = remember {
        try {
            context.assets.open("login/xinq_1.png").use { inputStream ->
                android.graphics.BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: java.io.IOException) {
            null
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 背景图片
        backgroundBitmap?.let { bitmap ->
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "心情背景",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        
        // 内容层
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部简洁按钮（去掉 PageHeader）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
            }
            
            // 添加顶部间距，避免挡住标题
            Spacer(modifier = Modifier.height(120.dp))
            
            // 主内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                if (moods.isEmpty()) {
                    EmptyMoodState(Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(moods) { mood ->
                            EnhancedMoodCard(
                                mood = mood,
                                onDelete = { viewModel.deleteMood(mood) }
                            )
                        }
                    }
                }
            }
        }
        
        // 可拖动的3D风格按钮
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.88f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )
        
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                .size(90.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isPressed = true
                        },
                        onDragEnd = {
                            isPressed = false
                        },
                        onDragCancel = {
                            isPressed = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(
                                0f,
                                screenWidthPx - buttonSize
                            )
                            offsetY = (offsetY + dragAmount.y).coerceIn(
                                0f,
                                screenHeightPx - buttonSize
                            )
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = {
                            showDialog = true
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
                // 最外层黄色光晕 - 更柔和
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFE5B4).copy(alpha = 0.6f),
                                    Color(0xFFFFE5B4).copy(alpha = 0.3f),
                                    Color(0xFFFFE5B4).copy(alpha = 0.1f),
                                    Color.Transparent
                                ),
                                center = Offset(0.5f, 0.45f)
                            ),
                            shape = CircleShape
                        )
                )
                
                // 橙色边框圈 - 带光泽
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .drawBehind {
                            // 边框底部阴影
                            drawCircle(
                                color = Color(0xFFFF9966).copy(alpha = 0.3f),
                                radius = size.width * 0.48f,
                                center = center.copy(y = center.y + 3.dp.toPx())
                            )
                        }
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFE4C4),  // 顶部更浅
                                    Color(0xFFFFD4A3),
                                    Color(0xFFFFB380)   // 底部更深
                                )
                            ),
                            shape = CircleShape
                        )
                        .drawBehind {
                            // 边框顶部高光
                            drawArc(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.4f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.5f, size.height * 0.3f)
                                ),
                                startAngle = -90f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(size.width * 0.1f, size.height * 0.1f),
                                size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 0.8f)
                            )
                        }
                )
                
                // 粉色主按钮 - 带明显渐变和光泽
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .drawBehind {
                            // 底部深色阴影 - 更柔和
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFF6B9D).copy(alpha = 0.5f),
                                        Color(0xFFFF6B9D).copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                radius = size.width * 0.55f,
                                center = center.copy(y = center.y + 8.dp.toPx())
                            )
                        }
                        .clip(CircleShape)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFE0EC),  // 顶部最浅
                                    Color(0xFFFFD4E5),  
                                    Color(0xFFFFB6D9),  
                                    Color(0xFFFF8FB8)   // 底部最深
                                )
                            )
                        )
                        .drawBehind {
                            // 顶部光泽效果
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.5f),
                                        Color.White.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                radius = size.width * 0.35f,
                                center = Offset(size.width * 0.5f, size.height * 0.25f)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 白色加号 - 3D凸起效果
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .drawBehind {
                                val strokeWidth = 8.dp.toPx()
                                val halfStroke = strokeWidth / 2
                                
                                // 加号底部阴影（粉色）- 更明显
                                val shadowOffset = 4.dp.toPx()
                                
                                // 横线底部阴影
                                drawRoundRect(
                                    color = Color(0xFFFF8FB8).copy(alpha = 0.7f),
                                    topLeft = Offset(0f, size.height / 2 - halfStroke + shadowOffset),
                                    size = androidx.compose.ui.geometry.Size(size.width, strokeWidth),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(halfStroke)
                                )
                                
                                // 竖线底部阴影
                                drawRoundRect(
                                    color = Color(0xFFFF8FB8).copy(alpha = 0.7f),
                                    topLeft = Offset(size.width / 2 - halfStroke, shadowOffset),
                                    size = androidx.compose.ui.geometry.Size(strokeWidth, size.height),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(halfStroke)
                                )
                                
                                // 白色横线主体
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(0f, size.height / 2 - halfStroke),
                                    size = androidx.compose.ui.geometry.Size(size.width, strokeWidth),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(halfStroke)
                                )
                                
                                // 白色竖线主体
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(size.width / 2 - halfStroke, 0f),
                                    size = androidx.compose.ui.geometry.Size(strokeWidth, size.height),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(halfStroke)
                                )
                                
                                // 加号顶部高光（更明显）
                                val highlightOffset = -1.5.dp.toPx()
                                val highlightWidth = strokeWidth * 0.5f
                                
                                // 横线顶部高光
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.9f),
                                    topLeft = Offset(3.dp.toPx(), size.height / 2 - halfStroke + highlightOffset),
                                    size = androidx.compose.ui.geometry.Size(size.width - 6.dp.toPx(), highlightWidth),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(highlightWidth / 2)
                                )
                                
                                // 竖线顶部高光
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.9f),
                                    topLeft = Offset(size.width / 2 - halfStroke + highlightOffset, 3.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(highlightWidth, size.height - 6.dp.toPx()),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(highlightWidth / 2)
                                )
                            }
                    )
                }
            }
        
        if (showDialog) {
            AddMoodDialog(
                onDismiss = { showDialog = false },
                onConfirm = { mood, level, note ->
                    viewModel.addMood(mood, level, note)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun EmptyMoodState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // 可爱的浮动提示 - 位于顶部偏下，不遮挡背景
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 180.dp)  // 和习惯页面一样的位置
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
                    Text("💭", fontSize = 24.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "还没有心情记录哦",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF87CEEB),  // 天蓝色 (Sky Blue)
                            fontSize = 16.sp
                        )
                        Text(
                            "点击右下角按钮记录心情",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFADD8E6),  // 浅蓝色 (Light Blue)
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoodOverviewCard(moods: List<MoodEntry>) {
    val totalMoods = moods.size
    val recentMoods = moods.take(7)
    
    // 统计心情分布
    val moodCounts = moods.groupingBy { it.mood }.eachCount()
    val mostFrequentMood = moodCounts.maxByOrNull { it.value }?.key ?: "😊"
    
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
                    "心情概览",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MoodStatItem("📝", "总记录", "$totalMoods 条")
                Divider(
                    modifier = Modifier
                        .height(50.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                MoodStatItem("😊", "最常心情", mostFrequentMood)
                Divider(
                    modifier = Modifier
                        .height(50.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                MoodStatItem("📅", "最近7天", "${recentMoods.size} 条")
            }
        }
    }
}

@Composable
fun MoodStatItem(icon: String, label: String, value: String) {
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
fun EnhancedMoodCard(mood: MoodEntry, onDelete: () -> Unit) {
    // 根据心情选择颜色和装饰
    val (moodColor, decorEmoji) = when (mood.mood) {
        "😊", "😃", "🥰", "😍", "🤗" -> Color(0xFF4ECDC4) to listOf("✨", "💫", "⭐")
        "😐", "🤔", "😶" -> Color(0xFFFFD700) to listOf("☁️", "🌤️", "💭")
        "😢", "😭", "🥺" -> Color(0xFF3498DB) to listOf("💧", "🌧️", "💙")
        "😡", "😤", "😠" -> Color(0xFFE74C3C) to listOf("💢", "⚡", "🔥")
        "😴", "🥱", "😪" -> Color(0xFF9B59B6) to listOf("💤", "🌙", "⭐")
        else -> Color(0xFF4ECDC4) to listOf("✨", "💫", "⭐")
    }
    
    var isPressed by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetailDialog = true }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            moodColor.copy(alpha = 0.08f),
                            moodColor.copy(alpha = 0.15f),
                            moodColor.copy(alpha = 0.08f)
                        )
                    )
                )
        ) {
            
            // 装饰圆圈 - 左上角
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-30).dp, y = (-30).dp)
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                moodColor.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // 装饰圆圈 - 右下角
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp, y = 40.dp)
                    .size(120.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                moodColor.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // 装饰emoji - 随机分布
            decorEmoji.forEachIndexed { index, emoji ->
                Text(
                    text = emoji,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(
                            when (index) {
                                0 -> Alignment.TopEnd
                                1 -> Alignment.BottomStart
                                else -> Alignment.CenterEnd
                            }
                        )
                        .offset(
                            x = when (index) {
                                0 -> (-20).dp
                                1 -> 20.dp
                                else -> (-15).dp
                            },
                            y = when (index) {
                                0 -> 15.dp
                                1 -> (-15).dp
                                else -> 0.dp
                            }
                        )
                        .graphicsLayer {
                            alpha = 0.4f
                        }
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // 超可爱的心情图标容器
                    Box(
                        modifier = Modifier.size(72.dp)
                    ) {
                        // 外层光晕
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            moodColor.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                        
                        // 内层圆形背景
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .align(Alignment.Center)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            moodColor.copy(alpha = 0.9f),
                                            moodColor.copy(alpha = 0.7f)
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mood.mood, fontSize = 36.sp)
                        }
                        
                        // 小星星装饰
                        Text(
                            "✨",
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                        )
                    }
                    
                    // 日期和备注
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 格式化日期显示
                        val date = try {
                            LocalDate.parse(mood.date)
                            val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
                            LocalDate.parse(mood.date).format(formatter)
                        } catch (e: Exception) {
                            mood.date
                        }
                        
                        Text(
                            date,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D3748),
                            fontSize = 17.sp
                        )
                        
                        if (mood.note.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = moodColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    mood.note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF4A5568),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    maxLines = 2,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                
                // 可爱的删除按钮
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF5F5))
                        .clickable { showDeleteDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(22.dp)
                    )
                }
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
                    "确定要删除这条心情记录吗？\n删除后将无法恢复。",
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
        MoodDetailDialog(
            mood = mood,
            color = moodColor,
            onDismiss = { showDetailDialog = false }
        )
    }
}

@Composable
fun MoodDetailDialog(
    mood: MoodEntry,
    color: Color,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFFFFFBF5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 顶部装饰
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("✨", fontSize = 24.sp)
                    Text("✨", fontSize = 24.sp)
                }
                
                // 大心情图标
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
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(mood.mood, fontSize = 72.sp)
                }
                
                // 日期信息
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = color.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val date = try {
                            val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
                            LocalDate.parse(mood.date).format(formatter)
                        } catch (e: Exception) {
                            mood.date
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📅", fontSize = 20.sp)
                            Text(
                                date,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }
                }
                
                // 心情备注
                if (mood.note.isNotEmpty()) {
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
                                Text("💭", fontSize = 20.sp)
                                Text(
                                    "今天的心情",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5D4037)
                                )
                            }
                            
                            Text(
                                mood.note,
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
                                "没有记录心情备注",
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
fun MoodCard(mood: MoodEntry, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
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
                Text(mood.mood, fontSize = 40.sp)
                Column {
                    Text(
                        mood.date,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (mood.note.isNotEmpty()) {
                        Text(
                            mood.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
fun AddMoodDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String) -> Unit
) {
    var selectedMood by remember { mutableStateOf("😊") }
    var selectedLevel by remember { mutableStateOf(5) }
    var note by remember { mutableStateOf("") }
    
    // 更多可爱的表情选项
    val moods = listOf(
        "🥰" to "超开心",
        "😊" to "开心",
        "😃" to "兴奋",
        "🤗" to "温暖",
        "😍" to "喜欢",
        "😐" to "平静",
        "🤔" to "思考",
        "😶" to "无语",
        "😴" to "困倦",
        "🥱" to "疲惫",
        "😢" to "难过",
        "😭" to "伤心",
        "🥺" to "委屈",
        "😡" to "生气",
        "😤" to "愤怒",
        "😠" to "不满"
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("✨", fontSize = 24.sp)
                    Text(
                        "记录心情",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "今天的心情",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // 使用LazyRow显示更多表情
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(moods.size) { index ->
                            val (emoji, label) = moods[index]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedMood = emoji
                                        selectedLevel = index + 1
                                    }
                                    .background(
                                        if (selectedMood == emoji)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            Color.Transparent
                                    )
                                    .padding(12.dp)
                                    .width(60.dp)
                            ) {
                                Text(
                                    emoji,
                                    fontSize = if (selectedMood == emoji) 36.sp else 28.sp
                                )
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedMood == emoji)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（选填）") },
                    placeholder = { Text("记录今天发生的事情...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
                
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
                        onClick = { onConfirm(selectedMood, selectedLevel, note) },
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
