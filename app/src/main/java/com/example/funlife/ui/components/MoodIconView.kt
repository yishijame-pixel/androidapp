// ═══════════════════════════════════════════════════════════════════════════
// MoodIconView.kt
// 心情图标渲染 + 文件存储工具（emoji 用 Text，自定义用 Coil AsyncImage）
// 文件命名：mood_icon_${userId}_${uuid}.png（严格遵循 HabitIcon 同款约束）
// ═══════════════════════════════════════════════════════════════════════════
package com.example.funlife.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.funlife.data.MoodIcon
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 渲染一个心情图标：emoji 走文字，自定义走图片。
 * 调用方通常包一层 Box 控制大小、背景、形状。
 */
@Composable
fun MoodIconView(
    icon: MoodIcon,
    iconSize: Dp,
    emojiFontSize: TextUnit = 22.sp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(iconSize), contentAlignment = Alignment.Center) {
        if (icon.isImage) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(icon.value.removePrefix("file://")))
                    .crossfade(false)
                    .build(),
                contentDescription = icon.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(
                icon.value,
                fontSize = emojiFontSize,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

/**
 * 把用户从相册选中的 Uri 复制到 App 私有目录。
 * @return 成功时返回文件绝对路径，失败返回 null
 */
fun saveCustomMoodIcon(context: Context, sourceUri: Uri, userId: Long): String? {
    if (userId <= 0) return null
    return try {
        val dir = File(context.filesDir, "mood_icons").apply { if (!exists()) mkdirs() }
        val uuid = UUID.randomUUID().toString().take(12)
        val file = File(dir, "mood_icon_${userId}_${uuid}.png")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        if (file.exists() && file.length() > 0) file.absolutePath else null
    } catch (e: Exception) {
        android.util.Log.e("MoodIconView", "保存心情图标失败", e)
        null
    }
}

/**
 * 删除心情图标文件（严格校验用户前缀，防止误删别人的）。
 */
fun deleteCustomMoodIconFile(context: Context, iconPath: String, userId: Long) {
    if (userId <= 0) return
    try {
        val file = File(iconPath.removePrefix("file://"))
        if (file.parentFile?.name == "mood_icons" &&
            file.name.startsWith("mood_icon_${userId}_")
        ) {
            file.delete()
        }
    } catch (_: Exception) {
    }
}
