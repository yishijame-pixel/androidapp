// Converters.kt - Room类型转换器
package com.example.funlife.data.database

import androidx.room.TypeConverter
import com.example.funlife.data.model.InventoryItemType
import com.example.funlife.data.model.ItemRarity

class Converters {
    
    @TypeConverter
    fun fromInventoryItemType(value: InventoryItemType): String {
        return value.name
    }
    
    @TypeConverter
    fun toInventoryItemType(value: String): InventoryItemType {
        return InventoryItemType.valueOf(value)
    }
    
    @TypeConverter
    fun fromItemRarity(value: ItemRarity): String {
        return value.name
    }
    
    @TypeConverter
    fun toItemRarity(value: String): ItemRarity {
        return ItemRarity.valueOf(value)
    }
}
