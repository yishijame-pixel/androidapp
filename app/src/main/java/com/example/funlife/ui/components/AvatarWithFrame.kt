// AvatarWithFrame.kt - 带头像框的头像组件（优化版 - 简单可靠）
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
import coil.compose.AsyncImage

/**
 * 带头像框的头像组件（优化版 - 简单可靠）
 * 
 * 🎯 核心原理（简单层叠方案）：
 * 1. 底层：圆形头像（60%大小，居中显示）
 * 2. 顶层：PNG头像框（完整显示，装饰保留）
 * 3. 头像在框的透明区域内显示，不会超出
 * 
 * ✨ 优势：
 * ✅ 简单可靠，兼容性好
 * ✅ 头像圆形裁剪，美观大方
 * ✅ 框的装饰完整保留
 * ✅ 性能优秀，无复杂计算
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
    
    // 加载头像框图片
    val frameBitmap = remember(frameAssetPath) {
        if (frameAssetPath != null) {
            try {
                context.assets.open(frameAssetPath).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                android.util.Log.e("AvatarWithFrame", "Failed to load frame: ${e.message}")
                null
            }
        } else {
            null
        }
    }
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(frameSize)
    ) {
        if (frameBitmap != null) {
            // ═══════════════════════════════════════════════════════
            // 🎨 有头像框的情况：简单层叠方案（框在上，头像在下）
            // ═══════════════════════════════════════════════════════
            
            // 🔥 底层：圆形头像（63%大小，确保不超出框）
            if (avatarUri != null) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(avatarUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "用户头像",
                    modifier = Modifier
                        .size(frameSize * 0.63f)  // 🔥 63%大小，确保完全在框内
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 默认头像（圆形 + 渐变背景）
                Box(
                    modifier = Modifier
                        .size(frameSize * 0.63f)
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F),
                                    Color(0xFFFF9800)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = defaultText,
                        fontSize = (frameSize.value * 0.26f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
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
            if (avatarUri != null) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data(avatarUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "用户头像",
                    modifier = Modifier
                        .size(frameSize)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 默认头像
                Box(
                    modifier = Modifier
                        .size(frameSize)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = defaultText,
                        fontSize = (frameSize.value * 0.48f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9C27B0)
                    )
                }
            }
            
            // VIP徽章（在右下角）
            if (vipLevel != com.example.funlife.data.model.VipLevel.NORMAL) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (frameSize.value * 0.08f).dp, y = (frameSize.value * 0.08f).dp)
                ) {
                    VipBadgeIcon(vipLevel = vipLevel)
                }
            }
        }
    }
}
