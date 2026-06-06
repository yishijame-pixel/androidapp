package com.example.funlife.ui.screens.socialgame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.model.FriendUiModel

@Composable
fun InviteFriendRail(
    friends: List<FriendUiModel>,
    pbAuthToken: String?,
    enabled: Boolean,
    onPick: (FriendUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(
                title = "邀请好友",
                subtitle = if (enabled) "点击头像即可发送对战邀请" else "当前有进行中的邀请",
            )
            Spacer(Modifier.height(14.dp))
            if (friends.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "暂无好友，先去好友页添加吧",
                        color = SocialGamePalette.inkMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    friends.forEach { friend ->
                        val name = friend.displayName.ifBlank { friend.funlifeUsername }
                        InviteFriendChip(
                            name = name,
                            avatarUrl = friend.avatarUrl,
                            pbAuthToken = pbAuthToken,
                            enabled = enabled,
                            onClick = { onPick(friend) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteFriendChip(
    name: String,
    avatarUrl: String?,
    pbAuthToken: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .then(
                    if (enabled) {
                        Modifier.background(
                            Brush.linearGradient(
                                listOf(SocialGamePalette.accentViolet, SocialGamePalette.accentIndigo),
                            ),
                        )
                    } else {
                        Modifier.background(Color.White.copy(alpha = 0.06f))
                    },
                )
                .padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(SocialGamePalette.bgMid),
            ) {
                SocialGameAvatar(
                    displayName = name,
                    avatarUrl = avatarUrl,
                    pbAuthToken = pbAuthToken,
                    size = 52.dp,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            color = if (enabled) SocialGamePalette.inkPrimary else SocialGamePalette.inkMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SocialGameFriendPickerSheet(
    friends: List<FriendUiModel>,
    pbAuthToken: String?,
    onPick: (FriendUiModel) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Text(
            "选择好友",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = SocialGamePalette.inkPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        if (friends.isEmpty()) {
            Text(
                "暂无好友，先去好友页添加吧",
                modifier = Modifier.padding(20.dp),
                color = SocialGamePalette.inkMuted,
            )
        } else {
            LazyColumn {
                items(
                    friends.sortedWith(
                        compareByDescending<com.example.funlife.social.model.FriendUiModel> { it.online }
                            .thenBy { it.displayName.ifBlank { it.funlifeUsername } },
                    ),
                    key = { it.friendPbId },
                ) { friend ->
                    val name = friend.displayName.ifBlank { friend.funlifeUsername }
                    val canInvite = friend.online
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SocialGameAvatar(
                            displayName = name,
                            avatarUrl = friend.avatarUrl,
                            pbAuthToken = pbAuthToken,
                            size = 48.dp,
                            showOnline = friend.online,
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        ) {
                            Text(
                                name,
                                color = SocialGamePalette.inkPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (friend.online) "在线 · @${friend.funlifeUsername}" else "离线 · @${friend.funlifeUsername}",
                                color = if (friend.online) SocialGamePalette.online else SocialGamePalette.inkMuted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (canInvite) {
                                        SocialGamePalette.accentViolet.copy(alpha = 0.25f)
                                    } else {
                                        SocialGamePalette.bgBase.copy(alpha = 0.6f)
                                    },
                                )
                                .border(
                                    1.dp,
                                    if (canInvite) {
                                        SocialGamePalette.accentViolet.copy(alpha = 0.5f)
                                    } else {
                                        SocialGamePalette.glassBorder
                                    },
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable(enabled = canInvite) { onPick(friend) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text(
                                if (canInvite) "邀请" else "离线",
                                color = if (canInvite) SocialGamePalette.inkPrimary else SocialGamePalette.inkMuted,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
