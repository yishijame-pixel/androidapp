// ReaderDnaRadar.kt — v53 阅光书房 · 6 维雷达图（理性/感性/向内/向外/温柔/锋利）
package com.example.funlife.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.funlife.repository.ReaderDnaRepository
import com.example.funlife.ui.theme.ReadingRoomTheme as RT
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ReaderDnaRadar(
    dna: ReaderDnaRepository.ParsedDna,
    modifier: Modifier = Modifier,
    fillColor: Color = RT.AccentRose,
    strokeColor: Color = RT.AccentOrange,
    gridColor: Color = RT.MutedInk.copy(alpha = 0.4f),
    labelColor: Color = RT.PrimaryInk,
) {
    val labels = listOf("理性", "感性", "向内", "向外", "温柔", "锋利")
    val values = listOf(
        dna.rationality, dna.sensibility,
        dna.inward, dna.outward,
        dna.gentleness, dna.sharpness,
    ).map { it.coerceIn(0f, 1f) }

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val radius = (kotlin.math.min(size.width, size.height) / 2) * 0.72f
            val n = 6
            val angles = (0 until n).map { -PI / 2 + 2 * PI * it / n }

            // 6 个等距同心多边形（30/45/60/75/90% 分级）
            for (level in listOf(0.25f, 0.5f, 0.75f, 1f)) {
                val path = Path()
                angles.forEachIndexed { i, a ->
                    val px = cx + radius * level * cos(a).toFloat()
                    val py = cy + radius * level * sin(a).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, color = gridColor, style = Stroke(width = 1f))
            }
            // 6 条放射轴
            angles.forEach { a ->
                drawLine(
                    color = gridColor,
                    start = Offset(cx, cy),
                    end = Offset(cx + radius * cos(a).toFloat(), cy + radius * sin(a).toFloat()),
                    strokeWidth = 1f
                )
            }
            // 数据多边形 + 渐变填充
            val dataPath = Path()
            values.forEachIndexed { i, v ->
                val a = angles[i]
                val px = cx + radius * v * cos(a).toFloat()
                val py = cy + radius * v * sin(a).toFloat()
                if (i == 0) dataPath.moveTo(px, py) else dataPath.lineTo(px, py)
            }
            dataPath.close()
            drawPath(
                path = dataPath,
                brush = Brush.radialGradient(
                    listOf(fillColor.copy(alpha = 0.45f), fillColor.copy(alpha = 0.18f)),
                    center = Offset(cx, cy), radius = radius
                )
            )
            drawPath(dataPath, color = strokeColor, style = Stroke(width = 2.5f))
            // 数据顶点
            values.forEachIndexed { i, v ->
                val a = angles[i]
                val px = cx + radius * v * cos(a).toFloat()
                val py = cy + radius * v * sin(a).toFloat()
                drawCircle(strokeColor, radius = 4f, center = Offset(px, py))
            }
            // 标签
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    (labelColor.alpha * 255).toInt(),
                    (labelColor.red * 255).toInt(),
                    (labelColor.green * 255).toInt(),
                    (labelColor.blue * 255).toInt(),
                )
                textSize = 36f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            labels.forEachIndexed { i, name ->
                val a = angles[i]
                val px = cx + (radius + 56f) * cos(a).toFloat()
                val py = cy + (radius + 56f) * sin(a).toFloat() + 12f
                drawContext.canvas.nativeCanvas.drawText(name, px, py, paint)
            }
        }
    }
}
