// PostcardDriftScreen.kt — v53 阅光书房 · 明信片漂流
//
// 上半部分：寄一张（仅 VIP2+）
// 下半部分：收件箱（已收到的、可点 ❤）
package com.example.funlife.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.funlife.ui.theme.ReadingRoomTheme as RT
import com.example.funlife.viewmodel.PostcardDriftViewModel
import com.example.funlife.vip.PostcardDriftCloudRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostcardDriftScreen(
    userId: Long,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as android.app.Application
    val vm: PostcardDriftViewModel = viewModel(
        key = "Postcard_$userId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                PostcardDriftViewModel(app, userId) as T
        }
    )
    val inbox by vm.inbox.collectAsState()
    val loading by vm.loading.collectAsState()
    val toast by vm.toast.collectAsState()
    val vipLevel by vm.vipLevel.collectAsState()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(toast) { toast?.let { snack.showSnackbar(it); vm.consumeToast() } }

    var showCompose by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        containerColor = Color.Transparent,
    ) { padding ->
        Box(Modifier.fillMaxSize().background(RT.pageBackground())) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = RT.PrimaryInk)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("📮 明信片漂流", color = RT.PrimaryInk,
                            fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text("匿名 · 随机送给一位陌生 VIP 读者",
                            color = RT.SecondaryInk, fontSize = 11.sp)
                    }
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, "刷新", tint = RT.PrimaryInk)
                    }
                }
                // 寄出 CTA
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (vipLevel >= 2) RT.heroGradient()
                            else androidx.compose.ui.graphics.SolidColor(RT.MutedInk.copy(alpha = 0.6f))
                        )
                        .clickable(enabled = vipLevel >= 2) { showCompose = true }
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Column {
                        Text(
                            if (vipLevel >= 2) "✉️ 寄一张明信片"
                            else "🔒 季卡及以上才能寄明信片",
                            color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black
                        )
                        Text(
                            if (vipLevel >= 2) "本月配额：VIP2 = 1 / VIP3 = 4"
                            else "你可以先收到陌生人寄来的明信片",
                            color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp
                        )
                    }
                }
                // 收件箱
                Text("📬 收件箱（${inbox.size}）",
                    color = RT.PrimaryInk, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
                if (loading && inbox.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RT.AccentOrange)
                    }
                } else if (inbox.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📮", fontSize = 48.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("还没有人寄信给你",
                            color = RT.PrimaryInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("先发出去几张试试看？",
                            color = RT.MutedInk, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(inbox, key = { it.id }) { p ->
                            PostcardItem(p, onReact = { vm.react(p.id) })
                        }
                        item { Spacer(Modifier.height(40.dp)) }
                    }
                }
            }
        }
    }

    if (showCompose) {
        ComposePostcardSheet(
            onDismiss = { showCompose = false },
            onSubmit = { txt, book -> vm.send(txt, book); showCompose = false }
        )
    }
}

@Composable
private fun PostcardItem(p: PostcardDriftCloudRepository.Postcard, onReact: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(RT.CardCream)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "「${p.text}」",
                color = RT.PrimaryInk, fontSize = 15.sp, lineHeight = 24.sp,
                fontStyle = FontStyle.Italic
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (p.bookTitle.isNotBlank()) {
                    Text("—— 出自《${p.bookTitle}》",
                        color = RT.SecondaryInk, fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(p.sentAt)),
                    color = RT.MutedInk, fontSize = 10.sp
                )
                Spacer(Modifier.width(10.dp))
                IconButton(onClick = onReact, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (p.reactedHeart) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "心动",
                        tint = if (p.reactedHeart) RT.AccentRose else RT.MutedInk
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposePostcardSheet(
    onDismiss: () -> Unit,
    onSubmit: (text: String, bookTitle: String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var book by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = RT.CardCream) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("寄一张明信片", color = RT.PrimaryInk,
                fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("匿名 · 系统会随机送给一位 VIP 陌生人",
                color = RT.SecondaryInk, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(200) },
                placeholder = { Text("一段你想寄给陌生人的话…") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = book, onValueChange = { book = it.take(80) },
                placeholder = { Text("出自哪本书？（选填）") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("${text.length} / 200", color = RT.MutedInk, fontSize = 10.sp)
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (text.trim().isNotEmpty()) RT.heroGradient()
                        else androidx.compose.ui.graphics.SolidColor(RT.MutedInk)
                    )
                    .clickable(enabled = text.trim().isNotEmpty()) {
                        onSubmit(text.trim(), book.trim())
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("✉️ 寄出", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
