// AccountSwitcher.kt — 聊天记账多账户切换条（Phase 2A）
//
// 横滑胶囊：每个账户一颗胶囊，含 emoji + 名称 + 余额。
// 选中态：彩色描边 + 主题色填充；其他：灰白底。
//
// 屏幕适配：使用 Spacing / Radius / TextSize 设计令牌（屏幕适配指南）。
package com.example.funlife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.funlife.data.model.Account
import com.example.funlife.ui.utils.Radius
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import java.util.Locale

@Composable
fun AccountSwitcher(
    accounts: List<Account>,
    currentAccountId: Long?,
    themeColor: Color,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(items = accounts, key = { it.id }) { acc ->
            AccountChip(
                account = acc,
                selected = acc.id == currentAccountId,
                themeColor = themeColor,
                onClick = { onSelect(acc.id) }
            )
        }
    }
}

@Composable
private fun AccountChip(
    account: Account,
    selected: Boolean,
    themeColor: Color,
    onClick: () -> Unit
) {
    val accentColor = Color(account.color or 0xFF000000L)
    val bg = if (selected) accentColor.copy(alpha = 0.15f) else Color.White
    val borderColor = if (selected) accentColor else Color(0xFFE0E0E0)
    val borderW = if (selected) 1.5.dp else 1.dp
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(bg)
            .border(borderW, borderColor, RoundedCornerShape(Radius.pill))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(account.icon, fontSize = TextSize.md)
        Text(
            account.name,
            fontSize = TextSize.xs,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            color = if (selected) accentColor else Color(0xFF424242)
        )
        // 余额：仅在选中时显示，避免横滑过宽
        if (selected) {
            Text(
                formatBalance(account.balance),
                fontSize = TextSize.tiny,
                fontWeight = FontWeight.Bold,
                color = accentColor.copy(alpha = 0.85f)
            )
        }
    }
}

private fun formatBalance(v: Double): String {
    val abs = kotlin.math.abs(v)
    val sign = if (v < 0) "-" else ""
    return when {
        abs >= 10_000 -> String.format(Locale.CHINA, "%s¥%.1f万", sign, abs / 10_000)
        else -> String.format(Locale.CHINA, "%s¥%.0f", sign, abs)
    }
}
