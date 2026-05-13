// AnniversaryViewMode.kt - 纪念日视图模式
package com.example.funlife.data.model

enum class AnniversaryViewMode(
    val displayName: String,
    val icon: String,
    val description: String
) {
    LIST("列表视图", "📋", "经典列表，详细展示"),
    GRID("网格视图", "⊞", "紧凑网格，快速浏览"),
    WATERFALL("瀑布流", "🌊", "错落有致，个性展示"),
    MEMORY_WALL("回忆墙", "📸", "照片墙风格，随机倾斜"),
    TIMELINE("时间轴", "⏰", "按时间排列，清晰明了")
}
