// BookDetailScreen.kt — v53 阅光书房 · 单本书详情
//
// 结构：
//   ① 顶栏：返回 + 分享快照 + 删除（沿用 BookViewModel 行为，本页只读型）
//   ② Hero：书名 + 作者 + 评分 + 阅读进度 + 已读分钟
//   ③ 开篇期待 / 完成宣言（点击编辑；空状态有引导文案）
//   ④ 阅读心电图（仅在有打卡 atPage 时显现）
//   ⑤ AI 读书伴侣 入口（VIP1+ / 免费用户日 1 次）
//   ⑥ 摘抄列表（支持加新摘抄 / 寄胶囊）
package com.example.funlife.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.data.model.Book
import com.example.funlife.ui.components.BookEcgCurve
import com.example.funlife.ui.components.QuoteCard
import com.example.funlife.ui.theme.ReadingRoomTheme as RT
import com.example.funlife.viewmodel.BookDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    userId: Long,
    bookId: Long,
    onNavigateBack: () -> Unit,
    onOpenChat: (bookId: Long, sessionId: Long) -> Unit,
    onOpenSnapshot: (bookId: Long) -> Unit = {},
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as android.app.Application
    val vm: BookDetailViewModel = viewModel(
        key = "BookDetail_${userId}_$bookId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                BookDetailViewModel(app, userId, bookId) as T
        }
    )
    val book by vm.book.collectAsState()
    val quotes by vm.quotes.collectAsState()
    val ecg by vm.ecg.collectAsState()
    val totalMin by vm.totalMinutes.collectAsState()
    val toast by vm.toast.collectAsState()
    val justFinished by vm.justFinished.collectAsState()
    val chatSessions by vm.chatSessions.collectAsState()
    val archiveUnlocked by vm.archiveUnlocked.collectAsState()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(toast) { toast?.let { snack.showSnackbar(it); vm.consumeToast() } }

    var showOpeningEditor by remember { mutableStateOf(false) }
    var showFinishedEditor by remember { mutableStateOf(false) }
    var showProgressEditor by remember { mutableStateOf(false) }
    var showQuoteSheet by remember { mutableStateOf(false) }
    var showCheckInSheet by remember { mutableStateOf(false) }
    var showSnapshotDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        containerColor = Color.Transparent,
    ) { padding ->
        Box(Modifier.fillMaxSize().background(RT.pageBackground())) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = RT.PrimaryInk)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showSnapshotDialog = true; onOpenSnapshot(bookId) }) {
                        Icon(Icons.Default.Share, "生成阅读快照", tint = RT.PrimaryInk)
                    }
                }
                val b = book
                if (b == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RT.AccentOrange)
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { HeroCard(b, totalMin,
                            onProgressClick = { showProgressEditor = true },
                            onCheckIn = { showCheckInSheet = true }) }
                        item {
                            OpeningSection(
                                opening = b.openingLetter,
                                openingMood = b.openingMood,
                                onEdit = { showOpeningEditor = true }
                            )
                        }
                        item {
                            BookEcgCurve(
                                points = ecg,
                                totalPages = b.totalPages,
                                currentPage = b.currentPage,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                        item {
                            AiCompanionEntry(
                                onOpen = { onOpenChat(bookId, 0L) }   // 0 = 新对话
                            )
                        }
                        // 🆕 v54 读书档案（VIP3 / 永久会员专享）
                        if (archiveUnlocked && chatSessions.isNotEmpty()) {
                            item {
                                BookChatArchiveCard(
                                    sessions = chatSessions,
                                    onOpenSession = { sid ->
                                        onOpenChat(bookId, sid)
                                    },
                                )
                            }
                        }
                        item {
                            FinishedSection(
                                finishedAt = b.finishedAt,
                                finishedMood = b.finishedMood,
                                onMark = { showFinishedEditor = true },
                            )
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📝 摘抄（${quotes.size}）",
                                    color = RT.PrimaryInk, fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { showQuoteSheet = true }) {
                                    Text("+ 抄一段", color = RT.AccentRose, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (quotes.isEmpty()) {
                            item {
                                Text(
                                    "还没有摘抄。读到舍不得忘的句子，点击右上角抄下来。可以选择立刻收纳，或寄给未来的自己。",
                                    color = RT.MutedInk, fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                        items(quotes, key = { it.id }) { q ->
                            QuoteCard(
                                q,
                                locked = q.capsuleDeliveryAt > 0 && !q.capsuleDelivered,
                                opened = q.capsuleDeliveryAt > 0 && q.capsuleDelivered,
                                onClick = {},
                                onPin = { vm.togglePinned(q) },
                                onDelete = { vm.deleteQuote(q) },
                            )
                        }
                        item { Spacer(Modifier.height(40.dp)) }
                    }
                }
            }
        }
    }

    // 各类编辑 sheet
    if (showOpeningEditor && book != null) {
        OpeningEditor(
            book = book!!,
            onDismiss = { showOpeningEditor = false },
            onSave = { letter, mood ->
                vm.patchBook { it.copy(openingLetter = letter, openingMood = mood) }
                showOpeningEditor = false
            }
        )
    }
    if (showFinishedEditor) {
        FinishedEditor(
            onDismiss = { showFinishedEditor = false },
            onSave = { mood ->
                vm.markFinished(mood); showFinishedEditor = false
            }
        )
    }
    if (showProgressEditor && book != null) {
        ProgressEditor(
            book = book!!,
            onDismiss = { showProgressEditor = false },
            onSave = { total, current ->
                vm.patchBook { it.copy(totalPages = total, currentPage = current) }
                showProgressEditor = false
            }
        )
    }
    if (showQuoteSheet) {
        QuoteComposeSheet(
            initialPage = book?.currentPage ?: 0,
            onDismiss = { showQuoteSheet = false },
            onSave = { text, page, rating, capsuleDelay ->
                val deliveryAt = if (capsuleDelay > 0L)
                    System.currentTimeMillis() + capsuleDelay
                else 0L
                vm.saveQuote(text, page, rating, false, deliveryAt)
                showQuoteSheet = false
            }
        )
    }
    // E2 双向胶囊穿越对话：在"未读完 → 读完"的瞬间自动弹出
    if (justFinished && book != null) {
        TimeTravelDialog(
            book = book!!,
            onDismiss = { vm.consumeJustFinished() }
        )
    }

    if (showSnapshotDialog && book != null) {
        SnapshotShareDialog(
            book = book!!,
            ecg = ecg,
            topQuotes = quotes.filter { it.capsuleDeliveryAt == 0L || it.capsuleDelivered }
                .sortedByDescending { (if (it.pinned) 1000 else 0) + it.rating }
                .take(3),
            totalMinutes = totalMin,
            onDismiss = { showSnapshotDialog = false }
        )
    }
    if (showCheckInSheet && book != null) {
        BookCheckInSheet(
            currentPage = book!!.currentPage,
            totalPages = book!!.totalPages,
            onDismiss = { showCheckInSheet = false },
            onSubmit = { mins, page ->
                vm.checkIn(mins, page)
                if (page > book!!.currentPage) {
                    vm.patchBook { it.copy(currentPage = page) }
                }
                showCheckInSheet = false
            }
        )
    }
}

/* ════════════════════════════════════════════════════════════
   各 section
   ════════════════════════════════════════════════════════════ */

@Composable
private fun HeroCard(
    b: Book,
    totalMinutes: Int,
    onProgressClick: () -> Unit,
    onCheckIn: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(RT.CardCream)
            .padding(20.dp)
    ) {
        Text(b.title, color = RT.PrimaryInk,
            fontSize = 22.sp, fontWeight = FontWeight.Black)
        if (b.author.isNotBlank()) {
            Text(b.author, color = RT.SecondaryInk, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (b.rating > 0) {
                Text("★".repeat(b.rating),
                    color = RT.AccentGold, fontSize = 14.sp)
            }
            if (b.tags.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Text(b.tags, color = RT.SecondaryInk, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        // 进度条
        val progress = if (b.totalPages > 0)
            (b.currentPage.toFloat() / b.totalPages).coerceIn(0f, 1f)
        else 0f
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(RT.MutedInk.copy(alpha = 0.2f))
                .clickable(onClick = onProgressClick)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(RT.heroGradient())
                    .clip(RoundedCornerShape(4.dp))
            )
        }
        Spacer(Modifier.height(6.dp))
        Row {
            Text(
                if (b.totalPages > 0) "${b.currentPage} / ${b.totalPages} 页"
                else "未设置页数 · 点进度条添加",
                color = RT.SecondaryInk, fontSize = 11.sp,
                modifier = Modifier.clickable(onClick = onProgressClick)
            )
            Spacer(Modifier.weight(1f))
            Text("⏱ 共读 $totalMinutes 分钟",
                color = RT.AccentOrange, fontSize = 11.sp,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RT.heroGradient())
                .clickable(onClick = onCheckIn)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("📖 阅读这本书", color = Color.White,
                fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OpeningSection(
    opening: String,
    openingMood: String,
    onEdit: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RT.CardSky)
            .clickable(onClick = onEdit)
            .padding(16.dp)
    ) {
        Row {
            Text("📜 翻开第一页时的我",
                color = RT.PrimaryInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (openingMood.isNotBlank()) {
                Text(openingMood, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (opening.isBlank()) {
            Text(
                "写一句话给这本书。等你读完后回头看，会知道这一年的自己是什么样子。",
                color = RT.MutedInk, fontSize = 12.sp, fontStyle = FontStyle.Italic
            )
        } else {
            Text(opening, color = RT.PrimaryInk, fontSize = 14.sp,
                fontStyle = FontStyle.Italic, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun FinishedSection(
    finishedAt: Long,
    finishedMood: String,
    onMark: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (finishedAt > 0) RT.CardPeach else RT.CardSoft)
            .clickable(onClick = onMark)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (finishedAt > 0) "🎉 已经读完了" else "📕 标记读完",
                color = RT.PrimaryInk, fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            if (finishedAt > 0) {
                val date = java.text.SimpleDateFormat(
                    "yyyy-MM-dd", java.util.Locale.getDefault()
                ).format(java.util.Date(finishedAt))
                Text(date, color = RT.SecondaryInk, fontSize = 11.sp)
            }
        }
        if (finishedMood.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(finishedMood, color = RT.PrimaryInk,
                fontSize = 14.sp, fontStyle = FontStyle.Italic, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun AiCompanionEntry(onOpen: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RT.CardCream)
            .clickable(onClick = onOpen)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RT.heroGradient()),
                contentAlignment = Alignment.Center
            ) { Text("🤖", fontSize = 18.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("AI 读书伴侣", color = RT.PrimaryInk,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("聊聊这本书 · 把感受讲给一个会回答的朋友",
                    color = RT.SecondaryInk, fontSize = 11.sp)
            }
            Text("›", color = RT.MutedInk, fontSize = 22.sp)
        }
    }
}

/* ════════════════════════════════════════════════════════════
   各编辑 Sheet
   ════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpeningEditor(
    book: Book,
    onDismiss: () -> Unit,
    onSave: (letter: String, mood: String) -> Unit,
) {
    var text by remember { mutableStateOf(book.openingLetter) }
    var mood by remember { mutableStateOf(book.openingMood) }
    val moodOptions = listOf("📖", "✨", "🌧", "🌅", "🌙", "🍃", "💭", "🎈")
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = RT.CardCream) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("翻开第一页时的我", color = RT.PrimaryInk,
                fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("不需要长，三言两语就够。重要的是这一刻你是什么样子。",
                color = RT.SecondaryInk, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(280) },
                placeholder = { Text("我希望读完它之后…") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text("当下的心情",
                color = RT.SecondaryInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                moodOptions.forEach { m ->
                    val sel = m == mood
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) RT.AccentOrange.copy(alpha = 0.3f) else RT.CardSoft)
                            .clickable { mood = if (sel) "" else m }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) { Text(m, fontSize = 18.sp) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RT.heroGradient())
                    .clickable { onSave(text.trim(), mood) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text("保存", color = Color.White, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinishedEditor(
    onDismiss: () -> Unit,
    onSave: (mood: String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = RT.CardCream) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("🎉 你读完了它", color = RT.PrimaryInk,
                fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("此刻什么感受？写一句话给以后的自己。",
                color = RT.SecondaryInk, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(120) },
                placeholder = { Text("合上书的瞬间，我想说…") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RT.heroGradient())
                    .clickable { onSave(text.trim()) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text("✓ 标记读完", color = Color.White, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressEditor(
    book: Book,
    onDismiss: () -> Unit,
    onSave: (total: Int, current: Int) -> Unit,
) {
    var total by remember { mutableStateOf(if (book.totalPages > 0) book.totalPages.toString() else "") }
    var current by remember { mutableStateOf(book.currentPage.toString()) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = RT.CardCream) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("阅读进度", color = RT.PrimaryInk,
                fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = total, onValueChange = { total = it.filter { c -> c.isDigit() }.take(5) },
                label = { Text("总页数") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = current, onValueChange = { current = it.filter { c -> c.isDigit() }.take(5) },
                label = { Text("当前页") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RT.heroGradient())
                    .clickable {
                        val t = total.toIntOrNull() ?: 0
                        val c = current.toIntOrNull() ?: 0
                        onSave(t, c)
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text("保存", color = Color.White, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteComposeSheet(
    initialPage: Int,
    onDismiss: () -> Unit,
    onSave: (text: String, page: Int, rating: Int, capsuleDelayMs: Long) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var page by remember { mutableStateOf(initialPage.takeIf { it > 0 }?.toString() ?: "") }
    var rating by remember { mutableStateOf(0) }
    var asCapsule by remember { mutableStateOf(false) }
    var delayDays by remember { mutableStateOf(30) }
    val delayPresets = listOf(1, 7, 30, 90, 180, 365)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = RT.CardCream) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("抄一段", color = RT.PrimaryInk,
                fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(500) },
                placeholder = { Text("舍不得忘的那一句…") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = page, onValueChange = { page = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("第几页") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("打几星", color = RT.SecondaryInk, fontSize = 11.sp)
                    Row {
                        repeat(5) { i ->
                            Text(
                                if (i < rating) "★" else "☆",
                                color = RT.AccentGold, fontSize = 22.sp,
                                modifier = Modifier
                                    .clickable { rating = if (rating == i + 1) 0 else i + 1 }
                                    .padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // 胶囊开关
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (asCapsule) RT.AccentSky.copy(alpha = 0.18f) else RT.CardSoft)
                    .clickable { asCapsule = !asCapsule }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(checked = asCapsule, onCheckedChange = { asCapsule = it })
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("✉️ 寄给未来的自己",
                        color = RT.PrimaryInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("到投递日才能再看到这段话",
                        color = RT.SecondaryInk, fontSize = 11.sp)
                }
            }
            if (asCapsule) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    delayPresets.forEach { d ->
                        val sel = d == delayDays
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) RT.PrimaryInk else RT.CardSoft)
                                .clickable { delayDays = d }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "$d 天",
                                color = if (sel) Color.White else RT.PrimaryInk,
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (text.trim().isNotEmpty()) RT.heroGradient()
                        else androidx.compose.ui.graphics.SolidColor(RT.MutedInk)
                    )
                    .clickable(enabled = text.trim().isNotEmpty()) {
                        val capsuleMs = if (asCapsule) delayDays * 24L * 3600 * 1000 else 0L
                        onSave(text.trim(), page.toIntOrNull() ?: 0, rating, capsuleMs)
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (asCapsule) "✨ 寄出胶囊" else "✓ 收入摘抄本",
                    color = Color.White, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookCheckInSheet(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onSubmit: (minutes: Int, atPage: Int) -> Unit,
) {
    var minutes by remember { mutableStateOf(15) }
    var page by remember { mutableStateOf(currentPage.toString()) }
    val presets = listOf(5, 10, 15, 30, 60)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = RT.CardCream) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("阅读这本书", color = RT.PrimaryInk,
                fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text("$minutes 分钟",
                color = RT.AccentOrange, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                presets.forEach { m ->
                    val sel = m == minutes
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) RT.PrimaryInk else RT.CardSoft)
                            .clickable { minutes = m }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("$m", color = if (sel) Color.White else RT.PrimaryInk,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Slider(
                value = minutes.toFloat(),
                onValueChange = { minutes = it.toInt().coerceIn(1, 240) },
                valueRange = 1f..240f
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = page,
                onValueChange = { page = it.filter { c -> c.isDigit() }.take(5) },
                label = { Text(if (totalPages > 0) "读到第几页（共 $totalPages 页）" else "读到第几页") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RT.heroGradient())
                    .clickable { onSubmit(minutes, page.toIntOrNull() ?: 0) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("✓ 完成打卡", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

/* ════════════════════════════════════════════════════════════
   🆕 v54 · 读书档案卡片（VIP3 / 永久会员专享）
   ════════════════════════════════════════════════════════════ */

@Composable
private fun BookChatArchiveCard(
    sessions: List<com.example.funlife.data.model.BookChatSession>,
    onOpenSession: (Long) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RT.CardCream)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📒 读书档案", color = RT.PrimaryInk,
                fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(RT.AccentGold.copy(alpha = 0.25f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("VIP3", color = RT.AccentOrange,
                    fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.weight(1f))
            Text("${sessions.size} 次对话", color = RT.SecondaryInk, fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text("和 AI 聊过的话都会留在这里。点开一次可以继续聊下去。",
            color = RT.SecondaryInk, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))
        sessions.take(5).forEach { s ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenSession(s.id) }
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(s.title.ifBlank { "未命名对话" },
                        color = RT.PrimaryInk, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        "${s.turnCount} 轮 · " + java.text.SimpleDateFormat(
                            "MM-dd HH:mm", java.util.Locale.getDefault()
                        ).format(java.util.Date(s.lastMessageAt)),
                        color = RT.MutedInk, fontSize = 11.sp
                    )
                }
                Text("›", color = RT.MutedInk, fontSize = 18.sp)
            }
        }
        if (sessions.size > 5) {
            Spacer(Modifier.height(4.dp))
            Text("还有 ${sessions.size - 5} 次更早的对话…",
                color = RT.MutedInk, fontSize = 11.sp)
        }
    }
}

/* ════════════════════════════════════════════════════════════
   E2 · 穿越对话 Dialog —— 读完瞬间弹出"开篇的我 ↔ 读完的我"对照
   ════════════════════════════════════════════════════════════ */

@Composable
private fun TimeTravelDialog(
    book: Book,
    onDismiss: () -> Unit,
) {
    val openingDate = if (book.createdAt > 0)
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date(book.createdAt))
    else "—"
    val finishedDate = java.text.SimpleDateFormat(
        "yyyy-MM-dd", java.util.Locale.getDefault()
    ).format(java.util.Date(book.finishedAt))

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RT.CardCream,
        title = {
            Column {
                Text("🎉 你读完了这本书",
                    color = RT.PrimaryInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text("和翻开第一页的自己打个招呼吧",
                    color = RT.SecondaryInk, fontSize = 11.sp)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // 左：开篇的我
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(RT.CardSky)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📜 开篇的我", color = RT.PrimaryInk,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            if (book.openingMood.isNotBlank()) {
                                Text(book.openingMood, fontSize = 16.sp)
                            }
                            Text("  $openingDate",
                                color = RT.MutedInk, fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (book.openingLetter.isBlank())
                                "（那时候没有留下话给自己。下一本书翻开前，写一句吧。）"
                            else book.openingLetter,
                            color = RT.PrimaryInk, fontSize = 13.sp,
                            fontStyle = FontStyle.Italic, lineHeight = 20.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                // 中间：穿越分隔符
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⋯  ⋯  ⋯", color = RT.AccentOrange,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                // 右：读完的我
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(RT.CardPeach)
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✨ 读完的我", color = RT.PrimaryInk,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text(finishedDate, color = RT.MutedInk, fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (book.finishedMood.isBlank())
                                "（合上书的那一刻，你没留下话。下次别忘记。）"
                            else book.finishedMood,
                            color = RT.PrimaryInk, fontSize = 13.sp,
                            fontStyle = FontStyle.Italic, lineHeight = 20.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("收下了 ☕", color = RT.AccentOrange, fontWeight = FontWeight.Bold)
            }
        }
    )
}
