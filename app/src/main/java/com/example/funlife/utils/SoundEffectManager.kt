// SoundEffectManager.kt - 音效管理器
package com.example.funlife.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

class SoundEffectManager private constructor(context: Context) {
    
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<SoundEffect, Int>()
    private val streamMap = mutableMapOf<SoundEffect, Int>()  // 保存正在播放的音效流ID
    private var isEnabled = true
    
    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
        
        // 加载音效文件
        loadSounds(context)
    }
    
    private fun loadSounds(context: Context) {
        try {
            // 转盘音效
            loadSound(context, SoundEffect.SPIN_ROTATING, "spin_rotating")
            loadSound(context, SoundEffect.RESULT_NORMAL, "result_normal")
            
            // 导航栏音效（如果文件存在）
            loadSound(context, SoundEffect.NAV_HOME, "nav_home")
            loadSound(context, SoundEffect.NAV_HABIT, "nav_habit")
            loadSound(context, SoundEffect.NAV_MOOD, "nav_mood")
            loadSound(context, SoundEffect.NAV_PROFILE, "nav_profile")
            
            // 背景音乐
            loadSound(context, SoundEffect.PET_BGM, "pet")
            
            Log.d(TAG, "Loaded ${soundMap.size} sound effects")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sounds", e)
        }
    }
    
    private fun loadSound(context: Context, effect: SoundEffect, fileName: String) {
        try {
            val resId = context.resources.getIdentifier(
                fileName,
                "raw",
                context.packageName
            )
            
            if (resId != 0) {
                val soundId = soundPool.load(context, resId, 1)
                soundMap[effect] = soundId
                Log.d(TAG, "Loaded sound: $fileName (ID: $soundId)")
            } else {
                Log.w(TAG, "Sound file not found: $fileName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sound: $fileName", e)
        }
    }
    
    fun play(effect: SoundEffect, volume: Float = 1.0f, loop: Boolean = false) {
        if (!isEnabled) return
        
        soundMap[effect]?.let { soundId ->
            try {
                val streamId = soundPool.play(
                    soundId,
                    volume,
                    volume,
                    1,
                    if (loop) -1 else 0,  // -1 表示无限循环
                    1.0f
                )
                
                if (loop) {
                    streamMap[effect] = streamId  // 保存流ID以便后续停止
                }
                
                Log.d(TAG, "Playing sound: $effect (loop: $loop, streamId: $streamId)")
            } catch (e: Exception) {
                Log.e(TAG, "Error playing sound: $effect", e)
            }
        } ?: run {
            Log.w(TAG, "Sound not loaded: $effect")
        }
    }
    
    fun stop(effect: SoundEffect) {
        streamMap[effect]?.let { streamId ->
            try {
                soundPool.stop(streamId)
                streamMap.remove(effect)
                Log.d(TAG, "Stopped sound: $effect")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping sound: $effect", e)
            }
        }
    }
    
    fun stopAll() {
        try {
            streamMap.keys.toList().forEach { effect ->
                stop(effect)
            }
            Log.d(TAG, "Stopped all sounds")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping all sounds", e)
        }
    }
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        Log.d(TAG, "Sound effects ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun release() {
        try {
            stopAll()  // 停止所有正在播放的音效
            soundPool.release()
            soundMap.clear()
            streamMap.clear()
            Log.d(TAG, "SoundPool released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing SoundPool", e)
        }
    }
    
    companion object {
        private const val TAG = "SoundEffectManager"
        
        @Volatile
        private var instance: SoundEffectManager? = null
        
        fun getInstance(context: Context): SoundEffectManager {
            return instance ?: synchronized(this) {
                instance ?: SoundEffectManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

// 音效类型枚举
enum class SoundEffect {
    // 转盘音效
    SPIN_ROTATING,      // 转盘旋转
    SPIN_STOP,          // 转盘停止
    RESULT_NORMAL,      // 普通模式结果
    RESULT_ADVANCED,    // 进阶模式结果
    RESULT_LUCKY,       // 幸运模式结果
    LUCKY_INCREASE,     // 幸运值增加
    
    // 导航栏音效
    NAV_HOME,           // 首页
    NAV_HABIT,          // 习惯
    NAV_MOOD,           // 心情
    NAV_PROFILE,        // 我的
    
    // 背景音乐
    PET_BGM,            // 宠物页面背景音乐
    
    // 通用按钮音效
    BUTTON_CLICK        // 按钮点击
}
