// QuoteCard.kt — v53 阅光书房 · 摘抄卡片
//
// 三态：
//   普通摘抄
//   locked = true  → 等待开启的胶囊（模糊文字 + 倒计时）
//   opened = true  → 已经开启的胶囊（保留时光感小标识）
package com.example.funlife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.data.model.Quote
import com.example.funlife.ui.theme.ReadingRoomTheme as RT
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuoteCard(
    q: Quote,
    locked: Boolean = false,
    opened: Boolean = false,
    onClick: () -> Unit = {},
    onPin: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val bg = when {
        locked -> RT.CardSky
        opened -> RT.CardPeach
        q.pinned -> RT.CardCream
        else -> RT.CardCream
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(enabled = !locked, onClick = onClick)
            .padding(16.dp)
    ) {
        // 标签行
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                locked -> SmallChip("⏳ 待开启", RT.AccentSky)
                opened -> SmallChip("✉️ 已开启", RT.AccentOrange)
                q.pinned -> SmallChip("📌 收藏", RT.AccentRose)
                else -> SmallChip("📝 摘抄", RT.MutedInk)
            }
            Spacer(Modifier.weight(1f))
            if (q.rating > 0) {
                Text("★".repeat(q.rating),
                    color = RT.AccentGold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        // 主体
        Text(
            text = if (locked) q.text else q.text,
            color = RT.PrimaryInk,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            fontStyle = FontStyle.Italic,
            modifier = if (locked) Modifier.blur(8.dp) else Modifier
        )
        if (locked) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = countdown(q.capsuleDeliveryAt),
                color = RT.AccentSky,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (q.page > 0) {
                Text("P. ${q.page}", color = RT.MutedInk, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
            }
            Text(formatDate(q.createdAt), color = RT.MutedInk, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            if (!locked) {
                TextButton(onClick = onPin, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text(if (q.pinned) "取消收藏" else "📌 收藏",
                        fontSize = 11.sp, color = RT.SecondaryInk)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, "删除",
                    tint = RT.MutedInk, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SmallChip(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatDate(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(ms))

private fun countdown(targetMs: Long): String {
    val now = System.currentTimeMillis()
    val delta = targetMs - now
    if (delta <= 0) return "已到投递时间"
    val days = delta / (24 * 3600 * 1000L)
    val hours = (delta / (3600 * 1000L)) % 24
    return when {
        days >= 30 -> "${days / 30} 个月后开启"
        days >= 1 -> "$days 天 ${hours} 小时后开启"
        else -> "${hours} 小时后开启"
    }
}
