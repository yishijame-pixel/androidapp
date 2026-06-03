package com.example.funlife.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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

/** 企业级社交页调色板 */
private object FriendsPalette {
    val bgTop = Color(0xFFF0F4FA)
    val bgBottom = Color(0xFFE8EEF6)
    val surface = Color(0xFFFFFFFF)
    val surfaceMuted = Color(0xFFF8FAFC)
    val primary = Color(0xFF2563EB)
    val primaryDark = Color(0xFF1D4ED8)
    val primarySoft = Color(0xFFEFF6FF)
    val ink = Color(0xFF0F172A)
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
    val headerLine = Color(0xFFF1F5F9)
    val searchFill = Color(0xFFF8FAFC)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    viewModel: FriendsViewModel,
    onNavigateBack: () -> Unit,
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
    val toast by viewModel.toast.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var remarkDialogFriend by remember { mutableStateOf<FriendUiModel?>(null) }
    var remarkText by remember { mutableStateOf("") }

    LaunchedEffect(toast) {
        toast?.let {
            delay(2_800)
            viewModel.consumeToast()
        }
    }

    val readyState = uiState as? FriendsUiState.Ready

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            FriendsTopBar(
                friendCount = readyState?.friends?.size ?: 0,
                pendingCount = (readyState?.pendingIn?.size ?: 0) + (readyState?.pendingOut?.size ?: 0),
                isRefreshing = isSyncing,
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
                onBack = onNavigateBack,
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (linkState is SocialLinkState.Linking) {
                    item { ConnectingBanner() }
                }
                when (val state = uiState) {
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
                            item {
                                SectionHeader(
                                    title = "待处理请求",
                                    badge = state.pendingIn.size.toString(),
                                    badgeColor = FriendsPalette.warning,
                                )
                            }
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
                            item {
                                SectionHeader(
                                    title = "等待对方确认",
                                    badge = state.pendingOut.size.toString(),
                                    badgeColor = FriendsPalette.primary,
                                )
                            }
                            items(state.pendingOut, key = { "out_${it.friendshipId}" }) { f ->
                                FriendListCard(
                                    friend = f,
                                    showPending = true,
                                    pbAuthToken = pbAuthToken,
                                    onDelete = { viewModel.removeFriend(f.friendshipId) },
                                    onRemark = {},
                                )
                            }
                        }
                        item {
                            SectionHeader(
                                title = "我的好友",
                                badge = state.friends.size.toString(),
                                badgeColor = FriendsPalette.success,
                            )
                        }
                        if (state.friends.isEmpty()) {
                            item { EmptyFriendsHint() }
                        } else {
                            items(state.friends, key = { it.friendshipId }) { f ->
                                FriendListCard(
                                    friend = f,
                                    showPending = false,
                                    pbAuthToken = pbAuthToken,
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
    friendCount: Int,
    pendingCount: Int,
    isRefreshing: Boolean,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, spotColor = Color(0x1A0F172A))
            .background(
                Brush.verticalGradient(
                    listOf(FriendsPalette.surface, FriendsPalette.headerLine),
                ),
            )
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolIconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "返回", tint = FriendsPalette.ink, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("好友", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FriendsPalette.ink, letterSpacing = 0.3.sp)
                    BetaBadge()
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniStatPill("好友", friendCount, FriendsPalette.primary)
                    MiniStatPill("待办", pendingCount, FriendsPalette.warning)
                }
            }
            ToolIconButton(onClick = onRefresh, enabled = !isRefreshing) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = FriendsPalette.primary,
                    )
                } else {
                    Icon(Icons.Default.Refresh, "刷新", tint = FriendsPalette.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
        EnterpriseSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearch = onSearch,
            searchEnabled = searchEnabled,
            isSearching = isSearching,
            sessionReady = sessionReady,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp),
        )
        if (!sessionReady) {
            Text(
                "正在连接社交服务… 搜索时将自动重试",
                fontSize = 11.sp,
                color = FriendsPalette.warning,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(FriendsPalette.border.copy(alpha = 0.7f)),
        )
    }
}

@Composable
private fun ToolIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(FriendsPalette.surfaceMuted)
            .border(1.dp, FriendsPalette.border.copy(alpha = 0.8f), RoundedCornerShape(10.dp)),
    ) {
        content()
    }
}

@Composable
private fun BetaBadge() {
    Text(
        "BETA",
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = FriendsPalette.primary,
        letterSpacing = 0.8.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(FriendsPalette.primary.copy(alpha = 0.1f))
            .border(1.dp, FriendsPalette.primary.copy(alpha = 0.22f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun MiniStatPill(label: String, count: Int, accent: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, fontSize = 10.sp, color = FriendsPalette.inkMuted)
        Text(
            count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
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
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .shadow(elevation = 1.dp, shape = shape, spotColor = Color(0x120F172A))
            .clip(shape)
            .background(FriendsPalette.searchFill)
            .border(1.dp, FriendsPalette.border, shape)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, null, tint = FriendsPalette.inkMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "@",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = FriendsPalette.primary,
        )
        Spacer(Modifier.width(4.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, color = FriendsPalette.ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (searchEnabled) onSearch() }),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text("搜索 FunLife 用户名", fontSize = 14.sp, color = FriendsPalette.inkMuted)
                    }
                    inner()
                }
            },
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .width(1.dp)
                .height(22.dp)
                .background(FriendsPalette.border),
        )
        TextButton(
            onClick = onSearch,
            enabled = searchEnabled,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            modifier = Modifier.height(34.dp),
        ) {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = FriendsPalette.primary,
                )
            } else {
                Text(
                    "搜索",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        !sessionReady -> FriendsPalette.warning
                        else -> FriendsPalette.primary
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
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, spotColor = FriendsPalette.primary.copy(alpha = 0.12f))
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(FriendsPalette.primarySoft, FriendsPalette.surface),
                ),
            )
            .border(1.dp, FriendsPalette.primary.copy(alpha = 0.15f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
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
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FriendsPalette.primary,
                disabledContainerColor = FriendsPalette.primary.copy(alpha = 0.4f),
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
private fun FriendListCard(
    friend: FriendUiModel,
    showPending: Boolean,
    pbAuthToken: String?,
    onDelete: () -> Unit,
    onRemark: () -> Unit,
) {
    val displayName = if (friend.remark.isNotBlank()) friend.remark else friend.displayName
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = Color(0x0A0F172A))
            .clip(RoundedCornerShape(12.dp))
            .background(FriendsPalette.surface)
            .border(1.dp, FriendsPalette.border.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .clickable(onClick = onRemark)
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarBubble(displayName, friend.avatarUrl, 40.dp, showOnline = false, pbAuthToken)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(displayName, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = FriendsPalette.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (showPending) {
                    Text(
                        "待确认",
                        fontSize = 10.sp,
                        color = FriendsPalette.primary,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(FriendsPalette.primarySoft)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
            Text("@${friend.funlifeUsername}", fontSize = 11.sp, color = FriendsPalette.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!showPending) {
            IconButton(onClick = onRemark, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, "备注", tint = FriendsPalette.inkMuted, modifier = Modifier.size(18.dp))
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.DeleteOutline, "删除", tint = FriendsPalette.inkMuted, modifier = Modifier.size(18.dp))
        }
    }
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
            "在顶部搜索 @用户名 发送好友申请",
            fontSize = 13.sp,
            color = FriendsPalette.inkMuted,
            textAlign = TextAlign.Center,
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
