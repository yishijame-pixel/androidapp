// AvatarFrameConfig.kt - 头像框配置数据模型
package com.example.funlife.data.model

/**
 * 头像框配置
 * 
 * 用于定义每个头像框PNG的实际尺寸和透明区域比例
 * 这样可以精确控制头像在不同头像框中的显示效果
 * 
 * @param assetPath 头像框资源路径
 * @param originalWidth PNG图片的原始宽度（像素）
 * @param originalHeight PNG图片的原始高度（像素）
 * @param transparentAreaRatio 中心透明区域占整体的比例（0.0-1.0）
 * @param offsetX 头像相对于中心的X轴偏移比例（-1.0到1.0，0为居中）
 * @param offsetY 头像相对于中心的Y轴偏移比例（-1.0到1.0，0为居中）
 */
data class AvatarFrameConfig(
    val assetPath: String,
    val originalWidth: Int,
    val originalHeight: Int,
    val transparentAreaRatio: Float = 0.60f,  // 默认60%为透明区域
    val offsetX: Float = 0f,  // 默认居中
    val offsetY: Float = 0f   // 默认居中
) {
    /**
     * 获取头像尺寸（基于显示尺寸）
     */
    fun getAvatarSize(displaySize: Float): Float {
        return displaySize * transparentAreaRatio
    }
    
    /**
     * 获取头像X轴偏移量（基于显示尺寸）
     */
    fun getAvatarOffsetX(displaySize: Float): Float {
        return displaySize * offsetX
    }
    
    /**
     * 获取头像Y轴偏移量（基于显示尺寸）
     */
    fun getAvatarOffsetY(displaySize: Float): Float {
        return displaySize * offsetY
    }
}

/**
 * 头像框配置管理器
 * 
 * 商业级别的解决方案：
 * 1. 预定义常见头像框的配置
 * 2. 支持动态添加新配置
 * 3. 自动回退到默认配置
 */
object AvatarFrameConfigManager {
    
    // 预定义的头像框配置
    private val configs = mutableMapOf<String, AvatarFrameConfig>()
    
    // 默认配置（适用于大多数标准头像框）
    private val defaultConfig = AvatarFrameConfig(
        assetPath = "",
        originalWidth = 600,
        originalHeight = 600,
        transparentAreaRatio = 0.60f,  // 60%透明区域
        offsetX = 0f,
        offsetY = 0f
    )
    
    init {
        // 使用自动生成的配置
        AvatarFrameConfigGeneratedInitializer.initializeGeneratedConfigs()
    }
    
    /**
     * 初始化预定义的头像框配置
     * 
     * 根据实际PNG图片的尺寸和设计，为每个头像框系列定义配置
     */
    private fun initializeConfigs() {
        // 头像框1系列 - 标准圆形框（600x600px左右，透明区域60%）
        addConfigForSeries("xiangkuang/头像框1", 
            originalWidth = 600,
            originalHeight = 600,
            transparentAreaRatio = 0.60f
        )
        
        // 头像框2系列 - 透明区域更大（需要更大的头像）
        addConfigForSeries("xiangkuang/头像框2",
            originalWidth = 585,
            originalHeight = 589,
            transparentAreaRatio = 0.68f  // 🔥 从0.58f增加到0.68f
        )
        
        // 头像框4系列
        addConfigForSeries("xiangkuang/头像框4",
            originalWidth = 600,
            originalHeight = 600,
            transparentAreaRatio = 0.62f
        )
        
        // 头像框5系列
        addConfigForSeries("xiangkuang/头像框5",
            originalWidth = 600,
            originalHeight = 600,
            transparentAreaRatio = 0.60f
        )
        
        // 头像框6系列
        addConfigForSeries("xiangkuang/头像框6",
            originalWidth = 600,
            originalHeight = 600,
            transparentAreaRatio = 0.60f
        )
        
        // 头像框7系列
        addConfigForSeries("xiangkuang/头像框7",
            originalWidth = 600,
            originalHeight = 600,
            transparentAreaRatio = 0.60f
        )
        
        // 头像框8系列
        addConfigForSeries("xiangkuang/头像框8",
            originalWidth = 600,
            originalHeight = 600,
            transparentAreaRatio = 0.60f
        )
        
        // A4头像框系列 - 可能有特殊尺寸
        addConfigForSeries("xiangkuang/A4头像框",
            originalWidth = 600,
            originalHeight = 600,
            transparentAreaRatio = 0.65f
        )
    }
    
    /**
     * 为整个系列添加配置（系列中的所有PNG使用相同配置）
     */
    fun addConfigForSeries(
        seriesPath: String,
        originalWidth: Int,
        originalHeight: Int,
        transparentAreaRatio: Float,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ) {
        val config = AvatarFrameConfig(
            assetPath = seriesPath,
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            transparentAreaRatio = transparentAreaRatio,
            offsetX = offsetX,
            offsetY = offsetY
        )
        configs[seriesPath] = config
    }
    
    /**
     * 添加单个头像框的配置
     */
    fun addConfig(config: AvatarFrameConfig) {
        configs[config.assetPath] = config
    }
    
    /**
     * 获取头像框配置
     * 
     * 智能匹配：
     * 1. 精确匹配完整路径
     * 2. 匹配系列路径（如 "xiangkuang/头像框1"）
     * 3. 回退到默认配置
     */
    fun getConfig(assetPath: String): AvatarFrameConfig {
        // 1. 精确匹配
        configs[assetPath]?.let { return it }
        
        // 2. 匹配系列路径（提取目录部分）
        val seriesPath = assetPath.substringBeforeLast("/", "")
        if (seriesPath.isNotEmpty()) {
            configs[seriesPath]?.let { 
                return it.copy(assetPath = assetPath)
            }
        }
        
        // 3. 回退到默认配置
        return defaultConfig.copy(assetPath = assetPath)
    }
    
    /**
     * 批量更新配置（用于从服务器同步配置）
     */
    fun updateConfigs(newConfigs: List<AvatarFrameConfig>) {
        newConfigs.forEach { config ->
            configs[config.assetPath] = config
        }
    }
    
    /**
     * 清除所有配置（用于重置）
     */
    fun clearConfigs() {
        configs.clear()
        initializeConfigs()
    }
}
