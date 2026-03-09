// SpinWheelMode.kt - 转盘模式枚举
package com.example.funlife.data.model

enum class SpinWheelMode(
    val displayName: String,
    val description: String,
    val costPerSpin: Int,
    val emoji: String,
    val features: List<String>
) {
    NORMAL(
        displayName = "普通模式",
        description = "免费使用，纯随机抽取",
        costPerSpin = 0,
        emoji = "🎯",
        features = listOf("完全免费", "纯随机", "简单快捷")
    ),
    ADVANCED(
        displayName = "进阶模式",
        description = "支持权重、排除、统计功能",
        costPerSpin = 5,
        emoji = "⚙️",
        features = listOf("权重系统", "排除选项", "数据统计", "消耗5金币/次")
    ),
    LUCKY(
        displayName = "幸运模式",
        description = "有机会获得金币奖励",
        costPerSpin = 10,
        emoji = "🍀",
        features = listOf("金币奖励", "特殊奖池", "高级统计", "消耗10金币/次")
    );
    
    fun canAfford(currentCoins: Int): Boolean {
        return currentCoins >= costPerSpin
    }
}
