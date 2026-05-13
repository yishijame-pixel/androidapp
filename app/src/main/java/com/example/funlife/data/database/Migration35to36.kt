// Migration35to36.kt - 数据库迁移：添加头像框商城系统
package com.example.funlife.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移：版本35到36
 * 添加头像框商城系统支持
 */
val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. 扩展 shop_items 表，添加头像框相关字段
        database.execSQL("""
            ALTER TABLE shop_items ADD COLUMN vipPrice INTEGER NOT NULL DEFAULT 0
        """)
        
        database.execSQL("""
            ALTER TABLE shop_items ADD COLUMN assetPath TEXT
        """)
        
        database.execSQL("""
            ALTER TABLE shop_items ADD COLUMN rarity TEXT NOT NULL DEFAULT 'COMMON'
        """)
        
        database.execSQL("""
            ALTER TABLE shop_items ADD COLUMN isAnimated INTEGER NOT NULL DEFAULT 0
        """)
        
        database.execSQL("""
            ALTER TABLE shop_items ADD COLUMN category TEXT
        """)
        
        database.execSQL("""
            ALTER TABLE shop_items ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0
        """)
        
        // 2. 创建用户头像框拥有表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS user_avatar_frames (
                userId INTEGER NOT NULL,
                frameId INTEGER NOT NULL,
                purchasedAt INTEGER NOT NULL,
                isEquipped INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(userId, frameId)
            )
        """)
        
        // 3. 扩展 users 表，添加头像框装备和VIP字段
        database.execSQL("""
            ALTER TABLE users ADD COLUMN equippedFrameId INTEGER
        """)
        
        database.execSQL("""
            ALTER TABLE users ADD COLUMN isVip INTEGER NOT NULL DEFAULT 0
        """)
        
        database.execSQL("""
            ALTER TABLE users ADD COLUMN vipExpireAt INTEGER
        """)
        
        // 4. 创建索引以提高查询性能
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_user_avatar_frames_userId 
            ON user_avatar_frames(userId)
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_user_avatar_frames_frameId 
            ON user_avatar_frames(frameId)
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_shop_items_type_rarity 
            ON shop_items(type, rarity)
        """)
        
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS index_shop_items_category 
            ON shop_items(category)
        """)
    }
}
