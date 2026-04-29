// PetItem.kt - 宠物物品数据模型
package com.example.funlife.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pet_items")
data class PetItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val itemId: Int,                     // 物品ID
    val itemType: ItemType,              // 物品类型
    val itemName: String,                // 物品名称
    val quantity: Int = 1,               // 数量
    val acquiredAt: Long = System.currentTimeMillis()
)

// 物品类型
enum class ItemType {
    FOOD,        // 食物
    TOY,         // 玩具
    DECORATION,  // 装饰
    MEDICINE     // 药品
}

// 预定义物品
object PetItems {
    // 食物
    val BASIC_FOOD = ItemDefinition(1, "普通食物", ItemType.FOOD, 10, 20)
    val PREMIUM_FOOD = ItemDefinition(2, "高级食物", ItemType.FOOD, 30, 40)
    val SNACK = ItemDefinition(3, "特殊零食", ItemType.FOOD, 50, 20, moodBonus = 10)
    
    // 玩具
    val BALL = ItemDefinition(11, "小球", ItemType.TOY, 20)
    val FRISBEE = ItemDefinition(12, "飞盘", ItemType.TOY, 40)
    val CAT_WAND = ItemDefinition(13, "逗猫棒", ItemType.TOY, 30)
    
    // 装饰
    val HAT_RED = ItemDefinition(21, "红色帽子", ItemType.DECORATION, 50)
    val HAT_BLUE = ItemDefinition(22, "蓝色帽子", ItemType.DECORATION, 50)
    val SCARF = ItemDefinition(23, "围巾", ItemType.DECORATION, 80)
    val GLASSES = ItemDefinition(24, "眼镜", ItemType.DECORATION, 100)
    
    // 药品
    val MEDICINE = ItemDefinition(31, "治疗药", ItemType.MEDICINE, 100, healthBonus = 50)
    val NUTRITION = ItemDefinition(32, "营养品", ItemType.MEDICINE, 80, allBonus = 10)
    
    fun getAllItems(): List<ItemDefinition> {
        return listOf(
            BASIC_FOOD, PREMIUM_FOOD, SNACK,
            BALL, FRISBEE, CAT_WAND,
            HAT_RED, HAT_BLUE, SCARF, GLASSES,
            MEDICINE, NUTRITION
        )
    }
    
    fun getItemById(id: Int): ItemDefinition? {
        return getAllItems().find { it.id == id }
    }
}

// 物品定义
data class ItemDefinition(
    val id: Int,
    val name: String,
    val type: ItemType,
    val price: Int,
    val hungerBonus: Int = 0,
    val cleanBonus: Int = 0,
    val moodBonus: Int = 0,
    val healthBonus: Int = 0,
    val allBonus: Int = 0  // 全属性加成
)
