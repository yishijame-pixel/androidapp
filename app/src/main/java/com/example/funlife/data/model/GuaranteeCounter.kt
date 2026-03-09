// GuaranteeCounter.kt - 保底计数器数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 保底计数器
 * 用于追踪特定选项的未中次数，达到保底次数后必中
 */
@Entity(tableName = "guarantee_counter")
data class GuaranteeCounter(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val optionText: String,              // 选项文本
    val currentCount: Int = 0,           // 当前未中次数
    val guaranteeThreshold: Int = 10,    // 保底阈值（默认10次）
    val isEnabled: Boolean = true,       // 是否启用保底
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * 保底设置
 */
data class GuaranteeSettings(
    val enabled: Boolean = false,        // 是否启用保底系统
    val defaultThreshold: Int = 10,      // 默认保底次数
    val showCounter: Boolean = true,     // 是否显示计数器
    val guaranteeAnimation: Boolean = true // 保底触发时是否显示特殊动画
)
