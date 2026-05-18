package com.example.funlife.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.Bill
import com.example.funlife.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// 科技感配色
private val DarkBg = Color(0xFF0D1117)
private val DarkCard = Color(0xFF161B22)
private val NeonCyan = Color(0xFF00F5D4)
private val NeonPurple = Color(0xFFBB86FC)
private val NeonPink = Color(0xFFFF6B9D)
private val NeonBlue = Color(0xFF64DFDF)
private val NeonOrange = Color(0xFFFF9E43)
private val TextPrimary = Color(0xFFE6EDF3)
private val TextSecondary = Color(0xFF8B949E)
private val GlowLine = Color(0xFF30363D)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillDetailScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val bills by viewModel.bills.collectAsState(initial = emptyList())
    // 编辑/删除状态
    var editingBill by remember { mutableStateOf<Bill?>(null) }
    var deletingBill by remember { mutableStateOf<Bill?>(null) }
    var longPressBill by remember { mutableStateOf<Bill?>(null) }

    val totalAmount = remember(bills) { Math.abs(bills.sumOf { it.amount }) }
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayBills = remember(bills, todayStart) { bills.filter { it.timestamp >= todayStart } }
    val todayAmount = remember(todayBills) { Math.abs(todayBills.sumOf { it.amount }) }

    val monthStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val monthBills = remember(bills, monthStart) { bills.filter { it.timestamp >= monthStart } }
    val monthAmount = remember(monthBills) { Math.abs(monthBills.sumOf { it.amount }) }

    val categoryStats = remember(monthBills) {
        monthBills.groupBy { it.category }
            .map { (cat, list) -> cat to Math.abs(list.sumOf { it.amount }) }
            .sortedByDescending { it.second }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("今天", "本月", "全部")
    val displayBills = when (selectedTab) {
        0 -> todayBills; 1 -> monthBills; else -> bills
    }

    // 呼吸动画
    val breathe = rememberInfiniteTransition(label = "glow")
    val glowAlpha by breathe.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // 背景网格装饰
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 40.dp.toPx()
            for (x in 0..((size.width / step).toInt())) {
                drawLine(GlowLine.copy(alpha = 0.3f), Offset(x * step, 0f), Offset(x * step, size.height), strokeWidth = 0.5f)
            }
            for (y in 0..((size.height / step).toInt())) {
                drawLine(GlowLine.copy(alpha = 0.3f), Offset(0f, y * step), Offset(size.width, y * step), strokeWidth = 0.5f)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // ===== 科技感顶部栏 =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(DarkCard, DarkBg.copy(alpha = 0.95f))
                        )
                    )
                    .drawBehind {
                        drawLine(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, NeonCyan.copy(alpha = glowAlpha * 0.6f), Color.Transparent)
                            ),
                            Offset(0f, size.height), Offset(size.width, size.height),
                            strokeWidth = 2f
                        )
                    }
                    .statusBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 12.dp)
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Rounded.ArrowBack, "返回", tint = NeonCyan, modifier = Modifier.size(24.dp))
                }
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "⚡ 资金监控中心",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "FUND MONITOR SYSTEM",
                        fontSize = 9.sp, color = NeonCyan.copy(alpha = 0.6f),
                        letterSpacing = 4.sp
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // ===== 核心数据面板 =====
                item {
                    Spacer(Modifier.height(12.dp))
                    // 大数字展示
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        DarkCard,
                                        Color(0xFF1A1F2E),
                                        DarkCard
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                Brush.linearGradient(
                                    listOf(NeonCyan.copy(alpha = 0.3f), NeonPurple.copy(alpha = 0.3f))
                                ),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("本月累计支出", fontSize = 12.sp, color = TextSecondary, letterSpacing = 2.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("¥", fontSize = 22.sp, fontWeight = FontWeight.Light, color = NeonCyan.copy(alpha = 0.8f))
                                Text(
                                    "%.2f".format(monthAmount),
                                    fontSize = 38.sp, fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            // 三个指标
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                CyberStatItem("今日", todayAmount, todayBills.size, NeonPink)
                                // 竖分隔线
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(40.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, GlowLine, Color.Transparent)
                                            )
                                        )
                                )
                                CyberStatItem("本月", monthAmount, monthBills.size, NeonBlue)
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(40.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, GlowLine, Color.Transparent)
                                            )
                                        )
                                )
                                CyberStatItem("累计", totalAmount, bills.size, NeonPurple)
                            }
                        }
                    }
                }

                // ===== 分类分析面板 =====
                if (categoryStats.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkCard)
                                .border(1.dp, GlowLine, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp, 16.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(NeonCyan)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("分类分析", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(Modifier.weight(1f))
                                Text("CATEGORY", fontSize = 9.sp, color = TextSecondary, letterSpacing = 2.sp)
                            }
                            Spacer(Modifier.height(14.dp))

                            val catColors = listOf(NeonCyan, NeonPurple, NeonPink, NeonBlue, NeonOrange)
                            categoryStats.forEachIndexed { index, (category, amount) ->
                                val ratio = if (monthAmount > 0.0) (amount / monthAmount).toFloat() else 0f
                                val barColor = catColors[index % catColors.size]
                                CyberCategoryBar(
                                    category = category,
                                    amount = amount,
                                    ratio = ratio,
                                    emoji = getCategoryEmoji(category),
                                    color = barColor,
                                    percent = (ratio * 100).toInt()
                                )
                                if (index < categoryStats.size - 1) {
                                    Spacer(Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }

                // ===== 时间筛选 =====
                item {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            val shape = RoundedCornerShape(10.dp)
                            Box(
                                modifier = Modifier
                                    .clip(shape)
                                    .background(
                                        if (isSelected) Brush.horizontalGradient(
                                            listOf(NeonCyan.copy(alpha = 0.2f), NeonPurple.copy(alpha = 0.15f))
                                        ) else Brush.linearGradient(listOf(DarkCard, DarkCard))
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonCyan.copy(alpha = 0.5f) else GlowLine,
                                        shape
                                    )
                                    .clickable { selectedTab = index }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) NeonCyan else TextSecondary
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${displayBills.size}笔记录",
                            fontSize = 11.sp, color = TextSecondary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // ===== 账单列表 =====
                if (displayBills.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("�", fontSize = 44.sp)
                                Spacer(Modifier.height(10.dp))
                                Text("暂无数据信号", fontSize = 14.sp, color = TextSecondary)
                                Text("NO DATA SIGNAL", fontSize = 9.sp, color = TextSecondary.copy(alpha = 0.5f), letterSpacing = 3.sp)
                            }
                        }
                    }
                } else {
                    items(displayBills, key = { it.id }) { bill ->
                        CyberBillItem(
                            bill = bill,
                            onLongPress = { longPressBill = bill }
                        )
                    }
                }
            }
        }
    }

    // 账单长按操作弹窗
    longPressBill?.let { bill ->
        AlertDialog(
            onDismissRequest = { longPressBill = null },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(getCategoryEmoji(bill.category), fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("账单操作", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                }
            },
            text = {
                Column {
                    Text(
                        "${bill.category}  ¥${String.format("%.2f", kotlin.math.abs(bill.amount))}",
                        fontSize = 14.sp, color = TextSecondary
                    )
                    if (bill.note.isNotEmpty()) {
                        Text(bill.note, fontSize = 12.sp, color = TextSecondary.copy(alpha = 0.7f))
                    }
                    Spacer(Modifier.height(16.dp))
                    // 编辑按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                editingBill = bill
                                longPressBill = null
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Edit, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("编辑账单", fontSize = 15.sp, color = TextPrimary)
                    }
                    // 删除按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                deletingBill = bill
                                longPressBill = null
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Delete, null, tint = NeonPink, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("删除账单", fontSize = 15.sp, color = NeonPink)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { longPressBill = null }) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    }

    // 删除确认弹窗
    deletingBill?.let { bill ->
        AlertDialog(
            onDismissRequest = { deletingBill = null },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🗑️", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("确认删除", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                }
            },
            text = {
                Text(
                    "确定要删除这笔账单吗？\n${bill.category} ¥${String.format("%.2f", kotlin.math.abs(bill.amount))}\n删除后无法恢复",
                    fontSize = 14.sp, color = TextSecondary, lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBill(bill)
                        deletingBill = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("删除", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deletingBill = null },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlowLine)
                ) { Text("取消", color = TextSecondary) }
            }
        )
    }

    // 编辑账单弹窗
    editingBill?.let { bill ->
        val categories = listOf("餐饮", "交通", "购物", "娱乐", "社交", "居住", "医疗", "教育", "服饰", "其他")
        var editAmount by remember { mutableStateOf(String.format("%.2f", kotlin.math.abs(bill.amount))) }
        var editCategory by remember { mutableStateOf(bill.category) }
        var editNote by remember { mutableStateOf(bill.note) }

        AlertDialog(
            onDismissRequest = { editingBill = null },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✏️", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("编辑账单", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                }
            },
            text = {
                Column {
                    // 金额
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("金额", color = TextSecondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GlowLine,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        )
                    )
                    Spacer(Modifier.height(12.dp))

                    // 分类选择
                    Text("分类", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    // 分类网格
                    val rows = categories.chunked(5)
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowItems.forEach { cat ->
                                val isSelected = cat == editCategory
                                val catColor = getCategoryColor(cat)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) catColor.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) catColor else GlowLine,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { editCategory = cat }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(getCategoryEmoji(cat), fontSize = 12.sp)
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            cat, fontSize = 11.sp,
                                            color = if (isSelected) catColor else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    Spacer(Modifier.height(10.dp))
                    // 备注
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text("备注", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = GlowLine,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAmount = editAmount.toDoubleOrNull() ?: kotlin.math.abs(bill.amount)
                        viewModel.updateBill(
                            bill.copy(
                                amount = -kotlin.math.abs(newAmount),
                                category = editCategory,
                                note = editNote
                            )
                        )
                        editingBill = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("保存", color = DarkBg) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { editingBill = null },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlowLine)
                ) { Text("取消", color = TextSecondary) }
            }
        )
    }
}

// ═══════════════════════════════════════════════
// 核心指标项
// ═══════════════════════════════════════════════
@Composable
private fun CyberStatItem(label: String, amount: Double, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "¥%.1f".format(amount),
            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color
        )
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text("${count}笔", fontSize = 9.sp, color = TextSecondary.copy(alpha = 0.6f))
    }
}

// ═══════════════════════════════════════════════
// 分类进度条 - 科技风
// ═══════════════════════════════════════════════
@Composable
private fun CyberCategoryBar(
    category: String,
    amount: Double,
    ratio: Float,
    emoji: String,
    color: Color,
    percent: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 分类图标
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.1f))
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(category, fontSize = 12.sp, color = TextPrimary)
                Text("${percent}%", fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(GlowLine)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(ratio.coerceIn(0.02f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color))
                        )
                        .animateContentSize()
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "¥%.1f".format(amount),
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
            modifier = Modifier.width(65.dp)
        )
    }
}

// ═══════════════════════════════════════════════
// 账单条目 - 赛博风卡片（支持长按操作）
// ═══════════════════════════════════════════════
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CyberBillItem(bill: Bill, onLongPress: () -> Unit = {}) {
    val timeText = remember(bill.timestamp) {
        SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(bill.timestamp))
    }
    val catColor = getCategoryColor(bill.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(1.dp, GlowLine, RoundedCornerShape(14.dp))
            .drawBehind {
                // 左侧发光边
                drawLine(
                    catColor.copy(alpha = 0.6f),
                    Offset(0f, size.height * 0.2f),
                    Offset(0f, size.height * 0.8f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onLongPress
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 分类图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(catColor.copy(alpha = 0.1f))
                .border(1.dp, catColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(getCategoryEmoji(bill.category), fontSize = 18.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                bill.note.ifEmpty { bill.category },
                fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(catColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(bill.category, fontSize = 10.sp, color = catColor)
                }
                Spacer(Modifier.width(8.dp))
                Text(timeText, fontSize = 10.sp, color = TextSecondary)
            }
        }

        // 金额
        Text(
            "-¥%.1f".format(Math.abs(bill.amount)),
            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeonPink
        )
    }
}

private fun getCategoryEmoji(category: String): String = when (category) {
    "餐饮" -> "🍜"
    "交通" -> "🚗"
    "购物" -> "🛍️"
    "娱乐" -> "🎮"
    "社交" -> "🍻"
    "居住" -> "🏠"
    "医疗" -> "💊"
    "教育" -> "📚"
    "服饰" -> "👗"
    "其他" -> "📦"
    else -> "💰"
}

private fun getCategoryColor(category: String): Color = when (category) {
    "餐饮" -> NeonOrange
    "交通" -> NeonBlue
    "购物" -> NeonPink
    "娱乐" -> NeonPurple
    "社交" -> Color(0xFFFFD93D)
    "居住" -> NeonCyan
    "医疗" -> Color(0xFF6BCB77)
    "教育" -> Color(0xFF4D96FF)
    "服饰" -> Color(0xFFFF6B9D)
    "其他" -> TextSecondary
    else -> NeonCyan
}
