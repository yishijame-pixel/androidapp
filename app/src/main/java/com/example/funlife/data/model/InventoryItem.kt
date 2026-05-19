// InventoryItem.kt - 背包物品数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 背包物品类型
 */
enum class InventoryItemType {
    FOOD,           // 食物
    TOY,            // 玩具
    DECORATION,     // 装饰品
    CONSUMABLE,     // 消耗品
    SPECIAL,        // 特殊物品
    PANEL_SKIN,     // 🔥 结算面板皮肤
    BUTTON_SKIN,    // 🔥 转盘按钮皮肤
    ANNIVERSARY_FRAME,  // 🔥 纪念日相框
    AVATAR_FRAME    // 🔥 头像框
}

/**
 * 物品稀有度
 */
enum class ItemRarity {
    COMMON,         // 普通
    RARE,           // 稀有
    EPIC,           // 史诗
    LEGENDARY       // 传说
}

/**
 * 背包物品实体
 */
@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val userId: Long,                // 🔒 安全修复：移除默认值 1，强制构造方传入，防止意外把物品挂到 user 1
    val itemId: String,              // 物品ID（对应商店物品）
    val itemName: String,            // 物品名称
    val itemType: InventoryItemType, // 物品类型
    val itemRarity: ItemRarity,      // 稀有度
    val iconEmoji: String,           // 图标emoji
    val description: String,         // 物品描述
    val quantity: Int = 1,           // 数量
    val isUsable: Boolean = true,    // 是否可使用
    val effectValue: Int = 0,        // 效果值（如恢复饥饿度等）
    val purchasePrice: Int = 0,      // 购买价格
    val obtainedTime: Long = System.currentTimeMillis()  // 获得时间
)
