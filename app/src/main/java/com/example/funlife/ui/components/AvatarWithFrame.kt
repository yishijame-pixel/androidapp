// AvatarWithFrame.kt - 带头像框的头像组件（优化版 - 自动适配）
package com.example.funlife.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.funlife.utils.AvatarStorageHelper
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.cos
import kotlin.math.sin

/**
 * 运行时分析头像框PNG透明区域，自动计算头像应占比例
 *
 * 原理：从图片中心向外发射36条射线（每10°一条），
 * 检测碰到不透明像素的距离，取保守值作为内切圆半径。
 * 不同尺寸/设计的头像框都能自动适配，无需手动配置。
 *
 * @return Pair(ImageBitmap用于渲染, 头像占比0.0~1.0)
 */
// 🚀 性能优化：跨 Composition / 跨 Screen 缓存头像框解析结果（PNG 像素扫描很贵）
// 用 LruCache 限制条目数，避免缓存膨胀导致 OOM
private val frameAnalysisCache = object : android.util.LruCache<String, Pair<ImageBitmap, Float>>(12) {
    override fun sizeOf(key: String, value: Pair<ImageBitmap, Float>) = 1
}

private fun loadAndAnalyzeFrame(
    bitmap: android.graphics.Bitmap
): Pair<ImageBitmap, Float> {
    val w = bitmap.width
    val h = bitmap.height
    if (w == 0 || h == 0) return Pair(bitmap.asImageBitmap(), 0.63f)

    val cx = w / 2
    val cy = h / 2

    // 中心点不透明则无法分析，返回默认值
    if (((bitmap.getPixel(cx, cy) ushr 24) and 0xFF) > 60) {
        return Pair(bitmap.asImageBitmap(), 0.63f)
    }

    val threshold = 128  // 高阈值：跳过半透明装饰/发光效果，只检测实际不透明边框
    val minConsecutive = 4  // 连续4个不透明像素才算边框，跳过零散粒子
    val numDirs = 36
    val maxR = minOf(cx, cy, w - cx - 1, h - cy - 1)
    val radii = IntArray(numDirs)

    for (i in 0 until numDirs) {
        val angle = 2.0 * Math.PI * i / numDirs
        val dx = cos(angle)
        val dy = sin(angle)
        var r = maxR
        var consecutive = 0
        for (step in 1..maxR) {
            val px = (cx + dx * step).toInt().coerceIn(0, w - 1)
            val py = (cy + dy * step).toInt().coerceIn(0, h - 1)
            if (((bitmap.getPixel(px, py) ushr 24) and 0xFF) > threshold) {
                consecutive++
                if (consecutive >= minConsecutive) {
                    r = step - minConsecutive + 1
                    break
                }
            } else {
                consecutive = 0
            }
        }
        radii[i] = r
    }

    // 排序取75百分位（排除装饰物干扰，取较大的实际内圈半径）
    val sorted = radii.toList().sorted()
    val safeRadius = sorted[numDirs * 3 / 4]

    // 比例 = 内切圆直径 / max(宽,高)
    // ×1.02 轻微超出：头像稍微延伸到框边缘下方，框PNG在上层覆盖交接处，视觉更贴合
    val ratio = (safeRadius * 2f / maxOf(w, h).toFloat()) * 1.02f
    return Pair(bitmap.asImageBitmap(), ratio.coerceIn(0.40f, 0.95f))
}

@Composable
private fun DefaultCircleAvatar(
    size: Dp,
    defaultText: String,
    useWarmGradient: Boolean,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (useWarmGradient) {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFD54F),
                            Color(0xFFFF9800),
                        ),
                    )
                } else {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color.White, Color.White),
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = defaultText,
            fontSize = (size.value * if (useWarmGradient) 0.26f else 0.48f).sp,
            fontWeight = FontWeight.Bold,
            color = if (useWarmGradient) Color.White else Color(0xFF9C27B0),
        )
    }
}

@Composable
private fun UserAvatarImage(
    avatarUri: String?,
    size: Dp,
    defaultText: String,
    useWarmGradient: Boolean,
) {
    val context = LocalContext.current
    val loadableUri = remember(avatarUri) {
        AvatarStorageHelper.resolveLoadableAvatarUri(context, avatarUri)
    }

    if (loadableUri == null) {
        DefaultCircleAvatar(size = size, defaultText = defaultText, useWarmGradient = useWarmGradient)
        return
    }

    val model = remember(loadableUri) {
        ImageRequest.Builder(context)
            .data(loadableUri)
            .crossfade(true)
            .build()
    }

    SubcomposeAsyncImage(
        model = model,
        contentDescription = "用户头像",
        modifier = Modifier
            .size(size)
            .clip(CircleShape),
        contentScale = ContentScale.Crop,
        loading = {
            DefaultCircleAvatar(size = size, defaultText = defaultText, useWarmGradient = useWarmGradient)
        },
        error = {
            DefaultCircleAvatar(size = size, defaultText = defaultText, useWarmGradient = useWarmGradient)
        },
    )
}

/**
 * 带头像框的头像组件（优化版 - 自动适配）
 * 
 * 🎯 核心原理（自动适配方案）：
 * 1. 加载头像框PNG时，自动分析透明区域大小
 * 2. 底层：圆形头像（按检测结果自适应大小，居中显示）
 * 3. 顶层：PNG头像框（完整显示，装饰保留）
 * 
 * ✨ 优势：
 * ✅ 自动适配不同尺寸的头像框素材
 * ✅ 头像圆形裁剪，紧贴框内边缘
 * ✅ 框的装饰完整保留
 * ✅ 无需手动配置每个头像框的比例
 * 
 * @param avatarUri 用户头像URI
 * @param frameAssetPath 头像框资源路径（如 "xiangkuang/头像框1/xxx.png"）
 * @param frameSize 显示尺寸（头像框的显示大小）
 * @param defaultText 默认显示的文字（当没有头像时）
 * @param vipLevel VIP等级（用于显示VIP光环）
 */
@Composable
fun AvatarWithFrame(
    avatarUri: String?,
    frameAssetPath: String?,
    frameSize: Dp = 120.dp,
    defaultText: String = "U",
    vipLevel: com.example.funlife.data.model.VipLevel = com.example.funlife.data.model.VipLevel.NORMAL
) {
    val context = LocalContext.current
    
    // 加载头像框图片并自动分析透明区域（🚀 LruCache 跨页面缓存，最多保留 12 个）
    val frameData = remember(frameAssetPath) {
        if (frameAssetPath == null) return@remember null
        frameAnalysisCache.get(frameAssetPath)?.let { return@remember it }
        try {
            context.assets.open(frameAssetPath).use { inputStream ->
                val bmp = BitmapFactory.decodeStream(inputStream)
                if (bmp != null) loadAndAnalyzeFrame(bmp).also {
                    frameAnalysisCache.put(frameAssetPath, it)
                } else null
            }
        } catch (e: Exception) {
            android.util.Log.e("AvatarWithFrame", "Failed to load frame: ${e.message}")
            null
        }
    }
    val frameBitmap = frameData?.first
    val avatarRatio = frameData?.second ?: 0.63f
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(frameSize)
    ) {
        if (frameBitmap != null) {
            // ═══════════════════════════════════════════════════════
            // 🎨 有头像框的情况：简单层叠方案（框在上，头像在下）
            // ═══════════════════════════════════════════════════════
            
            // 🔥 底层：圆形头像（自动适配大小，紧贴框内边缘）
            UserAvatarImage(
                avatarUri = avatarUri,
                size = frameSize * avatarRatio,
                defaultText = defaultText,
                useWarmGradient = true,
            )
            
            // 🔥 顶层：PNG头像框（完整显示，装饰保留）
            Image(
                bitmap = frameBitmap,
                contentDescription = "头像框",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
        } else {
            // ═══════════════════════════════════════════════════════
            // 🎨 没有头像框的情况：显示VIP光环和徽章
            // ═══════════════════════════════════════════════════════
            
            // VIP光环（在最底层）
            if (vipLevel != com.example.funlife.data.model.VipLevel.NORMAL) {
                VipAvatarHalo(
                    vipLevel = vipLevel,
                    modifier = Modifier.size(frameSize * 1.2f)
                )
            }
            
            // 用户头像（居中）
            UserAvatarImage(
                avatarUri = avatarUri,
                size = frameSize,
                defaultText = defaultText,
                useWarmGradient = false,
            )
            
        }
        
        // 🔥 VIP徽章（在右下角）—— 无论有没有头像框都显示
        if (vipLevel != com.example.funlife.data.model.VipLevel.NORMAL) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (frameSize.value * 0.06f).dp, y = (frameSize.value * 0.06f).dp)
            ) {
                VipBadgeIcon(vipLevel = vipLevel)
            }
        }
    }
}
