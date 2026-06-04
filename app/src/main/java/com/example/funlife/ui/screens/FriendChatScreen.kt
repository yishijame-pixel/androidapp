package com.example.funlife.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.funlife.social.model.ChatMessageUiModel
import com.example.funlife.social.model.ChatPeerProfile
import com.example.funlife.social.model.ChatUiState
import com.example.funlife.viewmodel.ChatToast
import com.example.funlife.viewmodel.ChatToastTone
import com.example.funlife.viewmodel.FriendChatViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 企业 IM 中性色板（微信风格） */
private object ChatPalette {
    /** 微信聊天页顶栏 / 状态栏同色 */
    val toolbar = Color(0xFFF7F7F7)
    val canvas = Color(0xFFEDEDED)
    val surface = Color.White
    val toolbarDivider = Color(0xFFE7E7E7)
    val mine = Color(0xFF95EC69)
    val mineText = Color(0xFF111111)
    val theirs = Color.White
    val theirsText = Color(0xFF111111)
    val ink = Color(0xFF111111)
    val inkSecondary = Color(0xFF576B95)
    val inkMuted = Color(0xFF888888)
    val inkFaint = Color(0xFFB2B2B2)
    val inputBg = Color(0xFFF7F7F7)
    val inputBorder = Color(0xFFE6E6E6)
    val sendActive = Color(0xFF07C160)
    val sendDisabled = Color(0xFFB8E6C8)
    val dateChip = Color(0xCCDAE1EA)
    val dateText = Color(0xFFFFFFFF)
    val avatarStart = Color(0xFF5B8DEF)
    val avatarEnd = Color(0xFF3D6FD8)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendChatScreen(
    viewModel: FriendChatViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val toast by viewModel.toast.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val isLoadingHistory by viewModel.isLoadingHistory.collectAsState()
    val hasMoreHistory by viewModel.hasMoreHistory.collectAsState()
    val pbAuthToken by viewModel.pbAuthToken.collectAsState()
    var input by remember { mutableStateOf("") }
    var inputFocused by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    val view = LocalView.current

    val messages = (uiState as? ChatUiState.Ready)?.messages.orEmpty()

    suspend fun scrollToLatest() {
        if (messages.isEmpty()) return
        listState.animateScrollToItem(messages.lastIndex)
    }

    LaunchedEffect(messages.lastOrNull()?.id) {
        if (messages.isEmpty()) return@LaunchedEffect
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: messages.lastIndex
        val nearBottom = lastVisible >= messages.lastIndex - 2
        if (nearBottom || inputFocused || imeVisible) {
            scrollToLatest()
        }
    }

    LaunchedEffect(imeVisible, inputFocused) {
        if ((imeVisible || inputFocused) && messages.isNotEmpty()) {
            delay(80)
            scrollToLatest()
            if (imeVisible) {
                delay(120)
                scrollToLatest()
            }
        }
    }

    DisposableEffect(Unit) {
        if (!view.isInEditMode) {
            val window = (view.context as Activity).window
            val prevStatus = window.statusBarColor
            val controller = WindowCompat.getInsetsController(window, view)
            val prevLight = controller.isAppearanceLightStatusBars
            @Suppress("DEPRECATION")
            window.statusBarColor = ChatPalette.toolbar.toArgb()
            controller.isAppearanceLightStatusBars = true
            onDispose {
                @Suppress("DEPRECATION")
                window.statusBarColor = prevStatus
                controller.isAppearanceLightStatusBars = prevLight
            }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(toast) {
        toast?.let {
            delay(2_500)
            viewModel.consumeToast()
        }
    }

    LaunchedEffect(listState, hasMoreHistory, isLoadingHistory) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            if (hasMoreHistory && !isLoadingHistory && index == 0 && offset == 0) {
                viewModel.loadOlderMessages()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatPalette.canvas)
            .imePadding(),
    ) {
        ChatAppBar(
            uiState = uiState,
            onNavigateBack = {
                focusManager.clearFocus()
                onNavigateBack()
            },
            onRefresh = { viewModel.refresh() },
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val state = uiState) {
                ChatUiState.Loading -> ChatLoadingSkeleton()
                is ChatUiState.Error -> ChatErrorPanel(
                    message = state.message,
                    onRetry = { viewModel.retryBootstrap() },
                )
                is ChatUiState.Ready -> {
                    if (state.messages.isEmpty()) {
                        ChatEmptyHint()
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 10.dp,
                                end = 10.dp,
                                top = 10.dp,
                                bottom = 8.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (isLoadingHistory) {
                                item(key = "history_loading") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = ChatPalette.inkMuted,
                                        )
                                    }
                                }
                            } else if (hasMoreHistory) {
                                item(key = "history_hint") {
                                    Text(
                                        text = "上滑加载更早消息",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp,
                                        color = ChatPalette.inkFaint,
                                    )
                                }
                            }
                            itemsIndexed(
                                state.messages,
                                key = { _, msg -> msg.id },
                            ) { index, msg ->
                                val prev = state.messages.getOrNull(index - 1)
                                if (shouldShowDateDivider(prev?.createdAt, msg.createdAt)) {
                                    ChatDateDivider(label = formatDateDivider(msg.createdAt))
                                    Spacer(Modifier.height(10.dp))
                                }
                                ChatMessageRow(
                                    message = msg,
                                    peer = state.peer,
                                    pbAuthToken = pbAuthToken,
                                    showAvatar = shouldShowPeerAvatar(state.messages, index),
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }

            toast?.let { t ->
                ChatToastBanner(
                    toast = t,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                )
            }
        }

        if (uiState is ChatUiState.Ready) {
            ChatComposer(
                value = input,
                onValueChange = { input = it },
                enabled = !isSending,
                isSending = isSending,
                onInputFocusChanged = { inputFocused = it },
                onSend = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        viewModel.sendMessage(text)
                        input = ""
                    }
                },
            )
        }
    }
}

@Composable
private fun ChatAppBar(
    uiState: ChatUiState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatPalette.toolbar),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "返回", modifier = Modifier.size(22.dp))
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when (uiState) {
                    is ChatUiState.Ready -> {
                        Text(
                            uiState.peer.displayName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = ChatPalette.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                    is ChatUiState.Error -> {
                        Text(
                            "私聊",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    ChatUiState.Loading -> {
                        Text(
                            "加载中…",
                            fontSize = 16.sp,
                            color = ChatPalette.inkMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            IconButton(onClick = onRefresh, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "刷新", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { /* 预留 */ }, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "更多", modifier = Modifier.size(22.dp))
            }
        }
        Divider(color = ChatPalette.toolbarDivider, thickness = 0.5.dp)
    }
}

@Composable
private fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    isSending: Boolean,
    onInputFocusChanged: (Boolean) -> Unit,
    onSend: () -> Unit,
) {
    val canSend = enabled && value.isNotBlank() && !isSending
    Column(modifier = Modifier.fillMaxWidth().background(ChatPalette.surface)) {
        Divider(color = ChatPalette.toolbarDivider, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp, max = 120.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ChatPalette.inputBg)
                    .border(0.5.dp, ChatPalette.inputBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (value.isEmpty()) {
                    Text(
                        "输入消息…",
                        color = ChatPalette.inkFaint,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterStart)
                        .onFocusChanged { onInputFocusChanged(it.isFocused) },
                    textStyle = TextStyle(
                        color = ChatPalette.ink,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                    ),
                    cursorBrush = SolidColor(ChatPalette.sendActive),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (canSend) ChatPalette.sendActive else ChatPalette.sendDisabled),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "发送",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageRow(
    message: ChatMessageUiModel,
    peer: ChatPeerProfile,
    pbAuthToken: String?,
    showAvatar: Boolean,
) {
    if (message.isMine) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(modifier = Modifier.widthIn(max = 260.dp)) {
                ChatBubble(message = message, isMine = true)
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top,
        ) {
            if (showAvatar) {
                ChatPeerAvatar(
                    name = peer.displayName,
                    avatarUrl = peer.avatarUrl,
                    pbAuthToken = pbAuthToken,
                    size = 38.dp,
                )
            } else {
                Spacer(Modifier.width(38.dp))
            }
            Spacer(Modifier.width(6.dp))
            Box(modifier = Modifier.widthIn(max = 260.dp)) {
                ChatBubble(message = message, isMine = false)
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessageUiModel, isMine: Boolean) {
    val shape = RoundedCornerShape(4.dp)
    val bg = if (isMine) ChatPalette.mine else ChatPalette.theirs
    Box(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .then(
                if (isMine) Modifier else Modifier.border(0.5.dp, Color(0xFFE0E0E0), shape),
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            message.body,
            color = if (isMine) ChatPalette.mineText else ChatPalette.theirsText,
            fontSize = 16.sp,
            lineHeight = 23.sp,
        )
    }
}

@Composable
private fun ChatDateDivider(label: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(ChatPalette.dateChip)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            color = ChatPalette.dateText,
        )
    }
}

@Composable
private fun ChatEmptyHint() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            Icon(
                Icons.Outlined.Forum,
                contentDescription = null,
                tint = ChatPalette.inkFaint,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "发送消息开始聊天",
                fontSize = 14.sp,
                color = ChatPalette.inkMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ChatLoadingSkeleton() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = ChatPalette.sendActive,
            strokeWidth = 2.dp,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun ChatErrorPanel(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            message,
            color = ChatPalette.inkMuted,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = ChatPalette.sendActive),
            shape = RoundedCornerShape(6.dp),
        ) {
            Text("重新连接")
        }
    }
}

@Composable
private fun ChatPeerAvatar(
    name: String,
    avatarUrl: String?,
    pbAuthToken: String?,
    size: Dp,
) {
    val context = LocalContext.current
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    @Composable
    fun InitialPlaceholder() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(ChatPalette.avatarStart, ChatPalette.avatarEnd))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initial,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = (size.value * 0.34f).sp,
            )
        }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            val needsAuth = avatarUrl.contains("/api/files/") && !pbAuthToken.isNullOrBlank()
            val model = remember(avatarUrl, pbAuthToken) {
                ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .crossfade(true)
                    .apply {
                        if (needsAuth) addHeader("Authorization", "Bearer $pbAuthToken")
                    }
                    .build()
            }
            SubcomposeAsyncImage(
                model = model,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { InitialPlaceholder() },
                error = { InitialPlaceholder() },
            )
        } else {
            InitialPlaceholder()
        }
    }
}

@Composable
private fun ChatToastBanner(toast: ChatToast, modifier: Modifier = Modifier) {
    val bg = when (toast.tone) {
        ChatToastTone.Error -> Color(0xFFFEE2E2)
        ChatToastTone.Success -> Color(0xFFDCFCE7)
        ChatToastTone.Info -> Color(0xFFEFF6FF)
    }
    Text(
        toast.message,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        fontSize = 13.sp,
        color = ChatPalette.ink,
    )
}

private fun shouldShowPeerAvatar(messages: List<ChatMessageUiModel>, index: Int): Boolean {
    return !messages[index].isMine
}

private fun shouldShowDateDivider(prevMs: Long?, currentMs: Long): Boolean {
    if (prevMs == null || prevMs <= 0L || currentMs <= 0L) return true
    return dayKey(prevMs) != dayKey(currentMs)
}

private fun dayKey(epochMs: Long): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
    return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
}

private fun formatDateDivider(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    val cal = Calendar.getInstance()
    val today = dayKey(cal.timeInMillis)
    cal.timeInMillis = epochMs
    val target = dayKey(epochMs)
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when (target) {
        today -> "今天"
        dayKey(yesterdayCal.timeInMillis) -> "昨天"
        else -> SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date(epochMs))
    }
}
