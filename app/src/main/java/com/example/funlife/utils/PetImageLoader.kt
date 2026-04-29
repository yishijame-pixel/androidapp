// PetImageLoader.kt - 宠物图片加载工具
package com.example.funlife.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.funlife.data.model.GrowthStage
import com.example.funlife.data.model.PetType
import com.example.funlife.viewmodel.AnimationState

object PetImageLoader {
    
    // 获取宠物图片路径（静态图）
    fun getPetImagePath(
        type: PetType,
        stage: GrowthStage,
        animationState: AnimationState
    ): String {
        return when (type) {
            PetType.CAT -> getCatImagePath(stage, animationState)
            PetType.DOG -> "pet/pet_cat_baby_idle_01.png" // 暂时用猫代替
            PetType.RABBIT -> "pet/pet_cat_baby_idle_01.png"
            PetType.HAMSTER -> "pet/pet_cat_baby_idle_01.png"
        }
    }
    
    private fun getCatImagePath(stage: GrowthStage, animationState: AnimationState): String {
        return when (stage) {
            GrowthStage.BABY -> when (animationState) {
                AnimationState.Idle -> "pet/pet_cat_baby_idle_sit_01.png"  // 使用坐姿作为待机
                AnimationState.Feeding -> "pet/pet_cat_baby_idle_eat_foot_01.png"
                AnimationState.Playing -> "pet/pet_cat_baby_idle__happy_01.png"
                AnimationState.Petting -> "pet/pet_cat_baby_adult_01.png"
                AnimationState.Cleaning -> "pet/pet_cat_baby_idle_sit_01.png"
                AnimationState.LevelUp -> "pet/pet_cat_baby_idle__happy_01.png"
            }
            GrowthStage.CHILD -> "pet/pet_cat_teenage__01.png"
            GrowthStage.ADULT -> "pet/pet_cat_baby_adult_01.png"
            GrowthStage.PERFECT -> "pet/pet_cat_majestic_01.png"
        }
    }
    
    // 判断是否需要播放帧动画
    fun shouldPlayFrameAnimation(animationState: AnimationState): Boolean {
        return when (animationState) {
            AnimationState.Feeding -> true
            AnimationState.Petting -> true
            AnimationState.Playing -> true
            else -> false  // 待机不播放帧动画
        }
    }
    
    // 获取背景图片路径
    fun getBackgroundPath(backgroundType: String = "home"): String {
        return when (backgroundType) {
            "garden" -> "pet/garden_background_01.png"
            "home" -> "pet/home_baclground.png"
            else -> "pet/home_baclground.png"
        }
    }
    
    // 获取食物图片路径
    fun getFoodImagePath(foodType: String = "basic"): String {
        return when (foodType) {
            "high" -> "pet/high_foot_01.png"
            "premium" -> "pet/pet_foot.png"
            else -> "pet/high_foot.png"
        }
    }
    
    // 从 assets 加载图片
    fun loadImageFromAssets(context: Context, path: String): Bitmap? {
        return try {
            context.assets.open(path).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// Composable 函数：加载图片
@Composable
fun rememberAssetImage(path: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(path) {
        PetImageLoader.loadImageFromAssets(context, path)?.asImageBitmap()
    }
}
