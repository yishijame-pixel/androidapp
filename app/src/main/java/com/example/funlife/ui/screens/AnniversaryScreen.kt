// AnniversaryScreen.kt
package com.example.funlife.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import coil.compose.AsyncImage
import com.example.funlife.R
import com.example.funlife.data.model.Anniversary
import com.example.funlife.data.model.AnniversaryType
import com.example.funlife.ui.components.AnniversaryCard
import com.example.funlife.ui.components.AnniversaryListView
import com.example.funlife.ui.components.AnniversaryGridView
import com.example.funlife.ui.components.AnniversaryWaterfallView
import com.example.funlife.ui.components.AnniversaryMemoryWallView
import com.example.funlife.ui.components.AnniversaryTimelineView
import com.example.funlife.ui.components.AnniversaryStatisticsDialog
import com.example.funlife.ui.components.PageHeader
import com.example.funlife.ui.components.PageHeaderGradients
import com.example.funlife.viewmodel.AnniversaryViewModel
import com.example.funlife.viewmodel.SortOrder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnniversaryScreen(
    viewModel: AnniversaryViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val anniversaries by viewModel.anniversaries.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState() // 🔥 视图模式状态
    
    // 🔥 监听用户变化，刷新数据
    val sessionManager = remember { com.example.funlife.utils.UserSessionManager(context) }
    val currentUserId = sessionManager.getCurrentUserId()
    
    LaunchedEffect(currentUserId) {
        android.util.Log.d("AnniversaryScreen", "用户ID变化: $currentUserId，刷新数据")
        viewModel.refreshForNewUser()
    }
    
    var showDialog by remember { mutableStateOf(false) }
    var editingAnniversary by remember { mutableStateOf<Anniversary?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showStatistics by remember { mutableStateOf(false) }
    var showViewModeMenu by remember { mutableStateOf(false) } // 🔥 视图模式菜单
    
    // 🔔 今日纪念日提醒
    var todayReminders by remember { mutableStateOf<List<Anniversary>>(emptyList()) }
    LaunchedEffect(anniversaries) {
        val todays = com.example.funlife.utils.AnniversaryReminderManager.findTodayAnniversaries(anniversaries)
        val newOnes = todays.filter { !com.example.funlife.utils.AnniversaryReminderManager.isTriggered(it.id) }
        if (newOnes.isNotEmpty()) {
            todayReminders = newOnes
            newOnes.forEach { com.example.funlife.utils.AnniversaryReminderManager.markTriggered(it.id) }
            com.example.funlife.utils.AnniversaryReminderManager.triggerAlarm(context, 4500L)
        }
    }
    
    // FAB 拖动状态
    var fabOffsetX by remember { mutableStateOf(0f) }
    var fabOffsetY by remember { mutableStateOf(0f) }
    
    val monthlyStats by viewModel.getMonthlyStatistics().collectAsState()
    val yearlyStats by viewModel.getYearlyStatistics().collectAsState()
    val topAnniversaries by viewModel.getTopImportantAnniversaries().collectAsState()
    val upcomingAnniversaries by viewModel.getUpcomingAnniversaries().collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 🔥 背景图片 - jinian.png
        val context = androidx.compose.ui.platform.LocalContext.current
        val backgroundBitmap = remember {
            com.example.funlife.utils.ImageCache.loadImage(context, "login/jinian.png")
        }
        
        backgroundBitmap?.let { bitmap ->
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = "纪念日背景",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            floatingActionButton = {}, // 移除默认的 FAB
            containerColor = Color.Transparent // 🔥 透明背景以显示背景图片
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize()) {
                // 🔥 添加顶部间距，避免遮挡"纪念日"标题
                Spacer(modifier = Modifier.height(150.dp))
            
            if (anniversaries.isEmpty()) {
                CuteAnniversaryEmptyState(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 🔥 根据视图模式显示不同的布局
                when (viewMode) {
                    com.example.funlife.data.model.AnniversaryViewMode.LIST -> {
                        AnniversaryListView(
                            anniversaries = anniversaries,
                            onDelete = { anniversary -> viewModel.deleteAnniversary(anniversary) },
                            onPin = { anniversary ->
                                if (anniversary.isPinned) {
                                    viewModel.unpinAnniversary(anniversary)
                                } else {
                                    viewModel.pinAnniversary(anniversary)
                                }
                            },
                            onEdit = { anniversary -> editingAnniversary = anniversary },
                            onShare = { anniversary ->
                                val shareHelper = com.example.funlife.utils.AnniversaryShareHelper(context)
                                shareHelper.shareImage(anniversary)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp)
                        )
                    }
                    com.example.funlife.data.model.AnniversaryViewMode.GRID -> {
                        AnniversaryGridView(
                            anniversaries = anniversaries,
                            onDelete = { anniversary -> viewModel.deleteAnniversary(anniversary) },
                            onPin = { anniversary ->
                                if (anniversary.isPinned) {
                                    viewModel.unpinAnniversary(anniversary)
                                } else {
                                    viewModel.pinAnniversary(anniversary)
                                }
                            },
                            onEdit = { anniversary -> editingAnniversary = anniversary },
                            onShare = { anniversary ->
                                val shareHelper = com.example.funlife.utils.AnniversaryShareHelper(context)
                                shareHelper.shareImage(anniversary)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp)
                        )
                    }
                    com.example.funlife.data.model.AnniversaryViewMode.WATERFALL -> {
                        AnniversaryWaterfallView(
                            anniversaries = anniversaries,
                            onDelete = { anniversary -> viewModel.deleteAnniversary(anniversary) },
                            onPin = { anniversary ->
                                if (anniversary.isPinned) {
                                    viewModel.unpinAnniversary(anniversary)
                                } else {
                                    viewModel.pinAnniversary(anniversary)
                                }
                            },
                            onEdit = { anniversary -> editingAnniversary = anniversary },
                            onShare = { anniversary ->
                                val shareHelper = com.example.funlife.utils.AnniversaryShareHelper(context)
                                shareHelper.shareImage(anniversary)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp)
                        )
                    }
                    com.example.funlife.data.model.AnniversaryViewMode.MEMORY_WALL -> {
                        AnniversaryMemoryWallView(
                            anniversaries = anniversaries,
                            onDelete = { anniversary -> viewModel.deleteAnniversary(anniversary) },
                            onPin = { anniversary ->
                                if (anniversary.isPinned) {
                                    viewModel.unpinAnniversary(anniversary)
                                } else {
                                    viewModel.pinAnniversary(anniversary)
                                }
                            },
                            onEdit = { anniversary -> editingAnniversary = anniversary },
                            onShare = { anniversary ->
                                val shareHelper = com.example.funlife.utils.AnniversaryShareHelper(context)
                                shareHelper.shareImage(anniversary)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp)
                        )
                    }
                    com.example.funlife.data.model.AnniversaryViewMode.TIMELINE -> {
                        AnniversaryTimelineView(
                            anniversaries = anniversaries,
                            onDelete = { anniversary -> viewModel.deleteAnniversary(anniversary) },
                            onPin = { anniversary ->
                                if (anniversary.isPinned) {
                                    viewModel.unpinAnniversary(anniversary)
                                } else {
                                    viewModel.pinAnniversary(anniversary)
                                }
                            },
                            onEdit = { anniversary -> editingAnniversary = anniversary },
                            onShare = { anniversary ->
                                val shareHelper = com.example.funlife.utils.AnniversaryShareHelper(context)
                                shareHelper.shareImage(anniversary)
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
            } // Column 结束
        } // Scaffold 结束
        
        // 🔥 返回按钮 - 固定在左上角
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(40.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "返回",
                tint = Color(0xFF8B4513)
            )
        }
        
        // 🔥 视图模式切换按钮 - 固定在右上角
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = { showViewModeMenu = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = when (viewMode) {
                        com.example.funlife.data.model.AnniversaryViewMode.LIST -> Icons.Default.ViewList
                        com.example.funlife.data.model.AnniversaryViewMode.GRID -> Icons.Default.GridView
                        com.example.funlife.data.model.AnniversaryViewMode.WATERFALL -> Icons.Default.ViewModule
                        com.example.funlife.data.model.AnniversaryViewMode.MEMORY_WALL -> Icons.Default.Photo
                        com.example.funlife.data.model.AnniversaryViewMode.TIMELINE -> Icons.Default.Timeline
                    },
                    contentDescription = "切换视图",
                    tint = Color(0xFF8B4513)
                )
            }
            
            // 视图模式菜单
            DropdownMenu(
                expanded = showViewModeMenu,
                onDismissRequest = { showViewModeMenu = false }
            ) {
                com.example.funlife.data.model.AnniversaryViewMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(mode.icon)
                                Text(mode.displayName)
                            }
                        },
                        onClick = {
                            viewModel.setViewMode(mode)
                            showViewModeMenu = false
                        },
                        leadingIcon = {
                            if (viewMode == mode) {
                                Icon(Icons.Default.Check, "已选择")
                            }
                        }
                    )
                }
            }
        }
        
        // 可拖动的 FAB
        val density = androidx.compose.ui.platform.LocalDensity.current
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
        
        CuteAddAnniversaryButton(
            onClick = { showDialog = true },
            text = stringResource(R.string.anniversary_add),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset {
                    androidx.compose.ui.unit.IntOffset(
                        fabOffsetX.toInt(),
                        fabOffsetY.toInt()
                    )
                }
                .padding(bottom = 96.dp, end = 16.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        fabOffsetX = (fabOffsetX + dragAmount.x).coerceIn(
                            -(screenWidthPx - 200f),
                            0f
                        )
                        fabOffsetY = (fabOffsetY + dragAmount.y).coerceIn(
                            -(screenHeightPx - 400f),
                            80f
                        )
                    }
                }
        )
    } // Box 结束
    
    // 🔔 今日纪念日庆祝对话框（自动触发震动+铃声）
    if (todayReminders.isNotEmpty()) {
        AnniversaryReminderDialog(
            reminders = todayReminders,
            onDismiss = {
                com.example.funlife.utils.AnniversaryReminderManager.stopAlarm(context)
                todayReminders = emptyList()
            }
        )
    }
    
    if (showDialog) {
        AddAnniversaryDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, date, imageUri, type, isYearly, note, importance, frameId ->
                android.util.Log.d("AnniversaryScreen", "添加纪念日: name=$name, date=$date, frameId=$frameId")
                viewModel.addAnniversary(
                    name = name,
                    date = date,
                    imageUri = imageUri,
                    type = type,
                    isYearly = isYearly,
                    note = note,
                    importance = importance,
                    frameId = frameId,
                    onSuccess = {
                        android.widget.Toast.makeText(context, "纪念日已添加", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        android.widget.Toast.makeText(context, "添加失败: $error", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                showDialog = false
            }
        )
    }
    
    editingAnniversary?.let { anniversary ->
        EditAnniversaryDialog(
            anniversary = anniversary,
            onDismiss = { editingAnniversary = null },
            onConfirm = { name, date, imageUri, type, isYearly, note, importance, frameId ->
                viewModel.updateAnniversary(
                    anniversary = anniversary,
                    name = name,
                    date = date,
                    imageUri = imageUri,
                    type = type,
                    isYearly = isYearly,
                    note = note,
                    importance = importance,
                    frameId = frameId
                )
                editingAnniversary = null
            }
        )
    }
    
    if (showStatistics) {
        AnniversaryStatisticsDialog(
            monthlyStats = monthlyStats,
            yearlyStats = yearlyStats,
            topAnniversaries = topAnniversaries,
            upcomingAnniversaries = upcomingAnniversaries,
            onDismiss = { showStatistics = false }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAnniversaryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, String, Boolean, String?, Int, String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = (context.applicationContext as com.example.funlife.FunLifeApplication).database
    val inventoryDao = remember { database.inventoryDao() }
    
    // 获取用户拥有的相框
    val ownedFrames by inventoryDao.getItemsByType(1L, com.example.funlife.data.model.InventoryItemType.ANNIVERSARY_FRAME)
        .collectAsState(initial = emptyList())
    
    var name by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(AnniversaryType.CUSTOM) }
    var isYearly by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var importance by remember { mutableStateOf(3) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var selectedFrameId by remember { mutableStateOf("jinian_card_1") } // 默认相框
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            CuteDialogHeader(
                title = stringResource(R.string.anniversary_dialog_add_title),
                subtitle = "记录每一个值得珍藏的瞬间 ﾟ✧",
                leadingEmoji = "🎀"
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.anniversary_name), color = Color(0xFFAD1457)) },
                        placeholder = { Text(stringResource(R.string.anniversary_name_hint), color = Color(0xFFAD1457).copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEC407A),
                            unfocusedBorderColor = Color(0xFFFFB6C1),
                            focusedTextColor = Color(0xFF424242),
                            unfocusedTextColor = Color(0xFF424242),
                            cursorColor = Color(0xFFEC407A)
                        )
                    )
                }
                
                // 类型 + 日期 合并到一行
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                stringResource(R.string.anniversary_type_label),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFAD1457)
                            )
                            Surface(
                                onClick = { showTypeMenu = true },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFF0F5),
                                border = BorderStroke(1.5.dp, Color(0xFFFFB6C1))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(selectedType.emoji, fontSize = 18.sp)
                                        Text(
                                            selectedType.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF424242),
                                            maxLines = 1
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        stringResource(R.string.anniversary_select_type),
                                        tint = Color(0xFFEC407A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showTypeMenu,
                                onDismissRequest = { showTypeMenu = false }
                            ) {
                                AnniversaryType.values().forEach { type ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(type.emoji, style = MaterialTheme.typography.titleLarge)
                                                Text(type.displayName)
                                            }
                                        },
                                        onClick = {
                                            selectedType = type
                                            showTypeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                stringResource(R.string.anniversary_date_label),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFAD1457)
                            )
                            Surface(
                                onClick = { showDatePicker = true },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFF0F5),
                                border = BorderStroke(1.5.dp, Color(0xFFFFB6C1))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = stringResource(R.string.anniversary_calendar),
                                        tint = Color(0xFFEC407A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = selectedDate.format(DateTimeFormatter.ofPattern("MM-dd")),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF424242)
                                    )
                                    Text(
                                        text = "${selectedDate.year}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFAD1457).copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 重要程度 + 每年重复 合并到一行
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 重要程度（左侧）
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFFF0F5))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                stringResource(R.string.anniversary_importance),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFAD1457)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                (1..5).forEach { star ->
                                    IconButton(
                                        onClick = { importance = star },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            if (star <= importance) Icons.Default.Star else Icons.Outlined.StarBorder,
                                            contentDescription = stringResource(R.string.anniversary_stars, star),
                                            tint = if (star <= importance) Color(0xFFFFC107) else Color(0xFFE0E0E0),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        // 每年重复（右侧）
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFFF0F5))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.anniversary_repeat_yearly),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFAD1457)
                                )
                                Text(
                                    stringResource(R.string.anniversary_repeat_yearly_hint),
                                    fontSize = 9.sp,
                                    color = Color(0xFFAD1457).copy(alpha = 0.6f),
                                    maxLines = 1
                                )
                            }
                            Switch(
                                checked = isYearly,
                                onCheckedChange = { isYearly = it },
                                modifier = Modifier.scale(0.75f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFEC407A),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFE0E0E0)
                                )
                            )
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(stringResource(R.string.anniversary_note), color = Color(0xFFAD1457)) },
                        placeholder = { Text(stringResource(R.string.anniversary_note_hint), color = Color(0xFFAD1457).copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        minLines = 2,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEC407A),
                            unfocusedBorderColor = Color(0xFFFFB6C1),
                            focusedTextColor = Color(0xFF424242),
                            unfocusedTextColor = Color(0xFF424242),
                            cursorColor = Color(0xFFEC407A)
                        )
                    )
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.anniversary_image),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFAD1457)
                        )
                        
                        if (selectedImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = stringResource(R.string.anniversary_selected_image),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                
                                IconButton(
                                    onClick = { selectedImageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.5f)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.anniversary_remove_image),
                                            tint = Color.White,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFF0F5),
                                border = BorderStroke(1.5.dp, Color(0xFFFFB6C1))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = stringResource(R.string.anniversary_add_image),
                                        modifier = Modifier.size(32.dp),
                                        tint = Color(0xFFEC407A)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        stringResource(R.string.anniversary_click_select_image),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFAD1457),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 🔥 相框选择器
                item {
                    com.example.funlife.ui.components.FrameSelector(
                        selectedFrameId = selectedFrameId,
                        ownedFrames = ownedFrames,
                        onFrameSelected = { frameId -> selectedFrameId = frameId },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(50), ambientColor = Color(0xFFEC407A), spotColor = Color(0xFFEC407A))
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF8FA3), Color(0xFFEC407A))
                        )
                    )
                    .clickable {
                        if (name.isNotBlank()) {
                            val dateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            onConfirm(
                                name,
                                dateStr,
                                selectedImageUri?.toString(),
                                selectedType.name,
                                isYearly,
                                note.ifBlank { null },
                                importance,
                                selectedFrameId
                            )
                        }
                    }
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Text(
                    stringResource(R.string.anniversary_confirm),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF5F5F5))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    stringResource(R.string.anniversary_cancel),
                    color = Color(0xFF757575),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color(0xFFFFF5F8)
    )
    
    if (showDatePicker) {
        CuteDatePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { newDate ->
                selectedDate = newDate
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAnniversaryDialog(
    anniversary: Anniversary,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, String, Boolean, String?, Int, String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = (context.applicationContext as com.example.funlife.FunLifeApplication).database
    val inventoryDao = remember { database.inventoryDao() }
    
    // 获取用户拥有的相框
    val ownedFrames by inventoryDao.getItemsByType(1L, com.example.funlife.data.model.InventoryItemType.ANNIVERSARY_FRAME)
        .collectAsState(initial = emptyList())
    
    var name by remember { mutableStateOf(anniversary.name) }
    var selectedDate by remember { 
        mutableStateOf(
            LocalDate.parse(anniversary.date, DateTimeFormatter.ISO_LOCAL_DATE)
        ) 
    }
    var selectedImageUri by remember { mutableStateOf(anniversary.imageUri?.let { Uri.parse(it) }) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(anniversary.getTypeEnum()) }
    var isYearly by remember { mutableStateOf(anniversary.isYearly) }
    var note by remember { mutableStateOf(anniversary.note ?: "") }
    var importance by remember { mutableStateOf(anniversary.importance) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var selectedFrameId by remember { mutableStateOf(anniversary.frameId) } // 使用当前相框
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            CuteDialogHeader(
                title = stringResource(R.string.anniversary_dialog_edit_title),
                subtitle = "完善这份珍贵的回忆 ﾟ✧",
                leadingEmoji = "✏️"
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.anniversary_name), color = Color(0xFFAD1457)) },
                        placeholder = { Text(stringResource(R.string.anniversary_name_hint), color = Color(0xFFAD1457).copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEC407A),
                            unfocusedBorderColor = Color(0xFFFFB6C1),
                            focusedTextColor = Color(0xFF424242),
                            unfocusedTextColor = Color(0xFF424242),
                            cursorColor = Color(0xFFEC407A)
                        )
                    )
                }
                
                // 类型 + 日期 合并到一行
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                stringResource(R.string.anniversary_type_label),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFAD1457)
                            )
                            Surface(
                                onClick = { showTypeMenu = true },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFF0F5),
                                border = BorderStroke(1.5.dp, Color(0xFFFFB6C1))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(selectedType.emoji, fontSize = 18.sp)
                                        Text(
                                            selectedType.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF424242),
                                            maxLines = 1
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        stringResource(R.string.anniversary_select_type),
                                        tint = Color(0xFFEC407A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showTypeMenu,
                                onDismissRequest = { showTypeMenu = false }
                            ) {
                                AnniversaryType.values().forEach { type ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(type.emoji, style = MaterialTheme.typography.titleLarge)
                                                Text(type.displayName)
                                            }
                                        },
                                        onClick = {
                                            selectedType = type
                                            showTypeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                stringResource(R.string.anniversary_date_label),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFAD1457)
                            )
                            Surface(
                                onClick = { showDatePicker = true },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFF0F5),
                                border = BorderStroke(1.5.dp, Color(0xFFFFB6C1))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = stringResource(R.string.anniversary_calendar),
                                        tint = Color(0xFFEC407A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = selectedDate.format(DateTimeFormatter.ofPattern("MM-dd")),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF424242)
                                    )
                                    Text(
                                        text = "${selectedDate.year}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFAD1457).copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 重要程度 + 每年重复 合并到一行
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 重要程度（左侧）
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFFF0F5))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                stringResource(R.string.anniversary_importance),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFAD1457)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                (1..5).forEach { star ->
                                    IconButton(
                                        onClick = { importance = star },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            if (star <= importance) Icons.Default.Star else Icons.Outlined.StarBorder,
                                            contentDescription = stringResource(R.string.anniversary_stars, star),
                                            tint = if (star <= importance) Color(0xFFFFC107) else Color(0xFFE0E0E0),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        // 每年重复（右侧）
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFFF0F5))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.anniversary_repeat_yearly),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFAD1457)
                                )
                                Text(
                                    stringResource(R.string.anniversary_repeat_yearly_hint),
                                    fontSize = 9.sp,
                                    color = Color(0xFFAD1457).copy(alpha = 0.6f),
                                    maxLines = 1
                                )
                            }
                            Switch(
                                checked = isYearly,
                                onCheckedChange = { isYearly = it },
                                modifier = Modifier.scale(0.75f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFEC407A),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFE0E0E0)
                                )
                            )
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(stringResource(R.string.anniversary_note), color = Color(0xFFAD1457)) },
                        placeholder = { Text(stringResource(R.string.anniversary_note_hint), color = Color(0xFFAD1457).copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        minLines = 2,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEC407A),
                            unfocusedBorderColor = Color(0xFFFFB6C1),
                            focusedTextColor = Color(0xFF424242),
                            unfocusedTextColor = Color(0xFF424242),
                            cursorColor = Color(0xFFEC407A)
                        )
                    )
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.anniversary_image),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFAD1457)
                        )
                        
                        if (selectedImageUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = stringResource(R.string.anniversary_selected_image),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                
                                IconButton(
                                    onClick = { selectedImageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.5f)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.anniversary_remove_image),
                                            tint = Color.White,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFF0F5),
                                border = BorderStroke(1.5.dp, Color(0xFFFFB6C1))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = stringResource(R.string.anniversary_add_image),
                                        modifier = Modifier.size(32.dp),
                                        tint = Color(0xFFEC407A)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        stringResource(R.string.anniversary_click_select_image),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFAD1457),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 相框选择器
                item {
                    com.example.funlife.ui.components.FrameSelector(
                        selectedFrameId = selectedFrameId,
                        ownedFrames = ownedFrames,
                        onFrameSelected = { frameId -> selectedFrameId = frameId },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(50), ambientColor = Color(0xFFEC407A), spotColor = Color(0xFFEC407A))
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFF8FA3), Color(0xFFEC407A))
                        )
                    )
                    .clickable {
                        if (name.isNotBlank()) {
                            val dateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            onConfirm(
                                name,
                                dateStr,
                                selectedImageUri?.toString(),
                                selectedType.name,
                                isYearly,
                                note.ifBlank { null },
                                importance,
                                selectedFrameId
                            )
                        }
                    }
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Text(
                    stringResource(R.string.anniversary_save),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF5F5F5))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    stringResource(R.string.anniversary_cancel),
                    color = Color(0xFF757575),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color(0xFFFFF5F8)
    )
    
    if (showDatePicker) {
        CuteDatePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { newDate ->
                selectedDate = newDate
                showDatePicker = false
            }
        )
    }
}

// ════════════════════════════════════════════════════════════
// 可爱的纪念日空状态 - Canvas 绘制日历精灵 + 装饰
// ════════════════════════════════════════════════════════════
@Composable
fun CuteAnniversaryEmptyState(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "emptyAnim")
    val bob by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    val tilt by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )
    val twinkle by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )
    val haloRot by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing)
        ),
        label = "halo"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    rotate(haloRot, Offset(cx, cy)) {
                        for (i in 0 until 12) {
                            val ang = (i * 30f) * (PI / 180f).toFloat()
                            val r = size.width * 0.45f
                            val x = cx + cos(ang) * r
                            val y = cy + sin(ang) * r
                            drawCircle(
                                color = if (i % 2 == 0) Color(0xFFFFB6C1).copy(alpha = 0.55f)
                                else Color(0xFFFFE0B2).copy(alpha = 0.45f),
                                radius = if (i % 2 == 0) 5f else 3f,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    drawSparkle4(Offset(cx - size.width * 0.42f, cy - size.height * 0.30f),
                        size.width * 0.06f * twinkle, Color(0xFFFFC107))
                    drawSparkle4(Offset(cx + size.width * 0.40f, cy - size.height * 0.35f),
                        size.width * 0.05f * (1.4f - twinkle), Color(0xFFFF80AB))
                    drawSparkle4(Offset(cx - size.width * 0.36f, cy + size.height * 0.30f),
                        size.width * 0.045f * twinkle, Color(0xFFAB47BC))
                    drawCuteHeart(Offset(cx + size.width * 0.40f, cy + size.height * 0.20f),
                        size.width * 0.06f, Color(0xFFEC407A).copy(alpha = 0.7f + twinkle * 0.3f))
                }
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .offset(y = bob.dp)
                        .graphicsLayer { rotationZ = tilt }
                ) {
                    CuteCalendarMascot()
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color(0xFFFF80AB),
                        spotColor = Color(0xFFFF80AB)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.92f),
                                Color(0xFFFFF0F5).copy(alpha = 0.92f)
                            )
                        )
                    )
                    .padding(horizontal = 28.dp, vertical = 18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("\u273F", fontSize = 18.sp, color = Color(0xFFEC407A))
                    Text(
                        text = "\u8FD8\u6CA1\u6709\u7EAA\u5FF5\u65E5\u54E6",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD81B60),
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = Shadow(
                                color = Color(0xFFFFB6C1),
                                offset = Offset(0f, 2f),
                                blurRadius = 4f
                            )
                        ),
                        letterSpacing = 1.sp
                    )
                    Text("\u273F", fontSize = 18.sp, color = Color(0xFFEC407A))
                }
                Text(
                    text = "\u8BB0\u5F55\u6BCF\u4E00\u4E2A\u503C\u5F97\u73CD\u85CF\u7684\u77AC\u95F4 \uFF9F\u2727*\uFF61",
                    fontSize = 13.sp,
                    color = Color(0xFFAD1457).copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(28.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color(0xFFFFB6C1))
                    )
                    Text("\u2665", fontSize = 11.sp, color = Color(0xFFEC407A))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(28.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color(0xFFFFB6C1))
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "\u70B9\u51FB\u4E0B\u65B9\u6309\u94AE\u6DFB\u52A0\u5427",
                    fontSize = 13.sp,
                    color = Color(0xFF8B4513),
                    fontWeight = FontWeight.SemiBold,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(
                            color = Color.White,
                            offset = Offset(0f, 1f),
                            blurRadius = 3f
                        )
                    )
                )
                Text(
                    text = "\u2193",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFEC407A),
                    modifier = Modifier.offset(y = bob.dp / 2)
                )
            }
        }
    }
}

@Composable
private fun CuteCalendarMascot() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2

        // 地面阴影
        drawOval(
            color = Color.Black.copy(alpha = 0.12f),
            topLeft = Offset(cx - w * 0.32f, cy + h * 0.44f),
            size = Size(w * 0.64f, h * 0.08f)
        )

        val bodyW = w * 0.70f
        val bodyH = h * 0.75f
        val bodyTop = cy - bodyH / 2 + h * 0.06f
        val bodyLeft = cx - bodyW / 2
        val cornerR = bodyW * 0.20f  // 更圆润的角

        // 身体阴影层（柔和粉色光晕在身体后）
        drawRoundRect(
            color = Color(0xFFFFB6C1).copy(alpha = 0.35f),
            topLeft = Offset(bodyLeft - 6f, bodyTop + 6f),
            size = Size(bodyW + 12f, bodyH + 12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR + 6f)
        )

        // 小脚（左右两只黑色小椭圆）
        val footW = bodyW * 0.16f
        val footH = bodyH * 0.08f
        drawOval(
            color = Color(0xFF3E2723),
            topLeft = Offset(cx - bodyW * 0.32f, bodyTop + bodyH * 0.96f),
            size = Size(footW, footH)
        )
        drawOval(
            color = Color(0xFF3E2723),
            topLeft = Offset(cx + bodyW * 0.16f, bodyTop + bodyH * 0.96f),
            size = Size(footW, footH)
        )

        // 小手（黑色圆球 + 粉色掌心）- 左右各一只
        val pawR = bodyW * 0.10f
        // 左手
        drawCircle(Color(0xFF3E2723), pawR, Offset(bodyLeft - pawR * 0.3f, bodyTop + bodyH * 0.55f))
        drawCircle(
            Color(0xFFFFB1C8),
            pawR * 0.45f,
            Offset(bodyLeft - pawR * 0.4f, bodyTop + bodyH * 0.52f)
        )
        // 右手
        drawCircle(Color(0xFF3E2723), pawR, Offset(bodyLeft + bodyW + pawR * 0.3f, bodyTop + bodyH * 0.55f))
        drawCircle(
            Color(0xFFFFB1C8),
            pawR * 0.45f,
            Offset(bodyLeft + bodyW + pawR * 0.2f, bodyTop + bodyH * 0.52f)
        )

        // 主体：白色圆角矩形（更圆润）
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bodyH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR)
        )
        drawRoundRect(
            color = Color(0xFFFFB6C1),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bodyH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f),
            style = Stroke(width = 4f)
        )

        val bandH = bodyH * 0.30f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFEC407A), Color(0xFFE91E63))
            ),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bandH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f)
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFEC407A), Color(0xFFE91E63))
            ),
            topLeft = Offset(bodyLeft, bodyTop + bandH * 0.55f),
            size = Size(bodyW, bandH * 0.45f)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.20f),
            topLeft = Offset(bodyLeft, bodyTop + 2f),
            size = Size(bodyW, bandH * 0.25f)
        )

        drawCircle(Color(0xFF8B4513), bodyW * 0.04f,
            Offset(bodyLeft + bodyW * 0.22f, bodyTop - bodyW * 0.04f))
        drawCircle(Color(0xFF8B4513), bodyW * 0.04f,
            Offset(bodyLeft + bodyW * 0.78f, bodyTop - bodyW * 0.04f))
        drawCircle(Color(0xFFA0522D), bodyW * 0.025f,
            Offset(bodyLeft + bodyW * 0.22f, bodyTop - bodyW * 0.04f))
        drawCircle(Color(0xFFA0522D), bodyW * 0.025f,
            Offset(bodyLeft + bodyW * 0.78f, bodyTop - bodyW * 0.04f))

        val faceY = bodyTop + bandH + bodyH * 0.18f
        drawArc(
            color = Color(0xFF3E2723),
            startAngle = 200f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(cx - bodyW * 0.28f, faceY - bodyH * 0.05f),
            size = Size(bodyW * 0.18f, bodyH * 0.10f),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF3E2723),
            startAngle = 200f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(cx + bodyW * 0.10f, faceY - bodyH * 0.05f),
            size = Size(bodyW * 0.18f, bodyH * 0.10f),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )
        drawCircle(
            Color(0xFFFF80AB).copy(alpha = 0.55f),
            bodyW * 0.06f,
            Offset(cx - bodyW * 0.25f, faceY + bodyH * 0.05f)
        )
        drawCircle(
            Color(0xFFFF80AB).copy(alpha = 0.55f),
            bodyW * 0.06f,
            Offset(cx + bodyW * 0.25f, faceY + bodyH * 0.05f)
        )
        drawArc(
            color = Color(0xFF3E2723),
            startAngle = 20f, sweepAngle = 140f, useCenter = false,
            topLeft = Offset(cx - bodyW * 0.10f, faceY + bodyH * 0.04f),
            size = Size(bodyW * 0.20f, bodyH * 0.10f),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )
        drawCuteHeart(
            Offset(cx, faceY + bodyH * 0.22f),
            bodyW * 0.06f,
            Color(0xFFEC407A).copy(alpha = 0.85f)
        )
    }
}

// ════════════════════════════════════════════════════════════
// 💖 可爱的"添加纪念日"按钮 - 渐变胶囊 + 心跳呼吸 + 扫光
// ════════════════════════════════════════════════════════════
@Composable
fun CuteAddAnniversaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "fabAnim")
    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val sweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing)
        ),
        label = "sweep"
    )
    val heartBeat by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartB"
    )
    val haloAlpha by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo"
    )
    val sideHeartFloat by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sideH"
    )
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pressS"
    )
    LaunchedEffect(pressed) {
        if (pressed) { delay(120); pressed = false }
    }

    // 外层容器：包含光环 + 浮动爱心 + 按钮主体
    Box(
        modifier = modifier.padding(16.dp),  // 为光环和飘心留空间
        contentAlignment = Alignment.Center
    ) {
        // 左侧浮动小爱心装饰
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-8).dp, y = sideHeartFloat.dp)
                .size(18.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCuteHeart(
                    Offset(size.width / 2, size.height / 2),
                    size.width * 0.40f,
                    Color(0xFFFF80AB).copy(alpha = 0.85f)
                )
            }
        }
        // 右侧浮动小爱心装饰
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 8.dp, y = (-sideHeartFloat).dp)
                .size(14.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCuteHeart(
                    Offset(size.width / 2, size.height / 2),
                    size.width * 0.40f,
                    Color(0xFFFF4081).copy(alpha = 0.75f)
                )
            }
        }
        // 顶部小星星
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 6.dp, y = (-2).dp)
                .size(12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawSparkle4(
                    Offset(size.width / 2, size.height / 2),
                    size.width * 0.45f,
                    Color(0xFFFFEB3B).copy(alpha = haloAlpha + 0.2f)
                )
            }
        }
        // 顶部右侧星星
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-12).dp, y = 0.dp)
                .size(10.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawSparkle4(
                    Offset(size.width / 2, size.height / 2),
                    size.width * 0.45f,
                    Color.White.copy(alpha = haloAlpha)
                )
            }
        }

        // 按钮主体
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pulse * pressScale
                    scaleY = pulse * pressScale
                }
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = Color(0xFFFF4081),
                    spotColor = Color(0xFFFF1744)
                )
                .clip(RoundedCornerShape(50))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFA8B6),
                            Color(0xFFFF5C8A),
                            Color(0xFFE91E63)
                        )
                    )
                )
                .clickable(
                    onClick = {
                        pressed = true
                        onClick()
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height * 0.55f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
            )
            val sweepX = -size.width * 0.4f + sweep * size.width * 1.8f
            val gradient = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.40f),
                    Color.Transparent
                ),
                start = Offset(sweepX - size.width * 0.15f, 0f),
                end = Offset(sweepX + size.width * 0.15f, size.height)
            )
            drawRoundRect(
                brush = gradient,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    drawCuteHeart(
                        Offset(cx, cy),
                        size.width * 0.50f * heartBeat,
                        Color.White
                    )
                    drawCuteHeart(
                        Offset(cx, cy + 1f),
                        size.width * 0.40f * heartBeat,
                        Color(0xFFFFB6C1).copy(alpha = 0.45f)
                    )
                }
                Text(
                    text = "+",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE91E63),
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(
                            color = Color.White,
                            offset = Offset(0f, 1f),
                            blurRadius = 2f
                        )
                    )
                )
            }
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = Shadow(
                        color = Color(0xFFAD1457),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                )
            )
        }
        } // 按钮主体 Box 结束
    } // 外层装饰 Box 结束
}

private fun DrawScope.drawSparkle4(center: Offset, s: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - s)
        lineTo(center.x + s * 0.25f, center.y - s * 0.25f)
        lineTo(center.x + s, center.y)
        lineTo(center.x + s * 0.25f, center.y + s * 0.25f)
        lineTo(center.x, center.y + s)
        lineTo(center.x - s * 0.25f, center.y + s * 0.25f)
        lineTo(center.x - s, center.y)
        lineTo(center.x - s * 0.25f, center.y - s * 0.25f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawCuteHeart(center: Offset, s: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + s * 0.7f)
        cubicTo(
            center.x - s * 1.4f, center.y - s * 0.2f,
            center.x - s * 0.6f, center.y - s * 1.2f,
            center.x, center.y - s * 0.3f
        )
        cubicTo(
            center.x + s * 0.6f, center.y - s * 1.2f,
            center.x + s * 1.4f, center.y - s * 0.2f,
            center.x, center.y + s * 0.7f
        )
        close()
    }
    drawPath(path, color)
}


// ════════════════════════════════════════════════════════════
// 🎀 可爱风日期选择器对话框（替代 Material3 DatePicker）
// ════════════════════════════════════════════════════════════
@Composable
fun CuteDatePickerDialog(
    initialDate: java.time.LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (java.time.LocalDate) -> Unit
) {
    var currentMonth by remember { mutableStateOf(java.time.YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    val today = remember { java.time.LocalDate.now() }
    // 视图模式: 0=日, 1=月, 2=年
    var pickerMode by remember { mutableStateOf(0) }
    // 年份面板的起始年
    var yearPageStart by remember { mutableStateOf(currentMonth.year - 6) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // 头部：粉渐变背景 + 大字日期
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFFB6C1), Color(0xFFFF80AB), Color(0xFFEC407A))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        // 装饰小爱心
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                        ) {
                            drawCuteHeart(Offset(size.width * 0.88f, size.height * 0.22f), 10f, Color.White.copy(alpha = 0.5f))
                            drawCuteHeart(Offset(size.width * 0.93f, size.height * 0.65f), 7f, Color.White.copy(alpha = 0.4f))
                            drawSparkle4(Offset(size.width * 0.10f, size.height * 0.30f), 8f, Color.White.copy(alpha = 0.6f))
                            drawSparkle4(Offset(size.width * 0.05f, size.height * 0.75f), 5f, Color.White.copy(alpha = 0.5f))
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("✿", fontSize = 14.sp, color = Color.White)
                                Text(
                                    text = "选择日期",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "${selectedDate.year} 年 ${selectedDate.monthValue} 月 ${selectedDate.dayOfMonth} 日",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp,
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = Shadow(
                                        color = Color(0xFFAD1457).copy(alpha = 0.4f),
                                        offset = Offset(0f, 2f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                            Text(
                                text = run {
                                    val w = when (selectedDate.dayOfWeek) {
                                        java.time.DayOfWeek.MONDAY -> "星期一"
                                        java.time.DayOfWeek.TUESDAY -> "星期二"
                                        java.time.DayOfWeek.WEDNESDAY -> "星期三"
                                        java.time.DayOfWeek.THURSDAY -> "星期四"
                                        java.time.DayOfWeek.FRIDAY -> "星期五"
                                        java.time.DayOfWeek.SATURDAY -> "星期六"
                                        else -> "星期日"
                                    }
                                    w
                                },
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 月份导航
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 上一年 «
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFFD6E2), Color(0xFFFFB6C1))
                                    )
                                )
                                .clickable { currentMonth = currentMonth.minusYears(1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("«", fontSize = 16.sp, color = Color(0xFFAD1457), fontWeight = FontWeight.Black)
                        }
                        // 上月 ‹
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFE0EC))
                                .clickable { currentMonth = currentMonth.minusMonths(1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("‹", fontSize = 18.sp, color = Color(0xFFEC407A), fontWeight = FontWeight.Black)
                        }
                        // 中间标题胶囊 - 年和月分别可点击切换视图
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFFFE0EC), Color(0xFFFFD0E0))
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // 年份点击 → 进入年视图
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (pickerMode == 2) Color(0xFFEC407A) else Color.Transparent
                                        )
                                        .clickable {
                                            pickerMode = if (pickerMode == 2) 0 else 2
                                            yearPageStart = currentMonth.year - 6
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${currentMonth.year} 年",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pickerMode == 2) Color.White else Color(0xFFAD1457).copy(alpha = 0.85f)
                                    )
                                }
                                Text(
                                    text = "·",
                                    fontSize = 13.sp,
                                    color = Color(0xFFAD1457).copy(alpha = 0.5f)
                                )
                                // 月份点击 → 进入月视图
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (pickerMode == 1) Color(0xFFEC407A) else Color.Transparent
                                        )
                                        .clickable {
                                            pickerMode = if (pickerMode == 1) 0 else 1
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${currentMonth.monthValue} 月",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (pickerMode == 1) Color.White else Color(0xFFD81B60)
                                    )
                                }
                            }
                        }
                        // 下月 ›
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFE0EC))
                                .clickable { currentMonth = currentMonth.plusMonths(1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("›", fontSize = 18.sp, color = Color(0xFFEC407A), fontWeight = FontWeight.Black)
                        }
                        // 下一年 »
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFFD6E2), Color(0xFFFFB6C1))
                                    )
                                )
                                .clickable { currentMonth = currentMonth.plusYears(1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("»", fontSize = 16.sp, color = Color(0xFFAD1457), fontWeight = FontWeight.Black)
                        }
                    }

                    when (pickerMode) {
                    // ═══════ 月份面板 (3 行 x 4 列) ═══════
                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 280.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (r in 0 until 3) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    for (c in 0 until 4) {
                                        val m = r * 4 + c + 1
                                        val isCurMonth = m == currentMonth.monthValue
                                        val isSelInYear = m == selectedDate.monthValue && currentMonth.year == selectedDate.year
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(56.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .then(
                                                    if (isCurMonth || isSelInYear) {
                                                        Modifier
                                                            .shadow(6.dp, RoundedCornerShape(16.dp),
                                                                ambientColor = Color(0xFFEC407A),
                                                                spotColor = Color(0xFFEC407A))
                                                            .background(
                                                                brush = Brush.verticalGradient(
                                                                    colors = listOf(Color(0xFFFF8FA3), Color(0xFFEC407A))
                                                                )
                                                            )
                                                    } else {
                                                        Modifier.background(Color(0xFFFFF0F5))
                                                    }
                                                )
                                                .clickable {
                                                    currentMonth = currentMonth.withMonth(m)
                                                    pickerMode = 0
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${m} 月",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isCurMonth || isSelInYear) Color.White else Color(0xFFD81B60),
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // ═══════ 年份面板 (3 行 x 4 列, 12 年/页, 可翻页) ═══════
                    2 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 280.dp)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 年份翻页栏
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFFFFE0EC))
                                        .clickable { yearPageStart -= 12 }
                                        .padding(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Text("‹‹ 上 12 年", fontSize = 12.sp, color = Color(0xFFEC407A), fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "$yearPageStart - ${yearPageStart + 11}",
                                    fontSize = 13.sp,
                                    color = Color(0xFFAD1457),
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFFFFE0EC))
                                        .clickable { yearPageStart += 12 }
                                        .padding(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Text("下 12 年 ››", fontSize = 12.sp, color = Color(0xFFEC407A), fontWeight = FontWeight.Bold)
                                }
                            }
                            // 年份网格
                            for (r in 0 until 3) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (c in 0 until 4) {
                                        val y = yearPageStart + r * 4 + c
                                        val isCurYear = y == currentMonth.year
                                        val isSelYear = y == selectedDate.year
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .then(
                                                    if (isCurYear || isSelYear) {
                                                        Modifier
                                                            .shadow(5.dp, RoundedCornerShape(14.dp),
                                                                ambientColor = Color(0xFFEC407A),
                                                                spotColor = Color(0xFFEC407A))
                                                            .background(
                                                                brush = Brush.verticalGradient(
                                                                    colors = listOf(Color(0xFFFF8FA3), Color(0xFFEC407A))
                                                                )
                                                            )
                                                    } else {
                                                        Modifier.background(Color(0xFFFFF0F5))
                                                    }
                                                )
                                                .clickable {
                                                    currentMonth = currentMonth.withYear(y)
                                                    pickerMode = 1
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$y",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isCurYear || isSelYear) Color.White else Color(0xFFD81B60)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // ═══════ 默认：日期面板 ═══════
                    else -> {
                    // 星期表头
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("日", "一", "二", "三", "四", "五", "六").forEachIndexed { idx, w ->
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = w,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (idx == 0 || idx == 6) Color(0xFFEC407A) else Color(0xFFAD1457).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))

                    // 日期网格
                    val firstDayOfMonth = currentMonth.atDay(1)
                    val firstDowValue = firstDayOfMonth.dayOfWeek.value % 7  // Mon=1..Sun=0
                    val daysInMonth = currentMonth.lengthOfMonth()
                    val totalCells = ((firstDowValue + daysInMonth + 6) / 7) * 7
                    val rows = totalCells / 7

                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (col in 0..6) {
                                    val cellIdx = row * 7 + col
                                    val dayNum = cellIdx - firstDowValue + 1
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (dayNum in 1..daysInMonth) {
                                            val date = currentMonth.atDay(dayNum)
                                            val isSelected = date == selectedDate
                                            val isToday = date == today
                                            val isWeekend = col == 0 || col == 6

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize(0.85f)
                                                    .clip(CircleShape)
                                                    .then(
                                                        if (isSelected) {
                                                            Modifier
                                                                .shadow(6.dp, CircleShape, ambientColor = Color(0xFFEC407A), spotColor = Color(0xFFEC407A))
                                                                .background(
                                                                    brush = Brush.verticalGradient(
                                                                        colors = listOf(Color(0xFFFF8FA3), Color(0xFFEC407A))
                                                                    )
                                                                )
                                                        } else if (isToday) {
                                                            Modifier.background(Color(0xFFFFE0EC))
                                                        } else Modifier
                                                    )
                                                    .clickable { selectedDate = date },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Canvas(modifier = Modifier.matchParentSize()) {
                                                        drawCuteHeart(
                                                            Offset(size.width * 0.85f, size.height * 0.18f),
                                                            size.width * 0.10f,
                                                            Color.White.copy(alpha = 0.85f)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "$dayNum",
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.SemiBold,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isToday -> Color(0xFFEC407A)
                                                        isWeekend -> Color(0xFFEC407A).copy(alpha = 0.85f)
                                                        else -> Color(0xFF424242)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    } // else 结束
                    } // when 结束

                    // 底部按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 取消
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFF5F5F5))
                                .clickable(onClick = onDismiss)
                                .padding(horizontal = 22.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "取消",
                                fontSize = 14.sp,
                                color = Color(0xFF757575),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        // 确定
                        Box(
                            modifier = Modifier
                                .shadow(8.dp, RoundedCornerShape(50), ambientColor = Color(0xFFEC407A), spotColor = Color(0xFFEC407A))
                                .clip(RoundedCornerShape(50))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFFF8FA3), Color(0xFFEC407A))
                                    )
                                )
                                .clickable { onConfirm(selectedDate) }
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("✓", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Black)
                                Text(
                                    text = "确定",
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ════════════════════════════════════════════════════════════
// 🎀 通用可爱对话框标题 banner - 渐变 + Canvas 装饰 + 双行文字
// ════════════════════════════════════════════════════════════
@Composable
fun CuteDialogHeader(
    title: String,
    subtitle: String,
    leadingEmoji: String = "✨"
) {
    val infinite = rememberInfiniteTransition(label = "headerAnim")
    val twinkle by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )
    val emojiBob by infinite.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp
                ),
                ambientColor = Color(0xFFEC407A),
                spotColor = Color(0xFFEC407A)
            )
            .clip(
                RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp
                )
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFCAD4),
                        Color(0xFFFF80AB),
                        Color(0xFFEC407A),
                        Color(0xFFD81B60)
                    )
                )
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        // Canvas 装饰层
        Canvas(modifier = Modifier.matchParentSize()) {
            // 右侧大爱心
            drawCuteHeart(
                Offset(size.width * 0.85f, size.height * 0.30f),
                14f * (0.8f + twinkle * 0.3f),
                Color.White.copy(alpha = 0.45f)
            )
            drawCuteHeart(
                Offset(size.width * 0.93f, size.height * 0.72f),
                9f,
                Color.White.copy(alpha = 0.35f)
            )
            // 左侧装饰
            drawSparkle4(
                Offset(size.width * 0.06f, size.height * 0.25f),
                12f * twinkle,
                Color.White.copy(alpha = 0.7f)
            )
            drawSparkle4(
                Offset(size.width * 0.03f, size.height * 0.75f),
                7f * (1.4f - twinkle),
                Color.White.copy(alpha = 0.55f)
            )
            // 顶部小光点
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = 3f,
                center = Offset(size.width * 0.45f, size.height * 0.18f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = 2f,
                center = Offset(size.width * 0.75f, size.height * 0.18f)
            )
            // 底部柔光圆弧
            drawArc(
                color = Color.White.copy(alpha = 0.18f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(-size.width * 0.2f, size.height * 0.85f),
                size = Size(size.width * 1.4f, size.height * 0.5f),
                style = Stroke(width = 6f)
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(end = 56.dp) // 让右侧装饰露出
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = leadingEmoji,
                    fontSize = 22.sp,
                    modifier = Modifier.graphicsLayer { translationY = emojiBob }
                )
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.5.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(
                            color = Color(0xFFAD1457),
                            offset = Offset(0f, 3f),
                            blurRadius = 8f
                        )
                    )
                )
            }
            // 副标题胶囊
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.28f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}


// ════════════════════════════════════════════════════════════
// 🔔 今日纪念日提醒对话框 - 震动 + 铃声 + 庆祝动画
// ════════════════════════════════════════════════════════════
@Composable
fun AnniversaryReminderDialog(
    reminders: List<Anniversary>,
    onDismiss: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "reminderAnim")
    val bellSwing by infinite.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(180, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bell"
    )
    val haloRot by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "halo"
    )
    val scale by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 顶部渐变 banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFCAD4),
                                        Color(0xFFFF80AB),
                                        Color(0xFFEC407A),
                                        Color(0xFFD81B60)
                                    )
                                )
                            )
                            .padding(top = 22.dp, bottom = 18.dp)
                    ) {
                        // 旋转光环装饰
                        Canvas(modifier = Modifier.matchParentSize()) {
                            rotate(haloRot, Offset(size.width / 2, size.height * 0.45f)) {
                                for (i in 0 until 16) {
                                    val ang = (i * 22.5f) * (PI / 180f).toFloat()
                                    val r = size.width * 0.30f
                                    drawCircle(
                                        color = Color.White.copy(alpha = if (i % 2 == 0) 0.5f else 0.25f),
                                        radius = if (i % 2 == 0) 4f else 2.5f,
                                        center = Offset(
                                            size.width / 2 + cos(ang) * r,
                                            size.height * 0.45f + sin(ang) * r
                                        )
                                    )
                                }
                            }
                            drawCuteHeart(Offset(size.width * 0.12f, size.height * 0.18f), 9f, Color.White.copy(alpha = 0.6f))
                            drawCuteHeart(Offset(size.width * 0.88f, size.height * 0.22f), 11f, Color.White.copy(alpha = 0.55f))
                            drawCuteHeart(Offset(size.width * 0.08f, size.height * 0.80f), 7f, Color.White.copy(alpha = 0.5f))
                            drawCuteHeart(Offset(size.width * 0.92f, size.height * 0.78f), 9f, Color.White.copy(alpha = 0.6f))
                            drawSparkle4(Offset(size.width * 0.20f, size.height * 0.50f), 6f, Color.White.copy(alpha = 0.7f))
                            drawSparkle4(Offset(size.width * 0.80f, size.height * 0.50f), 7f, Color.White.copy(alpha = 0.65f))
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 摆动的小铃铛
                            Text(
                                text = "🔔",
                                fontSize = 48.sp,
                                modifier = Modifier.graphicsLayer { rotationZ = bellSwing }
                            )
                            Text(
                                text = "今日纪念日提醒",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 2.sp,
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = Shadow(
                                        color = Color(0xFFAD1457),
                                        offset = Offset(0f, 3f),
                                        blurRadius = 8f
                                    )
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.30f))
                                    .padding(horizontal = 12.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "✨ ${reminders.size} 个值得庆祝的日子 ✨",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    // 提醒列表
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        reminders.take(3).forEach { a ->
                            val typeEnum = try { a.getTypeEnum() } catch (e: Exception) { com.example.funlife.data.model.AnniversaryType.CUSTOM }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color(0xFFFFF0F5), Color(0xFFFFE0EC))
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color(0xFFFF8FA3), Color(0xFFEC407A))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(typeEnum.emoji, fontSize = 22.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = a.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFD81B60),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = a.getFullDescription(),
                                        fontSize = 11.sp,
                                        color = Color(0xFFAD1457).copy(alpha = 0.75f),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                                Text("💝", fontSize = 20.sp)
                            }
                        }
                        if (reminders.size > 3) {
                            Text(
                                text = "还有 ${reminders.size - 3} 个纪念日 …",
                                fontSize = 11.sp,
                                color = Color(0xFFAD1457).copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 知道啦按钮
                    Box(
                        modifier = Modifier
                            .padding(bottom = 20.dp)
                            .shadow(10.dp, RoundedCornerShape(50), ambientColor = Color(0xFFEC407A), spotColor = Color(0xFFEC407A))
                            .clip(RoundedCornerShape(50))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFF8FA3), Color(0xFFEC407A))
                                )
                            )
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 36.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("💗", fontSize = 16.sp)
                            Text(
                                text = "我知道啦",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 1.5.sp,
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = Shadow(
                                        color = Color(0xFFAD1457),
                                        offset = Offset(0f, 2f),
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

