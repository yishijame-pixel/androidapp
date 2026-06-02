package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.funlife.data.model.ChatMessage
import com.example.funlife.data.model.ChatPersona
import com.example.funlife.ui.components.AccountSwitcher
import com.example.funlife.ui.components.BudgetRingStrip
import com.example.funlife.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// 智能时间格式化
private fun formatSmartTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L

    return when {
        diff < 60_000L -> "刚刚"
        diff < 3600_000L -> "${diff / 60_000}分钟前"
        timestamp >= todayStart -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        timestamp >= yesterdayStart -> "昨天 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}

// 每个人格对应的主题色
private fun getPersonaTheme(personaId: String): Pair<Color, Color> = when (personaId) {
    "dad" -> Color(0xFFB8860B) to Color(0xFFDAA520)
    "girlfriend" -> Color(0xFFFF6B9D) to Color(0xFFFF8FB3)
    "roast" -> Color(0xFF7C4DFF) to Color(0xFFB388FF)
    "gentle" -> Color(0xFF4FC3F7) to Color(0xFF81D4FA)
    "eunuch" -> Color(0xFFD4AF37) to Color(0xFFFFD700)
    "buddha" -> Color(0xFF8D6E63) to Color(0xFFBCAAA4)
    "cat" -> Color(0xFFFF7043) to Color(0xFFFF8A65)
    "grandma" -> Color(0xFF66BB6A) to Color(0xFF81C784)
    else -> Color(0xFFFF8A80) to Color(0xFFFFAB91)
}

private fun getPersonaSubtitle(personaId: String): String = when (personaId) {
    "dad" -> "省一分是一分，过日子要精打细算"
    "girlfriend" -> "和我一起记账，让爱情更甜蜜♡"
    "roast" -> "你的钱包需要被拯救"
    "gentle" -> "温柔地帮你管好每一笔～"
    "eunuch" -> "奴才为皇上管好内务府银两！"
    "buddha" -> "钱财皆身外之物，施主随缘"
    "cat" -> "本喵勉强帮你记账，感恩吧喵～"
    "grandma" -> "奶奶帮你看着钱，别乱花啊！"
    else -> "和我聊天记账吧～"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBillScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBillDetail: () -> Unit = {},
    onNavigateToBudgetManager: () -> Unit = {},
    onNavigateToAccountManager: () -> Unit = {},
    avatarUri: String? = null
) {
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    val personas by viewModel.personas.collectAsState(initial = emptyList())
    val currentPersona by viewModel.currentPersona.collectAsState()
    val currentPersonaId by viewModel.currentPersonaId.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    val fontSize by viewModel.fontSize.collectAsState()
    var showFontSlider by remember { mutableStateOf(false) }
    // 头像选择弹窗状态
    var showAvatarPicker by remember { mutableStateOf(false) }
    var avatarPickTargetId by remember { mutableStateOf("") }
    // 消息长按
    var longPressMsg by remember { mutableStateOf<ChatMessage?>(null) }
    // 搜索
    val isSearching by viewModel.isSearching.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState(initial = emptyList())
    // 预算（已迁移到独立页面，仅保留人格超预告警用的老字段）
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    // 分享今日账单卡（数据隔离：用 viewModel.userId）
    var showShareCardDialog by remember { mutableStateOf(false) }
    val allBills by viewModel.bills.collectAsState(initial = emptyList())
    // 导出
    val context = LocalContext.current
    // AI设置
    var showAiDialog by remember { mutableStateOf(false) }
    // 🆕 v50：极简模式快速设置月度预算
    var showBudgetDialog by remember { mutableStateOf(false) }
    val isAiAvailable by remember { derivedStateOf { viewModel.isAiAvailable } }
    // Toast 消息监听
    val toastMessage by viewModel.toastMessage.collectAsState()
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    val (themeColor, themeColorLight) = getPersonaTheme(currentPersonaId)
    val personaMap = remember(personas) { personas.associateBy { it.id } }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColor.copy(alpha = 0.08f),
                        Color(0xFFF8F6FA),
                        Color(0xFFFFF8F0)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 🆕 v50 记账模式（极简 / 进阶）
            val bookkeepingMode by viewModel.bookkeepingMode.collectAsState()
            val isAdvanced = bookkeepingMode == com.example.funlife.viewmodel.BookkeepingMode.ADVANCED

            // ===== 自定义顶部栏 =====
            ChatTopBar(
                persona = currentPersona,
                themeColor = themeColor,
                themeColorLight = themeColorLight,
                onBack = onNavigateBack,
                onClear = { showClearDialog = true },
                onBillDetail = onNavigateToBillDetail,
                onToggleFont = { showFontSlider = !showFontSlider },
                onAvatarUpload = {
                    avatarPickTargetId = currentPersonaId; showAvatarPicker = true
                },
                onSearch = { viewModel.toggleSearch() },
                isAdvanced = isAdvanced,
                onToggleMode = { viewModel.toggleBookkeepingMode() },
                currentMonthlyBudget = monthlyBudget,
                onSetMonthlyBudget = { showBudgetDialog = true },
                onBudget = onNavigateToBudgetManager,
                onAccountManager = onNavigateToAccountManager,
                onShareCard = { showShareCardDialog = true },
                onExport = {
                    scope.launch {
                        val csv = viewModel.exportBillsCsv()
                        // 🔒 安全修复：导出文件名包含 userId + 时间戳，避免多账号互相覆盖
                        // 同时清理旧文件，防止 cacheDir 残留其他账号导出
                        val sharedDir = java.io.File(context.cacheDir, "shared_images").apply { mkdirs() }
                        sharedDir.listFiles { f -> f.name.startsWith("bills_export_") }?.forEach { it.delete() }
                        val fileName = "bills_export_${viewModel.userId}_${System.currentTimeMillis()}.csv"
                        val file = java.io.File(sharedDir, fileName)
                        file.writeText(csv)
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", file
                        )
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "导出账单"))
                    }
                },
                onAiSettings = { showAiDialog = true },
                isAiAvailable = isAiAvailable
            )

            // ===== 人格选择栏 =====
            PersonaSelector(
                personas = personas,
                currentId = currentPersonaId,
                themeColor = themeColor,
                onSelect = { viewModel.switchPersona(it) },
                onAvatarPicked = { id, _ ->
                    avatarPickTargetId = id
                    showAvatarPicker = true
                }
            )

            // ===== 字体大小调整 =====
            androidx.compose.animation.AnimatedVisibility(visible = showFontSlider) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("A", fontSize = 12.sp, color = Color(0xFF999999))
                    Slider(
                        value = fontSize,
                        onValueChange = { viewModel.updateFontSize(it) },
                        valueRange = 12f..22f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = themeColor,
                            activeTrackColor = themeColor
                        )
                    )
                    Text("A", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF555555))
                    Spacer(Modifier.width(8.dp))
                    Text("${fontSize.toInt()}sp", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                }
            }

            // ===== 搜索栏 =====
            AnimatedVisibility(visible = isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Search, null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color(0xFF333333)),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) Text("搜索聊天记录...", fontSize = 14.sp, color = Color(0xFFCCCCCC))
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Text("${searchResults.size}条结果", fontSize = 11.sp, color = themeColor)
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = { viewModel.toggleSearch() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, "关闭", tint = Color(0xFFAAAAAA), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // ===== 聊天消息列表 =====
            val displayMessages = if (isSearching && searchQuery.isNotEmpty()) searchResults else messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
            ) {
                if (isTyping && !isSearching) {
                    item { TypingIndicator(persona = currentPersona, themeColor = themeColor) }
                }
                itemsIndexed(displayMessages, key = { _, m -> m.id }) { index, msg ->
                    val nextMsg = displayMessages.getOrNull(index + 1)
                    val showTime = nextMsg == null ||
                        Math.abs(msg.timestamp - nextMsg.timestamp) > 5_000L ||
                        msg.type == "system"
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(300))
                    ) {
                        ChatBubble(
                            message = msg, personaMap = personaMap, currentPersona = currentPersona,
                            themeColor = themeColor, userAvatarUri = avatarUri, fontSize = fontSize,
                            showTime = showTime, onLongPress = { longPressMsg = msg }
                        )
                    }
                }
            }

            // ===== 快捷提示 =====
            if (messages.size <= 3) {
                QuickTips(themeColor = themeColor, onTap = { viewModel.handleInput(it) })
            }

            // ===== 🆕 v49 预算进度环条（仅进阶模式显示） =====
            if (isAdvanced) {
                val budgetProgresses by viewModel.budgetProgresses.collectAsState()
                BudgetRingStrip(
                    progresses = budgetProgresses,
                    onClickItem = { onNavigateToBudgetManager() },
                    onAddBudget = { onNavigateToBudgetManager() }
                )

                // ===== 🆕 v48 多账户切换条（仅进阶模式显示） =====
                val accounts by viewModel.accounts.collectAsState()
                val currentAccountId by viewModel.currentAccountId.collectAsState()
                if (accounts.isNotEmpty()) {
                    AccountSwitcher(
                        accounts = accounts,
                        currentAccountId = currentAccountId,
                        themeColor = themeColor,
                        onSelect = { viewModel.selectAccount(it) }
                    )
                }
            }

            // ===== 底部输入栏 =====
            BillInputBar(themeColor = themeColor, onSubmit = { viewModel.handleInput(it) })
        }
    }

    // 清空确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🗑️", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("清空聊天", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Text(
                    "确定要清空所有聊天记录吗？\n账单数据不会被删除哦～",
                    fontSize = 14.sp, color = Color(0xFF666666), lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearChat(); showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("清空", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearDialog = false },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFDDDDDD))
                ) { Text("取消", color = Color(0xFF999999)) }
            }
        )
    }

    // 头像选择弹窗
    if (showAvatarPicker) {
        val targetPersona = personaMap[avatarPickTargetId]
        AvatarPickerDialog(
            themeColor = themeColor,
            currentAvatarUri = targetPersona?.customAvatarUri,
            onSelectPreset = { assetPath ->
                viewModel.updatePersonaAvatar(avatarPickTargetId, "file:///android_asset/$assetPath")
                showAvatarPicker = false
            },
            onSelectCustom = { uri ->
                viewModel.updatePersonaAvatar(avatarPickTargetId, uri)
                showAvatarPicker = false
            },
            onDismiss = { showAvatarPicker = false }
        )
    }

    // 消息长按操作弹窗
    longPressMsg?.let { msg ->
        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        AlertDialog(
            onDismissRequest = { longPressMsg = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📋", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("消息操作", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        msg.content.take(60) + if (msg.content.length > 60) "..." else "",
                        fontSize = 13.sp, color = Color(0xFF999999), lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    // 复制
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("msg", msg.content))
                                longPressMsg = null
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, tint = themeColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("复制消息", fontSize = 15.sp, color = Color(0xFF333333))
                    }
                    // 删除
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.deleteMessage(msg.id)
                                longPressMsg = null
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("删除消息", fontSize = 15.sp, color = Color(0xFFFF5252))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { longPressMsg = null }) { Text("取消", color = Color(0xFF999999)) }
            }
        )
    }

    // 🆕 v50：月度预算设置弹窗（极简模式核心入口）
    if (showBudgetDialog) {
        var budgetText by remember(monthlyBudget) {
            mutableStateOf(if (monthlyBudget > 0f) monthlyBudget.toInt().toString() else "")
        }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Savings, null, tint = Color(0xFFFFA726), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("月度预算", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text(
                        "设置后，超出 50% / 80% / 100% 时人格会主动提醒；超额还会推一条系统通知（24h 一次）。",
                        fontSize = 12.sp, color = Color(0xFF888888), lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { v -> budgetText = v.filter { it.isDigit() }.take(8) },
                        singleLine = true,
                        label = { Text("金额（元）") },
                        leadingIcon = { Text("¥", fontSize = 16.sp, color = Color(0xFFFFA726), fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (monthlyBudget > 0f) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "当前预算：¥${monthlyBudget.toInt()}",
                            fontSize = 12.sp, color = Color(0xFFAAAAAA)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val v = budgetText.toFloatOrNull() ?: 0f
                        viewModel.setMonthlyBudget(v)
                        showBudgetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("保存", color = Color.White) }
            },
            dismissButton = {
                if (monthlyBudget > 0f) {
                    TextButton(
                        onClick = {
                            viewModel.setMonthlyBudget(0f)
                            showBudgetDialog = false
                        }
                    ) { Text("取消预算", color = Color(0xFFFF5252)) }
                } else {
                    TextButton(onClick = { showBudgetDialog = false }) {
                        Text("关闭", color = Color(0xFF999999))
                    }
                }
            }
        )
    }

    // AI设置弹窗
    if (showAiDialog) {
        var apiKeyText by remember { mutableStateOf(viewModel.getAiApiKey()) }
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🤖", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("AI 智能设置", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    // 状态指示
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isAiAvailable) Color(0xFF4CAF50).copy(alpha = 0.1f)
                                else Color(0xFFFF9800).copy(alpha = 0.1f)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isAiAvailable) Color(0xFF4CAF50) else Color(0xFFFF9800))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isAiAvailable) "✅ 已连接 灵犀 AI" else "⚠️ 未配置 API Key",
                            fontSize = 13.sp,
                            color = if (isAiAvailable) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "输入 API Key 启用灵犀 AI 智能回复。\n不填则使用本地规则引擎。",
                        fontSize = 12.sp, color = Color(0xFF999999), lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = { apiKeyText = it.trim() },
                        label = { Text("API Key") },
                        placeholder = { Text("sk-...", color = Color(0xFFCCCCCC)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "未配置时使用本地规则引擎回复，配置后人格回复更智能",
                        fontSize = 11.sp, color = Color(0xFFBBBBBB)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setAiApiKey(apiKeyText)
                        showAiDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showAiDialog = false }) { Text("取消", color = Color(0xFF999999)) }
            }
        )
    }

    // 🔒 分享今日账单卡（数据按 viewModel.userId 严格隔离）
    if (showShareCardDialog) {
        BillShareDialog(
            currentUserId = viewModel.userId,
            nickname = currentPersona.name.ifBlank { "我" },
            allBills = allBills,
            onDismiss = { showShareCardDialog = false }
        )
    }
}

// ═══════════════════════════════════════════════
// 头像选择弹窗 - 预设 + 自定义上传
// ═══════════════════════════════════════════════
private val presetAvatars = (1..15).map { "renge/renge_$it.png" }

@Composable
private fun AvatarPickerDialog(
    themeColor: Color,
    currentAvatarUri: String? = null,
    onSelectPreset: (String) -> Unit,
    onSelectCustom: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val customLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onSelectCustom(uri.toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎨", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text("选择人格头像", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column {
                Text("预设头像", fontSize = 13.sp, color = Color(0xFF999999), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.heightIn(max = 300.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(presetAvatars) { assetPath ->
                        val fullUri = "file:///android_asset/$assetPath"
                        val isCurrentAvatar = currentAvatarUri == fullUri
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isCurrentAvatar) 3.dp else 1.dp,
                                        color = if (isCurrentAvatar) themeColor else Color(0xFFEEEEEE),
                                        shape = CircleShape
                                    )
                                    .then(
                                        if (isCurrentAvatar) Modifier
                                        else Modifier.clickable { onSelectPreset(assetPath) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = fullUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .graphicsLayer { alpha = if (isCurrentAvatar) 0.45f else 1f },
                                    contentScale = ContentScale.Crop
                                )
                                if (isCurrentAvatar) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(themeColor.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Check, "已选",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                            if (isCurrentAvatar) {
                                Text(
                                    "使用中",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                // 自定义上传按钮
                OutlinedButton(
                    onClick = { customLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, themeColor.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Rounded.CameraAlt, null, tint = themeColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("从相册上传自定义头像", color = themeColor, fontSize = 14.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF999999))
            }
        }
    )
}

// ═══════════════════════════════════════════════
// 自定义顶部栏
// ═══════════════════════════════════════════════
@Composable
private fun ChatTopBar(
    persona: ChatPersona,
    themeColor: Color,
    themeColorLight: Color,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onBillDetail: () -> Unit = {},
    onToggleFont: () -> Unit = {},
    onAvatarUpload: () -> Unit = {},
    onSearch: () -> Unit = {},
    isAdvanced: Boolean = true,
    onToggleMode: () -> Unit = {},
    currentMonthlyBudget: Float = 0f,
    onSetMonthlyBudget: () -> Unit = {},
    onBudget: () -> Unit = {},
    onAccountManager: () -> Unit = {},
    onShareCard: () -> Unit = {},
    onExport: () -> Unit = {},
    onAiSettings: () -> Unit = {},
    isAiAvailable: Boolean = false
) {
    val breathe = rememberInfiniteTransition(label = "breathe")
    val glowAlpha by breathe.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, spotColor = themeColor.copy(alpha = 0.5f))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(themeColor, themeColorLight, themeColor.copy(alpha = 0.85f))
                )
            )
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Rounded.ArrowBack, "返回", tint = Color.White, modifier = Modifier.size(22.dp))
            }

            // 头像（点击可换）
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = 2.dp, end = 10.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onAvatarUpload() }
            ) {
                // 呼吸光晕
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer { alpha = glowAlpha * 0.35f }
                        .background(Color.White, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (persona.customAvatarUri != null) {
                        AsyncImage(
                            model = persona.customAvatarUri,
                            contentDescription = persona.name,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(persona.avatar, fontSize = 20.sp)
                    }
                }
                // 在线绿点 + 相机角标
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                }
            }

            // 名字 + 副标题（占据剩余空间）
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        persona.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isAiAvailable) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("AI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                Text(
                    getPersonaSubtitle(persona.id),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            // 右侧按钮组
            var showMenu by remember { mutableStateOf(false) }
            IconButton(onClick = onSearch, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Search, "搜索", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onBillDetail, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.Receipt, "账单", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.MoreVert, "更多", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SmartToy, null, modifier = Modifier.size(18.dp),
                                tint = if (isAiAvailable) Color(0xFF4CAF50) else Color(0xFFFF9800))
                            Spacer(Modifier.width(8.dp))
                            Text("AI 设置", fontSize = 14.sp)
                            if (isAiAvailable) {
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier.size(6.dp).clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                            }
                        }},
                        onClick = { showMenu = false; onAiSettings() }
                    )
                    // 🆕 v50：月度预算（核心入口，两种模式都显示）
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Savings, null, modifier = Modifier.size(18.dp), tint = Color(0xFFFFA726))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (currentMonthlyBudget > 0f)
                                    "月度预算（${currentMonthlyBudget.toInt()}元）"
                                else "设置月度预算",
                                fontSize = 14.sp
                            )
                        }},
                        onClick = { showMenu = false; onSetMonthlyBudget() }
                    )
                    // 🆕 v50：仅进阶模式显示"账户管理 / 预算管理"
                    if (isAdvanced) {
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AccountBalance, null, modifier = Modifier.size(18.dp), tint = Color(0xFF00BCD4))
                                Spacer(Modifier.width(8.dp))
                                Text("账户管理", fontSize = 14.sp)
                            }},
                            onClick = { showMenu = false; onAccountManager() }
                        )
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AccountBalanceWallet, null, modifier = Modifier.size(18.dp), tint = Color(0xFFFF9800))
                                Spacer(Modifier.width(8.dp))
                                Text("预算管理", fontSize = 14.sp)
                            }},
                            onClick = { showMenu = false; onBudget() }
                        )
                    }
                    // 🆕 v50：模式切换入口（极简 ⇄ 进阶）
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isAdvanced) Icons.Rounded.Tune else Icons.Rounded.AutoAwesome,
                                null, modifier = Modifier.size(18.dp),
                                tint = if (isAdvanced) Color(0xFF7E57C2) else Color(0xFF4CAF50)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isAdvanced) "切换至极简模式" else "切换至进阶模式",
                                fontSize = 14.sp
                            )
                        }},
                        onClick = { showMenu = false; onToggleMode() }
                    )
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp), tint = Color(0xFFFF6B9D))
                            Spacer(Modifier.width(8.dp))
                            Text("分享今日卡片", fontSize = 14.sp)
                        }},
                        onClick = { showMenu = false; onShareCard() }
                    )
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.FileDownload, null, modifier = Modifier.size(18.dp), tint = Color(0xFF2196F3))
                            Spacer(Modifier.width(8.dp))
                            Text("导出账单", fontSize = 14.sp)
                        }},
                        onClick = { showMenu = false; onExport() }
                    )
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.FormatSize, null, modifier = Modifier.size(18.dp), tint = Color(0xFF9C27B0))
                            Spacer(Modifier.width(8.dp))
                            Text("字体大小", fontSize = 14.sp)
                        }},
                        onClick = { showMenu = false; onToggleFont() }
                    )
                    DropdownMenuItem(
                        text = { Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.DeleteSweep, null, modifier = Modifier.size(18.dp), tint = Color(0xFFFF5252))
                            Spacer(Modifier.width(8.dp))
                            Text("清空聊天", fontSize = 14.sp, color = Color(0xFFFF5252))
                        }},
                        onClick = { showMenu = false; onClear() }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// 人格选择器 - 横向滑动胶囊（长按更换头像）
// ═══════════════════════════════════════════════
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PersonaSelector(
    personas: List<ChatPersona>,
    currentId: String,
    themeColor: Color,
    onSelect: (String) -> Unit,
    onAvatarPicked: (String, String) -> Unit = { _, _ -> }
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 14.dp)
    ) {
        items(personas) { persona ->
            val isSelected = persona.id == currentId
            val (pColor, pColorLight) = getPersonaTheme(persona.id)

            Box(
                modifier = Modifier
                    .then(
                        if (isSelected)
                            Modifier.shadow(elevation = 4.dp, shape = RoundedCornerShape(18.dp), spotColor = pColor)
                        else Modifier
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isSelected) Brush.horizontalGradient(listOf(pColor, pColorLight))
                        else Brush.horizontalGradient(listOf(Color.White, Color.White))
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Color.Transparent else Color(0xFFEFE7F0),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(persona.id) },
                        onLongClick = {
                            onAvatarPicked(persona.id, "")
                        }
                    )
                    .padding(start = 6.dp, end = 14.dp, top = 5.dp, bottom = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    // 头像外圈
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.3f) else pColor.copy(alpha = 0.10f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        PersonaAvatarSmall(persona, 22.dp)
                    }
                    Text(
                        persona.name,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF666666)
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonaAvatarSmall(persona: ChatPersona, size: androidx.compose.ui.unit.Dp) {
    if (persona.customAvatarUri != null) {
        AsyncImage(
            model = persona.customAvatarUri,
            contentDescription = persona.name,
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Text(persona.avatar, fontSize = (size.value - 4).sp)
    }
}

// ═══════════════════════════════════════════════
// 聊天气泡 - 清爽无阴影
// ═══════════════════════════════════════════════
@Composable
private fun ChatBubble(
    message: ChatMessage,
    personaMap: Map<String, ChatPersona>,
    currentPersona: ChatPersona,
    themeColor: Color,
    userAvatarUri: String? = null,
    fontSize: Float = 15f,
    showTime: Boolean = true,
    onLongPress: () -> Unit = {}
) {
    val isUser = message.role == "user"
    val timeText = formatSmartTime(message.timestamp)
    // 根据消息的personaId查找对应人格，保证历史消息显示正确头像
    val msgPersona = personaMap[message.personaId] ?: currentPersona
    val msgThemeColor = getPersonaTheme(message.personaId).first

    // 系统消息
    if (message.type == "system") {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                message.content,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeColor.copy(alpha = 0.06f))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                fontSize = 12.sp, color = themeColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    // AI消息使用该消息对应人格的主题色
    val bubbleColor = if (isUser) themeColor else msgThemeColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // AI头像 - 使用消息对应的人格头像
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(2.dp, CircleShape, spotColor = msgThemeColor.copy(alpha = 0.3f))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(msgThemeColor.copy(alpha = 0.18f), msgThemeColor.copy(alpha = 0.08f))
                        )
                    )
                    .border(1.dp, msgThemeColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (msgPersona.customAvatarUri != null) {
                    AsyncImage(
                        model = msgPersona.customAvatarUri,
                        contentDescription = msgPersona.name,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(msgPersona.avatar, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        // 气泡 + 时间（用 fraction 占据约 75% 屏宽，避免在大屏只占一半显得局促）
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.78f)
        ) {
            // AI 名字小标签（仅多人格切换场景下展示）
            if (!isUser && msgPersona.id != currentPersona.id) {
                Text(
                    msgPersona.name,
                    fontSize = 10.sp, color = msgThemeColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (isUser) 3.dp else 2.dp,
                        shape = RoundedCornerShape(
                            topStart = 18.dp, topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        ),
                        spotColor = if (isUser) themeColor.copy(alpha = 0.4f) else Color(0x33000000)
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp, topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isUser) Brush.linearGradient(
                            listOf(bubbleColor, bubbleColor.copy(alpha = 0.82f))
                        ) else Brush.linearGradient(
                            listOf(Color.White, Color(0xFFFAFBFC))
                        )
                    )
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = onLongPress
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (message.type == "bill") {
                    BillBubbleContent(message, isUser, themeColor, fontSize)
                } else {
                    Text(
                        message.content,
                        fontSize = fontSize.sp,
                        color = if (isUser) Color.White else Color(0xFF333333),
                        lineHeight = (fontSize + 7).sp
                    )
                }
            }
            if (showTime) {
                Text(
                    timeText, fontSize = 10.sp, color = Color(0xFFCCCCCC),
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                )
            }
        }

        // 用户头像
        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(2.dp, CircleShape, spotColor = themeColor.copy(alpha = 0.3f))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(themeColor.copy(alpha = 0.22f), themeColor.copy(alpha = 0.10f))
                        )
                    )
                    .border(1.dp, themeColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (userAvatarUri != null) {
                    AsyncImage(
                        model = userAvatarUri,
                        contentDescription = "用户头像",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("😊", fontSize = 20.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// 账单气泡内容
// ═══════════════════════════════════════════════
@Composable
private fun BillBubbleContent(message: ChatMessage, isUser: Boolean, themeColor: Color, fontSize: Float = 15f) {
    val amountRegex = Regex("""(\d+\.?\d*)""")
    val amountMatch = amountRegex.find(message.content)
    val displayAmount = amountMatch?.value ?: ""

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("💰", fontSize = 14.sp)
            Text(
                "记账", fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isUser) Color.White.copy(alpha = 0.8f) else themeColor
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            message.content, fontSize = fontSize.sp,
            color = if (isUser) Color.White else Color(0xFF333333),
            lineHeight = (fontSize + 7).sp
        )
        if (displayAmount.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isUser) Color.White.copy(alpha = 0.2f) else themeColor.copy(alpha = 0.08f)
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    "¥$displayAmount",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = if (isUser) Color.White else themeColor
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
// 正在输入指示器
// ═══════════════════════════════════════════════
@Composable
private fun TypingIndicator(persona: ChatPersona, themeColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(themeColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (persona.customAvatarUri != null) {
                AsyncImage(
                    model = persona.customAvatarUri,
                    contentDescription = persona.name,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(persona.avatar, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val offsetY by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = -5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(400, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse,
                            initialStartOffset = StartOffset(index * 150)
                        ),
                        label = "bounce$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .graphicsLayer { translationY = offsetY }
                            .background(themeColor.copy(alpha = 0.4f + index * 0.15f), CircleShape)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// 快捷提示
// ═══════════════════════════════════════════════
@Composable
private fun QuickTips(themeColor: Color, onTap: (String) -> Unit) {
    val tips = listOf("🍱 午饭25" to "午饭25", "🚕 打车15" to "打车15", "🧋 奶茶12" to "奶茶12", "👗 买衣服200" to "买衣服200")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💡", fontSize = 13.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                "试试这样说",
                fontSize = 11.sp,
                color = Color(0xFF999999),
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tips) { (label, value) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    themeColor.copy(alpha = 0.10f),
                                    themeColor.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .border(1.dp, themeColor.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                        .clickable { onTap(value) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        label,
                        fontSize = 12.sp,
                        color = themeColor.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// 底部输入栏 - 简洁清爽
// ═══════════════════════════════════════════════
@Composable
private fun BillInputBar(themeColor: Color, onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val canSend = text.isNotBlank()

    // 发送按钮的呼吸缩放（仅在有内容时）
    val sendScale by animateFloatAsState(
        targetValue = if (canSend) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sendScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .shadow(elevation = 8.dp, spotColor = Color.Black.copy(alpha = 0.4f))
            .background(Color.White)
    ) {
        // 顶部细分割线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFFE5E5EA),
                            Color.Transparent
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧装饰图标（💰记账提示）
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text("💰", fontSize = 18.sp)
            }

            Spacer(Modifier.width(8.dp))

            // 输入框 - 固定单行高度，placeholder 强制单行省略
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFF7F7FB), Color(0xFFF1F1F6))
                        )
                    )
                    .border(
                        1.dp,
                        if (canSend) themeColor.copy(alpha = 0.35f) else Color(0xFFE8E8EE),
                        RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF222222)),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(themeColor),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (text.isNotBlank()) {
                            onSubmit(text.trim())
                            text = ""
                            focusManager.clearFocus()
                        }
                    }),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(
                                    "记账或聊天…",
                                    fontSize = 14.sp,
                                    color = Color(0xFFB0B0B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(Modifier.width(8.dp))

            // 发送按钮 - 渐变 + 缩放动画 + 飞机图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer { scaleX = sendScale; scaleY = sendScale }
                    .then(
                        if (canSend)
                            Modifier.shadow(8.dp, CircleShape, spotColor = themeColor.copy(alpha = 0.7f))
                        else Modifier
                    )
                    .clip(CircleShape)
                    .background(
                        if (canSend)
                            Brush.linearGradient(
                                listOf(themeColor, themeColor.copy(alpha = 0.75f))
                            )
                        else
                            Brush.linearGradient(
                                listOf(Color(0xFFE4E4E8), Color(0xFFD8D8DC))
                            )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (canSend) {
                            onSubmit(text.trim())
                            text = ""
                            focusManager.clearFocus()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Send, "发送",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = -28f }
                )
            }
        }
    }
}
