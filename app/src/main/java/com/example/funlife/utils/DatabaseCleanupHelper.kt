// DatabaseCleanupHelper.kt - 数据库清理工具
package com.example.funlife.utils

import android.content.Context
import android.util.Log
import com.example.funlife.data.dao.ShopDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 数据库清理工具
 * 用于清理重复数据和修复数据问题
 */
object DatabaseCleanupHelper {
    
    private const val TAG = "DatabaseCleanup"
    
    /**
     * 清理重复的头像框数据
     * 保留每个assetPath的第一条记录，删除其他重复项
     */
    suspend fun cleanupDuplicateAvatarFrames(context: Context, shopDao: ShopDao): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始清理重复的头像框数据...")
                
                // 获取所有头像框
                val allFrames = shopDao.getShopItemsByType("avatar_frame")
                Log.d(TAG, "找到 ${allFrames.size} 个头像框商品")
                
                // 按assetPath分组，找出重复项
                val groupedByPath = allFrames.groupBy { it.assetPath }
                
                val idsToKeep = mutableSetOf<Int>()
                groupedByPath.forEach { (assetPath, frames) ->
                    if (frames.size > 1) {
                        Log.d(TAG, "发现重复: $assetPath 有 ${frames.size} 条记录")
                        // 保留第一条（ID最小的）
                        idsToKeep.add(frames.minByOrNull { it.id }!!.id)
                    } else {
                        idsToKeep.add(frames.first().id)
                    }
                }
                
                val deletedCount = allFrames.size - idsToKeep.size
                
                if (deletedCount > 0) {
                    // 删除所有头像框，然后重新插入不重复的
                    shopDao.deleteAllAvatarFrames()
                    
                    val uniqueFrames = allFrames.filter { it.id in idsToKeep }
                    uniqueFrames.forEach { shopDao.insertShopItem(it) }
                    
                    Log.d(TAG, "清理完成，删除了 $deletedCount 条重复记录，保留了 ${uniqueFrames.size} 条")
                }
                
                deletedCount
            } catch (e: Exception) {
                Log.e(TAG, "清理失败", e)
                0
            }
        }
    }
    
    /**
     * 删除所有头像框数据（用于重新初始化）
     */
    suspend fun deleteAllAvatarFrames(shopDao: ShopDao): Int {
        return withContext(Dispatchers.IO) {
            try {
                val frames = shopDao.getShopItemsByType("avatar_frame")
                val count = frames.size
                shopDao.deleteAllAvatarFrames()
                Log.d(TAG, "已删除 $count 个头像框")
                count
            } catch (e: Exception) {
                Log.e(TAG, "删除失败", e)
                0
            }
        }
    }
}
