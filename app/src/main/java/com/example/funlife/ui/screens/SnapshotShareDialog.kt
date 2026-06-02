// SnapshotShareDialog.kt — v53 阅光书房 · 阅读快照分享卡
//
// 把"这本书的阅读心电图 + 摘抄精选 + tagline"画成一张可保存/可分享的图片。
// 实现：Compose 里渲染好 → 用 Picture 把它绘成 Bitmap → 写到缓存 → 通过 FileProvider 分享。
//
// 已有 FileProvider 配置（项目早期 v37 同步分享卡曾用过）。如果没有，可调用：
//   val uri = saveBitmapToCache(ctx, bitmap, "reading_snapshot.png")
package com.example.funlife.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.funlife.data.model.Book
import com.example.funlife.data.model.Quote
import com.example.funlife.repository.EcgPoint
import com.example.funlife.ui.components.BookEcgCurve
import com.example.funlife.ui.theme.ReadingRoomTheme as RT
import java.io.File
import java.io.FileOutputStream

@Composable
fun SnapshotShareDialog(
    book: Book,
    ecg: List<EcgPoint>,
    topQuotes: List<Quote>,
    totalMinutes: Int,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    var sharing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RT.CardCream,
        title = { Text("📸 阅读快照", fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // 仅作预览：UI 端渲染。导出图由下方 "分享" 走 paintSnapshotBitmap 重新画一遍。
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(RT.snapshotGradient())
                        .padding(16.dp)
                ) {
                    SnapshotInner(book, ecg, topQuotes, totalMinutes)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (sharing) return@TextButton
                    sharing = true
                    val bmp = paintSnapshotBitmap(
                        widthPx = 1080, heightPx = 1440,
                        book = book, ecg = ecg,
                        topQuotes = topQuotes, totalMinutes = totalMinutes
                    )
                    val uri = saveBitmapToCache(ctx, bmp, "reading_snapshot.png")
                    if (uri != null) {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        ctx.startActivity(Intent.createChooser(send, "分享快照"))
                    }
                    sharing = false
                }
            ) { Text("分享", fontWeight = FontWeight.Bold, color = RT.AccentOrange) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = RT.SecondaryInk) }
        }
    )
}

@Composable
private fun SnapshotInner(
    book: Book,
    ecg: List<EcgPoint>,
    topQuotes: List<Quote>,
    totalMinutes: Int,
) {
    Column(Modifier.fillMaxSize()) {
        Text("📖 ${book.title}", color = RT.PrimaryInk,
            fontSize = 20.sp, fontWeight = FontWeight.Black)
        if (book.author.isNotBlank()) {
            Text(book.author, color = RT.SecondaryInk, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Stat("⏱", "$totalMinutes 分钟")
            Spacer(Modifier.width(12.dp))
            Stat("⭐", "${book.rating} 星")
            Spacer(Modifier.width(12.dp))
            if (book.totalPages > 0) Stat("📄", "${book.currentPage}/${book.totalPages}")
        }
        Spacer(Modifier.height(10.dp))
        BookEcgCurve(
            points = ecg,
            totalPages = book.totalPages,
            currentPage = book.currentPage,
            modifier = Modifier.fillMaxWidth().height(140.dp),
        )
        Spacer(Modifier.height(10.dp))
        if (topQuotes.isNotEmpty()) {
            val q = topQuotes.first()
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(14.dp)
            ) {
                Column {
                    Text("「${q.text.take(80)}${if (q.text.length > 80) "…" else ""}」",
                        color = RT.PrimaryInk, fontSize = 13.sp,
                        fontStyle = FontStyle.Italic, lineHeight = 22.sp)
                    if (q.page > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text("—— P. ${q.page}",
                            color = RT.MutedInk, fontSize = 10.sp)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("阅光书房", color = RT.SecondaryInk,
                fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("FunLife · v53", color = RT.MutedInk, fontSize = 9.sp)
        }
    }
}

@Composable
private fun Stat(emoji: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 14.sp)
        Spacer(Modifier.width(2.dp))
        Text(label, color = RT.PrimaryInk, fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold)
    }
}

/* ════════════════════════════════════════════════════════════
   导出图：手绘 Bitmap（与上方 Compose 预览近似但不完全相同 —— 优势是稳定、与 Compose 内部 API 解耦）
   ════════════════════════════════════════════════════════════ */

private fun paintSnapshotBitmap(
    widthPx: Int,
    heightPx: Int,
    book: Book,
    ecg: List<EcgPoint>,
    topQuotes: List<Quote>,
    totalMinutes: Int,
): Bitmap {
    val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)

    // 背景：纵向渐变（米白 → 暖橙 → 玫瑰）
    val bgPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, heightPx.toFloat(),
            intArrayOf(0xFFFFF9F2.toInt(), 0xFFFCE0CC.toInt(), 0xFFFFAB66.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    c.drawRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), bgPaint)

    val pad = widthPx * 0.08f

    // 标题
    val titlePaint = Paint().apply {
        color = 0xFF2C3E50.toInt()
        textSize = widthPx * 0.062f
        isAntiAlias = true
        isFakeBoldText = true
    }
    c.drawText("📖 ${book.title.take(18)}", pad, pad + titlePaint.textSize, titlePaint)

    // 作者
    val subPaint = Paint().apply {
        color = 0xFF607185.toInt()
        textSize = widthPx * 0.034f
        isAntiAlias = true
    }
    if (book.author.isNotBlank()) {
        c.drawText(book.author.take(30), pad, pad + titlePaint.textSize + subPaint.textSize + 12f, subPaint)
    }

    // 统计行
    val statPaint = Paint().apply {
        color = 0xFF2C3E50.toInt()
        textSize = widthPx * 0.034f
        isAntiAlias = true
        isFakeBoldText = true
    }
    val statY = pad + titlePaint.textSize + subPaint.textSize + 64f
    val stats = buildString {
        append("⏱ $totalMinutes 分钟")
        if (book.rating > 0) append("   ⭐ ${book.rating} 星")
        if (book.totalPages > 0) append("   📄 ${book.currentPage}/${book.totalPages}")
    }
    c.drawText(stats, pad, statY, statPaint)

    // 心电图区域
    val ecgTop = statY + 40f
    val ecgBot = ecgTop + heightPx * 0.22f
    val ecgRect = RectF(pad, ecgTop, widthPx - pad, ecgBot)
    // 卡片背景
    val cardPaint = Paint().apply {
        color = 0xCCFFFCF7.toInt(); isAntiAlias = true
    }
    c.drawRoundRect(ecgRect, 36f, 36f, cardPaint)
    drawEcgOnCanvas(c, ecg, book.totalPages, book.currentPage, ecgRect)

    // 摘抄精选卡（白底）
    val quoteTop = ecgBot + 40f
    val quoteBot = quoteTop + heightPx * 0.22f
    val quoteRect = RectF(pad, quoteTop, widthPx - pad, quoteBot)
    val quoteCardPaint = Paint().apply {
        color = 0xF2FFFFFF.toInt(); isAntiAlias = true
    }
    c.drawRoundRect(quoteRect, 36f, 36f, quoteCardPaint)
    val q = topQuotes.firstOrNull()
    if (q != null) {
        val text = "「${q.text.take(80)}${if (q.text.length > 80) "…" else ""}」"
        val quotePaint = Paint().apply {
            color = 0xFF2C3E50.toInt()
            textSize = widthPx * 0.038f
            isAntiAlias = true
            textSkewX = -0.18f
        }
        wrapAndDrawText(c, text, quotePaint, quoteRect.left + 32f, quoteRect.top + 56f, quoteRect.width() - 64f, lineHeight = quotePaint.textSize * 1.5f, maxLines = 5)
        if (q.page > 0) {
            val tagPaint = Paint().apply {
                color = 0xFFA9B5C4.toInt(); textSize = widthPx * 0.028f; isAntiAlias = true
            }
            c.drawText("—— P. ${q.page}", quoteRect.left + 32f, quoteRect.bottom - 30f, tagPaint)
        }
    } else {
        val emptyPaint = Paint().apply {
            color = 0xFFA9B5C4.toInt(); textSize = widthPx * 0.034f; isAntiAlias = true
        }
        c.drawText("还没有摘抄。", quoteRect.left + 32f, quoteRect.centerY() + 20f, emptyPaint)
    }

    // 底部水印
    val watermarkPaint = Paint().apply {
        color = 0xFF607185.toInt(); textSize = widthPx * 0.028f
        isAntiAlias = true; isFakeBoldText = true
    }
    c.drawText("阅光书房 · FunLife", pad, heightPx - pad * 0.7f, watermarkPaint)
    val verPaint = Paint(watermarkPaint).apply { color = 0xFFA9B5C4.toInt(); isFakeBoldText = false }
    val verText = "v53"
    c.drawText(verText, widthPx - pad - verPaint.measureText(verText), heightPx - pad * 0.7f, verPaint)

    return bmp
}

/** 在指定 RectF 内绘制阅读心电图。 */
private fun drawEcgOnCanvas(
    c: Canvas,
    points: List<EcgPoint>,
    totalPages: Int,
    currentPage: Int,
    rect: RectF,
) {
    val padding = 24f
    val area = RectF(rect.left + padding, rect.top + padding, rect.right - padding, rect.bottom - padding)
    if (points.isEmpty()) {
        val p = Paint().apply {
            color = 0xFFA9B5C4.toInt(); textSize = rect.width() * 0.04f
            isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        c.drawText("📈 还没有心电图", rect.centerX(), rect.centerY(), p)
        return
    }
    val maxW = points.maxOf { it.weight }.coerceAtLeast(0.0001f)
    val pageMax = (totalPages.takeIf { it > 0 } ?: points.maxOf { it.page }).coerceAtLeast(1)
    val pts = points.map {
        val x = area.left + (it.page.toFloat() / pageMax) * area.width()
        val y = area.bottom - (it.weight / maxW) * area.height() * 0.85f
        x to y
    }
    // 填充
    val fillPath = Path().apply {
        moveTo(pts.first().first, area.bottom)
        pts.forEach { lineTo(it.first, it.second) }
        lineTo(pts.last().first, area.bottom); close()
    }
    val fillPaint = Paint().apply {
        isAntiAlias = true
        shader = LinearGradient(
            0f, area.top, 0f, area.bottom,
            0xCCF6C97A.toInt(), 0x10F6C97A,
            Shader.TileMode.CLAMP
        )
    }
    c.drawPath(fillPath, fillPaint)
    // 折线
    val linePath = Path().apply {
        pts.forEachIndexed { i, p ->
            if (i == 0) moveTo(p.first, p.second) else lineTo(p.first, p.second)
        }
    }
    val linePaint = Paint().apply {
        color = 0xFFFFAB66.toInt(); strokeWidth = 6f; style = Paint.Style.STROKE; isAntiAlias = true
    }
    c.drawPath(linePath, linePaint)
    // 数据点
    val dotPaint = Paint().apply { color = 0xFFE57373.toInt(); isAntiAlias = true }
    pts.forEach { c.drawCircle(it.first, it.second, 6f, dotPaint) }
    // 当前进度竖线
    if (currentPage in 1..pageMax) {
        val cx = area.left + (currentPage.toFloat() / pageMax) * area.width()
        val vPaint = Paint().apply {
            color = 0xFF7FB7E0.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE; isAntiAlias = true
        }
        c.drawLine(cx, area.top, cx, area.bottom, vPaint)
    }
}

/** 简易自动换行：给定文本和最大宽度，按字符宽度换行；超过 maxLines 用 … 截断。 */
private fun wrapAndDrawText(
    c: Canvas, text: String, paint: Paint,
    x: Float, yBaseline: Float, maxWidth: Float,
    lineHeight: Float, maxLines: Int,
) {
    var remaining = text
    var y = yBaseline
    var line = 0
    while (remaining.isNotEmpty() && line < maxLines) {
        // 二分找最大可容纳长度
        var lo = 1; var hi = remaining.length
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            val w = paint.measureText(remaining, 0, mid)
            if (w <= maxWidth) lo = mid else hi = mid - 1
        }
        val take = lo
        var draw = remaining.substring(0, take)
        if (line == maxLines - 1 && take < remaining.length) {
            draw = draw.dropLast(1) + "…"
        }
        c.drawText(draw, x, y, paint)
        remaining = remaining.substring(take)
        y += lineHeight
        line++
    }
}

private fun saveBitmapToCache(ctx: Context, bitmap: Bitmap, fileName: String): android.net.Uri? {
    return try {
        val dir = File(ctx.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    } catch (e: Exception) {
        android.util.Log.e("SnapshotShare", "save failed", e); null
    }
}
