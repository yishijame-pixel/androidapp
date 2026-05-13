// AppInitializer.kt - 应用初始化工具
package com.example.funlife.utils

import android.content.Context
import android.util.Log
import com.example.funlife.data.dao.ShopDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用初始化工具
 * 负责首次启动时的数据初始化
 */
object AppInitializer {
    
    private const val TAG = "AppInitializer"
    private const val PREFS_NAME = "app_init_prefs"
    private const val KEY_AVATAR_FRAMES_INITIALIZED = "avatar_frames_initialized"
    
    /**
     * 初始化应用数据
     * 只在首次启动时执行一次
     */
    suspend fun initialize(context: Context, shopDao: ShopDao) {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            // 🔥 先清理重复数据（每次启动都执行）
            try {
                val deletedCount = DatabaseCleanupHelper.cleanupDuplicateAvatarFrames(context, shopDao)
                if (deletedCount > 0) {
                    Log.d(TAG, "清理了 $deletedCount 条重复的头像框数据")
                }
            } catch (e: Exception) {
                Log.e(TAG, "清理重复数据失败", e)
            }
            
            // 检查是否已初始化头像框
            if (!prefs.getBoolean(KEY_AVATAR_FRAMES_INITIALIZED, false)) {
                try {
                    Log.d(TAG, "开始初始化头像框数据...")
                    val count = AvatarFrameInitializer.initializeAvatarFrames(context, shopDao)
                    Log.d(TAG, "头像框初始化完成，共导入 $count 个头像框")
                    
                    // 标记为已初始化
                    prefs.edit()
                        .putBoolean(KEY_AVATAR_FRAMES_INITIALIZED, true)
                        .apply()
                } catch (e: Exception) {
                    Log.e(TAG, "头像框初始化失败", e)
                }
            } else {
                Log.d(TAG, "头像框已初始化，跳过")
            }
        }
    }
    
    /**
     * 重置初始化状态（用于测试或重新导入）
     * 同时清除数据库中的头像框数据
     */
    suspend fun resetInitialization(context: Context, shopDao: ShopDao) {
        withContext(Dispatchers.IO) {
            try {
                // 清除数据库中的头像框
                DatabaseCleanupHelper.deleteAllAvatarFrames(shopDao)
                Log.d(TAG, "已清除数据库中的头像框数据")
                
                // 重置SharedPreferences标记
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean(KEY_AVATAR_FRAMES_INITIALIZED, false)
                    .apply()
                Log.d(TAG, "初始化状态已重置")
            } catch (e: Exception) {
                Log.e(TAG, "重置失败", e)
            }
        }
    }
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AVATAR_FRAMES_INITIALIZED, false)
    }
}
