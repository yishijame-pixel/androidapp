// ═════════════════════════════════════════════════════════════════════════
// SidePanelContent.kt
// 侧边面板正文 — 双布局可切换：
//   1) Glass 玻璃卡组（杂志风、2 列网格）
//   2) Wall 生活墙（Hero + 章节大标题 + Bento）
// ═════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.with
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.QuickNote
import com.example.funlife.data.SidePanelStore
import com.example.funlife.data.StarredItem
import com.example.funlife.notifications.InboxStore
import com.example.funlife.ui.utils.Radius
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.ui.utils.rdp
import com.example.funlife.ui.utils.rsp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@Composable
fun SidePanelContent(
    onCloseRequest: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val ctx = LocalContext.current
    var tick by remember { mutableStateOf(0) }
    fun bump() { tick++ }

    var layout by remember { mutableStateOf(SidePanelStore.getLayoutMode(ctx)) }
    val notes = remember(tick) { SidePanelStore.getNotes(ctx) }
    val stars = remember(tick) { SidePanelStore.getStars(ctx) }
    // 通知未读：使用响应式 Flow，新消息到达 / 标已读后自动刷新（与首页同步）
    val unreadCount by InboxStore.unreadFlow.collectAsState()
    val inboxPreview = remember(tick, unreadCount) { InboxStore.getAll(ctx).take(2) }
    val sidePanelLifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(sidePanelLifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, ev ->
            if (ev == androidx.lifecycle.Lifecycle.Event.ON_RESUME) InboxStore.refreshUnread(ctx)
        }
        sidePanelLifecycle.lifecycle.addObserver(obs)
        onDispose { sidePanelLifecycle.lifecycle.removeObserver(obs) }
    }
    var fortune by remember { mutableStateOf(randomFortune()) }
    var isSpinning by remember { mutableStateOf(false) }
    val haptic = remember { com.example.funlife.utils.VibrationHelper(ctx) }
    val scope = rememberCoroutineScope()

    fun go(route: String) { onCloseRequest(); onNavigate(route) }

    // 🎰 老虎机式抽奖：快速滚动→渐慢→落定
    fun spinFortune() {
        if (isSpinning) return
        isSpinning = true
        scope.launch {
            // 总步数 + 每步延迟（指数增长 = 减速效果）
            val totalSteps = 18
            for (i in 0 until totalSteps) {
                fortune = randomFortune(exclude = fortune)
                // 缓动：i 越大延迟越长，模拟减速
                val progress = i.toFloat() / totalSteps
                val delayMs = (40 + 280 * progress * progress).toLong()
                // 每隔几步震一下，结尾每步都震
                if (i < 6 && i % 3 == 0) haptic.vibrateShort(6)
                else if (i in 6..12 && i % 2 == 0) haptic.vibrateShort(8)
                else if (i > 12) haptic.vibrateShort(10)
                kotlinx.coroutines.delay(delayMs)
            }
            // 落定的最后一击
            haptic.vibrateShort(40)
            isSpinning = false
        }
    }

    val data = PanelData(
        notes = notes,
        stars = stars,
        unread = unreadCount,
        inboxPreview = inboxPreview,
        fortune = fortune,
        onGo = ::go,
        onAddNote = { text -> SidePanelStore.addNote(ctx, text); bump() },
        onDeleteNote = { id -> SidePanelStore.deleteNote(ctx, id); bump() },
        onDeleteStar = { id -> SidePanelStore.deleteStar(ctx, id); bump() },
        onCloseRequest = onCloseRequest,
        onSwitchLayout = {
            val next = if (layout == "glass") "wall" else "glass"
            SidePanelStore.setLayoutMode(ctx, next)
            layout = next
        },
        onDrawFortune = ::spinFortune,
        isSpinning = isSpinning,
        currentLayout = layout
    )

    when (layout) {
        "glass" -> GlassLayout(data)
        else -> WallLayout(data)
    }
}

private data class PanelData(
    val notes: List<QuickNote>,
    val stars: List<StarredItem>,
    val unread: Int,
    val inboxPreview: List<com.example.funlife.notifications.InboxEntry>,
    val fortune: Fortune,
    val onGo: (String) -> Unit,
    val onAddNote: (String) -> Unit,
    val onDeleteNote: (Long) -> Unit,
    val onDeleteStar: (Long) -> Unit,
    val onCloseRequest: () -> Unit,
    val onSwitchLayout: () -> Unit,
    val onDrawFortune: () -> Unit,
    val isSpinning: Boolean,
    val currentLayout: String
)

// ═══════════════════════════════════════════════════════════════════════════
// Layout 1: Wall 生活墙 (默认)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun WallLayout(d: PanelData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 36.dp, bottom = 32.dp)
    ) {
        // ─ Hero ─
        item { HeroBlock(d) }

        // ─ 章节：今日 ─
        item {
            SectionHeader("今 日", "📅")
            HeadlineCard(d)
            Spacer(Modifier.height(Spacing.sm))
            OverviewPillRow()
        }

        // ─ 章节：灵感 ─
        item {
            Spacer(Modifier.height(Spacing.lg))
            SectionHeader("灵 感", "💡")
            Box(modifier = Modifier.padding(horizontal = Spacing.md)) {
                NoteSection(d)
            }
            Spacer(Modifier.height(Spacing.sm))
            Box(modifier = Modifier.padding(horizontal = Spacing.md)) {
                StarSection(d)
            }
        }

        // ─ 章节：其他 ─
        item {
            Spacer(Modifier.height(Spacing.lg))
            SectionHeader("更 多", "✨")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                MiniBento("🐾", "宠物", "去看看", Color(0xFFFFAB91), Modifier.weight(1f)) { d.onGo("pet") }
                MiniBento("🕰️", "时光胶囊", "一年前", Color(0xFF7E57C2), Modifier.weight(1f)) { d.onGo("anniversary") }
                MiniBento("🔔", "通知", if (d.unread > 0) "${d.unread} 未读" else "查看", Color(0xFFFB8C00), Modifier.weight(1f)) { d.onGo("inbox") }
            }
            if (d.inboxPreview.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                Box(modifier = Modifier.padding(horizontal = Spacing.md)) {
                    InboxPreviewBlock(d)
                }
            }
        }
    }
}

@Composable
private fun HeroBlock(d: PanelData) {
    val dateText = remember {
        val fmt = SimpleDateFormat("M月d日 · EEEE", Locale.CHINA)
        fmt.format(Date())
    }
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) { in 5..10 -> "早上好"; in 11..13 -> "中午好"; in 14..17 -> "下午好"; in 18..22 -> "晚上好"; else -> "夜深了" }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFFFB199), Color(0xFFFF9CB0), Color(0xFFB8A4FF))
                )
            )
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        // 切换布局 + 关闭：emoji 圆形按钮
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CuteEmojiBtn(if (d.currentLayout == "wall") "🌌" else "🌸", onClick = d.onSwitchLayout)
            CuteEmojiBtn("✕", isText = true, onClick = d.onCloseRequest)
        }

        Column {
            Text(dateText, color = Color.White.copy(alpha = 0.95f), fontSize = TextSize.tiny, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            Text(greeting + "，旅人 ✨", color = Color.White, fontSize = 24.rsp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            FortuneRow(d.fortune, light = true, isSpinning = d.isSpinning, onDraw = d.onDrawFortune)
            Spacer(Modifier.height(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionPill("🌱", "打卡", modifier = Modifier.weight(1f)) { d.onGo("habit") }
                FloatingActionPill("💭", "心情", modifier = Modifier.weight(1f)) { d.onGo("mood") }
                FloatingActionPill("⏳", "倒数", modifier = Modifier.weight(1f)) { d.onGo("goal") }
                FloatingActionPill("🎀", "纪念", modifier = Modifier.weight(1f)) { d.onGo("anniversary") }
            }
        }
    }
}

/**
 * 可爱 emoji 圆形按钮（替代 Material Icon）。
 * @param isText true 时按文本渲染（用于 ✕、+ 等符号）
 */
@Composable
private fun CuteEmojiBtn(emoji: String, isText: Boolean = false, light: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.rdp)
            .clip(CircleShape)
            .background(if (light) Color.White.copy(alpha = 0.28f) else Color(0xFFF3F4F6))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            emoji,
            color = if (isText) (if (light) Color.White else Color(0xFF6B7280)) else Color.Unspecified,
            fontSize = if (isText) 16.rsp else 16.rsp,
            fontWeight = if (isText) FontWeight.Black else FontWeight.Normal
        )
    }
}

/**
 * 运势卡 + 抽签按钮。切换 fortune 时带动画；spinning 时骰子持续旋转。
 */
@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
private fun FortuneRow(fortune: Fortune, light: Boolean, isSpinning: Boolean, onDraw: () -> Unit) {
    // 骰子旋转动画（spinning 时持续 360°→360°→...）
    val rot = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(isSpinning) {
        if (isSpinning) {
            while (isSpinning) {
                rot.snapTo(0f)
                rot.animateTo(
                    360f,
                    animationSpec = androidx.compose.animation.core.tween(
                        700,
                        easing = androidx.compose.animation.core.LinearEasing
                    )
                )
            }
        }
    }
    val diceRotation = rot.value

    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.animation.AnimatedContent(
            targetState = fortune,
            transitionSpec = {
                // spinning 时：超快上滑切换；落定时：温柔慢一点
                val dur = if (isSpinning) 120 else 380
                val enter = androidx.compose.animation.slideInVertically(
                    animationSpec = androidx.compose.animation.core.tween(dur)
                ) { it / 2 } + androidx.compose.animation.fadeIn(
                    animationSpec = androidx.compose.animation.core.tween(dur)
                )
                val exit = androidx.compose.animation.slideOutVertically(
                    animationSpec = androidx.compose.animation.core.tween(dur / 2)
                ) { -it / 2 } + androidx.compose.animation.fadeOut(
                    animationSpec = androidx.compose.animation.core.tween(dur / 2)
                )
                enter with exit
            },
            modifier = Modifier.weight(1f),
            label = "fortune"
        ) { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f.emoji, fontSize = 22.rsp)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            f.tag,
                            color = if (light) Color.White else f.accent,
                            fontSize = 9.rsp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (light) Color.White.copy(alpha = 0.25f) else f.accent.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        f.text,
                        color = if (light) Color.White else Color(0xFF1F2937),
                        fontSize = TextSize.sm,
                        fontWeight = FontWeight.Black,
                        lineHeight = 17.rsp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        // 抽签按钮（🎲）— spinning 时旋转
        Box(
            modifier = Modifier
                .size(40.rdp)
                .clip(CircleShape)
                .background(if (light) Color.White.copy(alpha = 0.30f) else Color(0xFFFAF7F8))
                .border(1.dp, if (light) Color.White.copy(alpha = 0.4f) else Color(0xFFEFEAEE), CircleShape)
                .clickable(enabled = !isSpinning) { onDraw() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "🎲",
                fontSize = 20.rsp,
                modifier = Modifier.graphicsLayer {
                    rotationZ = if (isSpinning) diceRotation else 0f
                }
            )
        }
    }
}

@Composable
private fun FloatingActionPill(emoji: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 20.rsp)
        Spacer(Modifier.height(2.dp))
        Text(label, color = Color(0xFF8B5670), fontSize = TextSize.tiny, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SectionHeader(title: String, emoji: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 18.rsp)
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color(0xFF1F2937), fontSize = 18.rsp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFEFEAEE))
        )
    }
}

@Composable
private fun HeadlineCard(d: PanelData) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFFFFE5EC), Color(0xFFE8DBFF)))
                )
                .clickable { d.onGo("anniversary") }
                .padding(16.dp)
        ) {
            Column {
                Text("今日精选", color = Color(0xFFC2185B), fontSize = TextSize.tiny, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Text("🎀 今天没有特别的纪念日", color = Color(0xFF1F2937), fontSize = TextSize.title, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("但每一天都值得被认真对待 💗", color = Color(0xFF6B7280), fontSize = TextSize.xs, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun OverviewPillRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEFEAEE), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OverviewItem("😊", "心情", "—")
        OverviewDivider()
        OverviewItem("✓", "打卡", "0")
        OverviewDivider()
        OverviewItem("🎯", "目标", "0")
        OverviewDivider()
        OverviewItem("⏳", "倒数", "0")
    }
}

@Composable
private fun OverviewItem(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 14.rsp)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color(0xFF6B7280), fontSize = 10.rsp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(2.dp))
            Text(value, color = Color(0xFF1F2937), fontSize = 11.rsp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun OverviewDivider() {
    Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFFEFEAEE)))
}

@Composable
private fun NoteSection(d: PanelData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEFEAEE), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📝", fontSize = 16.rsp)
            Spacer(Modifier.width(6.dp))
            Text("随手记", color = Color(0xFF1F2937), fontSize = TextSize.sm, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text("${d.notes.size} 条", color = Color(0xFF66BB6A), fontSize = TextSize.tiny, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(10.dp))
        NoteInputRow(d)
        if (d.notes.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(d.notes, key = { it.id }) { NoteChip(it, d.onDeleteNote) }
            }
        }
    }
}

@Composable
private fun NoteInputRow(d: PanelData) {
    var input by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.rdp)
                .clip(RoundedCornerShape(Radius.pill))
                .background(Color(0xFFFAF7F8))
                .border(1.dp, Color(0xFFEFEAEE), RoundedCornerShape(Radius.pill))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                cursorBrush = SolidColor(Color(0xFF66BB6A)),
                textStyle = TextStyle(color = Color(0xFF1F2937), fontSize = TextSize.sm),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (input.isEmpty()) Text("记下此刻的灵感…", color = Color(0xFF9CA3AF), fontSize = TextSize.sm)
                    inner()
                }
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(38.rdp).clip(CircleShape).background(Color(0xFF66BB6A))
                .clickable {
                    if (input.isNotBlank()) { d.onAddNote(input); input = "" }
                },
            contentAlignment = Alignment.Center
        ) { Text("✨", fontSize = 18.rsp) }
    }
}

@Composable
private fun StarSection(d: PanelData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEFEAEE), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⭐", fontSize = 16.rsp)
            Spacer(Modifier.width(6.dp))
            Text("星标收藏", color = Color(0xFF1F2937), fontSize = TextSize.sm, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            Text("${d.stars.size} 项", color = Color(0xFFFFA726), fontSize = TextSize.tiny, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(10.dp))
        if (d.stars.isEmpty()) {
            EmptyHint("在卡片上点 ⭐ 收藏，会出现在这里")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(d.stars, key = { it.id }) { s ->
                    StarChip(s, onClick = { s.deepLink?.let(d.onGo) }, onDelete = d.onDeleteStar)
                }
            }
        }
    }
}

@Composable
private fun MiniBento(emoji: String, title: String, sub: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(emoji, fontSize = 22.rsp)
        Spacer(Modifier.height(4.dp))
        Text(title, color = Color(0xFF1F2937), fontSize = TextSize.xs, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(2.dp))
        Text(sub, color = accent, fontSize = TextSize.tiny, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InboxPreviewBlock(d: PanelData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEFEAEE), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        d.inboxPreview.forEachIndexed { i, e ->
            if (i > 0) Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable { d.onGo("inbox") },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFFFB8C00).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) { Text("🔔", fontSize = 13.rsp) }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(e.title, color = Color(0xFF1F2937), fontSize = TextSize.xs, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(e.body, color = Color(0xFF6B7280), fontSize = TextSize.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (!e.read) Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFB8C00)))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Layout 2: Glass 玻璃卡组（杂志风、2 列网格）
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun GlassLayout(d: PanelData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // 顶部条：运势卡 + 切换/关闭
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFFF1F5), Color(0xFFFCE7F3)))
                        )
                        .border(1.dp, Color(0xFFFDD3E0), RoundedCornerShape(20.dp))
                        .weight(1f)
                        .padding(12.dp)
                ) { FortuneRow(d.fortune, light = false, isSpinning = d.isSpinning, onDraw = d.onDrawFortune) }
                Spacer(Modifier.width(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CuteEmojiBtn(if (d.currentLayout == "wall") "🌌" else "🌸", light = false, onClick = d.onSwitchLayout)
                    CuteEmojiBtn("✕", isText = true, light = false, onClick = d.onCloseRequest)
                }
            }
        }

        // 4 个胶囊快捷
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                GlassQuick("🌱", "打卡", Color(0xFF26A69A), Modifier.weight(1f)) { d.onGo("habit") }
                GlassQuick("💭", "心情", Color(0xFF7E57C2), Modifier.weight(1f)) { d.onGo("mood") }
                GlassQuick("⏳", "倒数", Color(0xFFFF8F4F), Modifier.weight(1f)) { d.onGo("goal") }
                GlassQuick("🎀", "纪念", Color(0xFFEC407A), Modifier.weight(1f)) { d.onGo("anniversary") }
            }
        }

        // 第一行：今日总览（占满）
        item {
            GlassCard(emoji = "📊", title = "今日总览", accent = Color(0xFF42A5F5)) {
                OverviewPillRow()
            }
        }

        // 第二行：随手记（满）
        item {
            GlassCard(emoji = "📝", title = "随手记 · ${d.notes.size}", accent = Color(0xFF66BB6A)) {
                NoteInputRow(d)
                if (d.notes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(d.notes, key = { it.id }) { NoteChip(it, d.onDeleteNote) }
                    }
                }
            }
        }

        // 第三行：星标 + 通知 (2 列)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                GlassMiniCard(
                    emoji = "⭐", title = "星标", value = "${d.stars.size}",
                    accent = Color(0xFFFFA726), modifier = Modifier.weight(1f)
                ) { /* keep panel open */ }
                GlassMiniCard(
                    emoji = "🔔", title = "通知", value = if (d.unread > 0) "${d.unread} 未读" else "0",
                    accent = Color(0xFFFB8C00), modifier = Modifier.weight(1f)
                ) { d.onGo("inbox") }
            }
        }

        // 第四行：宠物 + 时光胶囊 (2 列)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                GlassMiniCard("🐾", "宠物", "去看看", Color(0xFFFFAB91), modifier = Modifier.weight(1f)) { d.onGo("pet") }
                GlassMiniCard("🕰️", "时光胶囊", "一年前", Color(0xFF7E57C2), modifier = Modifier.weight(1f)) { d.onGo("anniversary") }
            }
        }

        // 通知预览展开
        if (d.inboxPreview.isNotEmpty()) item { InboxPreviewBlock(d) }
    }
}

@Composable
private fun GlassQuick(emoji: String, label: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(40.rdp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 20.rsp) }
        Spacer(Modifier.height(6.dp))
        Text(label, color = accent, fontSize = TextSize.tiny, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun GlassCard(emoji: String, title: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEFEAEE), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.rdp).clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 14.rsp) }
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color(0xFF1F2937), fontSize = TextSize.sm, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun GlassMiniCard(emoji: String, title: String, value: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEFEAEE), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier.size(32.rdp).clip(CircleShape).background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 18.rsp) }
        Spacer(Modifier.height(8.dp))
        Text(title, color = Color(0xFF6B7280), fontSize = TextSize.tiny, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(value, color = accent, fontSize = TextSize.title, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 共享子组件
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun NoteChip(note: QuickNote, onDelete: (Long) -> Unit) {
    Box(
        modifier = Modifier
            .widthIn(min = 110.dp, max = 200.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE9F7EF))
            .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(note.text, color = Color(0xFF1B5E20), fontSize = TextSize.tiny, fontWeight = FontWeight.Medium, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 15.rsp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(relativeTime(note.createdAt), color = Color(0xFF66BB6A), fontSize = 9.rsp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White).clickable { onDelete(note.id) },
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = Color(0xFF9CA3AF), fontSize = 11.rsp, fontWeight = FontWeight.Black) }
            }
        }
    }
}

@Composable
private fun StarChip(item: StarredItem, onClick: () -> Unit, onDelete: (Long) -> Unit) {
    Box(
        modifier = Modifier
            .widthIn(min = 120.dp, max = 200.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF6E0))
            .border(1.dp, Color(0xFFFFE082), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.emoji.ifBlank { "⭐" }, fontSize = 14.rsp)
                Spacer(Modifier.width(6.dp))
                Text(item.title, color = Color(0xFF8D6E00), fontSize = TextSize.tiny, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.size(18.dp).clip(CircleShape).background(Color.White).clickable { onDelete(item.id) },
                    contentAlignment = Alignment.Center
                ) { Text("✕", color = Color(0xFF9CA3AF), fontSize = 10.rsp, fontWeight = FontWeight.Black) }
            }
            if (item.subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(item.subtitle, color = Color(0xFFB0823F), fontSize = 10.rsp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Color(0xFFFAF7F8))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color(0xFF9CA3AF), fontSize = TextSize.tiny, fontWeight = FontWeight.Medium) }
}

private fun relativeTime(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000 -> "刚刚"
        diff < 60 * 60_000 -> "${diff / 60_000} 分钟前"
        diff < 24 * 60 * 60_000 -> "${diff / (60 * 60_000)} 小时前"
        else -> "${diff / (24 * 60 * 60_000)} 天前"
    }
}

private data class Fortune(val emoji: String, val text: String, val tag: String, val accent: androidx.compose.ui.graphics.Color)

private val FORTUNES = listOf(
    Fortune("🌸", "今天的你，会被温柔以待。", "大吉", Color(0xFFFF8FB1)),
    Fortune("🌟", "适合做让自己开心的小事。", "上吉", Color(0xFFFFB347)),
    Fortune("🍀", "重要的不是去哪，而是和谁在一起。", "中吉", Color(0xFF66BB6A)),
    Fortune("💫", "你比想象中更受欢迎，相信自己。", "大吉", Color(0xFFAB8DFF)),
    Fortune("🌿", "停下来听听心里的声音。", "平", Color(0xFF66BB6A)),
    Fortune("🎯", "今天的小努力，是未来的好运。", "上吉", Color(0xFFFB8C00)),
    Fortune("☀️", "出门记得带笑容 ☀️", "上吉", Color(0xFFFFB347)),
    Fortune("⚡", "把'再等等'换成'现在就做'。", "上吉", Color(0xFFFFD54F)),
    Fortune("🎁", "宇宙正在偷偷为你安排惊喜。", "大吉", Color(0xFFEC407A)),
    Fortune("🍓", "甜的东西会让人变可爱。", "上吉", Color(0xFFEC407A)),
    Fortune("🐝", "忙起来也别忘了喘息一下。", "平", Color(0xFFFFB347)),
    Fortune("🌈", "雨过总会有彩虹。", "上吉", Color(0xFF7E57C2)),
    Fortune("🪷", "心静则万物皆静。", "平", Color(0xFFAB8DFF)),
    Fortune("🍵", "给生活留一点慢的时刻。", "中吉", Color(0xFF66BB6A)),
    Fortune("✨", "你已经做得很好了。", "上吉", Color(0xFFFFD54F)),
    Fortune("🦋", "蜕变就藏在那一点点不舒服里。", "中吉", Color(0xFFAB8DFF)),
    Fortune("🌙", "今晚的月色，刚好。", "平", Color(0xFF7E57C2)),
    Fortune("🍰", "对自己好一点，今天值得。", "大吉", Color(0xFFEC407A)),
    Fortune("📮", "想念某个人就告诉 ta 吧。", "上吉", Color(0xFFFF8FB1)),
    Fortune("🌷", "新的开始，从现在算。", "上吉", Color(0xFFFF8FB1)),
    Fortune("🍋", "酸甜苦辣都是味道。", "平", Color(0xFFFFD54F)),
    Fortune("🐬", "听见心里的浪花了吗？", "中吉", Color(0xFF4DD0E1)),
    Fortune("🪐", "你不必证明什么，闪闪发光就好。", "大吉", Color(0xFFAB8DFF)),
    Fortune("🌻", "向阳而生，不问西东。", "上吉", Color(0xFFFB8C00)),
    Fortune("🐱", "撸猫五分钟，治愈一整天。", "上吉", Color(0xFFFFB347)),
    Fortune("🍙", "好好吃饭也是一种修行。", "中吉", Color(0xFF66BB6A)),
    Fortune("🎈", "放下那些不属于你的烦恼。", "上吉", Color(0xFFEC407A)),
    Fortune("🛼", "记得给自己一点玩乐的时间。", "中吉", Color(0xFF4DD0E1)),
    Fortune("🌊", "潮起潮落，皆是风景。", "平", Color(0xFF4DD0E1)),
    Fortune("🎀", "今天，绑上属于你的小蝴蝶结。", "上吉", Color(0xFFFF8FB1)),
    Fortune("🐰", "蹦蹦跳跳地，迎接好运吧。", "大吉", Color(0xFFFF8FB1)),
    Fortune("🍑", "桃花运正在赶来的路上。", "大吉", Color(0xFFEC407A)),
    Fortune("☁️", "什么都不做也是好天气。", "平", Color(0xFFB0BEC5)),
    Fortune("🪞", "镜子里那个人比你想象中好看。", "上吉", Color(0xFFAB8DFF)),
    Fortune("🌅", "黎明总会到来。", "中吉", Color(0xFFFB8C00)),
    Fortune("💌", "你说出口的话，会变成真的。", "上吉", Color(0xFFFF8FB1)),
    Fortune("🪻", "别忘了对自己说一句辛苦了。", "中吉", Color(0xFFAB8DFF)),
    Fortune("🍯", "甜蜜要主动去找。", "上吉", Color(0xFFFFD54F)),
    Fortune("🐠", "在自己的节奏里游就好。", "中吉", Color(0xFF4DD0E1)),
    Fortune("🎵", "听一首你喜欢的歌吧。", "平", Color(0xFFAB8DFF)),
    Fortune("🌼", "好事总在不经意时发生。", "上吉", Color(0xFFFFD54F)),
    Fortune("🪴", "慢慢来，比较快。", "中吉", Color(0xFF66BB6A)),
    Fortune("🍡", "今天的烦恼，明天忘了大半。", "上吉", Color(0xFFEC407A)),
    Fortune("⭐", "把目光从别人身上挪开。", "中吉", Color(0xFFFFD54F)),
    Fortune("🎐", "风往哪吹，就跟着哪走一阵。", "平", Color(0xFFAB8DFF)),
    Fortune("🌌", "你正在成为更喜欢的自己。", "大吉", Color(0xFFAB8DFF)),
    Fortune("🍂", "落叶会归根，烦恼会散去。", "中吉", Color(0xFFFB8C00)),
    Fortune("🦔", "保留你的那份温柔。", "上吉", Color(0xFFFF8FB1)),
    Fortune("🪁", "想飞就放出你的风筝吧。", "上吉", Color(0xFF4DD0E1)),
    Fortune("🍩", "甜甜圈日记：今天值得记录。", "上吉", Color(0xFFEC407A))
)

private fun randomFortune(exclude: Fortune? = null): Fortune {
    if (FORTUNES.size <= 1) return FORTUNES.first()
    var f: Fortune
    do { f = FORTUNES.random() } while (f === exclude)
    return f
}
