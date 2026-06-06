package com.example.funlife.ui.screens.socialgame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.funlife.navigation.Screen
import com.example.funlife.social.game.catalog.SocialGameCatalog
import com.example.funlife.social.game.model.LocalGameRoomDraft
import com.example.funlife.viewmodel.GameCenterViewModel

private enum class InvitePanelAction { NONE, ACCEPT, REJECT }

@Composable
fun GlobalGameInviteLayer(
    viewModel: GameCenterViewModel,
    navController: NavController,
    suppressRoomId: String? = null,
) {
    val toast by viewModel.toast.collectAsState()
    val busyMessage by viewModel.busyMessage.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        SocialGameToastHost(
            toast = toast,
            onDismiss = viewModel::consumeToast,
        )
        CenteredBusyOverlay(busyMessage)
        GameInviteOverlayHost(
            viewModel = viewModel,
            suppressRoomId = suppressRoomId,
            onNavigateToLobby = { roomId ->
                navController.navigate(Screen.SocialGameLobby.route(roomId)) {
                    launchSingleTop = true
                }
            },
        )
    }
}

@Composable
fun GameInviteOverlayHost(
    viewModel: GameCenterViewModel,
    suppressRoomId: String? = null,
    onNavigateToLobby: (roomId: String) -> Unit,
) {
    val invite by viewModel.incomingDirectInvite.collectAsState()
    val handled by viewModel.handledInviteRoomIds.collectAsState()
    val pbToken by viewModel.pbAuthToken.collectAsState()

    val activeInvite = invite?.takeIf { draft ->
        draft.roomId != suppressRoomId && draft.roomId !in handled
    }
    if (activeInvite != null) {
        CenteredGameInviteOverlay(
            invite = activeInvite,
            pbAuthToken = pbToken,
            onAccept = { onSettled ->
                viewModel.acknowledgeIncomingInvite(activeInvite.roomId)
                viewModel.acceptInvite(
                    roomId = activeInvite.roomId,
                    onAccepted = { onNavigateToLobby(activeInvite.roomId) },
                    optimistic = true,
                    onSettled = onSettled,
                )
            },
            onReject = { onSettled ->
                viewModel.acknowledgeIncomingInvite(activeInvite.roomId)
                viewModel.exitLobby(
                    roomId = activeInvite.roomId,
                    action = com.example.funlife.social.game.model.LobbyExitAction.REJECT_INVITE,
                    onDone = {},
                    onSettled = onSettled,
                )
            },
        )
    }
}

@Composable
fun CenteredGameInviteOverlay(
    invite: LocalGameRoomDraft,
    pbAuthToken: String?,
    onAccept: (onSettled: () -> Unit) -> Unit,
    onReject: (onSettled: () -> Unit) -> Unit,
) {
    var action by remember(invite.roomId) { mutableStateOf(InvitePanelAction.NONE) }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.52f)),
            contentAlignment = Alignment.Center,
        ) {
            GameInviteRequestPanel(
                invite = invite,
                pbAuthToken = pbAuthToken,
                acceptLoading = action == InvitePanelAction.ACCEPT,
                rejectLoading = action == InvitePanelAction.REJECT,
                buttonsEnabled = action == InvitePanelAction.NONE,
                onAccept = {
                    action = InvitePanelAction.ACCEPT
                    onAccept {
                        if (action == InvitePanelAction.ACCEPT) {
                            action = InvitePanelAction.NONE
                        }
                    }
                },
                onReject = {
                    action = InvitePanelAction.REJECT
                    onReject {
                        if (action == InvitePanelAction.REJECT) {
                            action = InvitePanelAction.NONE
                        }
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
            )
        }
    }
}

@Composable
fun GameInviteRequestPanel(
    invite: LocalGameRoomDraft,
    pbAuthToken: String?,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
    acceptLoading: Boolean = false,
    rejectLoading: Boolean = false,
    buttonsEnabled: Boolean = true,
) {
    val entry = SocialGameCatalog.find(invite.gameId)
    val hostName = invite.hostDisplayName ?: invite.peerDisplayName ?: "好友"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White,
                        SocialGamePalette.bgBase,
                    ),
                ),
            )
            .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(24.dp))
            .padding(24.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "对战邀请",
                color = SocialGamePalette.accentPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(12.dp))
            Text(entry?.iconEmoji ?: "🎮", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                entry?.title ?: invite.gameTitle,
                color = SocialGamePalette.inkPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))
            SocialGameAvatar(
                displayName = hostName,
                avatarUrl = invite.hostAvatarUrl ?: invite.peerAvatarUrl,
                pbAuthToken = pbAuthToken,
                size = 72.dp,
                showOnline = true,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                hostName,
                color = SocialGamePalette.inkPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "邀请你加入对局",
                color = SocialGamePalette.inkMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HubSecondaryButton(
                    text = "婉拒",
                    onClick = onReject,
                    enabled = buttonsEnabled,
                    loading = rejectLoading,
                    modifier = Modifier.weight(1f),
                )
                HubPrimaryButton(
                    text = "接受挑战",
                    onClick = onAccept,
                    enabled = buttonsEnabled,
                    loading = acceptLoading,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
