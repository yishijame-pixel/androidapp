// PetFrameAnimation.kt - 宠物帧动画组件
package com.example.funlife.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.funlife.utils.PetImageLoader
import kotlinx.coroutines.delay

/**
 * 帧动画组件
 * @param frames 帧图片路径列表
 * @param frameDuration 每帧持续时间（毫秒）
 * @param loop 是否循环播放
 * @param onAnimationEnd 动画结束回调
 */
@Composable
fun FrameAnimation(
    frames: List<String>,
    frameDuration: Long = 100,
    loop: Boolean = true,
    onAnimationEnd: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    var currentFrame by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }
    
    // 预加载所有帧
    val loadedFrames = remember(frames) {
        frames.mapNotNull { path ->
            PetImageLoader.loadImageFromAssets(context, path)?.asImageBitmap()
        }
    }
    
    // 帧动画循环
    LaunchedEffect(isPlaying, currentFrame) {
        if (isPlaying && loadedFrames.isNotEmpty()) {
            delay(frameDuration)
            val nextFrame = currentFrame + 1
            
            if (nextFrame >= loadedFrames.size) {
                if (loop) {
                    currentFrame = 0
                } else {
                    isPlaying = false
                    onAnimationEnd?.invoke()
                }
            } else {
                currentFrame = nextFrame
            }
        }
    }
    
    Box(modifier = modifier) {
        if (loadedFrames.isNotEmpty() && currentFrame < loadedFrames.size) {
            Image(
                bitmap = loadedFrames[currentFrame],
                contentDescription = "动画帧",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
    }
}

/**
 * 宠物动画帧序列定义
 */
object PetAnimationFrames {
    
    // 吃东西动画（10帧）
    fun getEatingFrames(): List<String> = List(10) { index ->
        "pet/pet_cat_eat_foot_${String.format("%02d", index + 1)}.png"
    }
    
    // 舔爪子动画（5帧）
    fun getLickingFrames(): List<String> = List(5) { index ->
        "pet/pet_cat_baby_adult__tian_${String.format("%02d", index + 1)}.png"
    }
    
    // 睡觉动画（7帧）
    fun getSleepingFrames(): List<String> = listOf(
        "pet/pet_sz_baby__sleep_01.png",
        "pet/pet_sz_baby__sleep_02.png",
        "pet/pet_sz_baby__sleep_03.png",
        "pet/pet_sz_baby__sleep_4.png",
        "pet/pet_sz_baby__sleep_05.png",
        "pet/pet_sz_baby__sleep_06.png",
        "pet/pet_sz_baby__sleep_07.png"
    )
    
    // 开心动画（2帧）
    fun getHappyFrames(): List<String> = listOf(
        "pet/pet_cat_baby_idle__happy_01.png",
        "pet/pet_cat_baby_idle__happy_02.png"
    )
    
    // 球动画（4帧）
    fun getBallFrames(): List<String> = List(4) { index ->
        "pet/qiu_${String.format("%02d", index + 1)}.png"
    }
    
    // 星星动画（4帧）
    fun getStarFrames(): List<String> = List(4) { index ->
        "pet/star_${String.format("%02d", index + 1)}.png"
    }
    
    // 食物图片列表
    fun getFoodImages(): List<String> = listOf(
        "pet/dog_foot.png",
        "pet/high_foot.png",
        "pet/pet_foot.png",
        "pet/yu_foot.png"
    )
    
    // 行走动画（3帧）
    fun getWalkingFrames(): List<String> = listOf(
        "pet/work1.png",
        "pet/work2.png",
        "pet/work3.png"
    )
}
