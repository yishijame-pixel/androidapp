package com.example.funlife.ui.screens.socialgame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.example.funlife.navigation.Screen
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
            onNavigateToLobby = { invite ->
                if (invite.gameId == "pac_maze") {
                    navController.navigate(Screen.pacMazeRoute(onlineLobbyRoomId = invite.roomId)) {
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(Screen.SocialGameLobby.route(invite.roomId)) {
                        launchSingleTop = true
                    }
                }
            },
        )
    }
}

@Composable
fun GameInviteOverlayHost(
    viewModel: GameCenterViewModel,
    suppressRoomId: String? = null,
    onNavigateToLobby: (LocalGameRoomDraft) -> Unit,
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
                    onAccepted = { onNavigateToLobby(activeInvite) },
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
                .background(Color.Black.copy(alpha = 0.62f)),
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
                    .padding(horizontal = 20.dp)
                    .widthIn(max = 300.dp),
            )
        }
    }
}
