// ════════════════════════════════════════════════════════════════════════
// GoalScreen.kt —目标 & 倒数日（重写版）
// 参考：docs/屏幕适配指南.md / DEVELOPMENT_PRINCIPLES.md
// ════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.data.GoalAchievementStore
import com.example.funlife.data.GoalIconStore
import com.example.funlife.data.model.Countdown
import com.example.funlife.data.model.Goal
import com.example.funlife.ui.utils.Radius
import com.example.funlife.ui.utils.ResponsiveDialogBox
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.ui.utils.bottomTabContentPadding
import com.example.funlife.ui.utils.rdp
import com.example.funlife.ui.utils.rsp
import com.example.funlife.viewmodel.GoalViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import kotlin.math.max

// ── emoji 在圆/方块内垂直居──
private val EmojiCenteredStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

// ════════════════════════════════════════════════════════════════════════
// 自绘图标：靶/ 沙漏 / 奖杯 / 时钟（替emoji，避免厂商字体丑陋渲染）
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun TargetIcon(
    size: Dp,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") showDart: Boolean = true
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        // —山峰 + 旗帜 风格（更优雅、辨识度高） —
        // 山体（双峰，橙渐变）
        val mountain = Path().apply {
            moveTo(s * 0.04f, s * 0.86f)
            lineTo(s * 0.34f, s * 0.46f)
            lineTo(s * 0.50f, s * 0.66f)
            lineTo(s * 0.66f, s * 0.40f)
            lineTo(s * 0.96f, s * 0.86f)
            close()
        }
        drawPath(
            mountain,
            brush = Brush.verticalGradient(
                listOf(Color(0xFFFF8FB1), Color(0xFFFF6F91)),
                startY = s * 0.40f,
                endY = s * 0.90f
            )
        )
        // 雪顶高光
        val snow = Path().apply {
            moveTo(s * 0.60f, s * 0.46f)
            lineTo(s * 0.66f, s * 0.40f)
            lineTo(s * 0.74f, s * 0.50f)
            lineTo(s * 0.66f, s * 0.54f)
            close()
        }
        drawPath(snow, color = Color.White.copy(alpha = 0.85f))
        // 旗杆
        val poleX = s * 0.66f
        drawLine(
            color = Color(0xFF263238),
            start = Offset(poleX, s * 0.40f),
            end = Offset(poleX, s * 0.10f),
            strokeWidth = s * 0.05f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        // 旗帜（三角，橙黄渐变
        val flag = Path().apply {
            moveTo(poleX, s * 0.12f)
            lineTo(s * 0.94f, s * 0.20f)
            lineTo(poleX, s * 0.30f)
            close()
        }
        drawPath(
            flag,
            brush = Brush.linearGradient(
                listOf(Color(0xFFFFB300), Color(0xFFFF6F91)),
                start = Offset(poleX, 0f),
                end = Offset(s * 0.94f, s * 0.30f)
            )
        )
        // 旗顶小球
        drawCircle(
            color = Color(0xFFFFD54F),
            radius = s * 0.045f,
            center = Offset(poleX, s * 0.10f)
        )
        // 地面阴影
        drawOval(
            color = Color(0x33000000),
            topLeft = Offset(s * 0.20f, s * 0.86f),
            size = Size(s * 0.60f, s * 0.06f)
        )
    }
}

@Composable
private fun HourglassIcon(
    size: Dp,
    modifier: Modifier = Modifier,
    sandTop: Color = Color(0xFFFFB199),
    sandBottom: Color = Color(0xFFFF8F4F),
    frame: Color = Color(0xFF7E57C2)
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val pad = s * 0.10f
        val w = s - pad * 2f
        val h = s - pad * 2f
        val barH = h * 0.10f
        // 上下横杆
        drawRoundRect(
            color = frame,
            topLeft = Offset(pad, pad),
            size = Size(w, barH),
            cornerRadius = CornerRadius(barH / 2f, barH / 2f)
        )
        drawRoundRect(
            color = frame,
            topLeft = Offset(pad, pad + h - barH),
            size = Size(w, barH),
            cornerRadius = CornerRadius(barH / 2f, barH / 2f)
        )
        val cx = pad + w / 2f
        val topY = pad + barH
        val botY = pad + h - barH
        val midY = (topY + botY) / 2f
        // 玻璃外形（上下三角合体）
        val glass = Path().apply {
            moveTo(pad + w * 0.08f, topY)
            lineTo(pad + w * 0.92f, topY)
            lineTo(cx + w * 0.04f, midY)
            lineTo(pad + w * 0.92f, botY)
            lineTo(pad + w * 0.08f, botY)
            lineTo(cx - w * 0.04f, midY)
            close()
        }
        drawPath(glass, color = Color.White.copy(alpha = 0.55f))
        // 上层沙（上三角填充约 35%
        val topSand = Path().apply {
            val depth = (midY - topY) * 0.35f
            moveTo(pad + w * 0.18f, topY + depth * 0.2f)
            lineTo(pad + w * 0.82f, topY + depth * 0.2f)
            lineTo(cx + w * 0.025f, midY)
            lineTo(cx - w * 0.025f, midY)
            close()
        }
        drawPath(
            topSand,
            brush = Brush.verticalGradient(listOf(sandTop, sandBottom))
        )
        // 下层沙堆
        val botSand = Path().apply {
            val rise = (botY - midY) * 0.55f
            moveTo(cx - w * 0.025f, midY)
            lineTo(cx + w * 0.025f, midY)
            lineTo(pad + w * 0.85f, botY - rise * 0.2f)
            lineTo(pad + w * 0.78f, botY)
            lineTo(pad + w * 0.22f, botY)
            lineTo(pad + w * 0.15f, botY - rise * 0.2f)
            close()
        }
        drawPath(
            botSand,
            brush = Brush.verticalGradient(listOf(sandTop, sandBottom))
        )
        // 中部细沙
        drawLine(
            color = sandBottom,
            start = Offset(cx, midY - s * 0.04f),
            end = Offset(cx, midY + s * 0.10f),
            strokeWidth = s * 0.025f
        )
        // 玻璃描边
        drawPath(glass, color = frame, style = Stroke(width = s * 0.035f))
    }
}

@Composable
private fun TrophyIcon(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val gold = Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFB300)))
        // 杯体
        val body = Path().apply {
            moveTo(s * 0.25f, s * 0.18f)
            lineTo(s * 0.75f, s * 0.18f)
            lineTo(s * 0.70f, s * 0.55f)
            cubicTo(s * 0.70f, s * 0.70f, s * 0.30f, s * 0.70f, s * 0.30f, s * 0.55f)
            close()
        }
        drawPath(body, brush = gold)
        // 把手
        drawArc(
            color = Color(0xFFFFB300),
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(s * 0.06f, s * 0.22f),
            size = Size(s * 0.22f, s * 0.30f),
            style = Stroke(width = s * 0.05f)
        )
        drawArc(
            color = Color(0xFFFFB300),
            startAngle = 270f,
            sweepAngle = -180f,
            useCenter = false,
            topLeft = Offset(s * 0.72f, s * 0.22f),
            size = Size(s * 0.22f, s * 0.30f),
            style = Stroke(width = s * 0.05f)
        )
        // 底座
        drawRoundRect(
            color = Color(0xFFFFB300),
            topLeft = Offset(s * 0.40f, s * 0.68f),
            size = Size(s * 0.20f, s * 0.10f),
            cornerRadius = CornerRadius(s * 0.02f, s * 0.02f)
        )
        drawRoundRect(
            color = Color(0xFF8D6E63),
            topLeft = Offset(s * 0.28f, s * 0.78f),
            size = Size(s * 0.44f, s * 0.10f),
            cornerRadius = CornerRadius(s * 0.04f, s * 0.04f)
        )
        // 高光
        drawCircle(
            color = Color.White.copy(alpha = 0.40f),
            radius = s * 0.06f,
            center = Offset(s * 0.40f, s * 0.32f)
        )
    }
}

// 切换 Tab 时的图标
@Composable
private fun TabIcon(tab: GoalTab, size: Dp, modifier: Modifier = Modifier) {
    when (tab) {
        GoalTab.GOALS -> TargetIcon(size, modifier)
        GoalTab.COUNTDOWNS -> HourglassIcon(size, modifier)
    }
}

// ── 设计令牌：分──
private data class GoalCategory(val name: String, val emoji: String, val color: Color)

private val GoalCategories = listOf(
    GoalCategory("学习", "📚", Color(0xFF4FC3F7)),
    GoalCategory("工作", "💼", Color(0xFF7E57C2)),
    GoalCategory("健康", "💪", Color(0xFF66BB6A)),
    GoalCategory("财务", "💰", Color(0xFFFFA726)),
    GoalCategory("生活", "🏠", Color(0xFFFF6F91)),
    GoalCategory("旅行", "✈️", Color(0xFF26C6DA)),
    GoalCategory("创作", "🎨", Color(0xFFFF8A65)),
    GoalCategory("其他", "✨", Color(0xFF9E9E9E))
)

private val CountdownCategories = listOf(
    GoalCategory("节日", "🎉", Color(0xFFFF6F91)),
    GoalCategory("生日", "🎂", Color(0xFFFF8F4F)),
    GoalCategory("旅行", "✈️", Color(0xFF26C6DA)),
    GoalCategory("考试", "📝", Color(0xFFFFA726)),
    GoalCategory("纪念", "❤️", Color(0xFFE91E63)),
    GoalCategory("截止", "⏰", Color(0xFF7E57C2)),
    GoalCategory("其他", "✨", Color(0xFF9E9E9E))
)

private val CountdownPalette = listOf(
    "#FF6F91", "#FF8F4F", "#FFA726", "#66BB6A",
    "#26C6DA", "#4FC3F7", "#7E57C2", "#E91E63"
)

private fun goalCategoryOf(name: String): GoalCategory =
    GoalCategories.firstOrNull { it.name == name } ?: GoalCategories.last()

private fun countdownCategoryOf(name: String): GoalCategory =
    CountdownCategories.firstOrNull { it.name == name } ?: CountdownCategories.last()

private fun parseHexColor(hex: String, fallback: Color = Color(0xFFFF6F91)): Color = try {
    Color(android.graphics.Color.parseColor(hex.takeIf { it.isNotBlank() } ?: "#FF6F91"))
} catch (_: Throwable) { fallback }

private enum class GoalTab(val label: String, val emoji: String) {
    GOALS("目标", "🎯"),
    COUNTDOWNS("倒数日", "⏰")
}

// ════════════════════════════════════════════════════════════════════════
// 顶层入口
// ════════════════════════════════════════════════════════════════════════
@Composable
fun GoalScreen(onNavigateBack: () -> Unit) {
    val viewModel: GoalViewModel = viewModel()
    val activeGoals by viewModel.activeGoals.collectAsState()
    val completedGoals by viewModel.completedGoals.collectAsState()
    val countdowns by viewModel.countdowns.collectAsState()

    // 切换账号后保险刷新一
    LaunchedEffect(Unit) { viewModel.refreshForNewUser() }

    var selectedTab by remember { mutableStateOf(GoalTab.GOALS) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }
    var showAddGoal by remember { mutableStateOf(false) }
    var editingCountdown by remember { mutableStateOf<Countdown?>(null) }
    var showAddCountdown by remember { mutableStateOf(false) }

    // 扩展模块状
    var filter by remember { mutableStateOf(GoalFilterState()) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showDashboard by remember { mutableStateOf(false) }
    var showAchievementWall by remember { mutableStateOf(false) }
    var detailCountdown by remember { mutableStateOf<Countdown?>(null) }
    val ctx = LocalContext.current
    val userId = rememberCurrentUserId()

    // 实时评估成就
    LaunchedEffect(activeGoals.size, completedGoals.size, countdowns.size) {
        GoalAchievementStore.evaluate(
            ctx, userId,
            totalGoals = activeGoals.size + completedGoals.size,
            completedGoals = completedGoals.size,
            countdownTotal = countdowns.size
        )
    }

    // 应用筛排序
    val filteredActive = remember(activeGoals, filter) { applyFilter(activeGoals, filter) }
    val filteredCompleted = remember(completedGoals, filter) { applyFilter(completedGoals, filter) }
    val filteredCountdowns = remember(countdowns, filter) {
        val list = applyCountdownFilter(countdowns, filter)
        when (filter.sort) {
            GoalSortMode.DEADLINE -> list.sortedBy { runCatching { it.getDaysRemaining() }.getOrNull() ?: Long.MAX_VALUE }
            GoalSortMode.PROGRESS -> list.sortedBy { runCatching { it.getDaysRemaining() }.getOrNull() ?: Long.MAX_VALUE }
            GoalSortMode.CREATED -> list.sortedByDescending { it.createdAt }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFF1F2),
                        Color(0xFFFFE8E0),
                        Color(0xFFEAE6FB)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ─── 固定头部：导+ 工具栏胶+ （可选）搜索 ───
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFF1F2).copy(alpha = 0.96f), Color(0xFFFFF1F2).copy(alpha = 0.85f))
                        )
                    )
            ) {
                GoalTopBar(
                    onBack = onNavigateBack,
                    onAdd = {
                        if (selectedTab == GoalTab.GOALS) showAddGoal = true
                        else showAddCountdown = true
                    },
                    onToggleSearch = { showSearchBar = !showSearchBar; if (!showSearchBar) filter = filter.copy(query = "", category = null) },
                    searchActive = showSearchBar,
                    onDashboard = { showDashboard = true },
                    onAchievement = { showAchievementWall = true }
                )
                if (showSearchBar) {
                    val cats = if (selectedTab == GoalTab.GOALS)
                        GoalCategories.map { it.name to it.color }
                    else
                        CountdownCategories.map { it.name to it.color }
                    Spacer(Modifier.height(Spacing.xs))
                    Box(modifier = Modifier.padding(horizontal = Spacing.sm)) {
                        FilterSearchBar(state = filter, onState = { filter = it }, categories = cats)
                    }
                    Spacer(Modifier.height(Spacing.xs))
                }
            }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = bottomTabContentPadding(top = Spacing.none, horizontal = Spacing.none),
            verticalArrangement = Arrangement.spacedBy(Spacing.none)
        ) {
            item(key = "_hero") {
                GoalHeroPanel(
                    tab = selectedTab,
                    activeCount = activeGoals.size,
                    completedCount = completedGoals.size,
                    avgProgress = if (activeGoals.isEmpty()) 0
                        else activeGoals.sumOf { it.progress } / activeGoals.size,
                    countdownTotal = countdowns.size,
                    nearestDays = countdowns
                        .map { runCatching { it.getDaysRemaining() }.getOrNull() }
                        .filterNotNull()
                        .filter { it >= 0 }
                        .minOrNull(),
                    expiredCount = countdowns.count {
                        runCatching { it.getDaysRemaining() }.getOrNull()?.let { d -> d < 0 } ?: false
                    }
                )
            }
            item(key = "_tabs") {
                GoalTabSwitcher(selected = selectedTab, onSelect = { selectedTab = it })
            }
            item(key = "_content") {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 6 })
                            .togetherWith(fadeOut(tween(180)))
                    },
                    label = "tabSwap"
                ) { tab ->
                    when (tab) {
                        GoalTab.GOALS -> GoalsList(
                            active = filteredActive,
                            completed = filteredCompleted,
                            onTap = { editingGoal = it },
                            onToggle = { viewModel.toggleGoalCompletion(it) },
                            onDelete = { viewModel.deleteGoal(it) },
                            onAdjustProgress = { goal, p -> viewModel.setGoalProgress(goal, p) }
                        )
                        GoalTab.COUNTDOWNS -> CountdownsList(
                            countdowns = filteredCountdowns,
                            onTap = { detailCountdown = it },
                            onDelete = { viewModel.deleteCountdown(it) }
                        )
                    }
                }
            }
        }
        }
    }

    // ─── 弹窗 ───
    if (showAddGoal) {
        GoalEditorDialog(
            initial = null,
            initialIcon = null,
            onDismiss = { showAddGoal = false },
            onSave = { title, desc, cat, date, prog, icon ->
                viewModel.addGoal(title, cat, date, desc, prog, icon)
                showAddGoal = false
            }
        )
    }
    editingGoal?.let { g ->
        val savedIcon = remember(g.id) { GoalIconStore.get(ctx, userId, g.id) }
        GoalEditorDialog(
            initial = g,
            initialIcon = savedIcon,
            onDismiss = { editingGoal = null },
            onSave = { title, desc, cat, date, prog, icon ->
                viewModel.updateGoal(g, title, desc, cat, date, prog, icon)
                editingGoal = null
            }
        )
    }
    if (showAddCountdown) {
        CountdownEditorDialog(
            initial = null,
            onDismiss = { showAddCountdown = false },
            onSave = { title, date, cat, icon, color, note ->
                viewModel.addCountdown(title, date, cat, icon, color, note)
                showAddCountdown = false
            }
        )
    }
    editingCountdown?.let { c ->
        CountdownEditorDialog(
            initial = c,
            onDismiss = { editingCountdown = null },
            onSave = { title, date, cat, icon, color, note ->
                viewModel.updateCountdown(c, title, date, cat, icon, color, note)
                editingCountdown = null
            }
        )
    }
    if (showDashboard) {
        GoalDashboardDialog(
            activeGoals = activeGoals,
            completedGoals = completedGoals,
            countdowns = countdowns,
            categoryColorOf = { name -> goalCategoryOf(name).color },
            onDismiss = { showDashboard = false }
        )
    }
    if (showAchievementWall) {
        AchievementWallDialog(onDismiss = { showAchievementWall = false })
    }
    detailCountdown?.let { c ->
        CountdownDetailDialog(
            countdown = c,
            onEdit = {
                detailCountdown = null
                editingCountdown = c
            },
            onDelete = {
                viewModel.deleteCountdown(c)
                detailCountdown = null
            },
            onDismiss = { detailCountdown = null },
            parseColor = { hex -> parseHexColor(hex) }
        )
    }
}

// ════════════════════════════════════════════════════════════════════════
// 顶部
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun GoalTopBar(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onToggleSearch: () -> Unit,
    searchActive: Boolean,
    onDashboard: () -> Unit,
    onAchievement: () -> Unit
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = statusTop + Spacing.xs, start = Spacing.sm, end = Spacing.sm, bottom = Spacing.xs)
    ) {
        // ─── Row 1：返+ 标题 ───
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.rdp)
                    .shadow(2.dp, CircleShape, spotColor = Color(0xFFFF6F91))
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFFFD9E2), CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, "返回", tint = Color(0xFF3D1F2C), modifier = Modifier.size(18.rdp))
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "目标 · 倒数日",
                    fontSize = TextSize.headline,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF3D1F2C),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "把心愿变成可执行的小步骤",
                    fontSize = TextSize.tiny,
                    color = Color(0xFFAD6584),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // 新增按钮（渐变胶囊）
            Box(
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(Radius.pill), spotColor = Color(0xFFFF6F91))
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFF6F91), Color(0xFFFF8F4F))))
                    .clickable { onAdd() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.rdp))
                    Spacer(Modifier.width(3.dp))
                    Text("新增", color = Color.White, fontSize = TextSize.sm, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // ─── Row 2：工具栏胶囊 ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(Radius.pill), spotColor = Color(0xFFFF6F91).copy(alpha = 0.4f))
                .clip(RoundedCornerShape(Radius.pill))
                .background(Color.White)
                .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.pill))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarTab(
                label = "搜索",
                icon = Icons.Default.Search,
                accent = Color(0xFFFF6F91),
                active = searchActive,
                onClick = onToggleSearch,
                modifier = Modifier.weight(1f)
            )
            ToolbarTab(
                label = "看板",
                emoji = "📊",
                accent = Color(0xFF7E57C2),
                onClick = onDashboard,
                modifier = Modifier.weight(1f)
            )
            ToolbarTab(
                label = "成就",
                emoji = "🏆",
                accent = Color(0xFFFFB300),
                onClick = onAchievement,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ToolbarTab(
    label: String,
    icon: ImageVector? = null,
    emoji: String? = null,
    accent: Color,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(if (active) accent.copy(alpha = 0.14f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (active) accent else Color(0xFF6B4F5A), modifier = Modifier.size(15.rdp))
        } else if (emoji != null) {
            Text(emoji, fontSize = 14.rsp, style = EmojiCenteredStyle)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            fontSize = TextSize.tiny,
            color = if (active) accent else Color(0xFF6B4F5A),
            fontWeight = FontWeight.Black
        )
    }
}

// ════════════════════════════════════════════════════════════════════════
// Hero 数据面板（按 Tab 切换
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun GoalHeroPanel(
    tab: GoalTab,
    activeCount: Int,
    completedCount: Int,
    avgProgress: Int,
    countdownTotal: Int,
    nearestDays: Long?,
    expiredCount: Int
) {
    val accents = if (tab == GoalTab.GOALS)
        listOf(Color(0xFFFF8FB1), Color(0xFFFFAA85))
    else
        listOf(Color(0xFFB892E8), Color(0xFFFF8FB1))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            .shadow(8.dp, RoundedCornerShape(Radius.xxl), spotColor = accents.first())
            .clip(RoundedCornerShape(Radius.xxl))
            .background(Brush.linearGradient(accents))
    ) {
        // 装饰
        Box(
            modifier = Modifier
                .size(120.rdp)
                .align(Alignment.TopEnd)
                .offset(x = 20.rdp, y = (-30).rdp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
        )
        Box(
            modifier = Modifier
                .size(70.rdp)
                .align(Alignment.BottomStart)
                .offset(x = (-15).rdp, y = 15.rdp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        )

        Column(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.rdp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f))
                        .padding(4.rdp),
                    contentAlignment = Alignment.Center
                ) {
                    TabIcon(tab = tab, size = 28.rdp)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    if (tab == GoalTab.GOALS) "我的目标" else "我的倒数日",
                    fontSize = TextSize.title,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                if (tab == GoalTab.GOALS) "持之以恒，让每一步都看得见进步" else "提醒自己：值得期待的日子在这里",
                fontSize = TextSize.xs,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (tab == GoalTab.GOALS) {
                    HeroStatChip("$activeCount", "进行中", Modifier.weight(1f))
                    HeroStatChip("$completedCount", "已达成", Modifier.weight(1f))
                    HeroStatChip("$avgProgress%", "平均进度", Modifier.weight(1f))
                } else {
                    HeroStatChip("$countdownTotal", "总数", Modifier.weight(1f))
                    HeroStatChip(
                        nearestDays?.let { if (it == 0L) "今天" else "${it}天" } ?: "—",
                        "距最近",
                        Modifier.weight(1f)
                    )
                    HeroStatChip("$expiredCount", "已过期", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeroStatChip(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.lg))
            .background(Color.White.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(Radius.lg))
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(value, fontSize = 18.rsp, color = Color.White, fontWeight = FontWeight.Black)
            Text(label, fontSize = TextSize.tiny, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
// Tab 切换器（胶囊滑块
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun GoalTabSwitcher(selected: GoalTab, onSelect: (GoalTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            .clip(RoundedCornerShape(Radius.pill))
            .background(Color.White.copy(alpha = 0.85f))
            .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.pill))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GoalTab.values().forEach { tab ->
            val active = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Radius.pill))
                    .then(
                        if (active) Modifier.background(
                            Brush.horizontalGradient(listOf(Color(0xFFFF6F91), Color(0xFFFF8F4F)))
                        ) else Modifier
                    )
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TabIcon(tab = tab, size = 18.rdp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        tab.label,
                        fontSize = TextSize.sm,
                        color = if (active) Color.White else Color(0xFF8B5670),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
// 目标列表
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun GoalsList(
    active: List<Goal>,
    completed: List<Goal>,
    onTap: (Goal) -> Unit,
    onToggle: (Goal) -> Unit,
    onDelete: (Goal) -> Unit,
    onAdjustProgress: (Goal, Int) -> Unit
) {
    if (active.isEmpty() && completed.isEmpty()) {
        EmptyStateBox(
            title = "还没有目标",
            subtitle = "把心愿写下来，就开始了一半\n点击右上角「新增」添加第一个目标"
        ) { TargetIcon(size = 64.rdp) }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        if (active.isNotEmpty()) {
            SectionHeader("进行中", count = active.size)
            active.forEach { g ->
                GoalCard(
                    goal = g,
                    onTap = { onTap(g) },
                    onToggle = { onToggle(g) },
                    onDelete = { onDelete(g) },
                    onAdjustProgress = { p -> onAdjustProgress(g, p) }
                )
            }
        }
        if (completed.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xs))
            SectionHeader("已达成", count = completed.size, accent = Color(0xFF66BB6A))
            completed.forEach { g ->
                GoalCard(
                    goal = g,
                    onTap = { onTap(g) },
                    onToggle = { onToggle(g) },
                    onDelete = { onDelete(g) },
                    onAdjustProgress = null
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, accent: Color = Color(0xFFFF6F91)) {
    // 去掉传入emoji 前缀
    val cleanTitle = title.replace(Regex("^[\\p{So}\\p{Cn}\\s]+"), "").trim().ifBlank { title }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧色条
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.rdp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.6f))))
        )
        Spacer(Modifier.width(8.dp))
        Text(cleanTitle, fontSize = TextSize.title, fontWeight = FontWeight.Black, color = Color(0xFF3D1F2C), letterSpacing = 0.5.sp)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .shadow(2.dp, RoundedCornerShape(Radius.pill), spotColor = accent)
                .clip(RoundedCornerShape(Radius.pill))
                .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.78f))))
                .padding(horizontal = 9.dp, vertical = 2.dp)
        ) {
            Text("$count", fontSize = TextSize.tiny, color = Color.White, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun EmptyStateBox(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md)
            .clip(RoundedCornerShape(Radius.xl))
            .background(Brush.linearGradient(listOf(Color(0xFFFFF1F2), Color(0xFFFFFBF7))))
            .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.xl))
            .padding(vertical = 36.dp, horizontal = Spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 图标盘：浅色光晕底盘衬托
            Box(
                modifier = Modifier
                    .size(96.rdp)
                    .shadow(10.dp, CircleShape, spotColor = Color(0xFFFF8FB1))
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFFF6F1), Color(0xFFFFE4EC))
                        )
                    )
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(Modifier.height(Spacing.md))
            Text(title, fontSize = TextSize.title, fontWeight = FontWeight.Black, color = Color(0xFF3D1F2C))
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                fontSize = TextSize.xs,
                color = Color(0xFFAD6584),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 18.rsp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
// 目标卡片
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun GoalCard(
    goal: Goal,
    onTap: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onAdjustProgress: ((Int) -> Unit)?
) {
    val cat = goalCategoryOf(goal.category)
    val completed = goal.isCompleted
    val ctxLocal = LocalContext.current
    val cardBg = if (completed)
        Brush.linearGradient(listOf(Color(0xFFFAF6FF), Color(0xFFF5F0FA)))
    else
        Brush.linearGradient(listOf(Color.White, Color(0xFFFFFAF7)))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(Radius.xl), spotColor = cat.color)
            .clip(RoundedCornerShape(Radius.xl))
            .background(cardBg)
            .border(1.dp, cat.color.copy(alpha = if (completed) 0.18f else 0.30f), RoundedCornerShape(Radius.xl))
            .clickable { onTap() }
    ) {
        // 左侧颜色竖条
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .height(56.rdp)
                .width(4.dp)
                .clip(RoundedCornerShape(topEnd = Radius.sm, bottomEnd = Radius.sm))
                .background(cat.color)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 分类 emoji 圆头（支持自定义上传图）
                val customIcon: String? = remember(goal.id) { GoalIconStore.get(ctxLocal, goal.userId, goal.id) }
                val isCustomFile = customIcon != null && (customIcon.startsWith("file:") || customIcon.startsWith("/"))
                Box(
                    modifier = Modifier
                        .size(40.rdp)
                        .shadow(3.dp, CircleShape, spotColor = cat.color)
                        .clip(CircleShape)
                        .background(
                            if (isCustomFile) Brush.linearGradient(listOf(Color.White, Color.White))
                            else Brush.linearGradient(listOf(cat.color, cat.color.copy(alpha = 0.78f)))
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCustomFile) {
                        val path = customIcon!!.removePrefix("file:")
                        coil.compose.AsyncImage(
                            model = java.io.File(path as String),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.size(36.rdp).clip(CircleShape)
                        )
                    } else if (!customIcon.isNullOrBlank()) {
                        Text(customIcon as String, fontSize = 18.rsp, style = EmojiCenteredStyle)
                    } else {
                        Text(cat.emoji, fontSize = 18.rsp, style = EmojiCenteredStyle)
                    }
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        goal.title,
                        fontSize = TextSize.title,
                        fontWeight = FontWeight.Black,
                        color = if (completed) Color(0xFFA08AB5) else Color(0xFF3D1F2C),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MetaPill(text = cat.name, color = cat.color)
                        Spacer(Modifier.width(6.dp))
                        goal.targetDate?.takeIf { it.isNotBlank() }?.let { d ->
                            DatePill(date = d, color = Color(0xFF7E57C2))
                        }
                    }
                }
                // 切换完成
                IconCircleButton(
                    icon = if (completed) Icons.Default.Refresh else Icons.Default.Check,
                    accent = if (completed) Color(0xFF9E9E9E) else Color(0xFF66BB6A),
                    onClick = onToggle,
                    filled = !completed
                )
                Spacer(Modifier.width(6.dp))
                IconCircleButton(icon = Icons.Default.Delete, accent = Color(0xFFE53935), onClick = onDelete)
            }
            // 描述
            if (goal.description.isNotBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    goal.description,
                    fontSize = TextSize.xs,
                    color = Color(0xFF6B4F5A),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.rsp
                )
            }
            // 进度
            Spacer(Modifier.height(Spacing.sm))
            GoalProgressRow(
                progress = goal.progress.coerceIn(0, 100),
                accent = cat.color,
                onAdjust = onAdjustProgress
            )
            // 里程打卡 展开
            var expanded by remember(goal.id) { mutableStateOf(false) }
            Spacer(Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(cat.color.copy(alpha = 0.10f))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (expanded) "▾ 收起里程碑/ 打卡" else "▸ 展开里程碑/ 打卡",
                    fontSize = TextSize.tiny,
                    color = cat.color,
                    fontWeight = FontWeight.Black
                )
            }
            if (expanded) {
                MilestoneSection(
                    goal = goal,
                    accent = cat.color,
                    onProgressFromMilestones = { p ->
                        if (onAdjustProgress != null) onAdjustProgress(p)
                    },
                    onCheckedInToday = { /* 留空：成就评估在主屏 LaunchedEffect 中触*/ }
                )
            }
        }
    }
}

@Composable
private fun MetaPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = TextSize.tiny, color = color, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun IconCircleButton(icon: ImageVector, accent: Color, onClick: () -> Unit, filled: Boolean = false) {
    Box(
        modifier = Modifier
            .size(34.rdp)
            .shadow(3.dp, CircleShape, spotColor = accent)
            .clip(CircleShape)
            .then(
                if (filled) Modifier.background(
                    Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.78f)))
                ) else Modifier.background(Color.White)
            )
            .border(1.5.dp, accent.copy(alpha = if (filled) 0.0f else 0.55f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, null,
            tint = if (filled) Color.White else accent,
            modifier = Modifier.size(18.rdp)
        )
    }
}

@Composable
private fun GoalProgressRow(progress: Int, accent: Color, onAdjust: ((Int) -> Unit)?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "进度",
                fontSize = TextSize.tiny,
                color = Color(0xFF8B5670),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                "$progress%",
                fontSize = TextSize.sm,
                color = accent,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(Color(0xFFFFE9EF))
        ) {
            // 进度填充（按比例
            val frac = (progress.coerceIn(0, 100) / 100f).coerceAtLeast(0f)
            if (frac > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac)
                        .height(10.dp)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.7f))))
                )
            }
        }
        if (onAdjust != null) {
            Spacer(Modifier.height(6.dp))
            // 快捷调节按钮10 / -5 / +5 / +10 / 100
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(-10, -5, +5, +10).forEach { delta ->
                    ProgressStepChip(
                        label = if (delta > 0) "+$delta" else "$delta",
                        accent = accent,
                        modifier = Modifier.weight(1f),
                        onClick = { onAdjust((progress + delta).coerceIn(0, 100)) }
                    )
                }
                ProgressStepChip(
                    label = "+100",
                    accent = Color(0xFF66BB6A),
                    modifier = Modifier.weight(1f),
                    onClick = { onAdjust(100) }
                )
            }
        }
    }
}

@Composable
private fun ProgressStepChip(label: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(26.rdp)
            .clip(RoundedCornerShape(Radius.pill))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(Radius.pill))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = TextSize.tiny, color = accent, fontWeight = FontWeight.Black)
    }
}

// ════════════════════════════════════════════════════════════════════════
// 倒数日列+ 卡片
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun CountdownsList(
    countdowns: List<Countdown>,
    onTap: (Countdown) -> Unit,
    onDelete: (Countdown) -> Unit
) {
    if (countdowns.isEmpty()) {
        EmptyStateBox(
            title = "还没有倒数日",
            subtitle = "把值得期待的日子加进来\n它们会一天一天朝你走来"
        ) { HourglassIcon(size = 64.rdp) }
        return
    }
    // 排序：未by 天数升序 / 已过 by 越接近今天越靠前
    val sorted = remember(countdowns) {
        countdowns.sortedBy { runCatching { it.getDaysRemaining() }.getOrNull() ?: Long.MAX_VALUE }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        sorted.forEach { c ->
            CountdownCard(countdown = c, onTap = { onTap(c) }, onDelete = { onDelete(c) })
        }
    }
}

@Composable
private fun CountdownCard(countdown: Countdown, onTap: () -> Unit, onDelete: () -> Unit) {
    val accent = parseHexColor(countdown.color)
    val days = runCatching { countdown.getDaysRemaining() }.getOrNull()
    val (badgeNum, badgeUnit, sub) = when {
        days == null -> Triple("—", "", "日期异常")
        days == 0L -> Triple("今天", "", "")
        days > 0 -> Triple("$days", "天", "后到达")
        else -> Triple("${-days}", "天", "前已过")
    }
    // 进度（基创建→目区间
    val progress = remember(countdown.id, countdown.targetDate, countdown.createdAt) {
        runCatching {
            val target = LocalDate.parse(countdown.targetDate)
            val created = LocalDate.parse(countdown.createdAt.substring(0, 10))
            val total = ChronoUnit.DAYS.between(created, target).coerceAtLeast(1)
            val passed = ChronoUnit.DAYS.between(created, LocalDate.now()).coerceIn(0L, total)
            (passed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }.getOrDefault(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(Radius.xl), spotColor = accent.copy(alpha = 0.6f))
            .clip(RoundedCornerShape(Radius.xl))
            .background(Color.White)
            .border(1.dp, accent.copy(alpha = 0.20f), RoundedCornerShape(Radius.xl))
            .clickable { onTap() }
            .padding(horizontal = Spacing.md, vertical = 12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 左：小圆头像（accent 描边白底
                Box(
                    modifier = Modifier
                        .size(44.rdp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.10f))
                        .border(2.dp, accent.copy(alpha = 0.85f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CountdownIconRender(countdown.icon.ifBlank { "🎈" }, fontSize = 22.rsp, imageSize = 40.rdp)
                }
                Spacer(Modifier.width(Spacing.sm))
                // 中：标题 + 副信
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        countdown.title,
                        fontSize = TextSize.title,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2B1620),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "📅",
                            fontSize = 11.rsp,
                            style = EmojiCenteredStyle
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            countdown.targetDate ?: "",
                            fontSize = TextSize.tiny,
                            color = Color(0xFF8B5670),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFC8B0BB))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            countdown.category,
                            fontSize = TextSize.tiny,
                            color = accent,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                    if (countdown.note.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            countdown.note,
                            fontSize = TextSize.tiny,
                            color = Color(0xFFA08294),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                // 右：巨型数字（无背景，用 accent 颜色
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            badgeNum,
                            fontSize = if (badgeNum.length >= 3) 30.rsp else if (badgeNum == "今天") 26.rsp else 38.rsp,
                            color = accent,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp,
                            lineHeight = 38.rsp
                        )
                        if (badgeUnit.isNotEmpty()) {
                            Spacer(Modifier.width(2.dp))
                            Text(
                                badgeUnit,
                                fontSize = TextSize.sm,
                                color = accent.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                    if (sub.isNotEmpty()) {
                        Text(
                            sub,
                            fontSize = 10.rsp,
                            color = accent.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // 底部进度细条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.7f))))
                )
            }
        }
        // 右上角删除按
        Box(
            modifier = Modifier
                .size(22.rdp)
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .clip(CircleShape)
                .background(Color(0xFFFFE4EC))
                .clickable { onDelete() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, null, tint = Color(0xFFE53935), modifier = Modifier.size(11.rdp))
        }
    }
}


// ─── icon 渲染（emoji file:// 用户自定义图）───
@Composable
private fun CountdownIconRender(icon: String, fontSize: androidx.compose.ui.unit.TextUnit, imageSize: Dp) {
    val isFile = icon.startsWith("file:") || icon.startsWith("/")
    if (isFile) {
        val path = if (icon.startsWith("file:")) icon.removePrefix("file:") else icon
        coil.compose.AsyncImage(
            model = java.io.File(path),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.size(imageSize).clip(CircleShape)
        )
    } else {
        Text(icon, fontSize = fontSize, style = EmojiCenteredStyle)
    }
}

// ─── 图标 picker：预emoji + 上传 ───
@Composable
private fun CountdownIconPickerRow(current: String, onPick: (String) -> Unit) {
    val ctx = LocalContext.current
    val presetEmojis = listOf(
        "🎂", "🎉", "🎁", "💕", "💍", "✈️", "🏖️", "🎓", "📖", "💼",
        "🏃", "🍔", "☕", "🌸", "🎵", "🎮", "📅", "⏰", "🌙", "⭐"
    )
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            runCatching {
                val dir = java.io.File(ctx.filesDir, "countdown_icons").apply { mkdirs() }
                val file = java.io.File(dir, "icon_${System.currentTimeMillis()}.png")
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(file).use { out -> input.copyTo(out) }
                }
                onPick("file:" + file.absolutePath)
            }.onFailure {
                android.util.Log.e("GoalScreen", "导入图标失败", it)
                android.widget.Toast.makeText(ctx, "导入失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val isFileCurrent = current.startsWith("file:") || current.startsWith("/")
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 上传按钮（最左）
        Box(
            modifier = Modifier
                .size(44.rdp)
                .clip(RoundedCornerShape(Radius.lg))
                .background(
                    if (isFileCurrent) Brush.linearGradient(listOf(Color(0xFFFF6F91), Color(0xFFFF8F4F)))
                    else Brush.linearGradient(listOf(Color(0xFFFFF1F2), Color(0xFFFFE9EF)))
                )
                .border(
                    if (isFileCurrent) 2.dp else 1.dp,
                    if (isFileCurrent) Color(0xFFFF6F91) else Color(0xFFFFD9E2),
                    RoundedCornerShape(Radius.lg)
                )
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (isFileCurrent) {
                CountdownIconRender(current, fontSize = 18.rsp, imageSize = 40.rdp)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, null, tint = Color(0xFFFF6F91), modifier = Modifier.size(18.rdp))
                    Text("上传", fontSize = 9.rsp, color = Color(0xFFFF6F91), fontWeight = FontWeight.Black)
                }
            }
        }
        // 预设 emoji
        presetEmojis.forEach { e ->
            val active = current == e
            Box(
                modifier = Modifier
                    .size(44.rdp)
                    .clip(RoundedCornerShape(Radius.lg))
                    .background(if (active) Color(0xFFFF6F91).copy(alpha = 0.18f) else Color.White)
                    .border(
                        if (active) 2.dp else 1.dp,
                        if (active) Color(0xFFFF6F91) else Color(0xFFFFD9E2),
                        RoundedCornerShape(Radius.lg)
                    )
                    .clickable { onPick(e) },
                contentAlignment = Alignment.Center
            ) {
                Text(e, fontSize = 22.rsp, style = EmojiCenteredStyle)
            }
        }
    }
}

// ─── 日期胶囊：带迷你日历图标 ───
@Composable
private fun DatePill(
    date: String,
    color: Color,
    bg: Color = color.copy(alpha = 0.14f),
    border: Color = Color.Transparent
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(bg)
            .then(if (border != Color.Transparent) Modifier.border(1.dp, border, RoundedCornerShape(Radius.pill)) else Modifier)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniCalendarIcon(size = 12.rdp, tint = color)
        Spacer(Modifier.width(4.dp))
        Text(date, fontSize = TextSize.tiny, color = color, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun MiniCalendarIcon(size: Dp, tint: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val s = size.toPx()
        val r = s * 0.16f
        // 主体
        drawRoundRect(
            color = tint,
            topLeft = Offset(s * 0.05f, s * 0.18f),
            size = Size(s * 0.90f, s * 0.78f),
            cornerRadius = CornerRadius(r, r)
        )
        // 顶部色带
        drawRoundRect(
            color = tint.copy(alpha = 0.55f),
            topLeft = Offset(s * 0.05f, s * 0.18f),
            size = Size(s * 0.90f, s * 0.22f),
            cornerRadius = CornerRadius(r, r)
        )
        // 两根挂
        drawRoundRect(
            color = tint,
            topLeft = Offset(s * 0.22f, s * 0.02f),
            size = Size(s * 0.10f, s * 0.20f),
            cornerRadius = CornerRadius(s * 0.04f, s * 0.04f)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(s * 0.68f, s * 0.02f),
            size = Size(s * 0.10f, s * 0.20f),
            cornerRadius = CornerRadius(s * 0.04f, s * 0.04f)
        )
        // 内白
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(s * 0.10f, s * 0.45f),
            size = Size(s * 0.80f, s * 0.46f),
            cornerRadius = CornerRadius(s * 0.06f, s * 0.06f)
        )
        // 三个小点
        val dotR = s * 0.06f
        val dy = s * 0.68f
        drawCircle(tint, dotR, Offset(s * 0.30f, dy))
        drawCircle(tint, dotR, Offset(s * 0.50f, dy))
        drawCircle(tint, dotR, Offset(s * 0.70f, dy))
    }
}

// ════════════════════════════════════════════════════════════════════════
// 编辑器：目标
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun GoalEditorDialog(
    initial: Goal?,
    initialIcon: String? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, category: String, targetDate: String?, progress: Int, icon: String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var desc by remember { mutableStateOf(initial?.description.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: GoalCategories.first().name) }
    var date by remember { mutableStateOf(initial?.targetDate) }
    var progress by remember { mutableStateOf(initial?.progress?.coerceIn(0, 100) ?: 0) }
    var icon by remember { mutableStateOf(initialIcon ?: goalCategoryOf(category).emoji) }
    val context = LocalContext.current

    EditorDialogShell(
        title = if (initial == null) "新建目标" else "编辑目标",
        icon = { TargetIcon(size = 32.rdp) },
        onDismiss = onDismiss,
        canSave = title.isNotBlank(),
        onSave = { onSave(title.trim(), desc.trim(), category, date, progress, icon) }
    ) {
        LabeledTextField(label = "标题（必填）", value = title, onChange = { title = it.take(40) }, placeholder = "例如：每周读完一本书")
        Spacer(Modifier.height(Spacing.sm))
        LabeledTextField(label = "描述（可选）", value = desc, onChange = { desc = it.take(200) }, placeholder = "更具体的计划、动机", multiLine = true)
        Spacer(Modifier.height(Spacing.sm))
        Text("分类", fontSize = TextSize.xs, color = Color(0xFF8B5670), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        CategoryChipRow(categories = GoalCategories, selected = category, onSelect = { sel ->
            // 切换分类时，若当icon 仍是原分类的默认 emoji，则同步更新
            val prevDefault = goalCategoryOf(category).emoji
            category = sel
            if (icon == prevDefault) icon = goalCategoryOf(sel).emoji
        })
        Spacer(Modifier.height(Spacing.sm))
        Text("图标（可上传自定义）", fontSize = TextSize.xs, color = Color(0xFF8B5670), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        CountdownIconPickerRow(current = icon, onPick = { icon = it })
        Spacer(Modifier.height(Spacing.sm))
        Text("截止日期", fontSize = TextSize.xs, color = Color(0xFF8B5670), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DateSelectButton(
                date = date,
                onPick = { date = it },
                accent = Color(0xFF7E57C2),
                modifier = Modifier.weight(1f)
            )
            if (date != null) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(32.rdp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935).copy(alpha = 0.12f))
                        .clickable { date = null },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Close, null, tint = Color(0xFFE53935), modifier = Modifier.size(16.rdp)) }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "当前进度：$progress%",
            fontSize = TextSize.xs,
            color = Color(0xFF8B5670),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        ProgressQuickAdjust(progress = progress, onChange = { progress = it })
    }
}

@Composable
private fun ProgressQuickAdjust(progress: Int, onChange: (Int) -> Unit) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(Color(0xFFFFE9EF))
        ) {
            val frac = (progress.coerceIn(0, 100) / 100f).coerceAtLeast(0f)
            if (frac > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac)
                        .height(12.dp)
                        .clip(RoundedCornerShape(Radius.pill))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFF6F91), Color(0xFFFF8F4F)))
                        )
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(0, 25, 50, 75, 100).forEach { v ->
                ProgressStepChip(
                    label = "$v%",
                    accent = if (v == progress) Color(0xFFFF6F91) else Color(0xFF9E9E9E),
                    modifier = Modifier.weight(1f),
                    onClick = { onChange(v) }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
// 编辑器：倒数
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun CountdownEditorDialog(
    initial: Countdown?,
    onDismiss: () -> Unit,
    onSave: (title: String, targetDate: String, category: String, icon: String, color: String, note: String) -> Unit
) {
    val defaultCat = CountdownCategories.first()
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var date by remember { mutableStateOf(initial?.targetDate ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var category by remember { mutableStateOf(initial?.category ?: defaultCat.name) }
    var icon by remember { mutableStateOf(initial?.icon ?: defaultCat.emoji) }
    var color by remember { mutableStateOf(initial?.color ?: CountdownPalette.first()) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }

    EditorDialogShell(
        title = if (initial == null) "新建倒数日" else "编辑倒数日",
        icon = { HourglassIcon(size = 32.rdp) },
        onDismiss = onDismiss,
        canSave = title.isNotBlank() && date.isNotBlank(),
        onSave = { onSave(title.trim(), date, category, icon, color, note.trim()) }
    ) {
        LabeledTextField(label = "标题（必填）", value = title, onChange = { title = it.take(40) }, placeholder = "例如：暑假旅行")
        Spacer(Modifier.height(Spacing.sm))
        Text("目标日期", fontSize = TextSize.xs, color = Color(0xFF8B5670), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        DateSelectButton(
            date = date,
            onPick = { d -> if (d != null) date = d },
            accent = parseHexColor(color),
            modifier = Modifier.fillMaxWidth(),
            allowClear = false
        )
        Spacer(Modifier.height(Spacing.sm))
        Text("分类（点击同步图标）", fontSize = TextSize.xs, color = Color(0xFF8B5670), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        CategoryChipRow(
            categories = CountdownCategories,
            selected = category,
            onSelect = { name ->
                category = name
                icon = countdownCategoryOf(name).emoji
            }
        )
        Spacer(Modifier.height(Spacing.sm))
        Text("图标（可上传自定义）", fontSize = TextSize.xs, color = Color(0xFF8B5670), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        CountdownIconPickerRow(current = icon, onPick = { icon = it })
        Spacer(Modifier.height(Spacing.sm))
        Text("颜色", fontSize = TextSize.xs, color = Color(0xFF8B5670), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CountdownPalette.forEach { hex ->
                val c = parseHexColor(hex)
                val active = hex.equals(color, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(34.rdp)
                        .shadow(if (active) 6.dp else 2.dp, CircleShape, spotColor = c)
                        .clip(CircleShape)
                        .background(c)
                        .border(if (active) 3.dp else 1.dp, Color.White, CircleShape)
                        .clickable { color = hex }
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        LabeledTextField(label = "备注（可选）", value = note, onChange = { note = it.take(120) }, placeholder = "想说的话")
    }
}

// ════════════════════════════════════════════════════════════════════════
// 通用：编辑器外壳 + 输入+ 分类 chip + 日期按钮
// ════════════════════════════════════════════════════════════════════════
@Composable
private fun EditorDialogShell(
    title: String,
    icon: @Composable () -> Unit,
    onDismiss: () -> Unit,
    canSave: Boolean,
    onSave: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ResponsiveDialogBox {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.xxl))
                    .background(Color(0xFFFFFBF7))
            ) {
                // 头部
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.rdp)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF6F91), Color(0xFFFF8F4F))
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.rdp)
                            .align(Alignment.TopStart)
                            .offset(x = (-15).rdp, y = (-15).rdp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                    )
                    Box(
                        modifier = Modifier
                            .size(32.rdp)
                            .align(Alignment.TopEnd)
                            .padding(Spacing.sm)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.28f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.rdp)) }
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.rdp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.28f))
                                .padding(4.rdp),
                            contentAlignment = Alignment.Center
                        ) { icon() }
                        Spacer(Modifier.width(10.dp))
                        Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = TextSize.headline, letterSpacing = 1.sp)
                    }
                }
                Column(
                    modifier = Modifier
                        .heightIn(max = 480.rdp)
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.md)
                ) {
                    content()
                }
                // 底部操作
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.rdp)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(Color(0xFFF1E6EE))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("取消", color = Color(0xFF8B5670), fontSize = TextSize.sm, fontWeight = FontWeight.Black)
                    }
                    val saveBg = if (canSave)
                        Brush.horizontalGradient(listOf(Color(0xFFFF6F91), Color(0xFFFF8F4F)))
                    else
                        Brush.horizontalGradient(listOf(Color(0xFFD0C2CB), Color(0xFFD0C2CB)))
                    Box(
                        modifier = Modifier
                            .weight(1.4f)
                            .height(46.rdp)
                            .clip(RoundedCornerShape(Radius.pill))
                            .background(saveBg)
                            .clickable(enabled = canSave) { onSave() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("保存", color = Color.White, fontSize = TextSize.sm, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    multiLine: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = TextSize.xs, color = Color(0xFF8B5670), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (multiLine) 80.rdp else 44.rdp)
                .clip(RoundedCornerShape(Radius.lg))
                .background(Color(0xFFFFF6F1))
                .border(1.dp, Color(0xFFFFD9E2), RoundedCornerShape(Radius.lg))
                .padding(horizontal = Spacing.sm, vertical = Spacing.sm)
        ) {
            if (value.isEmpty()) {
                Text(placeholder, color = Color(0xFFC9A9BC), fontSize = TextSize.sm, fontWeight = FontWeight.Medium)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = !multiLine,
                cursorBrush = SolidColor(Color(0xFFFF6F91)),
                textStyle = TextStyle(
                    color = Color(0xFF3D1F2C),
                    fontSize = TextSize.sm,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CategoryChipRow(
    categories: List<GoalCategory>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        categories.forEach { cat ->
            val active = cat.name == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(
                        if (active) Brush.horizontalGradient(listOf(cat.color, cat.color.copy(alpha = 0.78f)))
                        else Brush.horizontalGradient(listOf(Color.White, Color.White))
                    )
                    .border(1.dp, cat.color.copy(alpha = if (active) 0.0f else 0.45f), RoundedCornerShape(Radius.pill))
                    .clickable { onSelect(cat.name) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cat.emoji, fontSize = 13.rsp, style = EmojiCenteredStyle)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        cat.name,
                        fontSize = TextSize.tiny,
                        fontWeight = FontWeight.Black,
                        color = if (active) Color.White else cat.color
                    )
                }
            }
        }
    }
}

@Composable
private fun DateSelectButton(
    date: String?,
    onPick: (String?) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    allowClear: Boolean = true
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .height(44.rdp)
            .clip(RoundedCornerShape(Radius.lg))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(Radius.lg))
            .clickable {
                val (year, month, day) = parseYMD(date)
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val picked = LocalDate.of(y, m + 1, d).format(DateTimeFormatter.ISO_LOCAL_DATE)
                        onPick(picked)
                    },
                    year, month, day
                ).show()
            }
            .padding(horizontal = Spacing.sm),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📅", fontSize = 14.rsp, style = EmojiCenteredStyle)
            Spacer(Modifier.width(8.dp))
            Text(
                date ?: "未设置（可选）",
                fontSize = TextSize.sm,
                color = accent,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun parseYMD(date: String?): Triple<Int, Int, Int> {
    // 兼容 yyyy-MM-dd；解析失fallback 到今
    val today = Calendar.getInstance()
    if (date.isNullOrBlank()) {
        return Triple(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
    }
    return try {
        val ld = LocalDate.parse(date)
        Triple(ld.year, ld.monthValue - 1, ld.dayOfMonth)
    } catch (_: Throwable) {
        Triple(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
    }
}
