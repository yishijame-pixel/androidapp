// SpinWheelHistory.kt - 转盘历史记录数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spin_wheel_history")
data class SpinWheelHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val templateId: Int? = null,        // 关联的模板ID（可选）
    val templateName: String,           // 模板名称
    val result: String,                 // 转盘结果
    val allOptions: String,             // 所有选项（逗号分隔）
    val mode: String = "NORMAL",        // 转盘模式
    val coinCost: Int = 0,              // 消耗的金币
    val coinReward: Int = 0,            // 获得的金币奖励
    val timestamp: Long = System.currentTimeMillis() // 时间戳
)
