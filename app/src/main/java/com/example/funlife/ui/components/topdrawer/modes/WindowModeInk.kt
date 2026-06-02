// ═══════════════════════════════════════════════════════════════════════════
// WindowModeInk.kt — 窗 · 宋画水墨皮肤
// 设计：宣纸纸面 + 三层墨色远山 + 一轮淡月/淡日 + 几只墨笔飞鸟 + 一句时辰诗
// 极致克制，留白多，低饱和。所有元素 drawPath/drawCircle 完成。
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components.topdrawer.modes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.funlife.ui.utils.Spacing
import com.example.funlife.ui.utils.TextSize
import com.example.funlife.ui.utils.rsp
import java.time.LocalTime
import kotlin.math.sin

@Composable
fun WindowModeInk() {
    val now = remember { LocalTime.now() }
    val hourF = now.hour + now.minute / 60f
    val isNight = hourF < 6f || hourF >= 18f
    val verse = remember(now.hour) { verseOfHourInk(now.hour) }

    // 纸色 —— 白天偏宣纸黄白，夜晚偏冷青黛
    val paperTop: Color
    val paperMid: Color
    val paperBot: Color
    val mountainNear: Color
    val mountainMid: Color
    val mountainFar: Color
    val bodyColor: Color   // 日 / 月
    if (isNight) {
        paperTop = Color(0xFFE6E2D6)   // 月光宣纸
        paperMid = Color(0xFFD8D2C2)
        paperBot = Color(0xFFC9C2B0)
        mountainNear = Color(0xFF2C2A28).copy(alpha = 0.88f)
        mountainMid = Color(0xFF55514A).copy(alpha = 0.55f)
        mountainFar = Color(0xFF7E776A).copy(alpha = 0.30f)
        bodyColor = Color(0xFFFAF5E6)         // 淡月（米白）
    } else {
        paperTop = Color(0xFFF5EFDD)   // 米色宣纸
        paperMid = Color(0xFFEDE5CE)
        paperBot = Color(0xFFE3D9BC)
        mountainNear = Color(0xFF3A352D).copy(alpha = 0.78f)
        mountainMid = Color(0xFF665E50).copy(alpha = 0.45f)
        mountainFar = Color(0xFF8E8576).copy(alpha = 0.22f)
        bodyColor = Color(0xFFF0B070).copy(alpha = 0.90f) // 淡日（赭石）
    }

    // 飞鸟微动 + 雾气缓慢漂移
    val tr = rememberInfiniteTransition(label = "inkAnim")
    val drift by tr.animateFloat(
        0f, 1f, infiniteRepeatable(tween(22000, easing = LinearEasing)), label = "d"
    )
    val mistDrift by tr.animateFloat(
        0f, 1f, infiniteRepeatable(tween(60000, easing = LinearEasing)), label = "m"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(paperTop, paperMid, paperBot)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // —— 1) 宣纸纹理：极少数斑点，模拟纸张颗粒
            for (i in 0 until 90) {
                val sx = ((i * 9301 + 49297) % 233280) / 233280f
                val sy = ((i * 12289 + 33191) % 233280) / 233280f
                drawCircle(
                    color = Color(0xFF6B5A3E).copy(alpha = 0.05f),
                    radius = 0.6f,
                    center = Offset(w * sx, h * sy)
                )
            }

            // —— 2) 日 / 月
            val bx = w * 0.74f
            val by = h * 0.18f
            val r = 26f
            if (isNight) {
                // 弯月：圆 - 偏移圆 = 月牙
                val moonPath = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(bx - r, by - r, bx + r, by + r))
                }
                val shadowPath = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(bx - r + 14f, by - r, bx + r + 14f, by + r))
                }
                val crescent = Path().apply {
                    op(moonPath, shadowPath, androidx.compose.ui.graphics.PathOperation.Difference)
                }
                drawPath(crescent, color = bodyColor)
                drawPath(crescent, color = Color(0xFF55514A).copy(alpha = 0.45f), style = Stroke(width = 0.8f))
            } else {
                // 日：实心圆 + 极细描边
                drawCircle(color = bodyColor, radius = r, center = Offset(bx, by))
                drawCircle(
                    color = Color(0xFF6B5A3E).copy(alpha = 0.35f),
                    radius = r,
                    center = Offset(bx, by),
                    style = Stroke(width = 0.6f)
                )
            }

            // —— 2.5) 横向飘雾（夜晚画在山腰；白天画在山间）—— 多段半透明椭圆
            val mistY = h * (if (isNight) 0.70f else 0.74f)
            for (i in 0 until 5) {
                val phase = ((mistDrift + i * 0.21f) % 1f)
                val mx = w * (-0.2f + phase * 1.4f) - w * 0.1f * i
                val my = mistY + i * 6f - 12f
                val mw = 180f + i * 30f
                val mh = 14f + i * 2f
                drawOval(
                    color = (if (isNight) Color(0xFFE6E2D6) else Color(0xFFF5EFDD)).copy(alpha = 0.22f),
                    topLeft = Offset(mx - mw / 2f, my - mh / 2f),
                    size = androidx.compose.ui.geometry.Size(mw, mh)
                )
            }

            // —— 3) 远山三层（path 起伏）
            fun drawMountain(layer: Int, baseY: Float, amp: Float, color: Color) {
                val path = Path()
                path.moveTo(0f, h)
                path.lineTo(0f, h * baseY)
                val steps = 24
                for (s in 0..steps) {
                    val tx = s.toFloat() / steps
                    // 使用 sin 叠加产生山脊感
                    val k = sin(tx * 6.28f * (1.5f + layer * 0.5f) + layer * 1.7f)
                    val k2 = sin(tx * 6.28f * (3f + layer) + layer * 0.4f) * 0.4f
                    val y = h * baseY - (k + k2) * amp
                    path.lineTo(w * tx, y)
                }
                path.lineTo(w, h)
                path.close()
                drawPath(path, color = color)
            }
            drawMountain(0, 0.78f, 8f, mountainFar)
            drawMountain(1, 0.84f, 14f, mountainMid)
            drawMountain(2, 0.92f, 20f, mountainNear)

            // —— 4) 飞鸟编队（V 字阵：领头一只 + 左右各两只）
            val flockX = (drift * w * 1.4f) - w * 0.2f
            val flockY = h * 0.30f
            val birdOffsets = listOf(
                0f to 0f,            // 领头
                -22f to 8f, -44f to 16f,   // 左翼
                22f to 8f, 44f to 16f      // 右翼
            )
            birdOffsets.forEachIndexed { i, (ox, oy) ->
                val cx = flockX + ox
                val cy = flockY + oy + sin((drift + i * 0.13f) * 6.28f) * 1.5f
                if (cx in -20f..(w + 20f)) {
                    val span = 7f
                    val dip = 2.8f
                    val path = Path().apply {
                        moveTo(cx - span, cy + dip)
                        quadraticBezierTo(cx - span / 2f, cy - dip, cx, cy + dip)
                        quadraticBezierTo(cx + span / 2f, cy - dip, cx + span, cy + dip)
                    }
                    drawPath(
                        path = path,
                        color = mountainNear.copy(alpha = 0.70f),
                        style = Stroke(width = 1.3f)
                    )
                }
            }

            // —— 5) 题款印章（朱砂方印 + 内嵌十字镂空，类似古印篆刻感）
            val stampSize = 22f
            val sx = w - 32f - stampSize
            val sy = h - 40f - stampSize
            // 朱砂底
            drawRect(
                color = Color(0xFFB23A48).copy(alpha = 0.82f),
                topLeft = Offset(sx, sy),
                size = androidx.compose.ui.geometry.Size(stampSize, stampSize)
            )
            // 内嵌"十"字镂空（白色，模拟篆字）
            drawLine(
                color = Color(0xFFEFE8D2),
                start = Offset(sx + stampSize / 2f, sy + 4f),
                end = Offset(sx + stampSize / 2f, sy + stampSize - 4f),
                strokeWidth = 2.2f
            )
            drawLine(
                color = Color(0xFFEFE8D2),
                start = Offset(sx + 4f, sy + stampSize / 2f),
                end = Offset(sx + stampSize - 4f, sy + stampSize / 2f),
                strokeWidth = 2.2f
            )
        }

        // —— 顶部：朝代体问候
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = Spacing.lg, start = Spacing.lg, end = Spacing.lg)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isNight) "夜  半" else "晨  昏",
                fontSize = TextSize.sm,
                color = mountainNear.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium,
                letterSpacing = 8.rsp,
                fontStyle = FontStyle.Italic
            )
        }

        // —— 中央：诗句（毛笔字感）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = verse.line1,
                fontSize = TextSize.headline,
                color = mountainNear,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 32.rsp,
                letterSpacing = 4.rsp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = verse.line2,
                fontSize = TextSize.md,
                color = mountainNear.copy(alpha = 0.78f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 28.rsp,
                letterSpacing = 4.rsp,
                fontStyle = FontStyle.Italic
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "— ${verse.author} —",
                fontSize = TextSize.tiny,
                color = mountainNear.copy(alpha = 0.45f),
            )
        }
    }
}

private data class VerseInk(val line1: String, val line2: String, val author: String)
private fun verseOfHourInk(hour: Int): VerseInk {
    val pool = when (hour) {
        in 5..10 -> listOf(
            VerseInk("人生若只如初见", "何事秋风悲画扇", "纳兰性德"),
            VerseInk("迟日江山丽", "春风花草香", "杜甫")
        )
        in 11..16 -> listOf(
            VerseInk("行到水穷处", "坐看云起时", "王维"),
            VerseInk("人闲桂花落", "夜静春山空", "王维")
        )
        in 17..19 -> listOf(
            VerseInk("夕阳无限好", "只是近黄昏", "李商隐"),
            VerseInk("落霞与孤鹜齐飞", "秋水共长天一色", "王勃")
        )
        in 20..22 -> listOf(
            VerseInk("但愿人长久", "千里共婵娟", "苏轼"),
            VerseInk("海上生明月", "天涯共此时", "张九龄")
        )
        else -> listOf(
            VerseInk("夜阑卧听风吹雨", "铁马冰河入梦来", "陆游"),
            VerseInk("此时无声胜有声", "夜深忽梦少年事", "白居易")
        )
    }
    return pool[(System.currentTimeMillis() / 60000 % pool.size).toInt()]
}
