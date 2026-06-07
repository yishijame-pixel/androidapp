// ProfileRepository.kt - 个人主页数据仓库
package com.example.funlife.repository

import androidx.room.withTransaction
import com.example.funlife.data.dao.*
import com.example.funlife.data.database.AppDatabase
import com.example.funlife.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ProfileRepository(
    private val userAvatarDao: UserAvatarDao,
    private val userDao: UserDao,
    private val coinDao: CoinDao,
    private val dailyRewardDao: DailyRewardDao,
    private val db: AppDatabase? = null
) {
    
    // ========== 用户头像信息 ==========
    
    fun getUserAvatar(userId: Long): Flow<UserAvatar?> {
        return userAvatarDao.getUserAvatar(userId)
    }
    
    suspend fun updateAvatarUri(userId: Long, avatarUri: String?) {
        // 确保用户头像记录存在
        val existing = userAvatarDao.getUserAvatar(userId).first()
        if (existing == null) {
            userAvatarDao.insertOrUpdateUserAvatar(
                UserAvatar(userId = userId, avatarUri = avatarUri)
            )
        } else {
            userAvatarDao.updateAvatarUri(userId, avatarUri)
        }
        com.example.funlife.utils.UserAvatarBitmapCache.publishUri(userId, avatarUri)
    }
    
    suspend fun updateFrameId(userId: Long, frameId: String?) {
        val existing = userAvatarDao.getUserAvatar(userId).first()
        if (existing == null) {
            userAvatarDao.insertOrUpdateUserAvatar(
                UserAvatar(userId = userId, frameId = frameId)
            )
        } else {
            userAvatarDao.updateFrameId(userId, frameId)
        }
    }
    
    suspend fun updateBackgroundId(userId: Long, backgroundId: String?) {
        val existing = userAvatarDao.getUserAvatar(userId).first()
        if (existing == null) {
            userAvatarDao.insertOrUpdateUserAvatar(
                UserAvatar(userId = userId, backgroundId = backgroundId)
            )
        } else {
            userAvatarDao.updateBackgroundId(userId, backgroundId)
        }
    }
    
    // ========== 头像框 ==========
    
    fun getAllFrames(): Flow<List<AvatarFrame>> {
        return userAvatarDao.getAllFrames()
    }
    
    fun getUserAvailableFrames(userId: Long, vipLevel: Int): Flow<List<AvatarFrame>> {
        return userAvatarDao.getUserAvailableFrames(userId, vipLevel)
    }
    
    suspend fun purchaseFrame(userId: Long, frameId: String, price: Int): Result<Unit> {
        val database = db ?: return Result.failure(IllegalStateException("DB not injected"))
        return try {
            database.withTransaction {
                // 已拥有检查（事务内，避免并发双发）
                if (userAvatarDao.hasFrame(userId, frameId)) {
                    throw IllegalStateException("已拥有该头像框")
                }
                // 原子扣币（同时校验余额）
                val rowsAffected = coinDao.spendCoinsAtomic(userId, price)
                if (rowsAffected == 0) {
                    throw IllegalStateException("金币不足")
                }
                userAvatarDao.addOwnedFrame(UserOwnedFrame(userId = userId, frameId = frameId))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== 背景主题 ==========
    
    fun getAllBackgrounds(): Flow<List<ProfileBackground>> {
        return userAvatarDao.getAllBackgrounds()
    }
    
    fun getUserAvailableBackgrounds(userId: Long, vipLevel: Int): Flow<List<ProfileBackground>> {
        return userAvatarDao.getUserAvailableBackgrounds(userId, vipLevel)
    }
    
    suspend fun purchaseBackground(userId: Long, backgroundId: String, price: Int): Result<Unit> {
        val database = db ?: return Result.failure(IllegalStateException("DB not injected"))
        return try {
            database.withTransaction {
                if (userAvatarDao.hasBackground(userId, backgroundId)) {
                    throw IllegalStateException("已拥有该背景")
                }
                val rowsAffected = coinDao.spendCoinsAtomic(userId, price)
                if (rowsAffected == 0) {
                    throw IllegalStateException("金币不足")
                }
                userAvatarDao.addOwnedBackground(UserOwnedBackground(userId = userId, backgroundId = backgroundId))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== 用户统计 ==========
    
    suspend fun getUserStatistics(userId: Long): UserStatistics {
        val user = userDao.getUserById(userId)
        val registeredDays = if (user != null) {
            val daysSinceRegistration = (System.currentTimeMillis() - user.createdAt) / (1000 * 60 * 60 * 24)
            daysSinceRegistration.toInt()
        } else {
            0
        }
        
        // 获取签到天数（从DailyReward表获取）
        val dailyReward = dailyRewardDao.getDailyReward(userId, "check_in")
        val checkInDays = dailyReward?.claimCount ?: 0
        
        // 获取累计金币
        val coins = coinDao.getUserCoins(userId).first()
        val totalCoins = coins?.totalEarned ?: 0
        
        // 获取VIP天数（暂时返回0，后续实现VIP系统后更新）
        val vipDays = 0
        
        return UserStatistics(
            registeredDays = registeredDays,
            totalCheckIns = checkInDays,
            totalCoins = totalCoins,
            vipDays = vipDays
        )
    }
    
    // ========== 初始化默认数据 ==========
    
    suspend fun initializeDefaultFramesAndBackgrounds() {
        // 初始化默认头像框
        val defaultFrames = listOf(
            // 普通用户
            AvatarFrame(
                id = "none",
                name = "无边框",
                icon = "⭕",
                price = 0,
                requiredVipLevel = 0,
                animationType = "none",
                category = "basic",
                description = "简洁的无边框样式",
                isDefault = true
            ),
            
            // VIP1头像框
            AvatarFrame(
                id = "gold_circle",
                name = "金色圆环",
                icon = "⭐",
                price = 50,
                requiredVipLevel = 1,
                animationType = "glow",
                category = "basic",
                description = "金色圆形边框，微光效果"
            ),
            AvatarFrame(
                id = "gold_square",
                name = "金色方框",
                icon = "🟨",
                price = 50,
                requiredVipLevel = 1,
                animationType = "glow",
                category = "basic",
                description = "金色方形边框，流光效果"
            ),
            
            // VIP2头像框
            AvatarFrame(
                id = "diamond_border",
                name = "钻石边框",
                icon = "💎",
                price = 100,
                requiredVipLevel = 2,
                animationType = "rotate",
                category = "premium",
                description = "钻石形状边框，旋转动画"
            ),
            AvatarFrame(
                id = "tech_border",
                name = "科技边框",
                icon = "🔷",
                price = 100,
                requiredVipLevel = 2,
                animationType = "scan",
                category = "premium",
                description = "科技感边框，扫描线动画"
            ),
            AvatarFrame(
                id = "starry_border",
                name = "星空边框",
                icon = "✨",
                price = 100,
                requiredVipLevel = 2,
                animationType = "twinkle",
                category = "premium",
                description = "星空主题，星星闪烁"
            ),
            
            // VIP3头像框
            AvatarFrame(
                id = "crown_border",
                name = "皇冠边框",
                icon = "👑",
                price = 200,
                requiredVipLevel = 3,
                animationType = "3d",
                category = "exclusive",
                description = "皇冠装饰，3D旋转效果"
            ),
            AvatarFrame(
                id = "dragon_border",
                name = "龙纹边框",
                icon = "🐉",
                price = 300,
                requiredVipLevel = 3,
                animationType = "flow",
                category = "exclusive",
                description = "中国风龙纹，流动动画"
            ),
            AvatarFrame(
                id = "phoenix_border",
                name = "凤凰边框",
                icon = "🦅",
                price = 300,
                requiredVipLevel = 3,
                animationType = "flame",
                category = "exclusive",
                description = "凤凰火焰主题，火焰跳动"
            ),
            AvatarFrame(
                id = "galaxy_border",
                name = "星河边框",
                icon = "🌟",
                price = 300,
                requiredVipLevel = 3,
                animationType = "flow",
                category = "exclusive",
                description = "星河主题，星光流动"
            ),
            AvatarFrame(
                id = "thunder_border",
                name = "雷电边框",
                icon = "⚡",
                price = 300,
                requiredVipLevel = 3,
                animationType = "lightning",
                category = "exclusive",
                description = "雷电主题，闪电动画"
            ),
            AvatarFrame(
                id = "rainbow_border",
                name = "彩虹边框",
                icon = "🌈",
                price = 300,
                requiredVipLevel = 3,
                animationType = "flow",
                category = "exclusive",
                description = "彩虹渐变，流动动画"
            )
        )
        
        userAvatarDao.insertFrames(defaultFrames)
        
        // 初始化默认背景
        val defaultBackgrounds = listOf(
            // 普通用户
            ProfileBackground(
                id = "default_gray",
                name = "默认背景",
                preview = "⚪",
                price = 0,
                requiredVipLevel = 0,
                gradientColors = "#F5F5F5,#FFFFFF",
                particleType = "none",
                description = "简洁灰色背景",
                isDefault = true
            ),
            
            // VIP1背景
            ProfileBackground(
                id = "gold_dream",
                name = "金色梦幻",
                preview = "✨",
                price = 0,
                requiredVipLevel = 1,
                gradientColors = "#FFD700,#FFA500",
                particleType = "gold",
                description = "金色渐变，微光粒子"
            ),
            ProfileBackground(
                id = "starlight",
                name = "星光闪耀",
                preview = "⭐",
                price = 0,
                requiredVipLevel = 1,
                gradientColors = "#1A237E,#283593",
                particleType = "star",
                description = "深蓝色，金色星星"
            ),
            
            // VIP2背景
            ProfileBackground(
                id = "diamond_ocean",
                name = "钻石海洋",
                preview = "💎",
                price = 50,
                requiredVipLevel = 2,
                gradientColors = "#00BCD4,#0097A7",
                particleType = "diamond",
                description = "青蓝渐变，钻石粒子"
            ),
            ProfileBackground(
                id = "tech_future",
                name = "科技未来",
                preview = "🔷",
                price = 50,
                requiredVipLevel = 2,
                gradientColors = "#1A237E,#0D47A1",
                particleType = "grid",
                description = "深蓝色，网格线条"
            ),
            ProfileBackground(
                id = "dream_starry",
                name = "梦幻星空",
                preview = "🌌",
                price = 50,
                requiredVipLevel = 2,
                gradientColors = "#4A148C,#6A1B9A",
                particleType = "star",
                description = "紫蓝渐变，星星"
            ),
            
            // VIP3背景
            ProfileBackground(
                id = "royal_prestige",
                name = "皇家尊贵",
                preview = "👑",
                price = 100,
                requiredVipLevel = 3,
                gradientColors = "#FF6B9D,#FFD700",
                particleType = "crown",
                description = "粉金渐变，皇冠粒子"
            ),
            ProfileBackground(
                id = "dragon_glory",
                name = "龙腾盛世",
                preview = "🐉",
                price = 300,
                requiredVipLevel = 3,
                gradientColors = "#D32F2F,#FFD700",
                particleType = "dragon",
                description = "金红渐变，龙纹"
            ),
            ProfileBackground(
                id = "phoenix_dance",
                name = "凤舞九天",
                preview = "🦅",
                price = 300,
                requiredVipLevel = 3,
                gradientColors = "#FF6F00,#FF3D00",
                particleType = "flame",
                description = "橙红渐变，凤凰"
            ),
            ProfileBackground(
                id = "galaxy_shine",
                name = "星河璀璨",
                preview = "🌟",
                price = 300,
                requiredVipLevel = 3,
                gradientColors = "#4A148C,#7B1FA2",
                particleType = "galaxy",
                description = "紫色渐变，星河"
            ),
            ProfileBackground(
                id = "thunder_power",
                name = "雷霆万钧",
                preview = "⚡",
                price = 300,
                requiredVipLevel = 3,
                gradientColors = "#1A237E,#4A148C",
                particleType = "lightning",
                description = "蓝紫渐变，闪电"
            ),
            ProfileBackground(
                id = "rainbow_heaven",
                name = "彩虹天堂",
                preview = "🌈",
                price = 300,
                requiredVipLevel = 3,
                gradientColors = "#FF6B6B,#FFD93D,#6BCF7F,#4D96FF,#9B59B6",
                particleType = "rainbow",
                description = "彩虹渐变，光效"
            )
        )
        
        userAvatarDao.insertBackgrounds(defaultBackgrounds)
    }
}
