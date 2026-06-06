package com.example.funlife.ui.screens.socialgame

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object SocialGamePalette {
    /** 浅色活力主题 —— 适配「趣生活」轻松社交气质，非强制暗黑。 */
    val bgDeep = Color(0xFFFFF9F5)
    val bgBase = Color(0xFFF6F2FF)
    val bgElevated = Color.White
    val bgTop = bgDeep
    val bgMid = bgBase
    val bgBottom = Color(0xFFEEF4FF)

    val accentCoral = Color(0xFFFF6B4A)
    val accentCoralDeep = Color(0xFFE85535)
    val accentPurple = Color(0xFF7C5CFC)
    val accentPurpleDeep = Color(0xFF6344E8)
    val accentTeal = Color(0xFF14B8A6)
    val accentSky = Color(0xFF38BDF8)
    val accentGold = accentCoral
    val accentGoldDeep = accentCoralDeep
    val accentBlue = accentPurple
    val accentMint = accentTeal
    val accentViolet = accentPurple
    val accentIndigo = accentPurpleDeep
    val accentCyan = accentTeal
    val accentPink = Color(0xFFFF8FAB)

    val inkPrimary = Color(0xFF1E293B)
    val inkSecondary = Color(0xFF475569)
    val inkMuted = Color(0xFF94A3B8)
    val online = Color(0xFF22C55E)
    val offline = Color(0xFFCBD5E1)
    val glassFill = Color.White.copy(alpha = 0.94f)
    val glassBorder = Color(0xFFE2E8F0)
    val stageGlow = accentPurple
    val danger = Color(0xFFEF4444)
}

@Composable
fun SocialGameBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SocialGamePalette.bgDeep,
                            SocialGamePalette.bgBase,
                            SocialGamePalette.bgBottom,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = (-20).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(SocialGamePalette.accentPink.copy(alpha = 0.22f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = 80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(SocialGamePalette.accentSky.copy(alpha = 0.20f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomStart)
                .offset(x = 30.dp, y = (-60).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(SocialGamePalette.accentPurple.copy(alpha = 0.12f), Color.Transparent),
                    ),
                ),
        )
    }
}

@Composable
fun SocialGameScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        SocialGameBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = SocialGamePalette.inkPrimary,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = SocialGamePalette.inkPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            color = SocialGamePalette.inkMuted,
                            fontSize = 12.sp,
                        )
                    }
                }
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            title,
            color = SocialGamePalette.inkPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = SocialGamePalette.inkMuted, fontSize = 12.sp)
        }
    }
}

@Composable
fun HubPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) = HubPrimaryButtonInternal(text, onClick, modifier, enabled && !loading, loading)

@Composable
fun HubPrimaryButtonInternal(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "hubBtnScale")
    Box(
        modifier = modifier
            .scale(scale)
            .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = SocialGamePalette.accentCoral.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(SocialGamePalette.accentCoral, SocialGamePalette.accentPurple),
                ),
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
fun HubSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "hubSecScale")
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.85f))
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = SocialGamePalette.accentPurple,
            )
        } else {
            Text(
                text = text,
                color = SocialGamePalette.inkSecondary,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color = SocialGamePalette.accentGold,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFF6366F1).copy(alpha = 0.12f))
            .clip(RoundedCornerShape(20.dp))
            .background(SocialGamePalette.glassFill)
            .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(20.dp)),
    ) {
        content()
    }
}

@Composable
fun PremiumStageCard(
    accent: List<Long>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c1 = Color((accent.getOrNull(0) ?: 0xFF7C5CFC).toInt())
    val c2 = Color((accent.getOrNull(1) ?: 0xFF38BDF8).toInt())
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = c1.copy(alpha = 0.18f))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White,
                        c1.copy(alpha = 0.08f),
                        c2.copy(alpha = 0.06f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(c1.copy(alpha = 0.35f), Color.White.copy(alpha = 0.9f), c2.copy(alpha = 0.30f)),
                ),
                shape = RoundedCornerShape(24.dp),
            ),
    ) {
        content()
    }
}

@Composable
fun RoomCodeChip(
    code: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SocialGamePalette.bgBase.copy(alpha = 0.8f))
            .border(1.dp, SocialGamePalette.accentPurple.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        code.forEach { ch ->
            Text(
                text = ch.toString(),
                color = SocialGamePalette.accentPurple,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 3.dp),
            )
        }
    }
}

@Composable
fun StatusDotBadge(
    label: String,
    dotColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.85f))
            .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(label, color = SocialGamePalette.inkSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun GameCatalogHeroIcon(
    entry: com.example.funlife.social.game.catalog.SocialGameEntry,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 88.dp,
    emojiFontSize: androidx.compose.ui.unit.TextUnit = 40.sp,
) {
    val c1 = Color((entry.accentColors.getOrNull(0) ?: 0xFF7C4DFF).toInt())
    val c2 = Color((entry.accentColors.getOrNull(1) ?: 0xFF38BDF8).toInt())
    Box(
        modifier = modifier
            .size(size)
            .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = c1.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(c1.copy(alpha = 0.28f), c2.copy(alpha = 0.18f))))
            .border(1.dp, c1.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when (entry.gameId) {
            "gomoku" -> GomokuStonePair()
            else -> Text(entry.iconEmoji, fontSize = emojiFontSize)
        }
    }
}

@Composable
private fun GomokuStonePair() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0xFF2D2D3A))
                .border(1.dp, Color(0xFF1A1A24), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), CircleShape),
        )
    }
}

@Composable
fun InfoPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 5.dp),
        color = SocialGamePalette.inkSecondary,
        fontSize = 11.sp,
    )
}

@Composable
fun InviteStatusBanner(
    peerName: String,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SocialGamePalette.accentBlue.copy(alpha = 0.10f))
            .border(1.dp, SocialGamePalette.accentBlue.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "邀请已发送",
                color = SocialGamePalette.accentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "等待 $peerName 在任意页面接受",
                color = SocialGamePalette.inkPrimary,
                fontSize = 13.sp,
            )
        }
        Text(
            "撤回",
            color = SocialGamePalette.inkMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onWithdraw)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

fun accentBrush(colors: List<Long>): Brush {
    val c1 = Color((colors.getOrNull(0) ?: 0xFF7C5CFC).toInt())
    val c2 = Color((colors.getOrNull(1) ?: 0xFF38BDF8).toInt())
    return Brush.linearGradient(listOf(c1, c2))
}

@Composable
fun EmptyHubState(
    emoji: String,
    title: String,
    subtitle: String,
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
                .size(92.dp)
                .clip(CircleShape)
                .background(SocialGamePalette.glassFill)
                .border(1.dp, SocialGamePalette.glassBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 42.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, color = SocialGamePalette.inkPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = SocialGamePalette.inkMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun HubArrowChevron(modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = SocialGamePalette.inkMuted,
        modifier = modifier.size(22.dp),
    )
}

@Composable
fun CenteredBusyOverlay(message: String?) {
    if (message.isNullOrBlank()) return
    BackHandler { /* 加载中拦截返回，避免半开房间 */ }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = SocialGamePalette.accentPurple.copy(0.15f))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = SocialGamePalette.accentPurple,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                message,
                color = SocialGamePalette.inkPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun CenteredConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确定离开",
    dismissText: String = "再想想",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(22.dp))
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Text(
                    title,
                    color = SocialGamePalette.inkPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    message,
                    color = SocialGamePalette.inkSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HubSecondaryButton(
                        text = dismissText,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    HubPrimaryButton(
                        text = confirmText,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
