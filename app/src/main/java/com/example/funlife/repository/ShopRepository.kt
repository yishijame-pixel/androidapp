// ShopRepository.kt - 商城仓库
package com.example.funlife.repository

import com.example.funlife.data.dao.ShopDao
import com.example.funlife.data.model.ShopItem
import com.example.funlife.data.model.PurchaseHistory
import kotlinx.coroutines.flow.Flow

class ShopRepository(
    private val shopDao: ShopDao,
    private val userAvatarFrameDao: com.example.funlife.data.dao.UserAvatarFrameDao
) {
    
    val allShopItems: Flow<List<ShopItem>> = shopDao.getAllShopItems()
    
    fun getPurchaseHistory(userId: Long): Flow<List<PurchaseHistory>> = shopDao.getPurchaseHistory(userId)
    
    suspend fun getShopItem(itemId: Int) = shopDao.getShopItem(itemId)
    
    suspend fun insertPurchaseHistory(purchase: PurchaseHistory) = shopDao.insertPurchaseHistory(purchase)
    
    suspend fun getPurchaseCount(userId: Long, itemId: Int) = shopDao.getPurchaseCount(userId, itemId)
    
    suspend fun insertShopItem(item: ShopItem) = shopDao.insertShopItem(item)
    
    // 🔥 新增：头像框相关方法
    
    /**
     * 获取所有头像框
     */
    fun getAvatarFrames(): Flow<List<ShopItem>> = shopDao.getAvatarFrames()
    
    /**
     * 获取用户拥有的头像框
     */
    fun getUserOwnedFrames(userId: Long): Flow<List<com.example.funlife.data.model.UserAvatarFrame>> = 
        userAvatarFrameDao.getUserFrames(userId)
    
    /**
     * 检查用户是否拥有某个头像框
     */
    suspend fun isFrameOwned(userId: Long, frameId: Int): Boolean = 
        userAvatarFrameDao.isFrameOwned(userId, frameId)
    
    /**
     * 添加头像框到用户背包
     */
    suspend fun addUserFrame(userId: Long, frameId: Int) {
        val userFrame = com.example.funlife.data.model.UserAvatarFrame(
            userId = userId,
            frameId = frameId,
            purchasedAt = System.currentTimeMillis(),
            isEquipped = false
        )
        userAvatarFrameDao.insertUserFrame(userFrame)
    }
    
    /**
     * 装备头像框
     */
    suspend fun equipFrame(userId: Long, frameId: Int) {
        userAvatarFrameDao.equipFrame(userId, frameId)
    }
    
    /**
     * 取消装备头像框
     */
    suspend fun unequipFrame(userId: Long) {
        userAvatarFrameDao.unequipAllFrames(userId)
    }
    
    /**
     * 获取用户当前装备的头像框
     */
    suspend fun getEquippedFrame(userId: Long): com.example.funlife.data.model.UserAvatarFrame? = 
        userAvatarFrameDao.getEquippedFrame(userId)
    
    /**
     * 根据稀有度筛选头像框
     */
    fun getFramesByRarity(rarity: String): Flow<List<ShopItem>> = 
        shopDao.getAvatarFramesByRarity(rarity)
    
    /**
     * 获取动态头像框
     */
    fun getAnimatedFrames(): Flow<List<ShopItem>> = shopDao.getAnimatedFrames()
    
    /**
     * 获取静态头像框
     */
    fun getStaticFrames(): Flow<List<ShopItem>> = shopDao.getStaticFrames()
    
    // 初始化商城商品
    suspend fun initializeShopItems() {
        // 检查按钮皮肤是否已存在
        val buttonSkinsCount = shopDao.getShopItemsByType("button_skin").size
        
        if (buttonSkinsCount == 0) {
            // 添加按钮皮肤商品
            val buttonSkinItems = listOf(
                ShopItem(
                    name = "初心如故",
                    description = "经典永恒，初心不改",
                    icon = "🎨",
                    price = 0,
                    type = "button_skin",
                    value = 1,
                    isAvailable = true
                ),
                ShopItem(
                    name = "粉黛流年",
                    description = "粉色如梦，温柔似水",
                    icon = "🌸",
                    price = 1,
                    type = "button_skin",
                    value = 2
                ),
                ShopItem(
                    name = "碧海青天",
                    description = "蓝色深邃，如海如天",
                    icon = "🌊",
                    price = 1,
                    type = "button_skin",
                    value = 3
                ),
                ShopItem(
                    name = "翠竹凝烟",
                    description = "绿意盎然，生机勃勃",
                    icon = "🎋",
                    price = 1,
                    type = "button_skin",
                    value = 4
                ),
                ShopItem(
                    name = "紫气东来",
                    description = "紫色高贵，祥瑞之兆",
                    icon = "💜",
                    price = 1,
                    type = "button_skin",
                    value = 5
                ),
                ShopItem(
                    name = "橙黄橘绿",
                    description = "橙色温暖，活力四射",
                    icon = "🍊",
                    price = 1,
                    type = "button_skin",
                    value = 6
                ),
                ShopItem(
                    name = "丹霞映日",
                    description = "红色热烈，如火如荼",
                    icon = "🔥",
                    price = 1,
                    type = "button_skin",
                    value = 7
                ),
                ShopItem(
                    name = "金风玉露",
                    description = "黄色明媚，温暖如春",
                    icon = "🌻",
                    price = 1,
                    type = "button_skin",
                    value = 8
                ),
                ShopItem(
                    name = "青山绿水",
                    description = "青色清新，自然和谐",
                    icon = "🏞️",
                    price = 1,
                    type = "button_skin",
                    value = 9
                ),
                ShopItem(
                    name = "金碧辉煌",
                    description = "金色璀璨，富贵荣华",
                    icon = "✨",
                    price = 1,
                    type = "button_skin",
                    value = 10
                ),
                ShopItem(
                    name = "银装素裹",
                    description = "银色纯净，素雅高洁",
                    icon = "❄️",
                    price = 1,
                    type = "button_skin",
                    value = 11
                ),
                ShopItem(
                    name = "霓虹幻彩",
                    description = "彩虹斑斓，梦幻绚丽",
                    icon = "🌈",
                    price = 1,
                    type = "button_skin",
                    value = 12
                ),
                ShopItem(
                    name = "星河璀璨",
                    description = "星空浩瀚，璀璨夺目",
                    icon = "⭐",
                    price = 1,
                    type = "button_skin",
                    value = 13
                ),
                ShopItem(
                    name = "烈焰焚天",
                    description = "火焰炽热，势不可挡",
                    icon = "🔥",
                    price = 1,
                    type = "button_skin",
                    value = 14
                ),
                ShopItem(
                    name = "冰清玉洁",
                    description = "冰霜晶莹，纯洁无瑕",
                    icon = "🧊",
                    price = 1,
                    type = "button_skin",
                    value = 15
                ),
                ShopItem(
                    name = "雷霆万钧",
                    description = "雷电震撼，威力无穷",
                    icon = "⚡",
                    price = 1,
                    type = "button_skin",
                    value = 16
                ),
                ShopItem(
                    name = "林深见鹿",
                    description = "森林幽深，生机盎然",
                    icon = "🌲",
                    price = 1,
                    type = "button_skin",
                    value = 17
                ),
                ShopItem(
                    name = "沧海桑田",
                    description = "海洋辽阔，波澜壮阔",
                    icon = "🌊",
                    price = 1,
                    type = "button_skin",
                    value = 18
                ),
                ShopItem(
                    name = "大漠孤烟",
                    description = "沙漠苍茫，孤烟直上",
                    icon = "🏜️",
                    price = 1,
                    type = "button_skin",
                    value = 19
                ),
                ShopItem(
                    name = "极光流转",
                    description = "极光绚烂，如梦如幻",
                    icon = "🌌",
                    price = 1,
                    type = "button_skin",
                    value = 20
                ),
                ShopItem(
                    name = "樱花烂漫",
                    description = "樱花飘落，浪漫唯美",
                    icon = "🌸",
                    price = 1,
                    type = "button_skin",
                    value = 21
                ),
                ShopItem(
                    name = "枫叶如丹",
                    description = "枫叶似火，层林尽染",
                    icon = "🍁",
                    price = 1,
                    type = "button_skin",
                    value = 22
                ),
                ShopItem(
                    name = "雪舞轻扬",
                    description = "雪花飞舞，银装世界",
                    icon = "❄️",
                    price = 1,
                    type = "button_skin",
                    value = 23
                ),
                ShopItem(
                    name = "星辰大海",
                    description = "星辰闪耀，征途无限",
                    icon = "🌟",
                    price = 1,
                    type = "button_skin",
                    value = 24
                ),
                ShopItem(
                    name = "月华如水",
                    description = "月光皎洁，温柔似水",
                    icon = "🌙",
                    price = 1,
                    type = "button_skin",
                    value = 25
                ),
                ShopItem(
                    name = "传世经典",
                    description = "传说永恒，经典不朽",
                    icon = "👑",
                    price = 1,
                    type = "button_skin",
                    value = 26
                )
            )
            
            buttonSkinItems.forEach { item ->
                insertShopItem(item)
            }
            
            android.util.Log.d("ShopRepository", "Initialized ${buttonSkinItems.size} button skin items")
        }
        
        // 检查纪念日相框是否已存在
        val anniversaryFramesCount = shopDao.getShopItemsByType("anniversary_frame").size
        android.util.Log.d("ShopRepository", "Current anniversary frames count: $anniversaryFramesCount")
        
        if (anniversaryFramesCount == 0) {
            android.util.Log.d("ShopRepository", "Initializing anniversary frames...")
            // 添加纪念日相框商品
            val anniversaryFrameItems = listOf(
                ShopItem(
                    name = "经典相框",
                    description = "默认相框，简约大方",
                    icon = "🖼️",
                    price = 0,
                    type = "anniversary_frame",
                    value = 1,
                    isAvailable = true
                ),
                ShopItem(
                    name = "温馨相框",
                    description = "默认相框，温馨浪漫",
                    icon = "🖼️",
                    price = 0,
                    type = "anniversary_frame",
                    value = 2,
                    isAvailable = true
                ),
                ShopItem(
                    name = "花卉相框",
                    description = "精美花卉装饰相框",
                    icon = "🌸",
                    price = 1,
                    type = "anniversary_frame",
                    value = 3
                ),
                ShopItem(
                    name = "星空相框",
                    description = "璀璨星空主题相框",
                    icon = "⭐",
                    price = 1,
                    type = "anniversary_frame",
                    value = 4
                ),
                ShopItem(
                    name = "爱心相框",
                    description = "浪漫爱心装饰相框",
                    icon = "💖",
                    price = 1,
                    type = "anniversary_frame",
                    value = 5
                ),
                ShopItem(
                    name = "彩虹相框",
                    description = "缤纷彩虹主题相框",
                    icon = "🌈",
                    price = 1,
                    type = "anniversary_frame",
                    value = 6
                ),
                ShopItem(
                    name = "金色相框",
                    description = "华丽金色装饰相框",
                    icon = "✨",
                    price = 1,
                    type = "anniversary_frame",
                    value = 7
                ),
                ShopItem(
                    name = "蝴蝶相框",
                    description = "优雅蝴蝶主题相框",
                    icon = "🦋",
                    price = 1,
                    type = "anniversary_frame",
                    value = 8
                ),
                ShopItem(
                    name = "樱花相框",
                    description = "唯美樱花装饰相框",
                    icon = "🌸",
                    price = 1,
                    type = "anniversary_frame",
                    value = 9
                ),
                ShopItem(
                    name = "皇冠相框",
                    description = "尊贵皇冠主题相框",
                    icon = "👑",
                    price = 1,
                    type = "anniversary_frame",
                    value = 10
                )
            )
            
            anniversaryFrameItems.forEach { item ->
                insertShopItem(item)
            }
            
            android.util.Log.d("ShopRepository", "Initialized ${anniversaryFrameItems.size} anniversary frame items")
        }
        
        // 使用一个简单的查询来检查是否已有其他商品
        val count = shopDao.getShopItemCount()
        
        if (count == buttonSkinsCount + anniversaryFramesCount) {
            // 只有按钮皮肤和相框，需要添加其他默认商品
            val defaultItems = listOf(
                ShopItem(
                    name = "补卡卡片",
                    description = "可以补签一次错过的打卡",
                    icon = "🎫",
                    price = 50,
                    type = "makeup_card",
                    value = 1
                ),
                ShopItem(
                    name = "补卡礼包",
                    description = "一次性获得5张补卡卡片",
                    icon = "🎁",
                    price = 200,
                    type = "makeup_card",
                    value = 5
                ),
                ShopItem(
                    name = "金币袋",
                    description = "获得100金币",
                    icon = "💰",
                    price = 0,
                    type = "coins",
                    value = 100,
                    isAvailable = false
                ),
                ShopItem(
                    name = "转盘次数+1",
                    description = "额外获得1次转盘机会",
                    icon = "🎰",
                    price = 30,
                    type = "spin_chance",
                    value = 1
                ),
                ShopItem(
                    name = "转盘礼包",
                    description = "额外获得5次转盘机会",
                    icon = "🎲",
                    price = 120,
                    type = "spin_chance",
                    value = 5
                ),
                ShopItem(
                    name = "宠物食物",
                    description = "喂养宠物，增加亲密度",
                    icon = "🍖",
                    price = 20,
                    type = "pet_food",
                    value = 1
                ),
                ShopItem(
                    name = "宠物玩具",
                    description = "和宠物玩耍，增加快乐值",
                    icon = "🎾",
                    price = 35,
                    type = "pet_toy",
                    value = 1
                ),
                ShopItem(
                    name = "宠物零食礼包",
                    description = "包含5份宠物食物",
                    icon = "🍗",
                    price = 80,
                    type = "pet_food",
                    value = 5
                ),
                ShopItem(
                    name = "幸运符",
                    description = "提升转盘中奖概率",
                    icon = "🍀",
                    price = 100,
                    type = "lucky_charm",
                    value = 1
                ),
                ShopItem(
                    name = "经验加速卡",
                    description = "宠物经验获取速度翻倍",
                    icon = "⚡",
                    price = 150,
                    type = "exp_boost",
                    value = 1
                ),
                ShopItem(
                    name = "彩虹主题",
                    description = "解锁转盘彩虹主题",
                    icon = "🌈",
                    price = 300,
                    type = "theme",
                    value = 1
                ),
                ShopItem(
                    name = "星空主题",
                    description = "解锁转盘星空主题",
                    icon = "✨",
                    price = 300,
                    type = "theme",
                    value = 1
                ),
                ShopItem(
                    name = "成就徽章",
                    description = "展示你的成就",
                    icon = "🏆",
                    price = 500,
                    type = "badge",
                    value = 1
                ),
                ShopItem(
                    name = "VIP月卡",
                    description = "30天VIP特权",
                    icon = "👑",
                    price = 1000,
                    type = "vip",
                    value = 30
                )
            )
            
            defaultItems.forEach { item ->
                insertShopItem(item)
            }
        }
        
        // 🔥 新增：初始化头像框商品
        // 注意：需要传入Context，所以这个初始化应该在Application或ViewModel中调用
        // 这里只是占位，实际初始化在ShopViewModel中进行
    }
    
    /**
     * 初始化头像框商品（使用真实的assets数据）
     * 注意：这个方法应该在有Context的地方调用，比如Application或ViewModel
     */
    suspend fun initializeAvatarFrames(context: android.content.Context): Int {
        // 检查头像框是否已存在
        val avatarFramesCount = shopDao.getShopItemsByType("avatar_frame").size
        
        if (avatarFramesCount > 0) {
            android.util.Log.d("ShopRepository", "Avatar frames already initialized: $avatarFramesCount items")
            return avatarFramesCount
        }
        
        // 使用AvatarFrameInitializer从assets目录初始化真实数据
        return try {
            com.example.funlife.utils.AvatarFrameInitializer.initializeAvatarFrames(context, shopDao)
        } catch (e: Exception) {
            android.util.Log.e("ShopRepository", "Failed to initialize avatar frames", e)
            0
        }
    }
}
