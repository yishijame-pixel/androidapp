// ShopItem.kt - 商城商品数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_items")
data class ShopItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,           // 商品名称
    val description: String,    // 商品描述
    val icon: String,           // 图标emoji
    val price: Int,             // 价格（金币）
    val type: String,           // 类型：makeup_card, theme, badge等
    val value: Int = 1,         // 商品价值（如补卡卡片数量）
    val isAvailable: Boolean = true  // 是否可购买
)

@Entity(tableName = "purchase_history")
data class PurchaseHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val itemId: Int,            // 商品ID
    val itemName: String,       // 商品名称
    val price: Int,             // 购买价格
    val timestamp: String       // 购买时间
)
