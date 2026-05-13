// AvatarFrameInitializer.kt - 头像框数据初始化工具
package com.example.funlife.utils

import android.content.Context
import android.util.Log
import com.example.funlife.data.dao.ShopDao
import com.example.funlife.data.model.InventoryItemType
import com.example.funlife.data.model.ItemRarity
import com.example.funlife.data.model.ShopItem

/**
 * 头像框初始化器
 * 自动扫描assets/xiangkuang目录并导入所有头像框
 */
object AvatarFrameInitializer {
    
    private const val TAG = "AvatarFrameInit"
    private const val BASE_FOLDER = "xiangkuang"
    
    /**
     * 初始化所有头像框商品
     */
    suspend fun initializeAvatarFrames(context: Context, shopDao: ShopDao): Int {
        try {
            val assetManager = context.assets
            
            // 检查是否已经初始化过
            val existingFrames = shopDao.getShopItemsByType("avatar_frame")
            if (existingFrames.isNotEmpty()) {
                Log.d(TAG, "Avatar frames already initialized (${existingFrames.size} items)")
                return existingFrames.size
            }
            
            // 扫描所有子文件夹
            val folders = assetManager.list(BASE_FOLDER) ?: emptyArray()
            Log.d(TAG, "Found ${folders.size} folders in $BASE_FOLDER")
            
            var sortOrder = 1
            var totalFrames = 0
            
            folders.forEach { folderName ->
                val folderPath = "$BASE_FOLDER/$folderName"
                val files = assetManager.list(folderPath) ?: emptyArray()
                
                files.filter { it.endsWith(".png") || it.endsWith(".gif") }
                    .forEach { fileName ->
                        val assetPath = "$folderPath/$fileName"
                        val isAnimated = fileName.endsWith(".gif")
                        val rarity = determineRarity(folderName, fileName)
                        val (price, vipPrice) = calculatePrice(rarity, isAnimated)
                        
                        val shopItem = ShopItem(
                            name = generateFrameName(folderName, fileName),
                            description = generateDescription(rarity, isAnimated),
                            icon = if (isAnimated) "✨" else "🖼️",
                            price = price,
                            vipPrice = vipPrice,
                            type = "avatar_frame",
                            value = 1,
                            isAvailable = true,
                            assetPath = assetPath,
                            rarity = rarity.name,
                            isAnimated = isAnimated,
                            category = folderName,
                            sortOrder = sortOrder++
                        )
                        
                        shopDao.insertShopItem(shopItem)
                        totalFrames++
                    }
            }
            
            Log.d(TAG, "Successfully initialized $totalFrames avatar frames")
            return totalFrames
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing avatar frames", e)
            return 0
        }
    }
    
    /**
     * 根据文件夹和文件名确定稀有度
     */
    private fun determineRarity(folderName: String, fileName: String): ItemRarity {
        return when {
            // GIF动态框 - 高稀有度
            fileName.endsWith(".gif") -> {
                when {
                    folderName.contains("8") -> ItemRarity.LEGENDARY  // 头像框8
                    folderName.contains("6") -> ItemRarity.EPIC       // 头像框6
                    else -> ItemRarity.RARE
                }
            }
            // PNG静态框 - 根据文件夹分配
            folderName.contains("A4") || folderName.contains("5") -> ItemRarity.COMMON
            folderName.contains("1") || folderName.contains("2") -> ItemRarity.COMMON
            folderName.contains("4") || folderName.contains("7") -> ItemRarity.RARE
            folderName.contains("6") || folderName.contains("8") -> ItemRarity.EPIC
            else -> ItemRarity.COMMON
        }
    }
    
    /**
     * 根据稀有度和类型计算价格
     */
    private fun calculatePrice(rarity: ItemRarity, isAnimated: Boolean): Pair<Int, Int> {
        val basePrice = when (rarity) {
            ItemRarity.COMMON -> if (isAnimated) 300 else 50
            ItemRarity.RARE -> if (isAnimated) 500 else 100
            ItemRarity.EPIC -> if (isAnimated) 800 else 200
            ItemRarity.LEGENDARY -> if (isAnimated) 1200 else 300
        }
        val vipPrice = (basePrice * 0.8).toInt()  // VIP 8折
        return Pair(basePrice, vipPrice)
    }
    
    /**
     * 生成头像框名称
     * 根据编号范围分配不同主题的诗意名称
     */
    private fun generateFrameName(folder: String, file: String): String {
        // 从文件名提取编号
        val number = file.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1
        
        return when {
            folder.contains("A4") -> {
                // A4系列：简约风格（41-60）
                val names = listOf(
                    "月光之环", "星辰之眸", "晨曦微光", "暮色流金",
                    "樱花飘落", "枫叶秋韵", "雪花纷飞", "春风拂面",
                    "碧海蓝天", "紫霞仙境", "翡翠之梦", "琥珀时光",
                    "水墨丹青", "云卷云舒", "花开半夏", "竹影清风",
                    "梅香暗涌", "兰韵幽香", "菊韵秋思", "荷塘月色"
                )
                val index = (number - 41).coerceIn(0, names.size - 1)
                names.getOrElse(index) { "简约之框 #$number" }
            }
            
            folder.contains("头像框1") -> {
                // 一十系列1：华丽风格（1-123）
                when {
                    number <= 20 -> {
                        val names = listOf(
                            "一十·星河璀璨", "一十·月华流转", "一十·云锦天章", "一十·霞光万丈",
                            "一十·琉璃幻境", "一十·翡翠华章", "一十·珊瑚之梦", "一十·琥珀流光",
                            "一十·水晶之心", "一十·玛瑙之韵", "一十·碧玉清辉", "一十·紫晶迷梦",
                            "一十·金辉耀世", "一十·银月如霜", "一十·铜雀春深", "一十·铁骨铮铮",
                            "一十·玉树临风", "一十·冰清玉洁", "一十·火树银花", "一十·水木清华"
                        )
                        names.getOrElse(number - 1) { "一十·华章 #$number" }
                    }
                    number <= 50 -> {
                        val names = listOf(
                            "一十·春暖花开", "一十·夏日炎炎", "一十·秋高气爽", "一十·冬雪皑皑",
                            "一十·东风破晓", "一十·南风知意", "一十·西风烈", "一十·北风呼啸",
                            "一十·天地玄黄", "一十·宇宙洪荒", "一十·日月盈昃", "一十·辰宿列张",
                            "一十·寒来暑往", "一十·秋收冬藏", "一十·闰余成岁", "一十·律吕调阳",
                            "一十·云腾致雨", "一十·露结为霜", "一十·金生丽水", "一十·玉出昆冈",
                            "一十·剑号巨阙", "一十·珠称夜光", "一十·果珍李柰", "一十·菜重芥姜",
                            "一十·海咸河淡", "一十·鳞潜羽翔", "一十·龙师火帝", "一十·鸟官人皇",
                            "一十·始制文字", "一十·乃服衣裳"
                        )
                        names.getOrElse(number - 21) { "一十·雅韵 #$number" }
                    }
                    number <= 80 -> "一十·锦绣 #${number - 50}"
                    number <= 100 -> "一十·繁华 #${number - 80}"
                    else -> "一十·盛世 #${number - 100}"
                }
            }
            
            folder.contains("头像框2") -> {
                // 一十系列2：优雅风格
                when {
                    number <= 30 -> "一十雅韵·${getElegantName(number)}"
                    else -> "一十雅韵·清风 #${number - 30}"
                }
            }
            
            folder.contains("头像框4") -> {
                // 一十系列4：华章风格
                when {
                    number <= 30 -> "一十华章·${getRegalName(number)}"
                    else -> "一十华章·盛世 #${number - 30}"
                }
            }
            
            folder.contains("头像框5") -> {
                // 经典系列：传统风格
                when {
                    number <= 20 -> "经典·${getClassicName(number)}"
                    else -> "经典·雅致 #${number - 20}"
                }
            }
            
            folder.contains("头像框6") -> {
                // 一十系列6：璀璨风格（史诗级）
                when {
                    number <= 25 -> "一十璀璨·${getGemName(number)}"
                    else -> "一十璀璨·宝石 #${number - 25}"
                }
            }
            
            folder.contains("头像框7") -> {
                // 一十系列7：流光风格（稀有级）
                when {
                    number <= 25 -> "一十流光·${getFlowingName(number)}"
                    else -> "一十流光·幻彩 #${number - 25}"
                }
            }
            
            folder.contains("头像框8") -> {
                // 一十系列8：传说风格（传说级）
                when {
                    number <= 20 -> "一十传说·${getLegendaryName(number)}"
                    else -> "一十传说·神话 #${number - 20}"
                }
            }
            
            else -> "头像框 #$number"
        }
    }
    
    // 优雅名称库
    private fun getElegantName(index: Int): String {
        val names = listOf(
            "兰亭序", "滕王阁", "岳阳楼", "黄鹤楼", "蓬莱阁",
            "醉翁亭", "爱晚亭", "沧浪亭", "拙政园", "留园",
            "网师园", "狮子林", "寒山寺", "灵隐寺", "少林寺",
            "白马寺", "大雁塔", "小雁塔", "钟楼", "鼓楼",
            "天坛", "地坛", "日坛", "月坛", "先农坛",
            "太庙", "社稷坛", "圜丘", "祈年殿", "皇穹宇"
        )
        return names.getOrElse(index - 1) { "清风明月" }
    }
    
    // 华贵名称库
    private fun getRegalName(index: Int): String {
        val names = listOf(
            "九天揽月", "五洋捉鳖", "龙腾四海", "凤舞九天", "麒麟献瑞",
            "玄武镇海", "朱雀翱翔", "白虎啸天", "青龙出水", "神龟长寿",
            "仙鹤延年", "灵鹿献瑞", "瑞狮呈祥", "金蟾纳福", "玉兔捣药",
            "天马行空", "神鹰展翅", "灵猴献桃", "瑞象太平", "金鱼戏水",
            "锦鲤跃龙门", "鸳鸯戏水", "喜鹊登梅", "燕子归巢", "孔雀开屏",
            "凤凰涅槃", "鲲鹏展翅", "鸿鹄之志", "鹏程万里", "扶摇直上"
        )
        return names.getOrElse(index - 1) { "华章盛世" }
    }
    
    // 经典名称库
    private fun getClassicName(index: Int): String {
        val names = listOf(
            "水墨丹青", "泼墨山水", "工笔花鸟", "写意人物", "青绿山水",
            "金碧辉煌", "浅绛山水", "没骨花卉", "白描人物", "界画楼阁",
            "梅兰竹菊", "松竹梅", "岁寒三友", "花中四君子", "富贵牡丹",
            "清荷出水", "秋菊傲霜", "冬梅凌寒", "春兰飘香", "夏荷清韵"
        )
        return names.getOrElse(index - 1) { "雅致风华" }
    }
    
    // 宝石名称库
    private fun getGemName(index: Int): String {
        val names = listOf(
            "钻石恒久", "红宝石之心", "蓝宝石之眼", "祖母绿之梦", "猫眼石之谜",
            "碧玺幻彩", "欧泊星云", "珍珠之泪", "珊瑚海", "琥珀时光",
            "翡翠青山", "和田美玉", "岫岩碧玉", "独山玉韵", "蓝田日暖",
            "昆仑玉洁", "南阳玉润", "密玉清辉", "黄龙玉华", "金丝玉彩",
            "玛瑙流光", "水晶之心", "紫晶迷梦", "黄晶耀世", "绿晶清韵"
        )
        return names.getOrElse(index - 1) { "璀璨宝石" }
    }
    
    // 流光名称库
    private fun getFlowingName(index: Int): String {
        val names = listOf(
            "极光之舞", "星河流转", "银河倒泻", "流星雨夜", "彩虹桥",
            "霓虹闪耀", "霞光万道", "晨曦初露", "暮色苍茫", "月华如水",
            "星光璀璨", "日晕光环", "月晕七彩", "云霞满天", "霞蔚云蒸",
            "光影交错", "幻彩流光", "七彩祥云", "五彩斑斓", "万紫千红",
            "姹紫嫣红", "五光十色", "流光溢彩", "光彩夺目", "熠熠生辉"
        )
        return names.getOrElse(index - 1) { "流光幻彩" }
    }
    
    // 传说名称库
    private fun getLegendaryName(index: Int): String {
        val names = listOf(
            "盘古开天", "女娲补天", "后羿射日", "嫦娥奔月", "夸父追日",
            "精卫填海", "愚公移山", "大禹治水", "神农尝百草", "燧人取火",
            "仓颉造字", "伏羲画卦", "黄帝战蚩尤", "共工怒触不周山", "女娲造人",
            "昆仑仙境", "蓬莱仙岛", "瑶池盛会", "天宫琼楼", "龙宫宝殿"
        )
        return names.getOrElse(index - 1) { "神话传说" }
    }
    
    /**
     * 生成商品描述
     * 根据稀有度和类型生成吸引人的描述
     */
    private fun generateDescription(rarity: ItemRarity, isAnimated: Boolean): String {
        val rarityDesc = when (rarity) {
            ItemRarity.COMMON -> listOf(
                "简约而不简单，彰显低调奢华",
                "清新淡雅，如沐春风",
                "素雅大方，尽显品味",
                "简洁明快，别具一格"
            ).random()
            ItemRarity.RARE -> listOf(
                "精致优雅，独具匠心",
                "华丽精美，光彩照人",
                "雅致非凡，气质出众",
                "精工细作，美轮美奂"
            ).random()
            ItemRarity.EPIC -> listOf(
                "华丽璀璨，尊贵非凡",
                "璀璨夺目，王者风范",
                "金碧辉煌，气势磅礴",
                "流光溢彩，尊享奢华"
            ).random()
            ItemRarity.LEGENDARY -> listOf(
                "传说品质，举世无双",
                "神话再现，万众瞩目",
                "绝世珍品，独一无二",
                "传世之作，永恒经典"
            ).random()
        }
        
        val animDesc = if (isAnimated) {
            listOf(
                "，动态效果惊艳全场",
                "，流光溢彩动人心魄",
                "，灵动飘逸美不胜收",
                "，华丽动画引人注目"
            ).random()
        } else ""
        
        return "$rarityDesc$animDesc"
    }
}
