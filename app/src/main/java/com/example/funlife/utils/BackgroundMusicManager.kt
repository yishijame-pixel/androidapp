// BackgroundMusicManager.kt - 背景音乐管理器
package com.example.funlife.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log

class BackgroundMusicManager private constructor(context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private val appContext = context.applicationContext
    private var isEnabled = true
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    fun playPetMusic() {
        try {
            // 停止当前播放
            stop()
            
            if (!isEnabled) return
            
            // 创建新的MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                // 设置音频属性为音乐类型
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                // 加载音频文件
                setDataSource(appContext, android.net.Uri.parse("android.resource://${appContext.packageName}/${com.example.funlife.R.raw.pet}"))
                prepare()
                
                isLooping = true  // 循环播放
                setVolume(1.0f, 1.0f)  // 设置音量为最大
                
                // 使用LoudnessEnhancer增强音量
                try {
                    loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                        enabled = true
                        setTargetGain(4000)  // 增加4000毫贝（约6倍音量，最大增益）
                    }
                    Log.d(TAG, "LoudnessEnhancer applied with gain 4000 (maximum)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to apply LoudnessEnhancer", e)
                }
                
                start()
                Log.d(TAG, "Pet background music started (looping) with max volume and enhancement")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing pet music", e)
        }
    }
    
    fun stop() {
        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = null
            
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            Log.d(TAG, "Background music stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping music", e)
        }
    }
    
    fun pause() {
        try {
            mediaPlayer?.pause()
            Log.d(TAG, "Background music paused")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing music", e)
        }
    }
    
    fun resume() {
        try {
            mediaPlayer?.start()
            Log.d(TAG, "Background music resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming music", e)
        }
    }
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            stop()
        }
        Log.d(TAG, "Background music ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun release() {
        stop()
        Log.d(TAG, "BackgroundMusicManager released")
    }
    
    companion object {
        private const val TAG = "BackgroundMusicManager"
        
        @Volatile
        private var instance: BackgroundMusicManager? = null
        
        fun getInstance(context: Context): BackgroundMusicManager {
            return instance ?: synchronized(this) {
                instance ?: BackgroundMusicManager(context).also {
                    instance = it
                }
            }
        }
    }
}
