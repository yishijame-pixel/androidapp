// ArtisticText.kt - 艺术字组件，糖果奶油风（可爱清晰好看）
package com.example.funlife.ui.components

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 艺术字颜色主题（糖果奶油风）
 *  - gradientTop / gradientBottom：本体竖向渐变填充
 *  - glow：背后柔光阴影色
 *  - 旧字段（shadow/outer/middle/inner）保留以兼容老代码（其它地方暂未用）
 */
enum class ArtisticTextStyle(
    val displayName: String,
    val gradientTop: Color,
    val gradientBottom: Color,
    val glow: Color,
    // 兼容字段（老组件曾使用）
    val shadow: Color = gradientBottom,
    val outer: Color = gradientBottom,
    val middle: Color = gradientTop,
    val inner: Color = Color.White,
) {
    PINK(
        displayName = "粉色梦幻",
        gradientTop = Color(0xFFFF80AB),
        gradientBottom = Color(0xFFD81B60),
        glow = Color(0xFFFF4F8B)
    ),
    PURPLE(
        displayName = "紫色魅惑",
        gradientTop = Color(0xFFCE93D8),
        gradientBottom = Color(0xFF6A1B9A),
        glow = Color(0xFFAB47BC)
    ),
    BLUE(
        displayName = "蓝色海洋",
        gradientTop = Color(0xFF64B5F6),
        gradientBottom = Color(0xFF1565C0),
        glow = Color(0xFF42A5F5)
    ),
    GOLD(
        displayName = "金色辉煌",
        gradientTop = Color(0xFFFFE082),
        gradientBottom = Color(0xFFE65100),
        glow = Color(0xFFFFB300)
    ),
    RAINBOW(
        displayName = "彩虹绚丽",
        gradientTop = Color(0xFFFF80AB),
        gradientBottom = Color(0xFF7C4DFF),
        glow = Color(0xFFE040FB)
    ),
}

/**
 * 艺术字文本组件
 */
@Composable
fun ArtisticText(
    text: String,
    style: ArtisticTextStyle = ArtisticTextStyle.PINK,
    modifier: Modifier = Modifier
) {
    // 🍬 糖果奶油风：白色奶油外圈 + 主题色渐变填充 + 柔光阴影
    //   - 字体长度自适应字号
    //   - 不堆叠多层描边，保持清晰干净
    //   - 让字体自然脱离背景，又不刺眼

    val len = text.length
    val fontSize: androidx.compose.ui.unit.TextUnit = when {
        len <= 6 -> 32.sp
        len <= 10 -> 26.sp
        len <= 16 -> 21.sp
        len <= 24 -> 17.sp
        len <= 36 -> 14.sp
        else -> 12.sp
    }
    val scale = (fontSize.value / 26f).coerceIn(0.5f, 1.3f)
    val maxLines = if (len > 10) 3 else 2
    val letterSpacing = (0.5f * scale).sp

    // 渐变填充画刷（竖向，从亮到深，糖果感）
    val fillBrush = Brush.verticalGradient(
        colors = listOf(
            style.gradientTop,
            style.gradientBottom,
        )
    )

    // ── Layer 1：奶油外圈（白色厚描边，让字脱离粉紫背景）──
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = Color.White,
            drawStyle = Stroke(
                width = 9f * scale,
                join = StrokeJoin.Round
            ),
            letterSpacing = letterSpacing,
            shadow = Shadow(
                color = style.glow.copy(alpha = 0.45f),
                offset = Offset(0f, 3f * scale),
                blurRadius = 14f * scale,
            )
        ),
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )

    // ── Layer 2：主体渐变填充 + 主题色柔光底阴影 ──
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            brush = fillBrush,
            letterSpacing = letterSpacing,
            shadow = Shadow(
                color = style.gradientBottom.copy(alpha = 0.35f),
                offset = Offset(0f, 2f * scale),
                blurRadius = 4f * scale,
            )
        ),
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )

    // ── Layer 3：顶部高光奶油层（轻微 offset 制造立体感）──
    Text(
        text = text,
        modifier = modifier.offset(x = 0.dp, y = (-0.6f * scale).dp),
        style = TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.0f),
                ),
                endY = fontSize.value * 1.6f
            ),
            letterSpacing = letterSpacing,
        ),
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}
