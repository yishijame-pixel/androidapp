// BillShareHelper.kt — 每日账单分享卡（每条数据严格按 userId 隔离）
//
// 与 AnniversaryShareHelper / shareCountdownCard 同一套风格：
// 用 android.graphics.Canvas 离屏绘制 1080×1920 bitmap，
// 写入 cacheDir/shared_images，用现有的 ${packageName}.fileprovider 分享。
//
// ⚠️ 隔离保证：调用方必须显式传 userId（无默认值），
//    生成的文件名也带 userId 防止跨账号缓存碰撞。
package com.example.funlife.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.funlife.data.model.Bill
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 卡片主题。颜色从背景渐变一路贯穿到强调色，保证 3 套主题对比清晰。
 */
enum class BillShareTheme(
    val nameZh: String,
    val bgTop: Int,
    val bgBottom: Int,
    val accent: Int,
    val accentDark: Int,
    val titleColor: Int,
    val subtitleColor: Int,
    val onCard: Int,
    val isDark: Boolean
) {
    SAKURA(
        nameZh = "樱花粉",
        bgTop = AColor.parseColor("#FFEAF1"),
        bgBottom = AColor.parseColor("#FFF8F0"),
        accent = AColor.parseColor("#FF6B9D"),
        accentDark = AColor.parseColor("#E91E63"),
        titleColor = AColor.parseColor("#3D1F2C"),
        subtitleColor = AColor.parseColor("#AD6584"),
        onCard = AColor.parseColor("#FFFFFFFF"),
        isDark = false
    ),
    MIST(
        nameZh = "晨雾蓝",
        bgTop = AColor.parseColor("#E8F5FF"),
        bgBottom = AColor.parseColor("#FFFBEC"),
        accent = AColor.parseColor("#4FA3D1"),
        accentDark = AColor.parseColor("#2E7BAD"),
        titleColor = AColor.parseColor("#1B3A52"),
        subtitleColor = AColor.parseColor("#6F8CA3"),
        onCard = AColor.parseColor("#FFFFFFFF"),
        isDark = false
    ),
    NIGHT(
        nameZh = "深夜紫",
        bgTop = AColor.parseColor("#1A1A2E"),
        bgBottom = AColor.parseColor("#16213E"),
        accent = AColor.parseColor("#9D4EDD"),
        accentDark = AColor.parseColor("#7B2CBF"),
        titleColor = AColor.parseColor("#FFFFFF"),
        subtitleColor = AColor.parseColor("#B8B8D9"),
        onCard = AColor.parseColor("#262648"),
        isDark = true
    );
}

/**
 * 一日账单聚合视图（与 Bill.kt 解耦，方便后续扩展）。
 */
data class DailyBillSummary(
    val date: Calendar,
    val income: Double,
    val expense: Double,
    val balance: Double,
    val items: List<Bill>,
    val categoryBreakdown: List<Pair<String, Double>> // (分类名, 支出金额) 倒序
) {
    val itemCount: Int get() = items.size
}

object BillShareHelper {

    /**
     * 计算指定 userId + 指定日期（默认今日）的账单聚合。
     */
    fun summarize(
        userId: Long,
        allBills: List<Bill>,
        date: Calendar = Calendar.getInstance()
    ): DailyBillSummary {
        require(userId > 0) { "userId 必须 > 0（数据隔离）" }
        val (start, end) = dayRange(date)
        val dayItems = allBills.filter {
            it.userId == userId && it.timestamp in start until end
        }.sortedByDescending { it.timestamp }
        val income = dayItems.filter { it.amount > 0 }.sumOf { it.amount }
        val expense = dayItems.filter { it.amount < 0 }.sumOf { -it.amount }
        val balance = income - expense
        val cat = dayItems
            .filter { it.amount < 0 }
            .groupBy { it.category.ifBlank { "其它" } }
            .mapValues { (_, list) -> list.sumOf { -it.amount } }
            .toList()
            .sortedByDescending { it.second }
        return DailyBillSummary(date, income, expense, balance, dayItems, cat)
    }

    private fun dayRange(date: Calendar): Pair<Long, Long> {
        val s = (date.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return s to s + 24L * 60 * 60 * 1000
    }

    /**
     * 生成卡片 bitmap（1080×1920）。调用方负责 recycle。
     */
    fun renderBitmap(
        ctx: Context,
        userId: Long,
        summary: DailyBillSummary,
        theme: BillShareTheme,
        nickname: String,
        avatarBitmap: Bitmap? = null,
        userNote: String? = null
    ): Bitmap {
        require(userId > 0) { "userId 必须 > 0（数据隔离）" }
        val w = 1080
        val h = 1920
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // ── 背景渐变 ──
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                theme.bgTop, theme.bgBottom, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // ── 顶部装饰光斑（柔和氛围）──
        drawSoftBlob(canvas, 100f, 120f, 320f, theme.accent, alpha = 50)
        drawSoftBlob(canvas, w - 80f, 280f, 240f, theme.accentDark, alpha = 36)

        // ── Hero 区域 ──
        val pad = 64f
        var y = 120f
        // 日期
        val dateLine = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA).format(summary.date.time)
        val weekday = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")[
            summary.date.get(Calendar.DAY_OF_WEEK) - 1
        ]
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.subtitleColor
            textSize = 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        canvas.drawText("$dateLine · $weekday", pad, y, datePaint)
        y += 70f

        // 头像 + 昵称 + 标题
        val avatarSize = 110f
        if (avatarBitmap != null) {
            drawCircleBitmap(canvas, avatarBitmap, pad, y, avatarSize)
        } else {
            // 默认渐变圆
            val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    pad, y, pad + avatarSize, y + avatarSize,
                    theme.accent, theme.accentDark, Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(pad + avatarSize / 2, y + avatarSize / 2, avatarSize / 2, avatarPaint)
            val initial = nickname.firstOrNull()?.toString() ?: "U"
            val initPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AColor.WHITE; textSize = 56f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                initial.uppercase(),
                pad + avatarSize / 2,
                y + avatarSize / 2 + 20f,
                initPaint
            )
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.titleColor
            textSize = 64f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(nickname, pad + avatarSize + 28f, y + 60f, titlePaint)
        val sloganPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.subtitleColor; textSize = 30f
        }
        canvas.drawText("今日财务复盘 · 一目了然", pad + avatarSize + 28f, y + 100f, sloganPaint)
        y += avatarSize + 50f

        // ── KPI 三卡 ──
        val kpiCardW = (w - pad * 2 - 40f) / 3f
        val kpiCardH = 200f
        val kpiData = listOf(
            Triple("收入", "+${formatMoney(summary.income)}", AColor.parseColor("#10B981")),
            Triple("支出", "-${formatMoney(summary.expense)}", AColor.parseColor("#EF4444")),
            Triple("结余", formatSignedMoney(summary.balance), theme.accent)
        )
        kpiData.forEachIndexed { i, (label, value, color) ->
            val cx = pad + i * (kpiCardW + 20f)
            drawKpiCard(canvas, cx, y, kpiCardW, kpiCardH, label, value, color, theme)
        }
        y += kpiCardH + 50f

        // ── 账单明细 ──
        val titleH = 60f
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.titleColor; textSize = 44f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("📜 今日明细", pad, y + 40f, sectionTitlePaint)
        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.subtitleColor; textSize = 28f
        }
        canvas.drawText(
            "共 ${summary.itemCount} 笔",
            w - pad - countPaint.measureText("共 ${summary.itemCount} 笔"),
            y + 40f, countPaint
        )
        y += titleH

        val maxItems = 8
        val visibleItems = summary.items.take(maxItems)
        val itemH = 80f
        visibleItems.forEach { bill ->
            drawBillRow(canvas, pad, y, w - pad * 2, itemH, bill, theme)
            y += itemH + 6f
        }
        if (summary.items.size > maxItems) {
            val morePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.subtitleColor; textSize = 28f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "+${summary.items.size - maxItems} 笔未显示",
                w / 2f, y + 40f, morePaint
            )
            y += 60f
        }

        // ── 分类甜甜圈 ──
        if (summary.categoryBreakdown.isNotEmpty() && summary.expense > 0) {
            y += 40f
            canvas.drawText("🍩 支出分类占比", pad, y + 40f, sectionTitlePaint)
            y += titleH
            drawDonut(canvas, w / 2f, y + 220f, 180f, summary.categoryBreakdown, theme)
            // 图例
            drawLegend(canvas, pad, y + 80f, summary.categoryBreakdown.take(5), summary.expense, theme)
            y += 460f
        }

        // ── 用户备注（手写体） ──
        if (!userNote.isNullOrBlank()) {
            y += 20f
            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.titleColor; textSize = 36f
                typeface = Typeface.create("sans-serif-medium", Typeface.ITALIC)
            }
            // 简单换行（每行 24 字）
            val safeNote = userNote.take(72)
            safeNote.chunked(24).forEachIndexed { idx, line ->
                canvas.drawText("「$line${if (idx == 0 && safeNote.length > 24) "" else "」"}", pad, y + 40f, notePaint)
                y += 56f
            }
        }

        // ── 底部 Footer ──
        val footerY = h - 100f
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.subtitleColor; textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("via 趣生活 · 用聊天的方式记账", w / 2f, footerY, footerPaint)
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.accent; textSize = 26f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("FunLife", w / 2f, footerY + 40f, brandPaint)

        return bmp
    }

    /**
     * 渲染并分享。失败返回 null。
     */
    fun renderAndShare(
        ctx: Context,
        userId: Long,
        summary: DailyBillSummary,
        theme: BillShareTheme,
        nickname: String,
        avatarBitmap: Bitmap? = null,
        userNote: String? = null
    ): Boolean = runCatching {
        require(userId > 0) { "userId 必须 > 0（数据隔离）" }
        val bmp = renderBitmap(ctx, userId, summary, theme, nickname, avatarBitmap, userNote)
        // 🔒 文件名按 userId + date + ts，自动清理同 userId 旧文件防膨胀
        val dir = File(ctx.cacheDir, "shared_images").apply { mkdirs() }
        val dayKey = SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(summary.date.time)
        val prefix = "bill_card_${userId}_"
        dir.listFiles { f -> f.name.startsWith(prefix) }?.forEach { it.delete() }
        val file = File(dir, "${prefix}${dayKey}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 92, it) }
        bmp.recycle()
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "分享今日账单").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        true
    }.getOrElse {
        android.util.Log.e("BillShareHelper", "share failed", it)
        false
    }

    // ─── 绘制工具 ───

    private fun drawSoftBlob(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, alpha: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                cx, cy, r,
                AColor.argb(alpha, AColor.red(color), AColor.green(color), AColor.blue(color)),
                AColor.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        c.drawCircle(cx, cy, r, p)
    }

    private fun drawCircleBitmap(c: Canvas, src: Bitmap, x: Float, y: Float, size: Float) {
        val scaled = Bitmap.createScaledBitmap(src, size.toInt().coerceAtLeast(1), size.toInt().coerceAtLeast(1), true)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shader = android.graphics.BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val matrix = android.graphics.Matrix().apply { postTranslate(x, y) }
        shader.setLocalMatrix(matrix)
        paint.shader = shader
        c.drawCircle(x + size / 2, y + size / 2, size / 2, paint)
        if (scaled !== src) scaled.recycle()
    }

    private fun drawKpiCard(
        c: Canvas, x: Float, y: Float, w: Float, h: Float,
        label: String, value: String, color: Int, theme: BillShareTheme
    ) {
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = if (theme.isDark) theme.onCard else AColor.WHITE
        }
        val rect = RectF(x, y, x + w, y + h)
        c.drawRoundRect(rect, 28f, 28f, cardPaint)
        // 顶部色条
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        c.drawRoundRect(RectF(x, y, x + w, y + 8f), 28f, 28f, accentPaint)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = theme.subtitleColor; textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        c.drawText(label, x + w / 2, y + 60f, labelPaint)
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; textSize = 56f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        c.drawText(value, x + w / 2, y + 140f, valuePaint)
    }

    private fun drawBillRow(
        c: Canvas, x: Float, y: Float, w: Float, h: Float,
        bill: Bill, theme: BillShareTheme
    ) {
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (theme.isDark) theme.onCard else AColor.argb(220, 255, 255, 255)
        }
        c.drawRoundRect(RectF(x, y, x + w, y + h), 18f, 18f, cardPaint)
        // 圆图标
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (bill.amount >= 0)
                AColor.parseColor("#10B981")
            else theme.accent
        }
        c.drawCircle(x + 30f + 22f, y + h / 2, 22f, iconPaint)
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.WHITE; textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        val emoji = if (bill.amount >= 0) "+" else "-"
        c.drawText(emoji, x + 30f + 22f, y + h / 2 + 10f, emojiPaint)

        // 分类
        val catPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.titleColor; textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val cat = bill.category.ifBlank { "其它" }
        c.drawText(cat, x + 100f, y + h / 2 - 8f, catPaint)
        // 备注
        if (bill.note.isNotBlank()) {
            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.subtitleColor; textSize = 24f
            }
            val noteShown = if (bill.note.length > 18) bill.note.take(18) + "…" else bill.note
            c.drawText(noteShown, x + 100f, y + h / 2 + 28f, notePaint)
        }
        // 金额（右）
        val amountColor = if (bill.amount >= 0)
            AColor.parseColor("#10B981")
        else AColor.parseColor("#EF4444")
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = amountColor; textSize = 38f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        c.drawText(formatSignedMoney(bill.amount), x + w - 30f, y + h / 2 + 12f, amountPaint)
    }

    private fun drawDonut(
        c: Canvas, cx: Float, cy: Float, r: Float,
        slices: List<Pair<String, Double>>, theme: BillShareTheme
    ) {
        val total = slices.sumOf { it.second }
        if (total <= 0.0) return
        val palette = listOf(
            theme.accent, theme.accentDark,
            AColor.parseColor("#FFA726"), AColor.parseColor("#26C6DA"),
            AColor.parseColor("#AB47BC"), AColor.parseColor("#66BB6A"),
            AColor.parseColor("#FFEB3B"), AColor.parseColor("#FF7043")
        )
        var start = -90f
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)
        slices.forEachIndexed { i, (_, v) ->
            val sweep = (v / total * 360.0).toFloat()
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette[i % palette.size] }
            c.drawArc(rect, start, sweep, true, p)
            start += sweep
        }
        // 中央留白
        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (theme.isDark) theme.bgTop else AColor.WHITE
        }
        c.drawCircle(cx, cy, r * 0.62f, holePaint)
        // 中央数字
        val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.titleColor; textSize = 40f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        c.drawText(formatMoney(total), cx, cy + 6f, totalPaint)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.subtitleColor; textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        c.drawText("总支出", cx, cy + 40f, labelPaint)
    }

    private fun drawLegend(
        c: Canvas, x: Float, y: Float,
        slices: List<Pair<String, Double>>, total: Double,
        theme: BillShareTheme
    ) {
        val palette = listOf(
            theme.accent, theme.accentDark,
            AColor.parseColor("#FFA726"), AColor.parseColor("#26C6DA"),
            AColor.parseColor("#AB47BC")
        )
        slices.forEachIndexed { i, (cat, v) ->
            val rowY = y + i * 56f
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette[i % palette.size] }
            c.drawCircle(x + 12f, rowY + 24f, 12f, dotPaint)
            val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = theme.titleColor; textSize = 28f
            }
            val pct = if (total > 0) (v / total * 100).toInt() else 0
            c.drawText("$cat  $pct%", x + 36f, rowY + 32f, txt)
        }
    }

    private fun formatMoney(v: Double): String {
        val abs = kotlin.math.abs(v)
        return when {
            abs >= 10000 -> String.format(Locale.CHINA, "%.2f万", abs / 10000)
            else -> String.format(Locale.CHINA, "¥%.2f", abs)
        }
    }

    private fun formatSignedMoney(v: Double): String {
        val sign = when {
            v > 0 -> "+"
            v < 0 -> "-"
            else -> ""
        }
        return "$sign${formatMoney(v)}"
    }
}
