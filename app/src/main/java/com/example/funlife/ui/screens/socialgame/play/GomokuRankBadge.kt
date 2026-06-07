package com.example.funlife.ui.screens.socialgame.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.social.game.engine.GomokuEloCalculator
import com.example.funlife.social.game.engine.GomokuPlayerStats
import com.example.funlife.social.game.engine.GomokuRank
import com.example.funlife.ui.screens.socialgame.SocialGamePalette

/**
 * 段位徽章组件
 */
@Composable
fun GomokuRankBadge(
    rank: GomokuRank,
    elo: Int? = null,
    size: RankBadgeSize = RankBadgeSize.MEDIUM,
    modifier: Modifier = Modifier,
) {
    val rankColor = Color(rank.colorHex)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(size.cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        rankColor.copy(alpha = 0.15f),
                        rankColor.copy(alpha = 0.08f),
                    ),
                ),
            )
            .border(1.dp, rankColor.copy(alpha = 0.35f), RoundedCornerShape(size.cornerRadius))
            .padding(horizontal = size.horizontalPadding, vertical = size.verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(size.spacing),
    ) {
        Text(
            text = rank.iconEmoji,
            fontSize = size.emojiSize,
        )
        Column {
            Text(
                text = rank.displayName,
                color = rankColor,
                fontSize = size.textSize,
                fontWeight = FontWeight.Bold,
            )
            if (elo != null && size != RankBadgeSize.SMALL) {
                Text(
                    text = "$elo 分",
                    color = SocialGamePalette.inkMuted,
                    fontSize = (size.textSize.value - 2).sp,
                )
            }
        }
    }
}

/**
 * 紧凑版段位标签（用于列表项）
 */
@Composable
fun CompactRankTag(
    rank: GomokuRank,
    modifier: Modifier = Modifier,
) {
    val rankColor = Color(rank.colorHex)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(rankColor.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(rank.iconEmoji, fontSize = 10.sp)
        Text(
            rank.shortName,
            color = rankColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * ELO 变化显示
 */
@Composable
fun EloChangeBadge(
    delta: Int,
    modifier: Modifier = Modifier,
) {
    val isPositive = delta >= 0
    val color = if (isPositive) SocialGamePalette.accentMint else SocialGamePalette.accentCoral
    val text = if (isPositive) "+$delta" else "$delta"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * 玩家战绩卡片
 */
@Composable
fun PlayerStatsCard(
    stats: GomokuPlayerStats,
    modifier: Modifier = Modifier,
) {
    val rank = stats.rank
    val rankColor = Color(rank.colorHex)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SocialGamePalette.glassFill)
            .border(1.dp, SocialGamePalette.glassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 段位和 ELO
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                rankColor.copy(alpha = 0.25f),
                                rankColor.copy(alpha = 0.08f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(rank.iconEmoji, fontSize = 24.sp)
            }
            Column {
                Text(
                    text = rank.displayName,
                    color = rankColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${stats.eloRating} 分",
                    color = SocialGamePalette.inkSecondary,
                    fontSize = 14.sp,
                )
            }
        }

        // 战绩统计
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatItem(label = "对局", value = "${stats.gamesPlayed}")
            StatItem(label = "胜利", value = "${stats.gamesWon}", color = SocialGamePalette.accentMint)
            StatItem(label = "失败", value = "${stats.gamesLost}", color = SocialGamePalette.accentCoral)
            StatItem(label = "胜率", value = "${stats.winRatePercent}%")
        }

        // 连胜记录
        if (stats.winStreak > 0 || stats.bestStreak > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (stats.winStreak > 0) {
                    StreakBadge(label = "连胜", count = stats.winStreak, isActive = true)
                }
                if (stats.bestStreak > 0) {
                    StreakBadge(label = "最佳", count = stats.bestStreak, isActive = false)
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color = SocialGamePalette.inkPrimary,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = SocialGamePalette.inkMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun StreakBadge(
    label: String,
    count: Int,
    isActive: Boolean,
) {
    val color = if (isActive) SocialGamePalette.accentGold else SocialGamePalette.inkMuted

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(if (isActive) "🔥" else "⭐", fontSize = 12.sp)
        Text(
            text = "$label $count",
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * 段位徽章尺寸
 */
enum class RankBadgeSize(
    val cornerRadius: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val spacing: Dp,
    val emojiSize: androidx.compose.ui.unit.TextUnit,
    val textSize: androidx.compose.ui.unit.TextUnit,
) {
    SMALL(8.dp, 6.dp, 3.dp, 4.dp, 12.sp, 11.sp),
    MEDIUM(10.dp, 10.dp, 6.dp, 6.dp, 16.sp, 13.sp),
    LARGE(12.dp, 14.dp, 10.dp, 8.dp, 20.sp, 15.sp),
}

/**
 * 对局结果中的 ELO 变化展示
 */
@Composable
fun MatchResultEloChange(
    myOldElo: Int,
    myNewElo: Int,
    opponentOldElo: Int,
    opponentNewElo: Int,
    isWinner: Boolean,
    modifier: Modifier = Modifier,
) {
    val myDelta = myNewElo - myOldElo
    val opponentDelta = opponentNewElo - opponentOldElo
    val myOldRank = GomokuEloCalculator.getRank(myOldElo)
    val myNewRank = GomokuEloCalculator.getRank(myNewElo)
    val rankUp = myNewRank.ordinal > myOldRank.ordinal

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ELO 变化
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "$myOldElo",
                color = SocialGamePalette.inkMuted,
                fontSize = 16.sp,
            )
            Text("→", color = SocialGamePalette.inkMuted, fontSize = 14.sp)
            Text(
                text = "$myNewElo",
                color = SocialGamePalette.inkPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            EloChangeBadge(delta = myDelta)
        }

        // 段位变化（如果升段）
        if (rankUp) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(myNewRank.colorHex).copy(alpha = 0.15f),
                                Color(myNewRank.colorHex).copy(alpha = 0.25f),
                            ),
                        ),
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("🎉", fontSize = 20.sp)
                Text(
                    text = "晋升 ${myNewRank.displayName}！",
                    color = Color(myNewRank.colorHex),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
