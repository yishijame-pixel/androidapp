// ═══════════════════════════════════════════════════════════════════════════
// SkinSwitcher.kt
// 通用皮肤切换浮标：右上角小药丸，点击循环切换；自带轻微毛玻璃感
// 用法：传入当前 skin 标签 + 点击回调
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.topdrawer.modes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.funlife.ui.utils.TextSize

/**
 * 三种皮肤的标识 —— Window / StarSea 共用一套，避免重复 enum
 */
enum class TopDrawerSkin(val label: String) {
    CINEMATIC("电影"),
    INK("水墨"),
    MINIMAL("极简");

    fun next(): TopDrawerSkin = values()[(ordinal + 1) % values().size]

    companion object {
        fun fromName(s: String?): TopDrawerSkin =
            values().firstOrNull { it.name == s } ?: CINEMATIC
    }
}

/**
 * 皮肤切换药丸 —— 放在内容右上角
 * @param tone "light" 适用于深底（白字），"dark" 适用于浅底（深字）
 */
@Composable
fun SkinSwitcherPill(
    skin: TopDrawerSkin,
    onCycle: () -> Unit,
    tone: String = "light",
    modifier: Modifier = Modifier
) {
    val fg = if (tone == "dark") Color(0xFF3D3328) else Color.White
    val bg = if (tone == "dark") Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.14f)
    val br = if (tone == "dark") Color(0xFF8B6F4E).copy(alpha = 0.30f) else Color.White.copy(alpha = 0.30f)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(0.6.dp, br, RoundedCornerShape(50))
            .clickable { onCycle() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("◐", fontSize = TextSize.tiny, color = fg.copy(alpha = 0.85f))
        Spacer(Modifier.width(5.dp))
        Text(
            skin.label,
            fontSize = TextSize.tiny,
            color = fg.copy(alpha = 0.92f),
            fontWeight = FontWeight.Medium,
        )
    }
}
