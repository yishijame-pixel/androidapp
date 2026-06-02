// ReadingRoomScreen.kt — v53 阅光书房 · 总入口（4 Tab）
//
// Tabs：
//   0 阅读     · 今日打卡 + 连续天数 + 月度阅读曲线
//   1 书架     · 复用 BookshelfScreen 列表骨架
//   2 摘抄     · 摘抄本 + 时光胶囊（按"已寄/已开"分组）
//   3 星河     · 跳转匿名摘抄星河（独立 Screen）
//
// 设计：
//   - 顶部 hero 区：今日已读 + 连续天数 + 一键打卡 CTA
//   - Tab 切换不重建 hero
//   - "去星河"是个跳转动作而非 Tab 内容，通过 onNavigateToGalaxy 暴露
package com.example.funlife.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.data.dao.DailyMinutes
import com.example.funlife.data.model.Quote
import com.example.funlife.ui.components.MonthlyMinutesChart
import com.example.funlife.ui.components.QuoteCard
import com.example.funlife.ui.theme.ReadingRoomTheme as RT
import com.example.funlife.viewmodel.BookViewModel
import com.example.funlife.viewmodel.ReadingRoomViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingRoomScreen(
    userId: Long,
    onNavigateBack: () -> Unit,
    onOpenBookshelf: () -> Unit,
    onOpenGalaxy: () -> Unit,
    onOpenDna: () -> Unit,
    onOpenPostcard: () -> Unit,
    onOpenBookDetail: (Long) -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as android.app.Application
    val vm: ReadingRoomViewModel = viewModel(
        key = "ReadingRoom_$userId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ReadingRoomViewModel(app, userId) as T
        }
    )
    val bookVm: BookViewModel = viewModel(
        key = "Book_$userId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                BookViewModel(app, userId) as T
        }
    )

    val today by vm.todayMinutes.collectAsState()
    val streak by vm.streakDays.collectAsState()
    val curve by vm.monthlyCurve.collectAsState()
    val quotes by vm.allQuotes.collectAsState()
    val books by bookVm.books.collectAsState()
    val toast by vm.toast.collectAsState()
    val checkInResult by vm.lastCheckIn.collectAsState()

    val snackHost = remember { SnackbarHostState() }
    LaunchedEffect(toast) {
        toast?.let { msg ->
            snackHost.showSnackbar(msg)
            vm.consumeToast()
        }
    }

    var tab by rememberSaveable { mutableStateOf(0) }
    var showCheckInSheet by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackHost) },
        containerColor = Color.Transparent,
        // 让 Scaffold 不自动消费 status bar inset；交给页面自己处理，避免双重 padding
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(RT.pageBackground())
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // TopBar 紧贴 status bar；TopBar 自身内部用 statusBarsPadding 处理刘海
                TopBar(
                    onBack = onNavigateBack,
                    onPostcard = onOpenPostcard,
                    onDna = onOpenDna,
                )
                HeroPanel(
                    todayMinutes = today,
                    streakDays = streak,
                    onCheckIn = { showCheckInSheet = true },
                    onGalaxy = onOpenGalaxy,
                )
                TabsBar(tab) { tab = it }
                when (tab) {
                    0 -> ReadingTab(curve = curve)
                    1 -> ShelfTab(
                        books = books,
                        onOpen = onOpenBookDetail,
                        onAddNew = onOpenBookshelf, // fallback：复用书架的添加流程
                    )
                    2 -> QuotesTab(
                        quotes = quotes,
                        onTogglePin = { vm.togglePinned(it) },
                        onDelete = { vm.deleteQuote(it) },
                        onOpenBook = { onOpenBookDetail(it) },
                    )
                    3 -> GalaxyTeaserTab(onEnter = onOpenGalaxy)
                }
            }
        }
    }

    if (showCheckInSheet) {
        CheckInBottomSheet(
            onDismiss = { showCheckInSheet = false },
            onSubmit = { mins ->
                vm.checkIn(mins)
                showCheckInSheet = false
            }
        )
    }

    // 打卡成功的浮动反馈
    AnimatedVisibility(
        visible = checkInResult != null,
        enter = fadeIn(), exit = fadeOut()
    ) {
        checkInResult?.let { r ->
            LaunchedEffect(r) { delay(2200); vm.consumeCheckIn() }
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 240.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCCFFFFFF))
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text("✨ 已打卡 +${r.todayMinutes} 分钟", color = RT.PrimaryInk,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (r.streakDays >= 1) {
                        Text("连续 ${r.streakDays} 天", color = RT.AccentOrange, fontSize = 13.sp)
                    }
                    if (r.coinAward > 0) {
                        Text("奖励 +${r.coinAward} 金币", color = RT.AccentRose, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/* ════════════════════════════════════════════════════════════
   顶栏 + Hero
   ════════════════════════════════════════════════════════════ */

@Composable
private fun TopBar(onBack: () -> Unit, onPostcard: () -> Unit, onDna: () -> Unit) {
    // 整个 TopBar 是一层"晨光天幕"，自带 status bar inset；视觉上与 Hero 卡无缝衔接
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        RT.CardCream.copy(alpha = 0.6f),   // 顶部柔白
                        Color.Transparent                   // 渐隐到主背景
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回 —— 圆形按钮
            RoundIconButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "返回",
                onClick = onBack,
                tint = RT.PrimaryInk,
                bg = RT.CardCream,
            )
            Spacer(Modifier.width(14.dp))
            // 标题 + 副标题
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "阅光书房",
                        color = RT.PrimaryInk,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    // 标题旁的小高光点缀
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(RT.AccentOrange)
                    )
                }
                Text(
                    "和书的私人时光",
                    color = RT.SecondaryInk,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            // 右上角两个动作 —— Material Icons 替代 emoji（避免 OPPO/Mi 字体下渲染异常）
            RoundIconButton(
                icon = Icons.Outlined.Email,
                contentDescription = "明信片漂流",
                onClick = onPostcard,
                tint = RT.AccentRose,
                bg = RT.CardCream,
            )
            Spacer(Modifier.width(8.dp))
            RoundIconButton(
                icon = Icons.Outlined.AutoAwesome,
                contentDescription = "读者 DNA",
                onClick = onDna,
                tint = RT.AccentOrange,
                bg = RT.CardCream,
            )
        }
    }
}

/**
 * 统一的圆形图标按钮：用于顶部栏右侧操作 + 左侧返回。
 * 36dp 直径，乳白底色，带轻微 shadow，避免 IconButton 默认无背景的"飞"感。
 */
@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .shadow(2.dp, CircleShape, spotColor = RT.MutedInk.copy(alpha = 0.18f))
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun HeroPanel(
    todayMinutes: Int,
    streakDays: Int,
    onCheckIn: () -> Unit,
    onGalaxy: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp)
            .shadow(10.dp, RoundedCornerShape(28.dp), spotColor = RT.AccentRose.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(28.dp))
            .background(RT.CardCream)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$todayMinutes",
                color = RT.PrimaryInk, fontSize = 42.sp, fontWeight = FontWeight.Black)
            Text("  分钟 · 今日", color = RT.SecondaryInk, fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp))
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("🔥 连续 $streakDays 天",
                    color = RT.AccentOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("今天写一段，让连签延长", color = RT.MutedInk, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(RT.heroGradient())
                    .clickable(onClick = onCheckIn)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("📖 阅读打卡", color = Color.White,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(RT.AccentSky.copy(alpha = 0.18f))
                    .clickable(onClick = onGalaxy)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("✨ 摘抄星河", color = RT.PrimaryInk,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/* ════════════════════════════════════════════════════════════
   Tabs
   ════════════════════════════════════════════════════════════ */

@Composable
private fun TabsBar(current: Int, onChange: (Int) -> Unit) {
    val labels = listOf("阅读", "书架", "摘抄", "星河")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        labels.forEachIndexed { idx, label ->
            val selected = idx == current
            Box(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) RT.PrimaryInk else RT.CardCream.copy(alpha = 0.6f)
                    )
                    .clickable { onChange(idx) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) Color.White else RT.SecondaryInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/* ════════════════════════════════════════════════════════════
   Tab 内容
   ════════════════════════════════════════════════════════════ */

@Composable
private fun ReadingTab(curve: List<DailyMinutes>) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionTitle("📈 最近 30 天阅读曲线")
            MonthlyMinutesChart(
                data = curve,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(RT.CardCream)
                    .padding(14.dp)
            )
        }
        item {
            SectionTitle("☕ 今天的小提醒")
            HintCard(
                "📚 阅读不是任务，是给自己的一段慢时间。打卡只是仪式，不是评分。",
                bg = RT.CardSky
            )
        }
        item {
            HintCard(
                "🧬 等读完 2 本以上后，可以在右上角生成一张「读者 DNA 卡」，看看自己读出了什么人格。",
                bg = RT.CardPeach
            )
        }
    }
}

@Composable
private fun ShelfTab(
    books: List<com.example.funlife.data.model.Book>,
    onOpen: (Long) -> Unit,
    onAddNew: () -> Unit,
) {
    if (books.isEmpty()) {
        EmptyHint(
            emoji = "📚",
            title = "书架还空着",
            sub = "去书架页面添加你的第一本书，或先在阅读 Tab 打个卡。",
            cta = "去人生书架",
            onCta = onAddNew
        )
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionTitle("📖 我的书架（${books.size}）") }
        items(books, key = { it.id }) { b ->
            BookRowCompact(b, onClick = { onOpen(b.id) })
        }
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onAddNew,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text("+ 添加新书") }
        }
    }
}

@Composable
private fun BookRowCompact(
    b: com.example.funlife.data.model.Book,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RT.CardCream)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(RT.AccentGold.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) { Text("📕", fontSize = 18.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(b.title, color = RT.PrimaryInk,
                fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            val sub = buildString {
                if (b.author.isNotBlank()) append(b.author)
                if (b.totalPages > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("${b.currentPage}/${b.totalPages} 页")
                }
                if (b.rating > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("★".repeat(b.rating))
                }
            }
            if (sub.isNotEmpty()) {
                Text(sub, color = RT.SecondaryInk, fontSize = 11.sp, maxLines = 1)
            }
        }
        if (b.finishedAt > 0) {
            Text("已读完", color = RT.AccentLeaf, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold)
        } else if (b.currentPage > 0) {
            Text("阅读中", color = RT.AccentOrange, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuotesTab(
    quotes: List<Quote>,
    onTogglePin: (Quote) -> Unit,
    onDelete: (Quote) -> Unit,
    onOpenBook: (Long) -> Unit,
) {
    if (quotes.isEmpty()) {
        EmptyHint(
            emoji = "📝",
            title = "还没有摘抄",
            sub = "在书的详情页里抄一段你舍不得忘的话。可以选择立即收纳，或寄给未来的自己。",
            cta = null,
            onCta = {}
        )
        return
    }
    // 互斥分组：每条 quote 只归到一个分区，优先级：等待胶囊 > 已开启胶囊 > 收藏 > 普通
    val pendingCapsule = quotes.filter {
        it.capsuleDeliveryAt > 0 && !it.capsuleDelivered
    }
    val openedCapsule = quotes.filter {
        it.capsuleDeliveryAt > 0 && it.capsuleDelivered
    }
    val pinned = quotes.filter { it.capsuleDeliveryAt == 0L && it.pinned }
    val regular = quotes.filter { it.capsuleDeliveryAt == 0L && !it.pinned }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (pendingCapsule.isNotEmpty()) {
            item { SectionTitle("⏳ 等待开启的胶囊（${pendingCapsule.size}）") }
            items(pendingCapsule, key = { "p_${it.id}" }) { q ->
                QuoteCard(q, locked = true, onClick = { onOpenBook(q.bookId) },
                    onPin = { onTogglePin(q) }, onDelete = { onDelete(q) })
            }
        }
        if (pinned.isNotEmpty()) {
            item { SectionTitle("📌 收藏的句子（${pinned.size}）") }
            items(pinned, key = { "f_${it.id}" }) { q ->
                QuoteCard(q, onClick = { onOpenBook(q.bookId) },
                    onPin = { onTogglePin(q) }, onDelete = { onDelete(q) })
            }
        }
        if (regular.isNotEmpty()) {
            item { SectionTitle("📝 摘抄本（${regular.size}）") }
            items(regular, key = { "r_${it.id}" }) { q ->
                QuoteCard(q, onClick = { onOpenBook(q.bookId) },
                    onPin = { onTogglePin(q) }, onDelete = { onDelete(q) })
            }
        }
        if (openedCapsule.isNotEmpty()) {
            item { SectionTitle("✉️ 已经开启的胶囊（${openedCapsule.size}）") }
            items(openedCapsule, key = { "o_${it.id}" }) { q ->
                QuoteCard(q, opened = true, onClick = { onOpenBook(q.bookId) },
                    onPin = { onTogglePin(q) }, onDelete = { onDelete(q) })
            }
        }
    }
}

@Composable
private fun GalaxyTeaserTab(onEnter: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(RT.galaxyBackground())
            .clickable(onClick = onEnter),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✨", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text("摘抄星河", color = Color.White,
                fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text("点进来，看陌生人的句子是怎么发光的。",
                color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(RT.GalaxyAccent)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) { Text("进入星河", color = RT.GalaxyBgTop, fontWeight = FontWeight.Bold) }
        }
    }
}

/* ════════════════════════════════════════════════════════════
   小组件
   ════════════════════════════════════════════════════════════ */

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = RT.PrimaryInk, fontSize = 14.sp,
        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
}

@Composable
private fun HintCard(text: String, bg: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(14.dp)
    ) {
        Text(text, color = RT.PrimaryInk, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun EmptyHint(
    emoji: String,
    title: String,
    sub: String,
    cta: String?,
    onCta: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, color = RT.PrimaryInk, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(sub, color = RT.SecondaryInk, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp))
        if (cta != null) {
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(RT.heroGradient())
                    .clickable(onClick = onCta)
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) { Text(cta, color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}

/* ════════════════════════════════════════════════════════════
   打卡 BottomSheet
   ════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckInBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit,
) {
    var minutes by remember { mutableStateOf(15) }
    val presets = listOf(5, 10, 15, 30, 60)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = RT.CardCream) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("阅读打卡", color = RT.PrimaryInk,
                fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("把今天读了多久记下来。",
                color = RT.SecondaryInk, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            Text("$minutes 分钟",
                color = RT.AccentOrange, fontSize = 36.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { m ->
                    val sel = m == minutes
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) RT.PrimaryInk else RT.CardSoft)
                            .clickable { minutes = m }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text("$m", color = if (sel) Color.White else RT.PrimaryInk,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Slider(
                value = minutes.toFloat(),
                onValueChange = { minutes = it.toInt().coerceIn(1, 240) },
                valueRange = 1f..240f
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RT.heroGradient())
                    .clickable { onSubmit(minutes) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("✓ 完成打卡", color = Color.White,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
