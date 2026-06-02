// HabitIcon.kt - 习惯图标渲染（自动识别 Material 矢量图标 / emoji / 自定义图片路径）
package com.example.funlife.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// ═══════════════════════════════════════════════════════════
// 习惯图标体系：三种格式
//   1. Material 矢量图标：以 "mi:" 前缀，保证全机型完美显示，可应用 tint
//   2. 自定义上传图片：绝对文件路径，含 "habit_icon_${userId}_" 前缀
//   3. emoji 字符：兼容历史数据（旧版本创建的习惯）
// ═══════════════════════════════════════════════════════════

const val MATERIAL_ICON_PREFIX = "mi:"

/**
 * 习惯图标库（32 个 Material 矢量图标，6 类）
 * key = 稳定 ID（存库），value = ImageVector
 */
val MATERIAL_HABIT_ICONS: Map<String, ImageVector> = linkedMapOf(
    // 运动健身
    "fitness" to Icons.Default.FitnessCenter,
    "run" to Icons.Default.DirectionsRun,
    "walk" to Icons.Default.DirectionsWalk,
    "yoga" to Icons.Default.SelfImprovement,
    "bike" to Icons.Default.DirectionsBike,
    "swim" to Icons.Default.Pool,
    "soccer" to Icons.Default.SportsSoccer,
    "basketball" to Icons.Default.SportsBasketball,
    // 学习成长
    "book" to Icons.Default.MenuBook,
    "edit" to Icons.Default.Edit,
    "note" to Icons.Default.Description,
    "school" to Icons.Default.School,
    "idea" to Icons.Default.EmojiObjects,
    "science" to Icons.Default.Science,
    "language" to Icons.Default.Language,
    "speak" to Icons.Default.RecordVoiceOver,
    // 健康饮食
    "water" to Icons.Default.WaterDrop,
    "food" to Icons.Default.Restaurant,
    "fruit" to Icons.Default.LocalDining,
    "sleep" to Icons.Default.Bedtime,
    "med" to Icons.Default.Medication,
    "health" to Icons.Default.HealthAndSafety,
    "bath" to Icons.Default.Bathtub,
    "sun" to Icons.Default.WbSunny,
    // 艺术 / 兴趣 / 心情
    "art" to Icons.Default.Palette,
    "music" to Icons.Default.MusicNote,
    "movie" to Icons.Default.Movie,
    "camera" to Icons.Default.PhotoCamera,
    "game" to Icons.Default.SportsEsports,
    "target" to Icons.Default.GpsFixed,
    "favorite" to Icons.Default.Favorite,
    "flower" to Icons.Default.LocalFlorist
)

/** 判断是否是自定义图片路径 */
fun isCustomHabitIcon(icon: String): Boolean {
    if (icon.isBlank()) return false
    val trimmed = icon.removePrefix("file://")
    return trimmed.startsWith("/") && trimmed.contains("habit_icon_")
}

/** 判断是否是 Material 矢量图标 */
fun isMaterialHabitIcon(icon: String): Boolean {
    if (!icon.startsWith(MATERIAL_ICON_PREFIX)) return false
    return MATERIAL_HABIT_ICONS.containsKey(icon.removePrefix(MATERIAL_ICON_PREFIX))
}

/**
 * 统一渲染习惯图标。
 * @param iconSize 矢量图标 / 自定义图片的方形尺寸
 * @param emojiFontSize emoji 时的字号
 * @param tint Material 图标的着色（emoji / 图片不受影响）
 */
@Composable
fun HabitIcon(
    icon: String,
    iconSize: Dp,
    emojiFontSize: TextUnit = 24.sp,
    tint: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    when {
        // 1. Material 矢量图标
        isMaterialHabitIcon(icon) -> {
            val key = icon.removePrefix(MATERIAL_ICON_PREFIX)
            val vector = MATERIAL_HABIT_ICONS[key] ?: Icons.Default.Star
            Icon(
                imageVector = vector,
                contentDescription = key,
                tint = tint,
                modifier = modifier.size(iconSize)
            )
        }
        // 2. 自定义上传图片
        isCustomHabitIcon(icon) -> {
            val path = icon.removePrefix("file://")
            val file = remember(path) { File(path) }
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = "自定义习惯图标",
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(iconSize)
                    .clip(CircleShape)
            )
        }
        // 3. emoji（兼容历史）— 注意部分 emoji 在低版本/ROM 上可能不显示
        else -> {
            Text(text = icon, fontSize = emojiFontSize, modifier = modifier)
        }
    }
}

/** 兼容旧调用签名 */
@Composable
fun HabitIcon(
    icon: String,
    sizeForImage: Dp,
    emojiFontSize: TextUnit = 24.sp,
    modifier: Modifier = Modifier
) = HabitIcon(icon = icon, iconSize = sizeForImage, emojiFontSize = emojiFontSize, modifier = modifier)

/**
 * 复制用户从相册/文件选择器选择的图片到 app 私有目录。
 * 文件命名严格遵守 DEVELOPMENT_PRINCIPLES §1.6：habit_icon_${userId}_${uuid}.png
 * @return 成功时返回文件绝对路径，失败返回 null
 */
fun saveCustomHabitIcon(context: Context, sourceUri: Uri, userId: Long): String? {
    if (userId <= 0) return null
    return try {
        val dir = File(context.filesDir, "habit_icons").apply { if (!exists()) mkdirs() }
        val uuid = UUID.randomUUID().toString().take(12)
        val file = File(dir, "habit_icon_${userId}_${uuid}.png")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        if (file.exists() && file.length() > 0) file.absolutePath else null
    } catch (e: Exception) {
        android.util.Log.e("HabitIcon", "保存自定义图标失败", e)
        null
    }
}

/**
 * 删除单个习惯自定义图标文件（严格按 userId 前缀校验，避免误删其他用户文件）。
 */
fun deleteCustomHabitIcon(context: Context, iconPath: String, userId: Long) {
    if (userId <= 0 || !isCustomHabitIcon(iconPath)) return
    try {
        val file = File(iconPath.removePrefix("file://"))
        // 严格校验：必须在 habit_icons 目录下，且文件名以 habit_icon_${userId}_ 开头
        if (file.parentFile?.name == "habit_icons" &&
            file.name.startsWith("habit_icon_${userId}_")
        ) {
            file.delete()
        }
    } catch (_: Exception) {
    }
}
