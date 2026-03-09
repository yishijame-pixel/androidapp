// AnniversaryStatistics.kt - 纪念日统计数据模型
package com.example.funlife.data.model

data class AnniversaryStatistics(
    val totalCount: Int = 0,                    // 总数
    val upcomingCount: Int = 0,                 // 即将到来的数量
    val passedCount: Int = 0,                   // 已过去的数量
    val todayCount: Int = 0,                    // 今天的数量
    val thisWeekCount: Int = 0,                 // 本周的数量
    val thisMonthCount: Int = 0,                // 本月的数量
    val nearestAnniversary: Anniversary? = null // 最近的纪念日
)

data class ScoreStatistics(
    val totalPlayers: Int = 0,                  // 总玩家数
    val totalGames: Int = 0,                    // 总游戏数
    val highestScore: Int = 0,                  // 最高分
    val averageScore: Double = 0.0,             // 平均分
    val topPlayer: Player? = null,              // 最高分玩家
    val mostActivePlayer: String = ""           // 最活跃玩家
)

data class SpinWheelStatistics(
    val totalSpins: Int = 0,                    // 总旋转次数
    val totalTemplates: Int = 0,                // 总模板数
    val mostUsedTemplate: SpinWheelTemplate? = null,  // 最常用模板
    val recentResults: List<String> = emptyList()     // 最近结果
)
