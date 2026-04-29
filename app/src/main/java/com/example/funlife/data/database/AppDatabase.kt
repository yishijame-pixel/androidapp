// AppDatabase.kt - 应用数据库
package com.example.funlife.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.funlife.data.dao.AnniversaryDao
import com.example.funlife.data.dao.AnniversaryReminderDao
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
import com.example.funlife.data.dao.OperationLogDao
import com.example.funlife.data.dao.DailyRewardDao
import com.example.funlife.data.model.Anniversary
import com.example.funlife.data.model.AnniversaryReminder
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
import com.example.funlife.data.model.OperationLog
import com.example.funlife.data.model.DailyReward

@Database(
    entities = [
        Anniversary::class,
        AnniversaryReminder::class,
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
        CustomSpinMode::class,
        com.example.funlife.data.model.User::class,
        OperationLog::class,
        com.example.funlife.data.model.Pet::class,
        com.example.funlife.data.model.PetItem::class,
        DailyReward::class,
        com.example.funlife.data.model.Riddle::class,
        com.example.funlife.data.model.RiddleProgress::class,
        com.example.funlife.data.model.RiddleStats::class
    ],
    version = 24,  // 🔥 升级到版本24 - 添加猜谜游戏系统
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun anniversaryDao(): AnniversaryDao
    abstract fun anniversaryReminderDao(): AnniversaryReminderDao
    abstract fun playerDao(): PlayerDao
    abstract fun gameHistoryDao(): GameHistoryDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun spinWheelTemplateDao(): SpinWheelTemplateDao
    abstract fun spinWheelHistoryDao(): SpinWheelHistoryDao
    abstract fun habitDao(): HabitDao
    abstract fun userDao(): com.example.funlife.data.dao.UserDao
    abstract fun moodDao(): MoodDao
    abstract fun goalDao(): GoalDao
    abstract fun coinDao(): CoinDao
    abstract fun shopDao(): ShopDao
    abstract fun guaranteeCounterDao(): GuaranteeCounterDao
    abstract fun customSpinModeDao(): CustomSpinModeDao
    abstract fun operationLogDao(): OperationLogDao
    abstract fun petDao(): com.example.funlife.data.dao.PetDao
    abstract fun petItemDao(): com.example.funlife.data.dao.PetItemDao
    abstract fun dailyRewardDao(): DailyRewardDao
    abstract fun riddleDao(): com.example.funlife.data.dao.RiddleDao
    abstract fun riddleProgressDao(): com.example.funlife.data.dao.RiddleProgressDao
    abstract fun riddleStatsDao(): com.example.funlife.data.dao.RiddleStatsDao
    
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
        
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建用户表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        username TEXT NOT NULL UNIQUE,
                        password TEXT NOT NULL,
                        email TEXT NOT NULL DEFAULT '',
                        nickname TEXT NOT NULL DEFAULT '',
                        avatar TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        lastLoginAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }
        
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建操作日志表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS operation_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL DEFAULT 0,
                        operation TEXT NOT NULL,
                        details TEXT NOT NULL,
                        result TEXT NOT NULL,
                        errorMessage TEXT NOT NULL DEFAULT '',
                        timestamp INTEGER NOT NULL
                    )
                """)
                
                // 创建索引以提高查询性能
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_operation_logs_userId 
                    ON operation_logs(userId)
                """)
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_operation_logs_operation 
                    ON operation_logs(operation)
                """)
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_operation_logs_timestamp 
                    ON operation_logs(timestamp)
                """)
            }
        }
        
        // 🔥 多用户数据隔离迁移
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 第一步：为所有表添加 userId 列
                try {
                    database.execSQL("ALTER TABLE game_history ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列可能已存在，忽略错误
                }
                
                try {
                    database.execSQL("ALTER TABLE players ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列可能已存在，忽略错误
                }
                
                try {
                    database.execSQL("ALTER TABLE anniversaries ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列可能已存在，忽略错误
                }
                
                try {
                    database.execSQL("ALTER TABLE spin_wheel_history ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列可能已存在，忽略错误
                }
                
                try {
                    database.execSQL("ALTER TABLE spin_wheel_templates ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列可能已存在，忽略错误
                }
                
                try {
                    database.execSQL("ALTER TABLE guarantee_counter ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列可能已存在，忽略错误
                }
                
                try {
                    database.execSQL("ALTER TABLE custom_spin_modes ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列可能已存在，忽略错误
                }
                
                try {
                    database.execSQL("ALTER TABLE habits ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列可能已存在，忽略错误
                }
                
                try {
                    database.execSQL("ALTER TABLE habit_records ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列可能已存在，忽略错误
                }
                
                try {
                    database.execSQL("ALTER TABLE mood_entries ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列可能已存在，忽略错误
                }
                
                try {
                    database.execSQL("ALTER TABLE goals ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列可能已存在，忽略错误
                }
                
                try {
                    database.execSQL("ALTER TABLE countdowns ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列可能已存在，忽略错误
                }
                
                try {
                    database.execSQL("ALTER TABLE user_coins ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列可能已存在，忽略错误
                }
                
                try {
                    database.execSQL("ALTER TABLE purchase_history ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列可能已存在，忽略错误
                }
                
                // 第二步：创建索引以提高查询性能
                database.execSQL("CREATE INDEX IF NOT EXISTS index_game_history_userId ON game_history(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_players_userId ON players(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_anniversaries_userId ON anniversaries(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_spin_wheel_history_userId ON spin_wheel_history(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_spin_wheel_templates_userId ON spin_wheel_templates(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_guarantee_counters_userId ON guarantee_counter(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_custom_spin_modes_userId ON custom_spin_modes(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_habits_userId ON habits(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_habit_records_userId ON habit_records(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_mood_entries_userId ON mood_entries(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_goals_userId ON goals(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_countdowns_userId ON countdowns(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_coins_userId ON user_coins(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_history_userId ON purchase_history(userId)")
            }
        }
        
        // 🔥 UserPreferences多用户支持和转盘设置持久化
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 步骤1：创建新的user_preferences表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_preferences_new (
                        userId INTEGER PRIMARY KEY NOT NULL,
                        isDarkMode INTEGER NOT NULL DEFAULT 0,
                        enableNotifications INTEGER NOT NULL DEFAULT 1,
                        notificationDaysBefore INTEGER NOT NULL DEFAULT 7,
                        defaultScoreIncrement INTEGER NOT NULL DEFAULT 1,
                        enableSound INTEGER NOT NULL DEFAULT 1,
                        enableVibration INTEGER NOT NULL DEFAULT 1,
                        autoBackup INTEGER NOT NULL DEFAULT 0,
                        language TEXT NOT NULL DEFAULT 'zh',
                        sortOrder TEXT NOT NULL DEFAULT 'date_asc',
                        wheelTheme TEXT NOT NULL DEFAULT 'default',
                        showWeightVisualization INTEGER NOT NULL DEFAULT 0,
                        particleEffectEnabled INTEGER NOT NULL DEFAULT 1,
                        fireworksEnabled INTEGER NOT NULL DEFAULT 1,
                        coinAnimationEnabled INTEGER NOT NULL DEFAULT 1,
                        lastTemplateId INTEGER,
                        lastCustomOptions TEXT NOT NULL DEFAULT '',
                        lastSpinMode TEXT NOT NULL DEFAULT 'NORMAL'
                    )
                """)
                
                // 步骤2：迁移旧数据（如果存在）
                // 注意：旧表使用id=1，新表使用userId，需要手动处理
                database.execSQL("""
                    INSERT OR IGNORE INTO user_preferences_new 
                    (userId, isDarkMode, enableNotifications, notificationDaysBefore, 
                     defaultScoreIncrement, enableSound, enableVibration, autoBackup, language, sortOrder)
                    SELECT 0, isDarkMode, enableNotifications, notificationDaysBefore,
                           defaultScoreIncrement, enableSound, enableVibration, autoBackup, language, sortOrder
                    FROM user_preferences WHERE id = 1
                """)
                
                // 步骤3：删除旧表
                database.execSQL("DROP TABLE IF EXISTS user_preferences")
                
                // 步骤4：重命名新表
                database.execSQL("ALTER TABLE user_preferences_new RENAME TO user_preferences")
                
                // 步骤5：创建索引
                database.execSQL("CREATE INDEX IF NOT EXISTS index_user_preferences_userId ON user_preferences(userId)")
            }
        }
        
        // 🔥 新增：版本15到16 - 添加lastCustomModeId字段
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加lastCustomModeId列到user_preferences表
                database.execSQL("""
                    ALTER TABLE user_preferences 
                    ADD COLUMN lastCustomModeId INTEGER DEFAULT NULL
                """.trimIndent())
            }
        }
        
        // 🔥 新增：版本16到17 - 添加纪念日提醒表
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建纪念日提醒表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS anniversary_reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        anniversaryId INTEGER NOT NULL,
                        userId INTEGER NOT NULL,
                        daysBeforeList TEXT NOT NULL DEFAULT '1,3,7',
                        reminderTime TEXT NOT NULL DEFAULT '09:00',
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        notifyOnDay INTEGER NOT NULL DEFAULT 1
                    )
                """)
                
                // 创建索引
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_anniversary_reminders_userId 
                    ON anniversary_reminders(userId)
                """)
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_anniversary_reminders_anniversaryId 
                    ON anniversary_reminders(anniversaryId)
                """)
            }
        }
        
        // 🔥 新增：版本17到18 - 添加自定义排序字段
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加customOrder列到anniversaries表
                database.execSQL("""
                    ALTER TABLE anniversaries 
                    ADD COLUMN customOrder INTEGER NOT NULL DEFAULT 0
                """)
            }
        }
        
        // 🔥 新增：版本18到19 - 添加模式独立选项配置
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加三个模式各自的选项存储字段
                database.execSQL("""
                    ALTER TABLE user_preferences 
                    ADD COLUMN normalModeOptions TEXT NOT NULL DEFAULT ''
                """)
                database.execSQL("""
                    ALTER TABLE user_preferences 
                    ADD COLUMN advancedModeOptions TEXT NOT NULL DEFAULT ''
                """)
                database.execSQL("""
                    ALTER TABLE user_preferences 
                    ADD COLUMN luckyModeOptions TEXT NOT NULL DEFAULT ''
                """)
            }
        }
        
        // 🔥 新增：版本19到20 - 添加首页面板自定义文字
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE user_preferences 
                    ADD COLUMN homePanelText TEXT NOT NULL DEFAULT '少女心面板'
                """)
            }
        }
        
        // 🔥 新增：版本20到21 - 添加艺术字颜色主题
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE user_preferences 
                    ADD COLUMN homePanelTextStyle TEXT NOT NULL DEFAULT 'pink'
                """)
            }
        }
        
        // 🔥 新增：版本21到22 - 添加宠物系统
        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建宠物表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS pets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        level INTEGER NOT NULL DEFAULT 1,
                        experience INTEGER NOT NULL DEFAULT 0,
                        hungerValue INTEGER NOT NULL DEFAULT 100,
                        cleanValue INTEGER NOT NULL DEFAULT 100,
                        moodValue INTEGER NOT NULL DEFAULT 100,
                        healthValue INTEGER NOT NULL DEFAULT 100,
                        intimacy INTEGER NOT NULL DEFAULT 0,
                        birthday INTEGER NOT NULL,
                        lastFeedTime INTEGER NOT NULL,
                        lastCleanTime INTEGER NOT NULL,
                        lastPlayTime INTEGER NOT NULL,
                        lastUpdateTime INTEGER NOT NULL,
                        appearance TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                
                // 创建宠物物品表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS pet_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        itemId INTEGER NOT NULL,
                        itemType TEXT NOT NULL,
                        itemName TEXT NOT NULL,
                        quantity INTEGER NOT NULL DEFAULT 1,
                        acquiredAt INTEGER NOT NULL
                    )
                """)
            }
        }
        
        // 🔥 新增：版本22到23 - 添加每日奖励系统
        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建每日奖励表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_rewards (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        rewardType TEXT NOT NULL,
                        lastClaimDate TEXT NOT NULL,
                        claimCount INTEGER NOT NULL DEFAULT 1
                    )
                """)
                
                // 创建索引
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_daily_rewards_userId_rewardType 
                    ON daily_rewards(userId, rewardType)
                """)
            }
        }
        
        // 🔥 新增：版本23到24 - 添加猜谜游戏系统
        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建谜题表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS riddles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        question TEXT NOT NULL,
                        answer TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT '脑筋急转弯',
                        difficulty INTEGER NOT NULL DEFAULT 1
                    )
                """)
                
                // 创建谜题进度表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS riddle_progress (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        riddleId INTEGER NOT NULL,
                        isAnswered INTEGER NOT NULL DEFAULT 0,
                        isCorrect INTEGER NOT NULL DEFAULT 0,
                        attempts INTEGER NOT NULL DEFAULT 0,
                        lastAttemptTime INTEGER NOT NULL
                    )
                """)
                
                // 创建谜题统计表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS riddle_stats (
                        userId INTEGER PRIMARY KEY NOT NULL,
                        totalAnswered INTEGER NOT NULL DEFAULT 0,
                        totalCorrect INTEGER NOT NULL DEFAULT 0,
                        currentStreak INTEGER NOT NULL DEFAULT 0,
                        maxStreak INTEGER NOT NULL DEFAULT 0,
                        totalScore INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // 创建索引
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_riddle_progress_userId_riddleId 
                    ON riddle_progress(userId, riddleId)
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
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,  // 🔥 新增
                    MIGRATION_18_19,  // 🔥 新增：模式独立选项
                    MIGRATION_19_20,  // 🔥 新增：首页面板自定义文字
                    MIGRATION_20_21,  // 🔥 新增：艺术字颜色主题
                    MIGRATION_21_22,  // 🔥 新增：宠物系统
                    MIGRATION_22_23,  // 🔥 新增：每日奖励系统
                    MIGRATION_23_24   // 🔥 新增：猜谜游戏系统
                )
                // 🔥 修复：移除破坏性降级，保护用户数据
                // .fallbackToDestructiveMigration()  // 已移除！
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
