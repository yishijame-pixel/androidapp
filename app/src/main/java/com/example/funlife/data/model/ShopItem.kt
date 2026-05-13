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
    val price: Int,             // 普通用户价格（金币）
    val vipPrice: Int = price,  // 🔥 VIP用户价格（默认与普通价格相同）
    val type: String,           // 类型：makeup_card, theme, badge, avatar_frame等
    val value: Int = 1,         // 商品价值（如补卡卡片数量）
    val isAvailable: Boolean = true,  // 是否可购买
    
    // 🔥 新增字段 - 头像框专用
    val assetPath: String? = null,     // 资源路径：xiangkuang/头像框1/xxx.png
    val rarity: String = "COMMON",     // 稀有度：COMMON/RARE/EPIC/LEGENDARY
    val isAnimated: Boolean = false,   // 是否为GIF动态框
    val category: String? = null,      // 分类：头像框1/头像框2/...
    val sortOrder: Int = 0             // 排序顺序
)

@Entity(tableName = "purchase_history")
data class PurchaseHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Long = 0,       // 🔥 用户ID
    val itemId: Int,            // 商品ID
    val itemName: String,       // 商品名称
    val price: Int,             // 购买价格
    val timestamp: String       // 购买时间
)
