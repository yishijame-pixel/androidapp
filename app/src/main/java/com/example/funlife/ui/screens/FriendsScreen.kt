package com.example.funlife.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.funlife.social.PocketBaseConfig
import com.example.funlife.social.model.ConversationUiModel
import com.example.funlife.social.model.FriendUiModel
import com.example.funlife.social.model.FriendsUiState
import com.example.funlife.social.model.PbUserProfile
import com.example.funlife.social.model.SocialLinkState
import com.example.funlife.viewmodel.FriendsToast
import com.example.funlife.viewmodel.FriendsToastTone
import com.example.funlife.viewmodel.FriendsViewModel
import com.example.funlife.viewmodel.PendingFriendAction
import com.example.funlife.viewmodel.PendingFriendActionKind
import kotlinx.coroutines.delay

/** 企业级社交页调色板（会话列表对齐微信） */
private object FriendsPalette {
    val bgTop = Color(0xFFEDEDED)
    val bgBottom = Color(0xFFEDEDED)
    val listRow = Color(0xFFFFFFFF)
    val listDivider = Color(0xFFE5E5E5)
    val sectionBg = Color(0xFFEDEDED)
    val sectionText = Color(0xFF888888)
    val previewText = Color(0xFFB2B2B2)
    val timeText = Color(0xFFB2B2B2)
    val surface = Color(0xFFFFFFFF)
    val surfaceMuted = Color(0xFFF8FAFC)
    val primary = Color(0xFF2563EB)
    val primaryDark = Color(0xFF1D4ED8)
    val primarySoft = Color(0xFFEFF6FF)
    val ink = Color(0xFF000000)
    val inkSecondary = Color(0xFF475569)
    val inkMuted = Color(0xFF94A3B8)
    val border = Color(0xFFE2E8F0)
    val success = Color(0xFF059669)
    val successSoft = Color(0xFFECFDF5)
    val warning = Color(0xFFD97706)
    val warningSoft = Color(0xFFFFFBEB)
    val danger = Color(0xFFDC2626)
    val dangerSoft = Color(0xFFFEF2F2)
    val online = Color(0xFF22C55E)
    val headerLine = Color(0xFFE7E7E7)
    val toolbar = Color(0xFFF7F7F7)
    val searchFill = Color(0xFFEDEDED)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (peerPbId: String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val linkState by viewModel.linkState.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val sendingToPbId by viewModel.sendingToPbId.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val pbAuthToken by viewModel.pbAuthToken.collectAsState()
    val sessionReady by viewModel.sessionReady.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val toast by viewModel.toast.collectAsState()
    var selectedTab by remember { mutableIntStateOf(1) }
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var remarkDialogFriend by remember { mutableStateOf<FriendUiModel?>(null) }
    var remarkText by remember { mutableStateOf("") }

    LaunchedEffect(toast) {
        toast?.let {
            delay(2_800)
            viewModel.consumeToast()
        }
    }

    // 进入页时自动尝试绑定一次（PocketBase 刚启动 / 换 WiFi 后常见）
    LaunchedEffect(Unit) {
        delay(400)
        if (!sessionReady) viewModel.retryBootstrap()
    }

    val readyState = uiState as? FriendsUiState.Ready

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            FriendsTopBar(
                pendingCount = (readyState?.pendingIn?.size ?: 0) + (readyState?.pendingOut?.size ?: 0),
                isRefreshing = isSyncing,
                searchExpanded = searchExpanded,
                onSearchExpandedChange = { expanded ->
                    searchExpanded = expanded
                    if (!expanded) searchQuery = ""
                },
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearch = { viewModel.searchUser(searchQuery) },
                searchEnabled = !isSearching,
                isSearching = isSearching,
                sessionReady = sessionReady,
                searchResult = searchResult,
                onSendRequest = { viewModel.sendRequest(it) },
                sendingToPbId = sendingToPbId,
                pbAuthToken = pbAuthToken,
                onBack = {
                    if (searchExpanded) {
                        searchExpanded = false
                        searchQuery = ""
                    } else {
                        onNavigateBack()
                    }
                },
                onRefresh = {
                    if (linkState is SocialLinkState.Linked) viewModel.refresh()
                    else viewModel.retryBootstrap()
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(FriendsPalette.bgTop, FriendsPalette.bgBottom),
                    ),
                ),
        ) {
            when (linkState) {
                is SocialLinkState.NotConfigured,
                is SocialLinkState.Error -> {
                    StatusPanel(
                        icon = Icons.Outlined.PeopleOutline,
                        title = "社交服务不可用",
                        message = (linkState as? SocialLinkState.Error)?.message
                            ?: "请在 local.properties 配置 POCKETBASE_URL",
                        accent = FriendsPalette.warning,
                        onRetry = { viewModel.retryBootstrap() },
                    )
                    return@Box
                }
                is SocialLinkState.Linked,
                SocialLinkState.Linking -> Unit
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                if (linkState is SocialLinkState.Linking) {
                    item { ConnectingBanner() }
                }
                item {
                    FriendsSegmentTabs(
                        selected = selectedTab,
                        friendCount = readyState?.friends?.size ?: 0,
                        messageCount = conversations.size,
                        onSelect = { selectedTab = it },
                    )
                }
                if (selectedTab == 1) {
                    if (conversations.isEmpty()) {
                        item { EmptyConversationsHint() }
                    } else {
                        items(
                            conversations.size,
                            key = { conversations[it].conversationId },
                        ) { index ->
                            val conv = conversations[index]
                            WeChatConversationRow(
                                conversation = conv,
                                pbAuthToken = pbAuthToken,
                                onClick = { onNavigateToChat(conv.peerPbId) },
                                showDivider = index < conversations.lastIndex,
                            )
                        }
                    }
                } else when (val state = uiState) {
                    FriendsUiState.Loading -> Unit
                    is FriendsUiState.Error -> {
                        item {
                            StatusPanel(
                                icon = Icons.Outlined.PeopleOutline,
                                title = "加载失败",
                                message = state.message,
                                accent = FriendsPalette.danger,
                            )
                        }
                    }
                    is FriendsUiState.Ready -> {
                        if (state.pendingIn.isNotEmpty()) {
                            item { WeChatSectionLabel("新的朋友") }
                            items(state.pendingIn, key = { "in_${it.friendshipId}" }) { f ->
                                PendingRequestCard(
                                    friend = f,
                                    pendingAction = pendingAction,
                                    pbAuthToken = pbAuthToken,
                                    onAccept = { viewModel.acceptRequest(f.friendshipId) },
                                    onReject = { viewModel.rejectRequest(f.friendshipId) },
                                )
                            }
                        }
                        if (state.pendingOut.isNotEmpty()) {
                            item { WeChatSectionLabel("等待对方确认") }
                            items(state.pendingOut, key = { "out_${it.friendshipId}" }) { f ->
                                WeChatFriendRow(
                                    friend = f,
                                    showPending = true,
                                    pbAuthToken = pbAuthToken,
                                    onClick = {},
                                    onDelete = { viewModel.removeFriend(f.friendshipId) },
                                )
                            }
                        }
                        item { WeChatSectionLabel("我的好友 ${state.friends.size}") }
                        if (state.friends.isEmpty()) {
                            item { EmptyFriendsHint() }
                        } else {
                            items(state.friends, key = { it.friendshipId }) { f ->
                                WeChatFriendRow(
                                    friend = f,
                                    showPending = false,
                                    pbAuthToken = pbAuthToken,
                                    onClick = { onNavigateToChat(f.friendPbId) },
                                    onDelete = { viewModel.removeFriend(f.friendshipId) },
                                    onRemark = {
                                        remarkDialogFriend = f
                                        remarkText = f.remark
                                    },
                                )
                            }
                        }
                    }
                }
            }

            FriendsToastOverlay(
                toast = toast,
                onDismiss = { viewModel.consumeToast() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .zIndex(10f),
            )
        }
    }

    remarkDialogFriend?.let { friend ->
        AlertDialog(
            onDismissRequest = { remarkDialogFriend = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = FriendsPalette.surface,
            title = {
                Text("编辑备注", fontWeight = FontWeight.Bold, color = FriendsPalette.ink)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "@${friend.funlifeUsername}",
                        fontSize = 13.sp,
                        color = FriendsPalette.inkMuted,
                    )
                    OutlinedTextField(
                        value = remarkText,
                        onValueChange = { remarkText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("仅本机可见的备注名") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FriendsPalette.primary,
                            cursorColor = FriendsPalette.primary,
                        ),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateRemark(friend.friendPbId, remarkText)
                        remarkDialogFriend = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FriendsPalette.primary),
                    shape = RoundedCornerShape(10.dp),
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { remarkDialogFriend = null }) {
                    Text("取消", color = FriendsPalette.inkSecondary)
                }
            },
        )
    }
}

@Composable
private fun FriendsTopBar(
    pendingCount: Int,
    isRefreshing: Boolean,
    searchExpanded: Boolean,
    onSearchExpandedChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    searchEnabled: Boolean,
    isSearching: Boolean,
    sessionReady: Boolean,
    searchResult: PbUserProfile?,
    onSendRequest: (PbUserProfile) -> Unit,
    sendingToPbId: String?,
    pbAuthToken: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val searchFocus = remember { FocusRequester() }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            delay(120)
            searchFocus.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FriendsPalette.toolbar)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WeChatHeaderIconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = FriendsPalette.ink,
                    modifier = Modifier.size(22.dp),
                )
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "好友",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FriendsPalette.ink,
                    )
                    if (pendingCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(FriendsPalette.warning),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (pendingCount > 9) "9+" else pendingCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
            WeChatHeaderIconButton(
                onClick = {
                    if (searchExpanded) {
                        onSearchExpandedChange(false)
                    } else {
                        onSearchExpandedChange(true)
                    }
                },
            ) {
                Icon(
                    if (searchExpanded) Icons.Default.Close else Icons.Outlined.Search,
                    contentDescription = if (searchExpanded) "关闭搜索" else "搜索好友",
                    tint = FriendsPalette.ink,
                    modifier = Modifier.size(if (searchExpanded) 20.dp else 22.dp),
                )
            }
            WeChatHeaderIconButton(onClick = onRefresh, enabled = !isRefreshing) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = FriendsPalette.inkSecondary,
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = FriendsPalette.inkSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = searchExpanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            ) {
                EnterpriseSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onSearch = onSearch,
                    searchEnabled = searchEnabled,
                    isSearching = isSearching,
                    sessionReady = sessionReady,
                    focusRequester = searchFocus,
                )
                if (!sessionReady) {
                    SocialConnectionHint(
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                AnimatedVisibility(
                    visible = searchResult != null,
                    enter = fadeIn() + slideInVertically { -it / 3 },
                ) {
                    searchResult?.let { profile ->
                        SearchResultRow(
                            profile = profile,
                            isSending = sendingToPbId == profile.id,
                            sendEnabled = sendingToPbId == null,
                            pbAuthToken = pbAuthToken,
                            onAdd = { onSendRequest(profile) },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        Divider(color = FriendsPalette.headerLine, thickness = 0.5.dp)
    }
}

@Composable
private fun WeChatHeaderIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp),
    ) {
        content()
    }
}

@Composable
private fun EnterpriseSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    searchEnabled: Boolean,
    isSearching: Boolean,
    sessionReady: Boolean,
    focusRequester: FocusRequester = FocusRequester(),
    modifier: Modifier = Modifier,
) {
    val placeholder = "搜索 一十 用户名"
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(shape)
            .background(FriendsPalette.searchFill)
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, null, tint = FriendsPalette.previewText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "@",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = FriendsPalette.inkSecondary,
        )
        Spacer(Modifier.width(2.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = FriendsPalette.ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (searchEnabled) onSearch() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(placeholder, fontSize = 15.sp, color = FriendsPalette.previewText)
                    }
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "清空",
                    tint = FriendsPalette.previewText,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        TextButton(
            onClick = onSearch,
            enabled = searchEnabled,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp),
        ) {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = FriendsPalette.success,
                )
            } else {
                Text(
                    "搜索",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        !sessionReady -> FriendsPalette.warning
                        searchEnabled -> FriendsPalette.success
                        else -> FriendsPalette.previewText
                    },
                )
            }
        }
    }
}

@Composable
private fun FriendsToastOverlay(
    toast: FriendsToast?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = toast != null,
        modifier = modifier.padding(horizontal = 20.dp),
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        toast?.let { t ->
            val (accent, soft, icon) = when (t.tone) {
                FriendsToastTone.Success -> Triple(
                    FriendsPalette.success,
                    FriendsPalette.successSoft,
                    Icons.Default.CheckCircle,
                )
                FriendsToastTone.Error -> Triple(
                    FriendsPalette.danger,
                    FriendsPalette.dangerSoft,
                    Icons.Default.ErrorOutline,
                )
                FriendsToastTone.Info -> Triple(
                    FriendsPalette.primary,
                    FriendsPalette.primarySoft,
                    Icons.Default.Info,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(16.dp), spotColor = accent.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(FriendsPalette.surface)
                    .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(soft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Text(
                    t.message,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = FriendsPalette.ink,
                    lineHeight = 20.sp,
                )
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = FriendsPalette.inkMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    profile: PbUserProfile,
    isSending: Boolean,
    sendEnabled: Boolean,
    pbAuthToken: String?,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FriendsPalette.listRow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBubble(
            profile.displayName.ifBlank { profile.funlifeUsername },
            profile.avatarUrl,
            42.dp,
            profile.online,
            pbAuthToken,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                profile.displayName.ifBlank { profile.funlifeUsername },
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = FriendsPalette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("@${profile.funlifeUsername}", fontSize = 12.sp, color = FriendsPalette.inkSecondary)
        }
        Button(
            onClick = onAdd,
            enabled = sendEnabled && !isSending,
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.height(34.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FriendsPalette.success,
                disabledContainerColor = FriendsPalette.success.copy(alpha = 0.4f),
            ),
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            } else {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("添加", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, badge: String, badgeColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FriendsPalette.inkSecondary)
        Text(
            badge,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeColor.copy(alpha = 0.1f))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun PendingRequestCard(
    friend: FriendUiModel,
    pendingAction: PendingFriendAction?,
    pbAuthToken: String?,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    val isThisCard = pendingAction?.friendshipId == friend.friendshipId
    val accepting = isThisCard && pendingAction?.kind == PendingFriendActionKind.ACCEPT
    val rejecting = isThisCard && pendingAction?.kind == PendingFriendActionKind.REJECT
    val buttonsEnabled = pendingAction == null || pendingAction.friendshipId != friend.friendshipId
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape, spotColor = FriendsPalette.warning.copy(alpha = 0.15f))
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(FriendsPalette.warningSoft, FriendsPalette.surface),
                ),
            )
            .border(1.dp, FriendsPalette.warning.copy(alpha = 0.22f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBubble(friend.displayName, friend.avatarUrl, 44.dp, showOnline = false, pbAuthToken)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "请求添加你为好友",
                fontSize = 10.sp,
                color = FriendsPalette.warning,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                friend.displayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = FriendsPalette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("@${friend.funlifeUsername}", fontSize = 11.sp, color = FriendsPalette.inkSecondary)
        }
        Spacer(Modifier.width(6.dp))
        OutlinedButton(
            onClick = onReject,
            enabled = buttonsEnabled,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.height(34.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = FriendsPalette.danger),
        ) {
            if (rejecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = FriendsPalette.danger,
                )
            } else {
                Text("拒绝", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.width(6.dp))
        Button(
            onClick = onAccept,
            enabled = buttonsEnabled,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.height(34.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FriendsPalette.success),
        ) {
            if (accepting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
            } else {
                Text("同意", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FriendsSegmentTabs(
    selected: Int,
    friendCount: Int,
    messageCount: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FriendsPalette.listRow)
            .padding(horizontal = 8.dp),
    ) {
        WeChatTabItem(
            label = "消息",
            badge = messageCount,
            selected = selected == 1,
            onClick = { onSelect(1) },
            modifier = Modifier.weight(1f),
        )
        WeChatTabItem(
            label = "通讯录",
            badge = friendCount,
            selected = selected == 0,
            onClick = { onSelect(0) },
            modifier = Modifier.weight(1f),
        )
    }
    Divider(color = FriendsPalette.listDivider, thickness = 0.5.dp)
}

@Composable
private fun WeChatTabItem(
    label: String,
    badge: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (badge > 0) "$label ($badge)" else label,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) FriendsPalette.ink else FriendsPalette.previewText,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .height(2.dp)
                .width(28.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (selected) FriendsPalette.success else Color.Transparent),
        )
    }
}

@Composable
private fun WeChatSectionLabel(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .background(FriendsPalette.sectionBg)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        fontSize = 13.sp,
        color = FriendsPalette.sectionText,
    )
}

@Composable
private fun WeChatConversationRow(
    conversation: ConversationUiModel,
    pbAuthToken: String?,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    WeChatListRow(
        name = conversation.peerDisplayName,
        avatarUrl = conversation.peerAvatarUrl,
        pbAuthToken = pbAuthToken,
        title = conversation.peerDisplayName,
        subtitle = if (conversation.lastPreview.isBlank()) "暂无消息" else conversation.lastPreview,
        timeText = if (conversation.lastMessageAt > 0L) {
            formatWeChatListTime(conversation.lastMessageAt)
        } else {
            null
        },
        onClick = onClick,
        showDivider = showDivider,
    )
}

@Composable
private fun WeChatFriendRow(
    friend: FriendUiModel,
    showPending: Boolean,
    pbAuthToken: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRemark: (() -> Unit)? = null,
) {
    val displayName = if (friend.remark.isNotBlank()) friend.remark else friend.displayName
    val subtitle = if (showPending) "等待对方确认" else "@${friend.funlifeUsername}"
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FriendsPalette.listRow)
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WeChatAvatar(displayName, friend.avatarUrl, pbAuthToken)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    displayName,
                    fontSize = 17.sp,
                    color = FriendsPalette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = FriendsPalette.previewText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onRemark != null) {
                IconButton(onClick = onRemark, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Edit, "备注", tint = FriendsPalette.previewText, modifier = Modifier.size(18.dp))
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.DeleteOutline, "删除", tint = FriendsPalette.previewText, modifier = Modifier.size(18.dp))
            }
        }
        Divider(
            modifier = Modifier.padding(start = 76.dp),
            thickness = 0.5.dp,
            color = FriendsPalette.listDivider,
        )
    }
}

@Composable
private fun WeChatListRow(
    name: String,
    avatarUrl: String?,
    pbAuthToken: String?,
    title: String,
    subtitle: String,
    timeText: String?,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FriendsPalette.listRow)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WeChatAvatar(name, avatarUrl, pbAuthToken)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        fontSize = 17.sp,
                        color = FriendsPalette.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (timeText != null) {
                        Text(
                            timeText,
                            fontSize = 12.sp,
                            color = FriendsPalette.timeText,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = FriendsPalette.previewText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showDivider) {
            Divider(
                modifier = Modifier.padding(start = 76.dp),
                thickness = 0.5.dp,
                color = FriendsPalette.listDivider,
            )
        }
    }
}

@Composable
private fun WeChatAvatar(
    name: String,
    url: String?,
    pbAuthToken: String?,
    size: androidx.compose.ui.unit.Dp = 48.dp,
) {
    val context = LocalContext.current
    val initial = name.trim().take(1).uppercase().ifBlank { "?" }
    val shape = RoundedCornerShape(4.dp)
    val boxModifier = Modifier
        .size(size)
        .clip(shape)
        .background(Color(0xFFCCCCCC))

    @Composable
    fun InitialPlaceholder() {
        Box(
            modifier = boxModifier.background(
                Brush.linearGradient(listOf(FriendsPalette.primary, FriendsPalette.primaryDark)),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(initial, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 18.sp)
        }
    }

    if (!url.isNullOrBlank()) {
        val needsAuth = url.contains("/api/files/") && !pbAuthToken.isNullOrBlank()
        val model = remember(url, pbAuthToken) {
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .apply {
                    if (needsAuth) addHeader("Authorization", "Bearer $pbAuthToken")
                }
                .build()
        }
        SubcomposeAsyncImage(
            model = model,
            contentDescription = null,
            modifier = boxModifier,
            contentScale = ContentScale.Crop,
            loading = { InitialPlaceholder() },
            error = { InitialPlaceholder() },
        )
    } else {
        InitialPlaceholder()
    }
}

@Composable
private fun EmptyConversationsHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("💬", fontSize = 36.sp)
        Spacer(Modifier.height(8.dp))
        Text("还没有聊天记录", fontSize = 14.sp, color = FriendsPalette.inkMuted)
        Text("在「好友」里点 💬 开始聊天", fontSize = 12.sp, color = FriendsPalette.inkMuted)
    }
}

private fun formatWeChatListTime(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    val now = java.util.Calendar.getInstance()
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
    fun isSameDay(a: java.util.Calendar, b: java.util.Calendar) =
        a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) &&
            a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR)
    if (isSameDay(now, cal)) {
        return java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(epochMs))
    }
    val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    if (isSameDay(yesterday, cal)) return "昨天"
    val weekAgo = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -6) }
    if (epochMs >= weekAgo.timeInMillis) {
        val weekdays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        return weekdays[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
    }
    if (cal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR)) {
        return java.text.SimpleDateFormat("M月d日", java.util.Locale.CHINA)
            .format(java.util.Date(epochMs))
    }
    return java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.CHINA)
        .format(java.util.Date(epochMs))
}

@Composable
private fun AvatarBubble(
    name: String,
    url: String?,
    size: androidx.compose.ui.unit.Dp,
    showOnline: Boolean,
    pbAuthToken: String? = null,
) {
    val context = LocalContext.current
    val initial = name.trim().take(1).uppercase().ifBlank { "?" }
    val circleModifier = Modifier
        .size(size)
        .clip(CircleShape)
        .border(2.dp, FriendsPalette.border, CircleShape)

    @Composable
    fun InitialPlaceholder(modifier: Modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    listOf(FriendsPalette.primary, FriendsPalette.primaryDark),
                ),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.38f).sp,
            )
        }
    }

    Box(Modifier.size(size)) {
        Box(modifier = circleModifier) {
            if (!url.isNullOrBlank()) {
                val needsAuth = url.contains("/api/files/") && !pbAuthToken.isNullOrBlank()
                val model = remember(url, pbAuthToken) {
                    ImageRequest.Builder(context)
                        .data(url)
                        .crossfade(true)
                        .apply {
                            if (needsAuth) addHeader("Authorization", "Bearer $pbAuthToken")
                        }
                        .build()
                }
                // 外层 Box 负责圆形裁剪；SubcomposeAsyncImage 直接 clip 在部分机型上不生效
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
        if (showOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(FriendsPalette.online)
                    .border(2.dp, Color.White, CircleShape),
            )
        }
    }
}

@Composable
private fun EmptyFriendsHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(FriendsPalette.surface.copy(alpha = 0.7f))
            .border(1.dp, FriendsPalette.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Outlined.PeopleOutline,
            contentDescription = null,
            tint = FriendsPalette.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(40.dp),
        )
        Text(
            "还没有好友",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = FriendsPalette.ink,
        )
        Text(
            "点击右上角搜索图标，输入 @用户名 发送好友申请",
            fontSize = 13.sp,
            color = FriendsPalette.inkMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SocialConnectionHint(modifier: Modifier = Modifier) {
    val server = PocketBaseConfig.baseUrl()
    Column(modifier = modifier) {
        Text(
            "手机暂时连不上社交服务器",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = FriendsPalette.warning,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "当前地址：$server\n" +
                "· 开发：电脑运行 pocketbase\\start.ps1，手机与电脑同一 WiFi\n" +
                "· 或 USB：adb reverse tcp:8090 tcp:8090，并把 POCKETBASE_URL 改为 http://127.0.0.1:8090\n" +
                "· 长期：部署到公网 HTTPS（见 pocketbase/README.md）",
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = FriendsPalette.inkMuted,
        )
    }
}

@Composable
private fun ConnectingBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(FriendsPalette.primarySoft)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = FriendsPalette.primary,
            strokeWidth = 2.dp,
        )
        Text(
            "正在连接社交服务…",
            fontSize = 12.sp,
            color = FriendsPalette.primaryDark,
        )
    }
}

@Composable
private fun LoadingPanel(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = FriendsPalette.primary,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(10.dp))
        Text(message, fontSize = 13.sp, color = FriendsPalette.inkMuted)
    }
}

@Composable
private fun StatusPanel(
    icon: ImageVector,
    title: String,
    message: String,
    accent: Color,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FriendsPalette.ink)
        Spacer(Modifier.height(8.dp))
        Text(message, fontSize = 14.sp, color = FriendsPalette.inkSecondary, textAlign = TextAlign.Center, lineHeight = 22.sp)
        if (onRetry != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRetry, shape = RoundedCornerShape(10.dp)) {
                Text("重试绑定", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
