package com.example.funlife.ui.screens.socialgame

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.catalog.GameCatalogStatus
import com.example.funlife.social.game.catalog.GameCategory
import com.example.funlife.social.game.catalog.SocialGameCatalog
import com.example.funlife.viewmodel.GameCenterViewModel

@Composable
fun GameDetailScreen(
    gameId: String,
    viewModel: GameCenterViewModel,
    preselectedPeerPbId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToLobby: (roomId: String) -> Unit,
    onNavigateToLocalGame: (route: String) -> Unit,
) {
    val entry = remember(gameId) { SocialGameCatalog.find(gameId) }
    val navigateRoomId by viewModel.navigateToRoomId.collectAsState()
    val busyMessage by viewModel.busyMessage.collectAsState()
    var rulesExpanded by remember { mutableStateOf(false) }
    var pacSubMode by remember { mutableStateOf("versus_duel") }
    var pacCoopLevel by remember { mutableStateOf(1) }

    LaunchedEffect(navigateRoomId) {
        navigateRoomId?.let { roomId ->
            onNavigateToLobby(roomId)
            viewModel.consumeNavigateToRoom()
        }
    }

    if (entry == null) {
        SocialGameScaffold(title = "游戏详情", onNavigateBack = onNavigateBack) {
            EmptyHubState("❓", "未找到该游戏", "请返回趣玩中心重试")
        }
        return
    }

    SocialGameScaffold(title = entry.title, onNavigateBack = onNavigateBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                PremiumStageCard(accent = entry.accentColors, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        GameCatalogHeroIcon(entry = entry, size = 128.dp)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            entry.title,
                            color = SocialGamePalette.inkPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            entry.subtitle,
                            color = SocialGamePalette.inkMuted,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(14.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            InfoPill(entry.playersLabel)
                            if (entry.durationLabel.isNotBlank()) {
                                InfoPill(entry.durationLabel)
                            }
                            entry.tags.forEach { tag -> InfoPill(tag) }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { rulesExpanded = !rulesExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("游戏规则", color = SocialGamePalette.inkPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Icon(
                                imageVector = if (rulesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = SocialGamePalette.inkMuted,
                            )
                        }
                        AnimatedVisibility(
                            visible = rulesExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            Text(
                                gameRulesText(entry.gameId),
                                modifier = Modifier.padding(top = 12.dp),
                                color = SocialGamePalette.inkMuted,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (entry.gameId == "pac_maze") {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("选择玩法", color = SocialGamePalette.inkPrimary, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PacModeChip("豆人对决", selected = pacSubMode == "versus_duel") {
                                    pacSubMode = "versus_duel"
                                }
                                PacModeChip("并肩闯关", selected = pacSubMode == "coop_campaign") {
                                    pacSubMode = "coop_campaign"
                                }
                            }
                            if (pacSubMode == "coop_campaign") {
                                Text("合作关卡 L1–L8", color = SocialGamePalette.inkMuted, fontSize = 12.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    (1..8).forEach { id ->
                                        PacModeChip("L$id", selected = pacCoopLevel == id, compact = true) {
                                            pacCoopLevel = id
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            when {
                entry.localRoute != null -> {
                    HubPrimaryButton(
                        text = if (entry.category == GameCategory.LOCAL_PARTY) "开始同屏玩" else "开始试玩",
                        onClick = {
                            viewModel.touchGame(entry.gameId)
                            onNavigateToLocalGame(entry.localRoute)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                entry.status == GameCatalogStatus.COMING_SOON -> {
                    HubSecondaryButton(
                        text = "即将上线",
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    HubPrimaryButton(
                        text = "开房间",
                        onClick = {
                            viewModel.createOpenRoom(
                                gameId = entry.gameId,
                                thenInviteGuestPbId = preselectedPeerPbId,
                                pacSubMode = if (entry.gameId == "pac_maze") pacSubMode else null,
                                pacLevelId = if (entry.gameId == "pac_maze" && pacSubMode == "coop_campaign") pacCoopLevel else null,
                            )
                        },
                        enabled = busyMessage == null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "开房后在对战大厅邀请好友，或分享房间号",
                        color = SocialGamePalette.inkMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun gameRulesText(gameId: String): String = when (gameId) {
    "gomoku" -> """
        支持 2~4 人对战，房主可邀请多名好友或分享房间号加入。
        
        · 2 人：经典对战，黑先白后，先在横、竖、斜连成五子者胜
        · 3~4 人：轮流落子，每人固定一种棋子颜色，先达成五连者获胜
        · 棋盘 15×15，单局最长 30 分钟或 200 手，超时判和
    """.trimIndent()
    "draw_guess" -> "共 3 轮。每轮一人作画 60 秒，另一人猜词，猜对得分。轮结束后互换角色，总分高者胜。"
    "dice_duel" -> "双方各掷一次骰子，点数大者胜。平局则加赛，最多 3 次。"
    "pac_maze" -> """
        在线双人豆人迷宫，各用各的手机横屏操控。
        
        · 豆人对决：对称竞技场竞速清豆，150 秒一局
        · 并肩闯关：共享 5 命，合作通过 L1–L8
        · 开房后邀请好友，双方就绪即可开始
    """.trimIndent()
    else -> "与好友在线对战，享受轻松社交时光。"
}

@Composable
private fun PacModeChip(
    label: String,
    selected: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(if (compact) 10.dp else 14.dp),
        color = if (selected) SocialGamePalette.accentCoral.copy(alpha = 0.18f) else SocialGamePalette.bgElevated,
        border = BorderStroke(
            1.dp,
            if (selected) SocialGamePalette.accentCoral else SocialGamePalette.inkMuted.copy(alpha = 0.35f),
        ),
    ) {
        Text(
            label,
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 14.dp,
                vertical = if (compact) 6.dp else 10.dp,
            ),
            color = if (selected) SocialGamePalette.inkPrimary else SocialGamePalette.inkMuted,
            fontSize = if (compact) 11.sp else 13.sp,
        )
    }
}
