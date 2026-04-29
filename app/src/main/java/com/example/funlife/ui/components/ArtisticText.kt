// ArtisticText.kt - 艺术字组件，支持多种颜色主题
package com.example.funlife.ui.components

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 艺术字颜色主题
 */
enum class ArtisticTextStyle(
    val displayName: String,
    val shadow: Color,
    val outer: Color,
    val middle: Color,
    val inner: Color,
    val glow: Color
) {
    PINK(
        "粉色梦幻",
        Color(0xFFD81B60),
        Color(0xFFEC407A),
        Color(0xFFF48FB1),
        Color(0xFFFCE4EC),
        Color(0xFFFF69B4)
    ),
    PURPLE(
        "紫色魅惑",
        Color(0xFF6A1B9A),
        Color(0xFF8E24AA),
        Color(0xFFAB47BC),
        Color(0xFFE1BEE7),
        Color(0xFF9C27B0)
    ),
    BLUE(
        "蓝色海洋",
        Color(0xFF1565C0),
        Color(0xFF1976D2),
        Color(0xFF42A5F5),
        Color(0xFFBBDEFB),
        Color(0xFF2196F3)
    ),
    GOLD(
        "金色辉煌",
        Color(0xFFF57F17),
        Color(0xFFFBC02D),
        Color(0xFFFFEB3B),
        Color(0xFFFFF9C4),
        Color(0xFFFFD700)
    ),
    RAINBOW(
        "彩虹绚丽",
        Color(0xFFE91E63),
        Color(0xFF9C27B0),
        Color(0xFF2196F3),
        Color(0xFF4CAF50),
        Color(0xFFFF9800)
    )
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
    // 第一层：深色外阴影（最外层，营造强烈3D立体感）
    Text(
        text = text,
        modifier = modifier.offset(x = 3.dp, y = 3.dp),
        style = androidx.compose.ui.text.TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = style.shadow.copy(alpha = 0.4f),
            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 14f,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            ),
            letterSpacing = 1.sp
        ),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    
    // 第二层：深色厚描边（立体感）
    Text(
        text = text,
        modifier = modifier.offset(x = 1.5.dp, y = 1.5.dp),
        style = androidx.compose.ui.text.TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = style.outer,
            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 10f,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            ),
            letterSpacing = 1.sp
        ),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    
    // 第三层：中色描边（华丽感）
    Text(
        text = text,
        modifier = modifier,
        style = androidx.compose.ui.text.TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = style.middle,
            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 7f,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            ),
            letterSpacing = 1.sp
        ),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    
    // 第四层：浅色细描边（过渡层）
    Text(
        text = text,
        modifier = modifier,
        style = androidx.compose.ui.text.TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = style.inner,
            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 4f,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            ),
            letterSpacing = 1.sp
        ),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    
    // 第五层：白色填充 + 强烈光晕（最内层）
    Text(
        text = text,
        modifier = modifier,
        style = androidx.compose.ui.text.TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            shadow = androidx.compose.ui.graphics.Shadow(
                color = style.glow.copy(alpha = 0.8f),
                offset = androidx.compose.ui.geometry.Offset(0f, 0f),
                blurRadius = 16f
            ),
            letterSpacing = 1.sp
        ),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    
    // 第六层：内部高光（增强立体感）
    Text(
        text = text,
        modifier = modifier.offset(x = (-0.5).dp, y = (-0.5).dp),
        style = androidx.compose.ui.text.TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        ),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
