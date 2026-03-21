// AnniversaryShareHelper.kt - 纪念日分享助手
package com.example.funlife.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.funlife.data.model.Anniversary
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AnniversaryShareHelper(private val context: Context) {
    
    // 生成分享卡片图片
    fun generateShareCard(anniversary: Anniversary): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 背景
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // 如果有背景图片，尝试加载
        if (!anniversary.imageUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(anniversary.imageUri)
                val inputStream = context.contentResolver.openInputStream(uri)
                val bgBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                if (bgBitmap != null) {
                    // 绘制背景图片
                    val srcRect = android.graphics.Rect(0, 0, bgBitmap.width, bgBitmap.height)
                    val dstRect = android.graphics.Rect(0, 0, width, height)
                    canvas.drawBitmap(bgBitmap, srcRect, dstRect, paint)
                    
                    // 添加半透明遮罩
                    val gradient = android.graphics.LinearGradient(
                        0f, 0f, 0f, height.toFloat(),
                        intArrayOf(
                            android.graphics.Color.argb(100, 0, 0, 0),
                            android.graphics.Color.argb(150, 0, 0, 0),
                            android.graphics.Color.argb(200, 0, 0, 0)
                        ),
                        null,
                        android.graphics.Shader.TileMode.CLAMP
                    )
                    paint.shader = gradient
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                    paint.shader = null
                    
                    bgBitmap.recycle()
                }
            } catch (e: Exception) {
                android.util.Log.e("AnniversaryShare", "Error loading background image", e)
                // 如果加载失败，使用渐变背景
                drawGradientBackground(canvas, paint, width, height, anniversary)
            }
        } else {
            // 没有背景图片，使用渐变
            drawGradientBackground(canvas, paint, width, height, anniversary)
        }
        
        // 绘制卡片内容
        paint.color = android.graphics.Color.WHITE
        
        // 绘制emoji
        paint.textSize = 200f
        paint.textAlign = Paint.Align.CENTER
        val emoji = anniversary.getTypeEnum().emoji
        canvas.drawText(emoji, width / 2f, 500f, paint)
        
        // 绘制纪念日名称
        paint.textSize = 100f
        paint.color = android.graphics.Color.WHITE
        paint.setShadowLayer(10f, 0f, 0f, android.graphics.Color.BLACK)
        canvas.drawText(anniversary.name, width / 2f, 700f, paint)
        paint.clearShadowLayer()
        
        // 绘制日期
        paint.textSize = 60f
        paint.color = android.graphics.Color.WHITE
        paint.alpha = 240
        canvas.drawText(anniversary.getFormattedDate(), width / 2f, 820f, paint)
        paint.alpha = 255
        
        // 绘制剩余天数 - 半透明白色背景
        val daysRemaining = anniversary.getDaysRemaining()
        val daysText = when {
            daysRemaining > 0 -> "还有 $daysRemaining 天"
            daysRemaining == 0L -> "🎉 就是今天！"
            else -> "已过去 ${-daysRemaining} 天"
        }
        
        // 绘制半透明背景
        val daysRect = RectF(140f, 920f, width - 140f, 1120f)
        paint.color = android.graphics.Color.WHITE
        paint.alpha = 128 // 50% 透明度
        canvas.drawRoundRect(daysRect, 40f, 40f, paint)
        paint.alpha = 255
        
        // 绘制剩余天数文字
        paint.textSize = 80f
        paint.color = android.graphics.Color.WHITE
        canvas.drawText(daysText, width / 2f, 1040f, paint)
        
        // 绘制星星（重要程度）
        val starSize = 50f
        val starSpacing = 70f
        val totalStarWidth = anniversary.importance * starSpacing
        var starX = (width - totalStarWidth) / 2f + starSpacing / 2f
        
        paint.textSize = starSize
        paint.color = android.graphics.Color.parseColor("#FFD700")
        repeat(anniversary.importance) {
            canvas.drawText("⭐", starX, 1220f, paint)
            starX += starSpacing
        }
        
        // 绘制备注（如果有）
        if (!anniversary.note.isNullOrEmpty()) {
            paint.textSize = 50f
            paint.color = android.graphics.Color.WHITE
            paint.alpha = 220
            
            // 简单的文本换行
            val maxWidth = width - 200f
            val words = anniversary.note.split(" ")
            var line = ""
            var y = 1360f
            
            words.forEach { word ->
                val testLine = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(testLine) > maxWidth) {
                    canvas.drawText(line, width / 2f, y, paint)
                    line = word
                    y += 70f
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, width / 2f, y, paint)
            }
            paint.alpha = 255
        }
        
        // 绘制底部水印
        paint.textSize = 40f
        paint.color = android.graphics.Color.WHITE
        paint.alpha = 180
        canvas.drawText("FunLife - 趣味生活", width / 2f, height - 100f, paint)
        
        return bitmap
    }
    
    // 绘制渐变背景
    private fun drawGradientBackground(
        canvas: Canvas,
        paint: Paint,
        width: Int,
        height: Int,
        anniversary: Anniversary
    ) {
        val daysRemaining = anniversary.getDaysRemaining()
        val colors = when {
            daysRemaining > 30 -> intArrayOf(
                android.graphics.Color.parseColor("#4ECDC4"),
                android.graphics.Color.parseColor("#44A5A0"),
                android.graphics.Color.parseColor("#2C7A7B")
            )
            daysRemaining in 8..30 -> intArrayOf(
                android.graphics.Color.parseColor("#FF8C42"),
                android.graphics.Color.parseColor("#FF6B35"),
                android.graphics.Color.parseColor("#E85D04")
            )
            daysRemaining >= 0 -> intArrayOf(
                android.graphics.Color.parseColor("#FF6B9D"),
                android.graphics.Color.parseColor("#E74C3C"),
                android.graphics.Color.parseColor("#C0392B")
            )
            else -> intArrayOf(
                android.graphics.Color.parseColor("#95A5A6"),
                android.graphics.Color.parseColor("#7F8C8D"),
                android.graphics.Color.parseColor("#5D6D7E")
            )
        }
        
        val gradient = android.graphics.LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            colors,
            null,
            android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null
    }
    
    // 保存图片到缓存
    private fun saveBitmapToCache(bitmap: Bitmap, fileName: String): File {
        val cacheDir = File(context.cacheDir, "shared_images")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        val file = File(cacheDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        return file
    }
    
    // 分享图片
    fun shareImage(anniversary: Anniversary) {
        try {
            val bitmap = generateShareCard(anniversary)
            val fileName = "anniversary_${anniversary.id}_${System.currentTimeMillis()}.png"
            val file = saveBitmapToCache(bitmap, fileName)
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "分享我的纪念日：${anniversary.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(intent, "分享纪念日"))
        } catch (e: Exception) {
            android.util.Log.e("AnniversaryShare", "Error sharing image", e)
        }
    }
    
    // 分享文本
    fun shareText(anniversary: Anniversary) {
        val text = buildString {
            append("📅 ${anniversary.name}\n")
            append("${anniversary.getFormattedDate()}\n")
            
            val daysRemaining = anniversary.getDaysRemaining()
            when {
                daysRemaining > 0 -> append("还有 $daysRemaining 天\n")
                daysRemaining == 0L -> append("就是今天！🎉\n")
                else -> append("已过去 ${-daysRemaining} 天\n")
            }
            
            if (anniversary.isYearly) {
                val years = anniversary.getYearsPassed()
                if (years > 0) {
                    append("已经 $years 年了 💝\n")
                }
            }
            
            if (!anniversary.note.isNullOrEmpty()) {
                append("\n${anniversary.note}\n")
            }
            
            append("\n来自 FunLife - 趣味生活")
        }
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        
        context.startActivity(Intent.createChooser(intent, "分享纪念日"))
    }
}
