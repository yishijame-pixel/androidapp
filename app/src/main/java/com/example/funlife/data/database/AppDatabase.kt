// AppDatabase.kt - 应用数据库
package com.example.funlife.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.funlife.data.dao.AnniversaryDao
import com.example.funlife.data.dao.PlayerDao
import com.example.funlife.data.dao.GameHistoryDao
import com.example.funlife.data.dao.UserPreferencesDao
import com.example.funlife.data.dao.SpinWheelTemplateDao
import com.example.funlife.data.dao.SpinWheelHistoryDao
import com.example.funlife.data.dao.HabitDao
import com.example.funlife.data.dao.MoodDao
import com.example.funlife.data.dao.GoalDao
import com.example.funlife.data.dao.CoinDao
import com.example.funlife.data.dao.ShopDao
import com.example.funlife.data.dao.GuaranteeCounterDao
import com.example.funlife.data.dao.CustomSpinModeDao
import com.example.funlife.data.model.Anniversary
import com.example.funlife.data.model.Player
import com.example.funlife.data.model.GameHistory
import com.example.funlife.data.model.UserPreferences
import com.example.funlife.data.model.SpinWheelTemplate
import com.example.funlife.data.model.SpinWheelHistory
import com.example.funlife.data.model.Habit
import com.example.funlife.data.model.HabitRecord
import com.example.funlife.data.model.MoodEntry
import com.example.funlife.data.model.Goal
import com.example.funlife.data.model.Countdown
import com.example.funlife.data.model.UserCoins
import com.example.funlife.data.model.ShopItem
import com.example.funlife.data.model.PurchaseHistory
import com.example.funlife.data.model.GuaranteeCounter
import com.example.funlife.data.model.CustomSpinMode

@Database(
    entities = [
        Anniversary::class, 
        Player::class,
        GameHistory::class,
        UserPreferences::class,
        SpinWheelTemplate::class,
        SpinWheelHistory::class,
        Habit::class,
        HabitRecord::class,
        MoodEntry::class,
        Goal::class,
        Countdown::class,
        UserCoins::class,
        ShopItem::class,
        PurchaseHistory::class,
        GuaranteeCounter::class,
        CustomSpinMode::class
    ],
    version = 11,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun anniversaryDao(): AnniversaryDao
    abstract fun playerDao(): PlayerDao
    abstract fun gameHistoryDao(): GameHistoryDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun spinWheelTemplateDao(): SpinWheelTemplateDao
    abstract fun spinWheelHistoryDao(): SpinWheelHistoryDao
    abstract fun habitDao(): HabitDao
    abstract fun moodDao(): MoodDao
    abstract fun goalDao(): GoalDao
    abstract fun coinDao(): CoinDao
    abstract fun shopDao(): ShopDao
    abstract fun guaranteeCounterDao(): GuaranteeCounterDao
    abstract fun customSpinModeDao(): CustomSpinModeDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // 数据库迁移策略
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建游戏历史表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS game_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        gameType TEXT NOT NULL,
                        playerName TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        result TEXT NOT NULL,
                        timestamp TEXT NOT NULL
                    )
                """)
                
                // 创建用户偏好表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_preferences (
                        id INTEGER PRIMARY KEY NOT NULL,
                        isDarkMode INTEGER NOT NULL,
                        enableNotifications INTEGER NOT NULL,
                        notificationDaysBefore INTEGER NOT NULL,
                        defaultScoreIncrement INTEGER NOT NULL,
                        enableSound INTEGER NOT NULL,
                        enableVibration INTEGER NOT NULL,
                        autoBackup INTEGER NOT NULL,
                        language TEXT NOT NULL,
                        sortOrder TEXT NOT NULL
                    )
                """)
                
                // 创建转盘模板表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS spin_wheel_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        options TEXT NOT NULL,
                        category TEXT NOT NULL,
                        isDefault INTEGER NOT NULL,
                        usageCount INTEGER NOT NULL,
                        createdAt TEXT NOT NULL
                    )
                """)
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为 anniversaries 表添加 imageUri 列
                database.execSQL("""
                    ALTER TABLE anniversaries ADD COLUMN imageUri TEXT
                """)
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为 anniversaries 表添加 isPinned 列
                database.execSQL("""
                    ALTER TABLE anniversaries ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0
                """)
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建习惯表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS habits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        color TEXT NOT NULL,
                        targetDays INTEGER NOT NULL,
                        createdAt TEXT NOT NULL,
                        isActive INTEGER NOT NULL
                    )
                """)
                
                // 创建习惯打卡记录表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        habitId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        note TEXT NOT NULL,
                        timestamp TEXT NOT NULL
                    )
                """)
                
                // 创建心情日记表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS mood_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        mood TEXT NOT NULL,
                        moodLevel INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        tags TEXT NOT NULL,
                        timestamp TEXT NOT NULL
                    )
                """)
                
                // 创建目标表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        category TEXT NOT NULL,
                        targetDate TEXT,
                        progress INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        createdAt TEXT NOT NULL,
                        completedAt TEXT
                    )
                """)
                
                // 创建倒数日表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS countdowns (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        targetDate TEXT NOT NULL,
                        category TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        color TEXT NOT NULL,
                        note TEXT NOT NULL,
                        createdAt TEXT NOT NULL
                    )
                """)
            }
        }
        
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加补卡卡片字段到习惯表
                database.execSQL("""
                    ALTER TABLE habits ADD COLUMN makeupCards INTEGER NOT NULL DEFAULT 0
                """)
            }
        }
        
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建用户金币表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_coins (
                        id INTEGER PRIMARY KEY NOT NULL,
                        coins INTEGER NOT NULL DEFAULT 0,
                        totalEarned INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // 初始化金币记录
                database.execSQL("INSERT OR IGNORE INTO user_coins (id, coins, totalEarned) VALUES (1, 0, 0)")
                
                // 创建商城商品表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shop_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        price INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        value INTEGER NOT NULL DEFAULT 1,
                        isAvailable INTEGER NOT NULL DEFAULT 1
                    )
                """)
                
                // 创建购买历史表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS purchase_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        itemName TEXT NOT NULL,
                        price INTEGER NOT NULL,
                        timestamp TEXT NOT NULL
                    )
                """)
                
                // 插入初始商品
                database.execSQL("""
                    INSERT INTO shop_items (name, description, icon, price, type, value) VALUES
                    ('补卡卡片', '可以补打卡一次', '🎫', 50, 'makeup_card', 1),
                    ('补卡卡片包(5张)', '一次获得5张补卡卡片', '🎫', 200, 'makeup_card', 5),
                    ('金币礼包', '获得100金币', '💰', 0, 'coins', 100),
                    ('幸运徽章', '展示你的坚持', '🏆', 300, 'badge', 1)
                """)
            }
        }
        
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为 anniversaries 表添加新字段
                database.execSQL("ALTER TABLE anniversaries ADD COLUMN type TEXT NOT NULL DEFAULT 'CUSTOM'")
                database.execSQL("ALTER TABLE anniversaries ADD COLUMN isYearly INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE anniversaries ADD COLUMN note TEXT")
                database.execSQL("ALTER TABLE anniversaries ADD COLUMN importance INTEGER NOT NULL DEFAULT 3")
            }
        }
        
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为 spin_wheel_templates 表添加 weights 字段
                database.execSQL("ALTER TABLE spin_wheel_templates ADD COLUMN weights TEXT NOT NULL DEFAULT ''")
                
                // 创建转盘历史记录表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS spin_wheel_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        templateId INTEGER,
                        templateName TEXT NOT NULL,
                        result TEXT NOT NULL,
                        allOptions TEXT NOT NULL,
                        mode TEXT NOT NULL DEFAULT 'NORMAL',
                        coinCost INTEGER NOT NULL DEFAULT 0,
                        coinReward INTEGER NOT NULL DEFAULT 0,
                        timestamp INTEGER NOT NULL
                    )
                """)
            }
        }
        
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建保底计数器表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS guarantee_counter (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        optionText TEXT NOT NULL,
                        currentCount INTEGER NOT NULL DEFAULT 0,
                        guaranteeThreshold INTEGER NOT NULL DEFAULT 10,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        lastUpdated INTEGER NOT NULL
                    )
                """)
            }
        }
        
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建自定义转盘模式表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_spin_modes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        emoji TEXT NOT NULL DEFAULT '🎲',
                        description TEXT NOT NULL DEFAULT '',
                        costPerSpin INTEGER NOT NULL DEFAULT 0,
                        hasReward INTEGER NOT NULL DEFAULT 0,
                        rewardMultiplier REAL NOT NULL DEFAULT 1.0,
                        primaryColor TEXT NOT NULL DEFAULT '#6366F1',
                        secondaryColor TEXT NOT NULL DEFAULT '#8B5CF6',
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        usageCount INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                
                // 插入默认模式
                database.execSQL("""
                    INSERT INTO custom_spin_modes (name, emoji, description, costPerSpin, hasReward, rewardMultiplier, primaryColor, secondaryColor, isDefault, createdAt, updatedAt)
                    VALUES 
                    ('普通模式', '🎯', '免费使用，适合日常决策', 0, 0, 1.0, '#6366F1', '#8B5CF6', 1, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}),
                    ('进阶模式', '⚡', '消耗金币，体验更刺激', 10, 0, 1.0, '#6366F1', '#4F46E5', 1, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}),
                    ('幸运模式', '💰', '消耗金币，有机会获得奖励', 20, 1, 1.5, '#FFD700', '#FFA500', 1, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
                """)
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "funlife_database"
                )
                .addMigrations(
                    MIGRATION_1_2, 
                    MIGRATION_2_3, 
                    MIGRATION_3_4, 
                    MIGRATION_4_5, 
                    MIGRATION_5_6, 
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
