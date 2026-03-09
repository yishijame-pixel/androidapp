// SpinWheelTemplate.kt - 转盘模板数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spin_wheel_templates")
data class SpinWheelTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,                   // 模板名称
    val options: String,                // 选项列表（用逗号分隔）
    val weights: String = "",           // 权重列表（用逗号分隔，与options对应）
    val category: String = "custom",    // 分类：custom, food, game, decision
    val isDefault: Boolean = false,     // 是否为默认模板
    val usageCount: Int = 0,           // 使用次数
    val createdAt: String = ""         // 创建时间
) {
    // 获取选项列表
    fun getOptionsList(): List<String> {
        return options.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    
    // 获取权重列表
    fun getWeightsList(): List<Int> {
        if (weights.isEmpty()) {
            // 如果没有权重，返回全1
            return getOptionsList().map { 1 }
        }
        return weights.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
    
    // 获取带权重的选项列表
    fun getWheelOptions(): List<WheelOption> {
        val optionsList = getOptionsList()
        val weightsList = getWeightsList()
        
        return optionsList.mapIndexed { index, option ->
            val weight = weightsList.getOrNull(index) ?: 1
            WheelOption(text = option, weight = weight)
        }
    }
    
    // 从列表创建选项字符串
    companion object {
        fun createOptionsString(optionsList: List<String>): String {
            return optionsList.joinToString(",")
        }
        
        fun createWeightsString(weightsList: List<Int>): String {
            return weightsList.joinToString(",")
        }
    }
}

