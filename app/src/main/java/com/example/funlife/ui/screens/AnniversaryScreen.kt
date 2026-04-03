// AnniversaryScreen.kt
package com.example.funlife.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.funlife.R
import com.example.funlife.data.model.Anniversary
import com.example.funlife.data.model.AnniversaryType
import com.example.funlife.ui.components.AnniversaryCard
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
    
    // FAB 拖动状态
    var fabOffsetX by remember { mutableStateOf(0f) }
    var fabOffsetY by remember { mutableStateOf(0f) }
    
    val monthlyStats by viewModel.getMonthlyStatistics().collectAsState()
    val yearlyStats by viewModel.getYearlyStatistics().collectAsState()
    val topAnniversaries by viewModel.getTopImportantAnniversaries().collectAsState()
    val upcomingAnniversaries by viewModel.getUpcomingAnniversaries().collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {} // 移除默认的 FAB
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PageHeader(
                title = stringResource(R.string.anniversary_title),
                emoji = "🎂",
                gradientColors = PageHeaderGradients.Anniversary,
                subtitle = stringResource(R.string.anniversary_subtitle),
                showBackButton = true,
                onBackClick = onNavigateBack
            )
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showSearchBar) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.anniversary_search)) },
                            leadingIcon = { Icon(Icons.Default.Search, stringResource(R.string.anniversary_search_button)) },
                            trailingIcon = {
                                IconButton(onClick = { 
                                    viewModel.setSearchQuery("")
                                    showSearchBar = false
                                }) {
                                    Icon(Icons.Default.Close, stringResource(R.string.anniversary_close))
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!showSearchBar) {
                            FilterChip(
                                selected = false,
                                onClick = { showSearchBar = true },
                                label = { Text(stringResource(R.string.anniversary_search_button)) },
                                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) }
                            )
                        }
                        
                        FilterChip(
                            selected = selectedType != null,
                            onClick = { showFilterMenu = true },
                            label = { 
                                Text(
                                    if (selectedType != null) {
                                        AnniversaryType.valueOf(selectedType!!).displayName
                                    } else {
                                        stringResource(R.string.anniversary_type)
                                    }
                                ) 
                            },
                            leadingIcon = { Icon(Icons.Default.FilterList, null, Modifier.size(18.dp)) }
                        )
                        
                        FilterChip(
                            selected = sortOrder != SortOrder.DEFAULT,
                            onClick = { showSortMenu = true },
                            label = { Text(sortOrder.displayName) },
                            leadingIcon = { Icon(Icons.Default.Sort, null, Modifier.size(18.dp)) }
                        )
                        
                        if (selectedType != null || sortOrder != SortOrder.DEFAULT || searchQuery.isNotBlank()) {
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.clearFilters() },
                                label = { Text(stringResource(R.string.anniversary_clear)) },
                                leadingIcon = { Icon(Icons.Default.Close, null, Modifier.size(18.dp)) }
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        FilterChip(
                            selected = false,
                            onClick = { showStatistics = true },
                            label = { Text(stringResource(R.string.anniversary_stats)) },
                            leadingIcon = { Icon(Icons.Default.BarChart, null, Modifier.size(18.dp)) }
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.anniversary_all_types)) },
                            onClick = {
                                viewModel.setTypeFilter(null)
                                showFilterMenu = false
                            }
                        )
                        Divider()
                        AnniversaryType.values().forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(type.emoji)
                                        Text(type.displayName)
                                    }
                                },
                                onClick = {
                                    viewModel.setTypeFilter(type.name)
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOrder.values().forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.displayName) },
                                onClick = {
                                    viewModel.setSortOrder(order)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
            
            if (anniversaries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "📅",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = stringResource(R.string.anniversary_empty),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.anniversary_empty_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(anniversaries, key = { it.id }) { anniversary ->
                        AnniversaryCard(
                            anniversary = anniversary,
                            onDelete = { viewModel.deleteAnniversary(anniversary) },
                            onPin = {
                                if (anniversary.isPinned) {
                                    viewModel.unpinAnniversary(anniversary)
                                } else {
                                    viewModel.pinAnniversary(anniversary)
                                }
                            },
                            onEdit = {
                                editingAnniversary = anniversary
                            },
                            onShare = {
                                val shareHelper = com.example.funlife.utils.AnniversaryShareHelper(context)
                                shareHelper.shareImage(anniversary)
                            }
                        )
                    }
                }
            }
        }
        } // Scaffold 结束
        
        // 可拖动的 FAB
        val density = androidx.compose.ui.platform.LocalDensity.current
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
        
        ExtendedFloatingActionButton(
            onClick = { showDialog = true },
            icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.anniversary_add)) },
            text = { Text(stringResource(R.string.anniversary_add), style = MaterialTheme.typography.labelLarge) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
                .offset { 
                    androidx.compose.ui.unit.IntOffset(
                        fabOffsetX.toInt(), 
                        fabOffsetY.toInt()
                    ) 
                }
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        fabOffsetX = (fabOffsetX + dragAmount.x).coerceIn(
                            -(screenWidthPx - 200f), // 左边界
                            0f // 右边界
                        )
                        fabOffsetY = (fabOffsetY + dragAmount.y).coerceIn(
                            -(screenHeightPx - 400f), // 上边界，留出更多空间
                            0f // 下边界，0表示在导航栏上方
                        )
                    }
                }
        )
    } // Box 结束
    
    if (showDialog) {
        AddAnniversaryDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name, date, imageUri, type, isYearly, note, importance ->
                android.util.Log.d("AnniversaryScreen", "添加纪念日: name=$name, date=$date")
                viewModel.addAnniversary(
                    name = name,
                    date = date,
                    imageUri = imageUri,
                    type = type,
                    isYearly = isYearly,
                    note = note,
                    importance = importance,
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
            onConfirm = { name, date, imageUri, type, isYearly, note, importance ->
                viewModel.updateAnniversary(
                    anniversary = anniversary,
                    name = name,
                    date = date,
                    imageUri = imageUri,
                    type = type,
                    isYearly = isYearly,
                    note = note,
                    importance = importance
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
    onConfirm: (String, String, String?, String, Boolean, String?, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(AnniversaryType.CUSTOM) }
    var isYearly by remember { mutableStateOf(true) }
    var note by remember { mutableStateOf("") }
    var importance by remember { mutableStateOf(3) }
    var showTypeMenu by remember { mutableStateOf(false) }
    
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
            Text(
                stringResource(R.string.anniversary_dialog_add_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.anniversary_name)) },
                        placeholder = { Text(stringResource(R.string.anniversary_name_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.anniversary_type_label), 
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Surface(
                            onClick = { showTypeMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
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
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        selectedType.emoji,
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Text(
                                        selectedType.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, stringResource(R.string.anniversary_select_type))
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
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.anniversary_date_label), 
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Surface(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
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
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = stringResource(R.string.anniversary_calendar),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = selectedDate.format(
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.anniversary_repeat_yearly),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.anniversary_repeat_yearly_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isYearly,
                            onCheckedChange = { isYearly = it }
                        )
                    }
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.anniversary_importance),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { importance = star },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        if (star <= importance) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = stringResource(R.string.anniversary_stars, star),
                                        tint = if (star <= importance) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(stringResource(R.string.anniversary_note)) },
                        placeholder = { Text(stringResource(R.string.anniversary_note_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 3
                    )
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.anniversary_image), 
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
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
                                    .height(100.dp)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = stringResource(R.string.anniversary_add_image),
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        stringResource(R.string.anniversary_click_select_image),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val dateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        onConfirm(
                            name, 
                            dateStr, 
                            selectedImageUri?.toString(),
                            selectedType.name,
                            isYearly,
                            note.ifBlank { null },
                            importance
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.anniversary_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.anniversary_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = java.time.Instant
                                .ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.anniversary_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.anniversary_cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        stringResource(R.string.anniversary_select_date),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAnniversaryDialog(
    anniversary: Anniversary,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, String, Boolean, String?, Int) -> Unit
) {
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
            Text(
                stringResource(R.string.anniversary_dialog_edit_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.anniversary_name)) },
                        placeholder = { Text(stringResource(R.string.anniversary_name_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.anniversary_type_label), 
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Surface(
                            onClick = { showTypeMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
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
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        selectedType.emoji,
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Text(
                                        selectedType.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, stringResource(R.string.anniversary_select_type))
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
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.anniversary_date_label), 
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Surface(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
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
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = stringResource(R.string.anniversary_calendar),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = selectedDate.format(
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.anniversary_repeat_yearly),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.anniversary_repeat_yearly_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isYearly,
                            onCheckedChange = { isYearly = it }
                        )
                    }
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.anniversary_importance),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { importance = star },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        if (star <= importance) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = stringResource(R.string.anniversary_stars, star),
                                        tint = if (star <= importance) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(stringResource(R.string.anniversary_note)) },
                        placeholder = { Text(stringResource(R.string.anniversary_note_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 3
                    )
                }
                
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.anniversary_image), 
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
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
                                    .height(100.dp)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = stringResource(R.string.anniversary_add_image),
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        stringResource(R.string.anniversary_click_select_image),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val dateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        onConfirm(
                            name, 
                            dateStr, 
                            selectedImageUri?.toString(),
                            selectedType.name,
                            isYearly,
                            note.ifBlank { null },
                            importance
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.anniversary_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.anniversary_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = java.time.Instant
                                .ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.anniversary_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.anniversary_cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        stringResource(R.string.anniversary_select_date),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    }
}
