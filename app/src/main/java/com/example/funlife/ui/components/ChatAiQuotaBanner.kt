package com.example.funlife.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.ui.utils.rdp
import com.example.funlife.ui.utils.rsp
import com.example.funlife.vip.ChatAiBarState
import com.example.funlife.vip.ChatAiEntitlementUi
import com.example.funlife.vip.VipQuota

/**
 * P2 · 聊天输入栏上方额度提示条（≥80% 或已用尽时显示）
 */
@Composable
fun ChatAiQuotaBanner(
    entitlement: ChatAiEntitlementUi,
    themeColor: Color,
    onOpenQuota: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = entitlement.progress
    val show = entitlement.hasCloudEntitlement && progress != null && (
        entitlement.state == ChatAiBarState.EXHAUSTED_DAY ||
            entitlement.state == ChatAiBarState.EXHAUSTED_MONTH ||
            (entitlement.dailyLimit > 0 && progress >= 0.8f)
        )
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val accent = when (entitlement.state) {
            ChatAiBarState.EXHAUSTED_DAY, ChatAiBarState.EXHAUSTED_MONTH -> Color(0xFFE65100)
            else -> themeColor
        }
        val limitText = "${entitlement.dailyLimit}"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.rdp, vertical = 4.rdp)
                .clip(RoundedCornerShape(12.rdp))
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.14f), accent.copy(alpha = 0.06f))
                    )
                )
                .clickable(onClick = onOpenQuota)
                .padding(horizontal = 12.rdp, vertical = 8.rdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(16.rdp))
            Spacer(Modifier.width(8.rdp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (entitlement.state == ChatAiBarState.EXHAUSTED_MONTH)
                        "本月 AI 额度已用完"
                    else if (entitlement.state == ChatAiBarState.EXHAUSTED_DAY)
                        "今日 AI 额度已用完"
                    else
                        "今日 AI 额度即将用完",
                    fontSize = 12.rsp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
                if (entitlement.dailyLimit > 0) {
                    Spacer(Modifier.height(4.rdp))
                    LinearProgressIndicator(
                        progress = progress!!.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.rdp)
                            .clip(RoundedCornerShape(2.rdp)),
                        color = accent,
                        trackColor = accent.copy(alpha = 0.2f),
                    )
                    Spacer(Modifier.height(2.rdp))
                    Text(
                        "${entitlement.usedToday} / $limitText 条 · 点击激活卡密",
                        fontSize = 10.rsp,
                        color = Color(0xFF777777)
                    )
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = accent.copy(alpha = 0.7f), modifier = Modifier.size(18.rdp))
        }
    }
}
