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
import com.example.funlife.resource.ResourceStore

object PetImageLoader {
    
    // 获取宠物图片路径（静态图）
    fun getPetImagePath(
        type: PetType,
        stage: GrowthStage,
        animationState: AnimationState
    ): String {
        return when (type) {
            PetType.CAT -> getCatImagePath(stage, animationState)
            PetType.TIGER -> getTigerImagePath(stage, animationState)
            PetType.DOG -> "pet/pet_cat_baby_idle_01.png" // 暂时用猫代替
            PetType.RABBIT -> "pet/pet_cat_baby_idle_01.png"
            PetType.HAMSTER -> "pet/pet_cat_baby_idle_01.png"
            PetType.PANDA -> ""  // 🐼 由 Canvas 绘制，不需要图片资源
        }
    }
    
    private fun getCatImagePath(stage: GrowthStage, animationState: AnimationState): String {
        return when (stage) {
            GrowthStage.BABY -> when (animationState) {
                AnimationState.Idle -> "pet/cat/pet_cat_baby_idle_sit_01.png"  // 使用坐姿作为待机
                AnimationState.Feeding -> "pet/cat/pet_cat_baby_idle_eat_foot_01.png"
                AnimationState.Playing -> "pet/cat/pet_cat_baby_idle__happy_02.png"
                AnimationState.Petting -> "pet/cat/pet_cat_baby_idle_01.png"
                AnimationState.Cleaning -> "pet/cat/pet_cat_baby_idle_sit_01.png"
                AnimationState.LevelUp -> "pet/cat/pet_cat_baby_idle__happy_02.png"
            }
            GrowthStage.CHILD -> "pet/cat/pet_cat_baby_idle_01.png"
            GrowthStage.ADULT -> "pet/cat/pet_cat_baby_idle_01.png"
            GrowthStage.PERFECT -> "pet/cat/pet_cat_baby_idle_01.png"
        }
    }
    
    private fun getTigerImagePath(stage: GrowthStage, animationState: AnimationState): String {
        return when (stage) {
            GrowthStage.BABY -> when (animationState) {
                AnimationState.Idle -> "pet/tiger/pet_cat_baby_adult_01.png"  // 待机
                AnimationState.Feeding -> "pet/tiger/pet_cat_eat_foot_01.png"  // 吃东西
                AnimationState.Playing -> "pet/tiger/pet_cat_baby_idle__happy_01.png"  // 开心
                AnimationState.Petting -> "pet/tiger/pet_cat_baby_adult_01.png"
                AnimationState.Cleaning -> "pet/tiger/pet_cat_baby_adult_01.png"
                AnimationState.LevelUp -> "pet/tiger/pet_cat_baby_idle__happy_01.png"
            }
            GrowthStage.CHILD -> "pet/tiger/pet_cat_teenage__01.png"
            GrowthStage.ADULT -> "pet/tiger/pet_cat_baby_adult_01.png"
            GrowthStage.PERFECT -> "pet/tiger/pet_cat_majestic_01.png"
        }
    }
    
    // 获取帧动画序列
    fun getFrameAnimationPaths(
        type: PetType,
        stage: GrowthStage,
        animationState: AnimationState
    ): List<String> {
        return when (type) {
            PetType.CAT -> getCatFrameAnimationPaths(stage, animationState)
            PetType.TIGER -> getTigerFrameAnimationPaths(stage, animationState)
            else -> emptyList()
        }
    }
    
    private fun getCatFrameAnimationPaths(
        stage: GrowthStage,
        animationState: AnimationState
    ): List<String> {
        return when (stage) {
            GrowthStage.BABY -> when (animationState) {
                AnimationState.Feeding -> listOf(
                    "pet/cat/pet_cat_baby_idle_eat_foot_01.png",
                    "pet/cat/pet_cat_baby_idle_sit_01.png",
                    "pet/cat/pet_cat_baby_idle_eat_foot_01.png"
                )
                AnimationState.Playing -> listOf(
                    "pet/cat/pet_cat_baby_idle__happy_02.png",
                    "pet/cat/pet_cat_baby_idle_01.png",
                    "pet/cat/pet_cat_baby_idle__happy_02.png"
                )
                AnimationState.Petting -> listOf(
                    "pet/cat/pet_cat_baby_idle_01.png",
                    "pet/cat/pet_cat_baby_idle_sit_01.png",
                    "pet/cat/pet_cat_baby_idle_01.png"
                )
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
    
    private fun getTigerFrameAnimationPaths(
        stage: GrowthStage,
        animationState: AnimationState
    ): List<String> {
        return when (stage) {
            GrowthStage.BABY -> when (animationState) {
                AnimationState.Feeding -> listOf(
                    "pet/tiger/pet_cat_eat_foot_01.png",
                    "pet/tiger/pet_cat_eat_foot_02.png",
                    "pet/tiger/pet_cat_eat_foot_03.png",
                    "pet/tiger/pet_cat_eat_foot_04.png",
                    "pet/tiger/pet_cat_eat_foot_05.png",
                    "pet/tiger/pet_cat_eat_foot_06.png",
                    "pet/tiger/pet_cat_eat_foot_07.png",
                    "pet/tiger/pet_cat_eat_foot_08.png",
                    "pet/tiger/pet_cat_eat_foot_09.png",
                    "pet/tiger/pet_cat_eat_foot_10.png"
                )
                AnimationState.Playing -> listOf(
                    "pet/tiger/pet_cat_baby_idle__happy_01.png",
                    "pet/tiger/pet_cat_baby_adult_01.png",
                    "pet/tiger/pet_cat_baby_idle__happy_01.png"
                )
                AnimationState.Petting -> listOf(
                    "pet/tiger/pet_cat_baby_adult__tian_01.png",
                    "pet/tiger/pet_cat_baby_adult__tian_02.png",
                    "pet/tiger/pet_cat_baby_adult__tian_03.png",
                    "pet/tiger/pet_cat_baby_adult__tian_04.png",
                    "pet/tiger/pet_cat_baby_adult__tian_05.png"
                )
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
    
    // 获取睡眠动画序列
    fun getSleepAnimationPaths(type: PetType): List<String> {
        return when (type) {
            PetType.CAT -> listOf(
                "pet/cat/pet_sz_baby__sleep_01.png",
                "pet/cat/pet_sz_baby__sleep_02.png",
                "pet/cat/pet_sz_baby__sleep_03.png",
                "pet/cat/pet_sz_baby__sleep_4.png",
                "pet/cat/pet_sz_baby__sleep_05.png",
                "pet/cat/pet_sz_baby__sleep_07.png"
            )
            PetType.TIGER -> listOf(
                "pet/pet_sz_baby__sleep_01.png",
                "pet/pet_sz_baby__sleep_02.png",
                "pet/pet_sz_baby__sleep_03.png",
                "pet/pet_sz_baby__sleep_4.png",
                "pet/pet_sz_baby__sleep_05.png",
                "pet/pet_sz_baby__sleep_07.png"
            )
            else -> emptyList()
        }
    }
    
    // 获取工作动画序列
    fun getWorkAnimationPaths(type: PetType): List<String> {
        return when (type) {
            PetType.CAT -> listOf(
                "pet/cat/work1.png",
                "pet/cat/work2.png",
                "pet/cat/work3.png"
            )
            PetType.TIGER -> listOf(
                "pet/work1.png",
                "pet/work2.png",
                "pet/work3.png"
            )
            else -> emptyList()
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
            ResourceStore.openInputStream(path)?.use { inputStream ->
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
