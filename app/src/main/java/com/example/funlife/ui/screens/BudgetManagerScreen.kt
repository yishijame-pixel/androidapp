// BudgetManagerScreen.kt — 预算指挥中心（聊天记账 Phase 2C · 暗黑科技风）
//
// 🎨 个性化定位：
//   与"⚡ 资金监控中心"（BillDetailScreen）形成姐妹页，统一暗黑科技配色，
//   霓虹色环 + 动态网格 + 流光描边 + 英文小标"BUDGET COMMAND"。
//
// 🔒 数据隔离：所有数据通过 ViewModel（构造时绑定 userId）。
package com.example.funlife.ui.screens

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.funlife.data.model.Account
import com.example.funlife.data.model.Budget
import com.example.funlife.data.model.BudgetPeriod
import com.example.funlife.data.model.BudgetScope
import com.example.funlife.repository.BudgetProgress
import com.example.funlife.ui.utils.Radius
import com.example.funlife.ui.utils.ResponsiveDialogBox
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.util.Locale

// ─── 暗黑科技配色（与 BillDetailScreen 完全一致）───
private val DarkBg = Color(0xFF0D1117)
private val DarkCard = Color(0xFF161B22)
private val DarkCardLight = Color(0xFF1A1F2E)
private val NeonCyan = Color(0xFF00F5D4)
private val NeonPurple = Color(0xFFBB86FC)
private val NeonPink = Color(0xFFFF6B9D)
private val NeonOrange = Color(0xFFFF9E43)
private val NeonRed = Color(0xFFFF4D6D)
private val NeonGreen = Color(0xFF22C55E)
private val TextPrimary = Color(0xFFE6EDF3)
private val TextSecondary = Color(0xFF8B949E)
private val GlowLine = Color(0xFF30363D)

@Composable
fun BudgetManagerScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val budgetList by viewModel.budgets.collectAsState()
    val progresses by viewModel.budgetProgresses.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val progressMap = remember(progresses) { progresses.associateBy { it.budget.id } }

    var editorBudget by remember { mutableStateOf<Budget?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deletePending by remember { mutableStateOf<Budget?>(null) }

    // 聚合
    val totalAmount = budgetList.filter { it.isActive }.sumOf { it.amount }
    val totalUsed = progresses.filter { it.budget.isActive }.sumOf { it.used }
    val totalPct = if (totalAmount > 0) (totalUsed / totalAmount).toFloat().coerceIn(0f, 2f) else 0f
    val activeCount = budgetList.count { it.isActive }
    val warnedCount = progresses.count { it.warned }
    val exceededCount = progresses.count { it.exceeded }

    // 全局呼吸光效
    val infinite = rememberInfiniteTransition(label = "neon")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "g"
    )
    val gridShift by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "gs"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // ── 背景动态网格 ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = 40f
            val offset = gridShift * cellSize
            val w = size.width
            val h = size.height
            var x = -cellSize + (offset % cellSize)
            while (x < w) {
                drawLine(
                    color = GlowLine.copy(alpha = 0.3f),
                    start = Offset(x, 0f), end = Offset(x, h),
                    strokeWidth = 0.6f
                )
                x += cellSize
            }
            var y = -cellSize + (offset % cellSize)
            while (y < h) {
                drawLine(
                    color = GlowLine.copy(alpha = 0.3f),
                    start = Offset(0f, y), end = Offset(w, y),
                    strokeWidth = 0.6f
                )
                y += cellSize
            }
        }

        // 顶部光晕装饰
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = NeonCyan.copy(alpha = 0.10f),
                radius = size.width * 0.55f,
                center = Offset(-size.width * 0.1f, -size.width * 0.2f)
            )
            drawCircle(
                color = NeonPurple.copy(alpha = 0.08f),
                radius = size.width * 0.45f,
                center = Offset(size.width * 1.05f, -size.width * 0.15f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                // 状态栏安全区 + 系统手势
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            TopBar(
                glowAlpha = glowAlpha,
                onNavigateBack = onNavigateBack,
                onAdd = { editorBudget = null; showEditor = true }
            )

            if (budgetList.isEmpty()) {
                BudgetEmptyState(
                    glowAlpha = glowAlpha,
                    onCreate = { editorBudget = null; showEditor = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 12.dp, bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        TotalProgressCard(
                            usedAmount = totalUsed,
                            totalAmount = totalAmount,
                            percent = totalPct,
                            activeCount = activeCount,
                            warnedCount = warnedCount,
                            exceededCount = exceededCount,
                            glowAlpha = glowAlpha
                        )
                    }
                    item { SectionHeader(title = "预算列表", count = budgetList.size, glowAlpha = glowAlpha) }
                    items(items = budgetList, key = { it.id }) { b ->
                        BudgetItemCard(
                            budget = b,
                            progress = progressMap[b.id],
                            accounts = accounts,
                            onClickEdit = { editorBudget = b; showEditor = true },
                            onToggleActive = { scope.launch { viewModel.setBudgetActive(b.id, !b.isActive) } },
                            onRequestDelete = { deletePending = b }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        BudgetEditorDialog(
            initial = editorBudget,
            accounts = accounts,
            onDismiss = { showEditor = false },
            onConfirm = { newBudget ->
                scope.launch { viewModel.saveBudget(newBudget) }
                showEditor = false
            }
        )
    }

    deletePending?.let { target ->
        AlertDialog(
            onDismissRequest = { deletePending = null },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp),
            title = { Text("删除「${target.name}」？", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("删除后历史进度不会消失，但该预算将不再统计。", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { viewModel.deleteBudget(target) }
                        deletePending = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                ) { Text("删除", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { deletePending = null }) { Text("取消", color = TextSecondary) }
            }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 顶栏：科技感 Hero
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun TopBar(glowAlpha: Float, onNavigateBack: () -> Unit, onAdd: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(DarkCard, DarkBg.copy(alpha = 0.95f)))
            )
            .drawBehind {
                // 底部流光描边
                drawLine(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, NeonCyan.copy(alpha = glowAlpha * 0.6f), Color.Transparent)
                    ),
                    Offset(0f, size.height), Offset(size.width, size.height),
                    strokeWidth = 1.5f
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 左：返回
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCardLight)
                .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .clickable { onNavigateBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowBack, "返回", tint = NeonCyan, modifier = Modifier.size(20.dp))
        }
        // 中：标题
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("💎", fontSize = 18.sp)
                Text(
                    "预算指挥中心",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 2.sp
                )
            }
            Text(
                "BUDGET · COMMAND",
                fontSize = 9.sp,
                color = NeonPurple.copy(alpha = 0.7f),
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // 右：新建
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = NeonPink)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(listOf(NeonPink, NeonPurple))
                )
                .clickable { onAdd() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, "新建", tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 总进度 Hero 卡（霓虹大圆环）
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun TotalProgressCard(
    usedAmount: Double,
    totalAmount: Double,
    percent: Float,
    activeCount: Int,
    warnedCount: Int,
    exceededCount: Int,
    glowAlpha: Float
) {
    val statusColor = when {
        percent >= 1f -> NeonRed
        percent >= 0.8f -> NeonOrange
        else -> NeonCyan
    }
    val statusLabel = when {
        percent >= 1f -> "已超额"
        percent >= 0.8f -> "接近上限"
        percent <= 0f -> "未启用"
        else -> "运行中"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = statusColor)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(listOf(DarkCard, DarkCardLight, DarkCard))
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(statusColor.copy(alpha = 0.45f * glowAlpha), NeonPurple.copy(alpha = 0.35f))
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                        .drawBehind {
                            drawCircle(
                                color = statusColor.copy(alpha = glowAlpha * 0.6f),
                                radius = size.minDimension * 1.6f
                            )
                        }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "TOTAL · 总预算进度",
                    fontSize = 11.sp, color = TextSecondary,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                StatusChip(statusLabel, statusColor)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 大圆环
                Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val sw = 14f
                        // 外圈光晕
                        drawCircle(
                            color = statusColor.copy(alpha = glowAlpha * 0.18f),
                            radius = size.minDimension / 2f - 2f
                        )
                        drawArc(
                            color = GlowLine.copy(alpha = 0.6f),
                            startAngle = -90f, sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = sw, cap = StrokeCap.Round),
                            topLeft = Offset(sw, sw),
                            size = Size(size.width - sw * 2, size.height - sw * 2)
                        )
                        val sweep = (percent.coerceIn(0f, 1f)) * 360f
                        if (sweep > 0f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(statusColor.copy(alpha = 0.4f), statusColor, statusColor.copy(alpha = 0.4f))
                                ),
                                startAngle = -90f, sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = sw, cap = StrokeCap.Round),
                                topLeft = Offset(sw, sw),
                                size = Size(size.width - sw * 2, size.height - sw * 2)
                            )
                        }
                        // 中心十字定位线（科技感）
                        val cx = size.width / 2; val cy = size.height / 2
                        drawLine(
                            color = statusColor.copy(alpha = 0.2f),
                            start = Offset(cx - 8f, cy), end = Offset(cx + 8f, cy),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = statusColor.copy(alpha = 0.2f),
                            start = Offset(cx, cy - 8f), end = Offset(cx, cy + 8f),
                            strokeWidth = 1f
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${(percent * 100).toInt()}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor,
                            letterSpacing = 1.sp
                        )
                        Text("PERCENT", fontSize = 8.sp, color = TextSecondary, letterSpacing = 2.sp)
                    }
                }
                Spacer(Modifier.width(20.dp))
                Column {
                    Text("已用", fontSize = 10.sp, color = TextSecondary, letterSpacing = 1.sp)
                    Text(
                        money(usedAmount),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("/ ${money(totalAmount)}", fontSize = 12.sp, color = TextSecondary)
                }
            }
            Spacer(Modifier.height(16.dp))
            // 状态徽章组
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(label = "启用", value = "$activeCount", color = NeonGreen)
                StatusBadge(label = "警示", value = "$warnedCount", color = NeonOrange)
                StatusBadge(label = "超额", value = "$exceededCount", color = NeonRed)
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 1.sp)
    }
}

@Composable
private fun StatusBadge(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier.size(6.dp).clip(CircleShape).background(color)
            )
            Text(label, fontSize = 10.sp, color = TextSecondary, letterSpacing = 1.sp)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 区段标题
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun SectionHeader(title: String, count: Int, glowAlpha: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(4.dp, 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(NeonCyan)
                .drawBehind {
                    drawRect(NeonCyan.copy(alpha = glowAlpha * 0.5f))
                }
        )
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary, letterSpacing = 2.sp)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(NeonCyan.copy(alpha = 0.12f))
                .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("$count", fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeonCyan)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 列表项
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun BudgetItemCard(
    budget: Budget,
    progress: BudgetProgress?,
    accounts: List<Account>,
    onClickEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val accent = mapBudgetColorToNeon(budget.color)
    val pct = progress?.percent ?: 0f
    val statusColor = when {
        progress?.exceeded == true -> NeonRed
        progress?.warned == true -> NeonOrange
        else -> accent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = statusColor)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(DarkCard, DarkCardLight)
                )
            )
            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
    ) {
        // 左侧霓虹色条（带光晕）
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(statusColor)
                .align(Alignment.CenterStart)
                .drawBehind {
                    drawRect(statusColor.copy(alpha = 0.4f))
                }
        )
        Column(modifier = Modifier.padding(start = 18.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // emoji 圆形 chip
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(scopeEmoji(budget.scope), fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            budget.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        if (!budget.isActive) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GlowLine)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text("已停用", fontSize = 9.sp, color = TextSecondary, letterSpacing = 1.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        scopeDesc(budget, accounts),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${(pct * 100).toInt()}%",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                    Text(
                        periodLabel(budget.period),
                        fontSize = 9.sp,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            // 进度条（霓虹）
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
                        .fillMaxWidth(pct.coerceIn(0.02f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(statusColor.copy(alpha = 0.6f), statusColor)
                            )
                        )
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${money(progress?.used ?: 0.0)} / ${money(budget.amount)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Spacer(Modifier.weight(1f))
                ActionPill(
                    label = if (budget.isActive) "停用" else "启用",
                    color = if (budget.isActive) TextSecondary else NeonGreen,
                    onClick = onToggleActive
                )
                Spacer(Modifier.width(6.dp))
                ActionIcon(icon = Icons.Default.Edit, color = NeonPurple, onClick = onClickEdit)
                Spacer(Modifier.width(6.dp))
                ActionIcon(icon = Icons.Default.Delete, color = NeonRed, onClick = onRequestDelete)
            }
        }
    }
}

@Composable
private fun ActionPill(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 1.sp)
    }
}

@Composable
private fun ActionIcon(icon: ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 空态：霓虹光环 + 飞舞钞票
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun BudgetEmptyState(glowAlpha: Float, onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 霓虹光环 + emoji
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 双层光晕
                drawCircle(
                    color = NeonCyan.copy(alpha = glowAlpha * 0.18f),
                    radius = size.minDimension / 2f
                )
                drawCircle(
                    color = NeonPurple.copy(alpha = glowAlpha * 0.15f),
                    radius = size.minDimension / 2.4f
                )
                drawArc(
                    color = NeonCyan.copy(alpha = 0.5f * glowAlpha),
                    startAngle = -90f, sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 2f, cap = StrokeCap.Round),
                    topLeft = Offset(20f, 20f),
                    size = Size(size.width - 40f, size.height - 40f)
                )
                drawArc(
                    color = NeonPurple.copy(alpha = 0.4f * glowAlpha),
                    startAngle = 60f, sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = 1.5f, cap = StrokeCap.Round),
                    topLeft = Offset(35f, 35f),
                    size = Size(size.width - 70f, size.height - 70f)
                )
            }
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(DarkCardLight, DarkCard))
                    )
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f * glowAlpha), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("💸", fontSize = 56.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "BUDGET · STANDBY",
            fontSize = 11.sp, color = NeonPurple.copy(alpha = 0.8f),
            letterSpacing = 4.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "尚未配置任何预算",
            fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "为不同分类 / 账户 / 周期定预算\n超 80% 警示 · 超额自动飘红",
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(50.dp)
                .shadow(14.dp, RoundedCornerShape(25.dp), spotColor = NeonPink)
                .clip(RoundedCornerShape(25.dp))
                .background(
                    Brush.horizontalGradient(listOf(NeonPink, NeonPurple, NeonCyan))
                )
                .clickable { onCreate() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("✨", fontSize = 16.sp)
                Text(
                    "立即创建预算",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 编辑/新增 弹层（科技风）
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun BudgetEditorDialog(
    initial: Budget?,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (Budget) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "月度总预算") }
    var amountText by remember { mutableStateOf(initial?.amount?.let { String.format(Locale.CHINA, "%.0f", it) } ?: "") }
    var scopeSel by remember { mutableStateOf(initial?.scope ?: BudgetScope.TOTAL) }
    var periodSel by remember { mutableStateOf(initial?.period ?: BudgetPeriod.MONTHLY) }
    var categoryKey by remember { mutableStateOf(initial?.targetKey?.takeIf { initial.scope == BudgetScope.CATEGORY } ?: "餐饮") }
    var accountKey by remember { mutableStateOf(initial?.targetKey?.takeIf { initial.scope == BudgetScope.ACCOUNT }) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ResponsiveDialogBox {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkBg)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(DarkCardLight, DarkCard)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (initial == null) "✨" else "✏️", fontSize = 18.sp)
                            Text(
                                if (initial == null) "新建预算" else "编辑预算",
                                fontSize = 18.sp, fontWeight = FontWeight.Black,
                                color = TextPrimary, letterSpacing = 2.sp
                            )
                        }
                        Text(
                            "BUDGET · CONFIG",
                            fontSize = 9.sp, color = NeonPurple.copy(alpha = 0.7f),
                            letterSpacing = 4.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FieldLabel("名称")
                    NeonInput(value = name, onChange = { name = it.take(20) }, placeholder = "例如：月度总预算")

                    FieldLabel("金额（元）")
                    NeonInput(
                        value = amountText,
                        onChange = { v -> amountText = v.filter { it.isDigit() || it == '.' }.take(10) },
                        placeholder = "例如：3000",
                        keyboardType = KeyboardType.Number
                    )

                    FieldLabel("周期")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            BudgetPeriod.MONTHLY to "📅 月",
                            BudgetPeriod.WEEKLY to "🗓️ 周",
                            BudgetPeriod.YEARLY to "📆 年"
                        ).forEach { (k, lbl) ->
                            NeonChip(label = lbl, selected = periodSel == k, modifier = Modifier.weight(1f), onClick = { periodSel = k })
                        }
                    }

                    FieldLabel("范围")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            BudgetScope.TOTAL to "💰 总",
                            BudgetScope.CATEGORY to "🏷️ 分类",
                            BudgetScope.ACCOUNT to "🏦 账户"
                        ).forEach { (k, lbl) ->
                            NeonChip(label = lbl, selected = scopeSel == k, modifier = Modifier.weight(1f), onClick = { scopeSel = k })
                        }
                    }

                    if (scopeSel == BudgetScope.CATEGORY) {
                        FieldLabel("分类")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("餐饮", "交通", "购物", "娱乐").forEach { c ->
                                NeonChip(label = c, selected = categoryKey == c, modifier = Modifier.weight(1f), onClick = { categoryKey = c })
                            }
                        }
                        NeonInput(value = categoryKey, onChange = { categoryKey = it.take(10) }, placeholder = "或自定义分类名")
                    }
                    if (scopeSel == BudgetScope.ACCOUNT) {
                        FieldLabel("账户")
                        if (accounts.isEmpty()) {
                            Text("尚无账户", fontSize = 12.sp, color = TextSecondary)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                accounts.forEach { a ->
                                    val sel = accountKey == a.id.toString()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (sel) NeonCyan.copy(alpha = 0.10f) else DarkCard)
                                            .border(
                                                if (sel) 1.5.dp else 1.dp,
                                                if (sel) NeonCyan else GlowLine,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { accountKey = a.id.toString() }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(a.icon, fontSize = 16.sp)
                                        Text(a.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(DarkCard)
                                .border(1.dp, GlowLine, RoundedCornerShape(23.dp))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("取消", color = TextSecondary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .shadow(12.dp, RoundedCornerShape(23.dp), spotColor = NeonPink)
                                .clip(RoundedCornerShape(23.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(NeonPink, NeonPurple, NeonCyan))
                                )
                                .clickable {
                                    val amount = amountText.toDoubleOrNull()
                                    if (amount == null || amount <= 0.0) return@clickable
                                    val targetKey = when (scopeSel) {
                                        BudgetScope.CATEGORY -> categoryKey.takeIf { it.isNotBlank() }
                                        BudgetScope.ACCOUNT -> accountKey
                                        else -> null
                                    }
                                    val color: Long = when (scopeSel) {
                                        BudgetScope.TOTAL -> 0xFFFF6B9DL
                                        BudgetScope.CATEGORY -> 0xFFFFA726L
                                        BudgetScope.ACCOUNT -> 0xFF7E57C2L
                                        else -> 0xFFFF8A80L
                                    }
                                    val newBudget = (initial ?: Budget(userId = 0L, name = name, amount = amount)).copy(
                                        name = name.ifBlank { defaultName(scopeSel, targetKey) },
                                        amount = amount,
                                        scope = scopeSel,
                                        period = periodSel,
                                        targetKey = targetKey,
                                        color = color,
                                        isActive = true
                                    )
                                    onConfirm(newBudget)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (initial == null) "创建预算" else "保存修改",
                                color = Color.White, fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// 工具
// ═════════════════════════════════════════════════════════════════════════
@Composable
private fun FieldLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(3.dp, 12.dp).clip(RoundedCornerShape(1.5.dp)).background(NeonCyan))
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextSecondary, letterSpacing = 2.sp)
    }
}

@Composable
private fun NeonInput(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(1.dp, GlowLine, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
            modifier = Modifier.fillMaxWidth()
        )
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 15.sp, color = TextSecondary.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun NeonChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.18f), NeonPurple.copy(alpha = 0.18f)))
                else Brush.linearGradient(listOf(DarkCard, DarkCard))
            )
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) NeonCyan else GlowLine,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, fontSize = 12.sp, fontWeight = FontWeight.Black,
            color = if (selected) NeonCyan else TextSecondary,
            letterSpacing = 1.sp
        )
    }
}

private fun mapBudgetColorToNeon(rawColor: Long): Color {
    // 把 Phase 2B 的明亮粉/橘/紫映射到霓虹版
    return when (rawColor) {
        0xFFFF6B9DL -> NeonPink
        0xFFFFA726L -> NeonOrange
        0xFF7E57C2L -> NeonPurple
        else -> NeonCyan
    }
}

private fun scopeEmoji(scope: String): String = when (scope) {
    BudgetScope.TOTAL -> "💎"
    BudgetScope.CATEGORY -> "🏷️"
    BudgetScope.ACCOUNT -> "🏦"
    else -> "📊"
}

private fun scopeDesc(b: Budget, accounts: List<Account>): String {
    val pl = periodLabel(b.period)
    return when (b.scope) {
        BudgetScope.TOTAL -> "TOTAL · $pl"
        BudgetScope.CATEGORY -> "${b.targetKey ?: "-"} · $pl"
        BudgetScope.ACCOUNT -> {
            val aid = b.targetKey?.toLongOrNull()
            val name = accounts.firstOrNull { it.id == aid }?.let { "${it.icon} ${it.name}" } ?: "未知账户"
            "$name · $pl"
        }
        else -> pl
    }
}

private fun periodLabel(p: String): String = when (p) {
    BudgetPeriod.WEEKLY -> "每周"
    BudgetPeriod.YEARLY -> "每年"
    else -> "每月"
}

private fun defaultName(scope: String, key: String?): String = when (scope) {
    BudgetScope.TOTAL -> "总预算"
    BudgetScope.CATEGORY -> "${key ?: "分类"} 预算"
    BudgetScope.ACCOUNT -> "账户预算"
    else -> "预算"
}

private fun money(v: Double): String {
    val abs = kotlin.math.abs(v)
    return if (abs >= 10_000) String.format(Locale.CHINA, "¥%.1f万", abs / 10_000)
    else String.format(Locale.CHINA, "¥%.0f", abs)
}
