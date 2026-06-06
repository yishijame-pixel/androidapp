package com.example.funlife.ui.screens.socialgame

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.catalog.GameCatalogStatus
import com.example.funlife.social.game.catalog.GameCategory
import com.example.funlife.social.game.catalog.SocialGameEntry
import com.example.funlife.social.game.model.GameCenterTab
import com.example.funlife.social.game.model.GameRoomStatus
import com.example.funlife.social.game.model.MyGameItemUi
import com.example.funlife.viewmodel.GameCenterViewModel

@Composable
fun SocialGameCenterScreen(
    viewModel: GameCenterViewModel,
    initialTab: GameCenterTab = GameCenterTab.ONLINE,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (gameId: String) -> Unit,
    onNavigateToLocalGame: (route: String) -> Unit,
    onNavigateToLobby: (roomId: String) -> Unit,
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val joinCode by viewModel.joinCode.collectAsState()
    val onlineGames by viewModel.sortedOnlineGames.collectAsState()
    val myGames by viewModel.myGames.collectAsState()
    val showTutorial by viewModel.showTutorial.collectAsState()
    val navigateRoomId by viewModel.navigateToRoomId.collectAsState()
    val pbConfigured = viewModel.pocketBaseConfigured()
    val pendingCount by viewModel.pendingInviteCount.collectAsState()

    LaunchedEffect(initialTab) { viewModel.selectTab(initialTab) }

    LaunchedEffect(navigateRoomId) {
        navigateRoomId?.let { roomId ->
            onNavigateToLobby(roomId)
            viewModel.consumeNavigateToRoom()
        }
    }

    SocialGameScaffold(
        title = "趣玩中心",
        subtitle = "与好友随时开战",
        onNavigateBack = onNavigateBack,
        trailing = {
            if (pendingCount > 0) {
                PendingBadge(count = pendingCount)
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HubSegmentTabs(
                selected = selectedTab,
                onSelect = viewModel::selectTab,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(18.dp))

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "hubTabContent",
                modifier = Modifier.fillMaxSize(),
            ) { tab ->
                when (tab) {
                    GameCenterTab.ONLINE -> OnlineTabContent(
                        pbConfigured = pbConfigured,
                        joinCode = joinCode,
                        onJoinCodeChange = viewModel::onJoinCodeChange,
                        onJoinClick = viewModel::joinByCode,
                        games = onlineGames,
                        onGameClick = onNavigateToDetail,
                    )
                    GameCenterTab.LOCAL -> LocalTabContent(
                        games = com.example.funlife.social.game.catalog.SocialGameCatalog.localPartyGames(),
                        onGameClick = { entry ->
                            entry.localRoute?.let(onNavigateToLocalGame)
                        },
                    )
                    GameCenterTab.MY_GAMES -> MyGamesTabContent(
                        pbConfigured = pbConfigured,
                        items = myGames.filter {
                            it.status != GameRoomStatus.CANCELLED && it.status != GameRoomStatus.EXPIRED
                        },
                        onContinue = onNavigateToLobby,
                    )
                }
            }
        }
    }

    if (showTutorial) {
        AlertDialog(
            onDismissRequest = viewModel::dismissTutorial,
            title = { Text("欢迎来到趣玩中心") },
            text = {
                Text(
                    "「好友对战」需要和好友在线一起玩；「同屏乐玩」适合聚会传手机。全部游戏均为全屏体验，不会显示底部导航栏。",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissTutorial) { Text("知道了") }
            },
        )
    }
}

@Composable
private fun PendingBadge(count: Int) {
    Row(
        modifier = Modifier
            .padding(end = 4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(SocialGamePalette.accentCoral.copy(alpha = 0.10f))
            .border(1.dp, SocialGamePalette.accentCoral.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(SocialGamePalette.accentCoral),
        )
        Text(
            "待处理 $count",
            color = SocialGamePalette.accentCoral,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HubSegmentTabs(
    selected: GameCenterTab,
    onSelect: (GameCenterTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        GameCenterTab.ONLINE to "好友对战",
        GameCenterTab.LOCAL to "同屏乐玩",
        GameCenterTab.MY_GAMES to "我的对局",
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        tabs.forEach { (tab, label) ->
            val active = tab == selected
            Column(
                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    label,
                    color = if (active) SocialGamePalette.inkPrimary else SocialGamePalette.inkMuted,
                    fontSize = 15.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(if (active) 28.dp else 0.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (active) {
                                Brush.horizontalGradient(
                                    listOf(SocialGamePalette.accentCoral, SocialGamePalette.accentPurple),
                                )
                            } else {
                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun OnlineTabContent(
    pbConfigured: Boolean,
    joinCode: String,
    onJoinCodeChange: (String) -> Unit,
    onJoinClick: () -> Unit,
    games: List<SocialGameEntry>,
    onGameClick: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!pbConfigured) {
            item {
                HubNoticeCard("配置社交服务后，可与好友在线对战。同屏乐玩无需联网。")
            }
        } else {
            item {
                JoinRoomCommandCard(
                    joinCode = joinCode,
                    onJoinCodeChange = onJoinCodeChange,
                    onJoinClick = onJoinClick,
                )
            }
        }

        item {
            HubSectionTitle("精选对战", "点击游戏开房间，邀请好友加入")
        }

        items(games, key = { it.gameId }) { game ->
            HubGameListCard(
                entry = game,
                onClick = { onGameClick(game.gameId) },
            )
        }
    }
}

@Composable
private fun LocalTabContent(
    games: List<SocialGameEntry>,
    onGameClick: (SocialGameEntry) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HubNoticeCard("同屏乐玩适合聚会传手机，无需联网即可畅玩。")
        }
        item {
            HubSectionTitle("聚会游戏", "一部手机，多人轮流挑战")
        }
        items(games, key = { it.gameId }) { game ->
            HubGameListCard(entry = game, onClick = { onGameClick(game) })
        }
    }
}

@Composable
private fun MyGamesTabContent(
    pbConfigured: Boolean,
    items: List<MyGameItemUi>,
    onContinue: (String) -> Unit,
) {
    if (!pbConfigured) {
        EmptyHubState(
            emoji = "🌐",
            title = "社交服务未配置",
            subtitle = "配置完成后可查看进行中的对局",
        )
        return
    }
    if (items.isEmpty()) {
        EmptyHubState(
            emoji = "🎮",
            title = "暂无进行中对局",
            subtitle = "去好友对战开一局吧",
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            HubSectionTitle("进行中的对局", "点击继续回到对战大厅")
        }
        items(items, key = { it.roomId }) { item ->
            ActiveMatchCard(item = item, onClick = { onContinue(item.roomId) })
        }
    }
}

@Composable
private fun HubSectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)) {
        Text(title, color = SocialGamePalette.inkPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = SocialGamePalette.inkMuted, fontSize = 12.sp)
    }
}

@Composable
private fun HubNoticeCard(message: String) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = SocialGamePalette.accentBlue,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SocialGamePalette.accentBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("ℹ", color = SocialGamePalette.accentBlue, fontWeight = FontWeight.Bold)
            }
            Text(
                message,
                modifier = Modifier.padding(start = 12.dp),
                color = SocialGamePalette.inkSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
    }
}

@Composable
private fun JoinRoomCommandCard(
    joinCode: String,
    onJoinCodeChange: (String) -> Unit,
    onJoinClick: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = SocialGamePalette.accentMint,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    SocialGamePalette.accentMint.copy(alpha = 0.25f),
                                    SocialGamePalette.accentBlue.copy(alpha = 0.18f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🎫", fontSize = 18.sp)
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        "快速加入",
                        color = SocialGamePalette.inkPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "输入好友分享的房间号",
                        color = SocialGamePalette.inkMuted,
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SocialGamePalette.bgBase.copy(alpha = 0.7f))
                        .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    BasicTextField(
                        value = joinCode,
                        onValueChange = onJoinCodeChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = SocialGamePalette.inkPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 5.sp,
                        ),
                        cursorBrush = SolidColor(SocialGamePalette.accentMint),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (joinCode.isEmpty()) {
                                Text(
                                    "ROOM ID",
                                    color = SocialGamePalette.inkMuted.copy(alpha = 0.35f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 4.sp,
                                )
                            }
                            inner()
                        },
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(SocialGamePalette.accentMint, SocialGamePalette.accentBlue),
                            ),
                        )
                        .clickable(onClick = onJoinClick)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Text(
                        "加入",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HubGameListCard(
    entry: SocialGameEntry,
    onClick: () -> Unit,
) {
    val enabled = entry.status != GameCatalogStatus.COMING_SOON

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SocialGamePalette.glassFill)
            .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameCatalogHeroIcon(
            entry = entry,
            size = 58.dp,
            emojiFontSize = 28.sp,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    entry.title,
                    color = SocialGamePalette.inkPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StatusDotBadge(
                    label = when {
                        entry.category == GameCategory.LOCAL_PARTY -> "同屏"
                        entry.status == GameCatalogStatus.BETA -> "BETA"
                        entry.status == GameCatalogStatus.COMING_SOON -> "即将上线"
                        else -> "在线"
                    },
                    dotColor = when {
                        entry.status == GameCatalogStatus.COMING_SOON -> SocialGamePalette.inkMuted
                        entry.category == GameCategory.LOCAL_PARTY -> SocialGamePalette.accentGold
                        else -> SocialGamePalette.accentMint
                    },
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                entry.subtitle,
                color = SocialGamePalette.inkMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                entry.playersLabel,
                color = SocialGamePalette.inkSecondary.copy(alpha = 0.75f),
                fontSize = 11.sp,
            )
        }

        HubArrowChevron()
    }
}

@Composable
private fun ActiveMatchCard(
    item: MyGameItemUi,
    onClick: () -> Unit,
) {
    val c1 = Color((item.accentColors.getOrNull(0) ?: 0xFFFFB84D).toInt())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SocialGamePalette.glassFill)
            .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentBrush(item.accentColors))
                .border(1.dp, c1.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(item.gameEmoji, fontSize = 24.sp)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(item.gameTitle, color = SocialGamePalette.inkPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(item.subtitle, color = SocialGamePalette.inkMuted, fontSize = 12.sp)
        }
        Text(
            "继续",
            color = SocialGamePalette.accentGold,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

/** @deprecated Use HubGameListCard in center screen; kept for external references if any. */
@Composable
fun GameCatalogCard(
    entry: SocialGameEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        HubGameListCard(entry = entry, onClick = onClick)
    }
}
