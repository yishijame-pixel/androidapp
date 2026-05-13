// PetWalkingAnimation.kt - 宠物行走动画组件
package com.example.funlife.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.funlife.data.model.PetType
import com.example.funlife.utils.rememberAssetImage
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun PetWalkingAnimation(
    modifier: Modifier = Modifier,
    petType: PetType = PetType.CAT,
    size: Dp = 200.dp,
    autoWalk: Boolean = true,
    walkSpeed: Float = 0.3f,
    idleTime: Long = 3000L
) {
    var offsetX by remember { mutableStateOf(0.dp) }  // 使用 Dp 单位的绝对位置
    var direction by remember { mutableStateOf(1f) }  // 1 = right, -1 = left
    var currentFrame by remember { mutableStateOf(0) }
    
    // 根据宠物类型获取行走动画路径
    val walkFramePaths = remember(petType) {
        when (petType) {
            PetType.CAT -> listOf(
                "pet/cat/work1.png",
                "pet/cat/work2.png",
                "pet/cat/work3.png"
            )
            PetType.TIGER -> listOf(
                "pet/tiger/work1.png",
                "pet/tiger/work2.png",
                "pet/tiger/work3.png"
            )
            else -> listOf(
                "pet/work1.png",
                "pet/work2.png",
                "pet/work3.png"
            )
        }
    }
    
    // 帧动画 - 持续循环播放3帧
    LaunchedEffect(Unit) {
        while (true) {
            currentFrame = (currentFrame + 1) % 3  // 0, 1, 2 循环
            delay(150)  // 每帧150ms
        }
    }
    
    // 自动行走（位置移动）
    LaunchedEffect(autoWalk) {
        if (!autoWalk) return@LaunchedEffect
        
        while (true) {
            // 随机选择目标位置（0dp 到屏幕宽度-宠物大小）
            // 使用 0-300dp 的范围来覆盖大部分手机屏幕宽度
            val targetOffsetX = Random.nextInt(0, 250).dp
            
            // 根据目标位置决定方向
            direction = if (targetOffsetX > offsetX) 1f else -1f  // 往右走不翻转(1)，往左走翻转(-1)
            
            // 计算移动距离和步数
            val startOffsetX = offsetX
            val distance = (targetOffsetX - startOffsetX).value
            val steps = (kotlin.math.abs(distance) / 3).toInt().coerceAtLeast(10)  // 根据距离计算步数
            
            // 平滑移动
            for (i in 1..steps) {
                delay(100)
                offsetX = startOffsetX + (distance * (i.toFloat() / steps)).dp
            }
            
            // 短暂停顿后继续下一次行走
            delay(idleTime + Random.nextLong(0, 2000))
        }
    }
    
    // 加载图片 - 3帧
    val frame1 = rememberAssetImage(walkFramePaths[0])
    val frame2 = rememberAssetImage(walkFramePaths[1])
    val frame3 = rememberAssetImage(walkFramePaths[2])
    
    // 根据当前帧选择图片
    val currentImage = when (currentFrame) {
        0 -> frame1
        1 -> frame2
        2 -> frame3
        else -> frame1
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(size)
    ) {
        if (currentImage != null) {
            Image(
                bitmap = currentImage,
                contentDescription = "宠物行走",
                modifier = Modifier
                    .size(size)
                    .offset(x = offsetX)  // 使用绝对偏移
                    .graphicsLayer {
                        scaleX = direction  // 控制朝向
                    },
                contentScale = ContentScale.Fit
            )
        }
    }
}
