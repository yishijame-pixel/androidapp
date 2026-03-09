// WheelOption.kt - 转盘选项（带权重）
package com.example.funlife.data.model

data class WheelOption(
    val text: String,
    val weight: Int = 1,  // 权重，默认为1（相等概率）
    val isExcluded: Boolean = false,  // 是否被排除
    val coinReward: Int = 0  // 金币奖励（幸运模式）
) {
    // 获取显示文本（包含权重提示）
    fun getDisplayText(showWeight: Boolean = false): String {
        return if (showWeight && weight > 1) {
            "$text (${weight}x)"
        } else {
            text
        }
    }
    
    // 获取实际概率百分比
    fun getProbability(totalWeight: Int): Float {
        return if (totalWeight > 0) {
            (weight.toFloat() / totalWeight) * 100
        } else {
            0f
        }
    }
}

// 转盘选项统计
data class OptionStatistics(
    val option: String,
    val hitCount: Int = 0,
    val lastHitTime: Long? = null
) {
    fun getHitRate(totalSpins: Int): Float {
        return if (totalSpins > 0) {
            (hitCount.toFloat() / totalSpins) * 100
        } else {
            0f
        }
    }
}
