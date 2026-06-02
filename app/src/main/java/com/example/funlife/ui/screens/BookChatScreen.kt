// BookChatScreen.kt — v53 阅光书房 · AI 读书伴侣
package com.example.funlife.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import com.example.funlife.viewmodel.BookChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookChatScreen(
    userId: Long,
    bookId: Long,
    onBack: () -> Unit,
    /** > 0 表示加载已有对话档案；0 = 新对话 */
    sessionId: Long = 0L,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as android.app.Application
    val vm: BookChatViewModel = viewModel(
        key = "BookChat_${userId}_${bookId}_$sessionId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                BookChatViewModel(app, userId, bookId, sessionId) as T
        }
    )
    val msgs by vm.msgs.collectAsState()
    val sending by vm.sending.collectAsState()
    val quota by vm.quota.collectAsState()
    val title by vm.bookTitle.collectAsState()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(msgs.size) {
        if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1)
    }

    Box(Modifier.fillMaxSize().background(RT.pageBackground())) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = RT.PrimaryInk)
                }
                Column(Modifier.weight(1f)) {
                    Text("AI 读书伴侣", color = RT.PrimaryInk,
                        fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    if (title.isNotBlank()) {
                        Text("聊《$title》", color = RT.SecondaryInk, fontSize = 11.sp)
                    }
                }
                if (quota.second > 0) {
                    Text("${quota.first}/${quota.second}",
                        color = RT.SecondaryInk, fontSize = 11.sp,
                        modifier = Modifier.padding(end = 12.dp))
                } else if (quota.second == -1) {
                    Text("∞", color = RT.AccentOrange, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 12.dp))
                }
            }
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(msgs.size) { i ->
                    when (val m = msgs[i]) {
                        is BookChatViewModel.Msg.User -> Bubble(m.text, isUser = true)
                        is BookChatViewModel.Msg.Ai -> Bubble(m.text, isUser = false)
                        is BookChatViewModel.Msg.System ->
                            Box(
                                Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(m.text,
                                    color = RT.MutedInk, fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.7f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                    }
                }
                if (sending) {
                    item {
                        Row {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(RT.heroGradient()),
                                contentAlignment = Alignment.Center
                            ) { Text("🤖", fontSize = 18.sp) }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(RT.CardCream)
                                    .padding(14.dp)
                            ) {
                                Text("正在思考…",
                                    color = RT.MutedInk, fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic)
                            }
                        }
                    }
                }
            }
            // 输入条
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(RT.CardCream)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(800) },
                    placeholder = { Text("写下你的疑问、感受、想跟书里那个人说的话…") },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (input.trim().isNotEmpty() && !sending)
                                RT.heroGradient()
                            else androidx.compose.ui.graphics.SolidColor(RT.MutedInk)
                        )
                        .clickable(enabled = input.trim().isNotEmpty() && !sending) {
                            vm.send(input)
                            input = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Send, "发送", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun Bubble(text: String, isUser: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RT.heroGradient()),
                contentAlignment = Alignment.Center
            ) { Text("🤖", fontSize = 18.sp) }
            Spacer(Modifier.width(8.dp))
        }
        Box(
            Modifier
                .widthIn(max = 280.dp)
                .clip(
                    if (isUser) RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
                    else RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                )
                .background(if (isUser) RT.PrimaryInk else RT.CardCream)
                .padding(12.dp)
        ) {
            Text(
                text,
                color = if (isUser) Color.White else RT.PrimaryInk,
                fontSize = 14.sp, lineHeight = 22.sp
            )
        }
    }
}
