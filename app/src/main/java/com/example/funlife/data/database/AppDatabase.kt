// AppDatabase.kt - 应用数据库
package com.example.funlife.data.database

import android.content.Context
import com.example.funlife.BuildConfig
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.funlife.data.dao.AnniversaryDao
import com.example.funlife.data.dao.AnniversaryReminderDao
import com.example.funlife.data.dao.PlayerDao
import com.example.funlife.data.dao.PlayerVictoryRecordDao
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
import com.example.funlife.data.dao.InventoryDao
import com.example.funlife.data.dao.ScoreOperationDao
import com.example.funlife.data.model.Anniversary
import com.example.funlife.data.model.AnniversaryReminder
import com.example.funlife.data.model.Player
import com.example.funlife.data.model.PlayerVictoryRecord
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
import com.example.funlife.data.model.InventoryItem
import com.example.funlife.data.model.ScoreOperation
import com.example.funlife.data.model.UserVip
import com.example.funlife.data.model.RedeemCode
import com.example.funlife.data.model.UserRedeemHistory
import com.example.funlife.data.model.Bill
import com.example.funlife.data.model.ChatMessage
import com.example.funlife.data.model.ChatPersona
import com.example.funlife.data.model.ChatPersonaState
import com.example.funlife.data.dao.BillDao
import com.example.funlife.data.dao.ChatMessageDao
import com.example.funlife.data.dao.ChatPersonaDao
import com.example.funlife.data.dao.AccountDao
import com.example.funlife.data.dao.BudgetDao
import com.example.funlife.data.dao.RecurringBillDao

@Database(
    entities = [
        Anniversary::class,
        AnniversaryReminder::class,
        Player::class,
        PlayerVictoryRecord::class,
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
        com.example.funlife.data.model.RiddleStats::class,
        InventoryItem::class,
        ScoreOperation::class,
        UserVip::class,
        RedeemCode::class,
        UserRedeemHistory::class,
        com.example.funlife.data.model.UserAvatar::class,
        com.example.funlife.data.model.AvatarFrame::class,
        com.example.funlife.data.model.ProfileBackground::class,
        com.example.funlife.data.model.UserOwnedFrame::class,
        com.example.funlife.data.model.UserOwnedBackground::class,
        com.example.funlife.data.model.UserAvatarFrame::class,  // 🔥 新增：用户头像框表
        Bill::class,
        ChatMessage::class,
        ChatPersona::class,
        ChatPersonaState::class,
        com.example.funlife.data.model.Account::class,  // 🆕 v48：多账户系统
        com.example.funlife.data.model.Budget::class,   // 🆕 v49：预算系统
        com.example.funlife.data.model.RecurringBill::class,  // 🆕 v50：定期账单
        com.example.funlife.data.model.LetterRecipient::class,  // 🆕 v51：时光信箱（收信人）
        com.example.funlife.data.model.Letter::class,           // 🆕 v51：时光信箱（信件）
        com.example.funlife.data.model.Book::class,              // 🆕 v52：人生书架
        com.example.funlife.data.model.ReadingSession::class,     // 🆕 v53：阅读时长打卡
        com.example.funlife.data.model.Quote::class,              // 🆕 v53：摘抄 + 时光胶囊
        com.example.funlife.data.model.ReaderDnaCard::class,      // 🆕 v53：读者 DNA
        com.example.funlife.data.model.MorningHeraldLog::class,   // 🆕 v53：晨光信使日志
        com.example.funlife.data.model.SystemQuotaUsed::class,     // 🆕 v53：系统赠送配额
        com.example.funlife.data.model.BookChatSession::class,     // 🆕 v54：AI 读书伴侣长对话存档（VIP3）
        com.example.funlife.data.model.DiaryEntry::class,            // 🆕 v55：古籍日记本
        com.example.funlife.data.model.SocialPocketBaseLink::class,  // 🆕 v57：PocketBase 绑定
        com.example.funlife.data.model.SocialFriendCache::class,     // 🆕 v57：好友本地缓存
        com.example.funlife.data.model.SocialConversationCache::class, // 🆕 v58：私聊会话缓存
        com.example.funlife.data.model.SocialMessageCache::class,      // 🆕 v58：私聊消息缓存
        com.example.funlife.data.model.SocialGameRoomCache::class,     // 🆕 v59：趣玩中心房间缓存
        com.example.funlife.data.model.PacMazeProgress::class,         // 🆕 v64：豆人迷宫进度
        com.example.funlife.data.model.PlatformerClipCacheEntity::class, // 🆕 v67：横版 clip 磁盘索引
    ],
    version = 67,  // 🆕 v67 - platformer clip cache metadata (Room)
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun anniversaryDao(): AnniversaryDao
    abstract fun anniversaryReminderDao(): AnniversaryReminderDao
    abstract fun playerDao(): PlayerDao
    abstract fun playerVictoryRecordDao(): PlayerVictoryRecordDao
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
    abstract fun scoreOperationDao(): ScoreOperationDao
    abstract fun guaranteeCounterDao(): GuaranteeCounterDao
    abstract fun customSpinModeDao(): CustomSpinModeDao
    abstract fun operationLogDao(): OperationLogDao
    abstract fun petDao(): com.example.funlife.data.dao.PetDao
    abstract fun petItemDao(): com.example.funlife.data.dao.PetItemDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun dailyRewardDao(): DailyRewardDao
    abstract fun riddleDao(): com.example.funlife.data.dao.RiddleDao
    abstract fun riddleProgressDao(): com.example.funlife.data.dao.RiddleProgressDao
    abstract fun riddleStatsDao(): com.example.funlife.data.dao.RiddleStatsDao
    abstract fun userVipDao(): com.example.funlife.data.dao.UserVipDao
    abstract fun redeemCodeDao(): com.example.funlife.data.dao.RedeemCodeDao
    abstract fun userAvatarDao(): com.example.funlife.data.dao.UserAvatarDao
    abstract fun userAvatarFrameDao(): com.example.funlife.data.dao.UserAvatarFrameDao  // 🔥 新增：用户头像框DAO
    abstract fun billDao(): BillDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun chatPersonaDao(): ChatPersonaDao
    abstract fun accountDao(): AccountDao  // 🆕 v48
    abstract fun budgetDao(): BudgetDao    // 🆕 v49
    abstract fun recurringBillDao(): RecurringBillDao  // 🆕 v50
    abstract fun letterRecipientDao(): com.example.funlife.data.dao.LetterRecipientDao  // 🆕 v51
    abstract fun letterDao(): com.example.funlife.data.dao.LetterDao                    // 🆕 v51
    abstract fun bookDao(): com.example.funlife.data.dao.BookDao                          // 🆕 v52
    abstract fun readingSessionDao(): com.example.funlife.data.dao.ReadingSessionDao       // 🆕 v53
    abstract fun quoteDao(): com.example.funlife.data.dao.QuoteDao                          // 🆕 v53
    abstract fun readerDnaCardDao(): com.example.funlife.data.dao.ReaderDnaCardDao          // 🆕 v53
    abstract fun morningHeraldLogDao(): com.example.funlife.data.dao.MorningHeraldLogDao    // 🆕 v53
    abstract fun systemQuotaUsedDao(): com.example.funlife.data.dao.SystemQuotaUsedDao      // 🆕 v53
    abstract fun bookChatSessionDao(): com.example.funlife.data.dao.BookChatSessionDao      // 🆕 v54
    abstract fun diaryDao(): com.example.funlife.data.dao.DiaryDao                          // 🆕 v55
    abstract fun socialDao(): com.example.funlife.data.dao.SocialDao                        // 🆕 v57
    abstract fun pacMazeProgressDao(): com.example.funlife.data.dao.PacMazeProgressDao      // 🆕 v64
    abstract fun platformerClipCacheDao(): com.example.funlife.data.dao.PlatformerClipCacheDao // 🆕 v67

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
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS riddles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        question TEXT NOT NULL,
                        answer TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT '脑筋急转弯',
                        difficulty INTEGER NOT NULL DEFAULT 1
                    )
                """)
                
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
                
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_riddle_progress_userId_riddleId 
                    ON riddle_progress(userId, riddleId)
                """)
            }
        }
        
        // 🔥 新增：版本24到25 - 占位迁移（保持版本连续性）
        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 占位迁移，确保版本号连续
                // 如果之前有其他功能在版本25，可以在这里添加
            }
        }
        
        // 🔥 新增：版本25到26 - 添加玩家胜利记录表
        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS player_victory_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        playerName TEXT NOT NULL,
                        avatar TEXT NOT NULL,
                        victoryCount INTEGER NOT NULL DEFAULT 0,
                        lastVictoryTime INTEGER NOT NULL
                    )
                """)
                
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_player_victory_records_playerName 
                    ON player_victory_records(playerName)
                """)
            }
        }
        
        // 🔥 新增：版本26到27 - 添加背包系统
        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS inventory_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL DEFAULT 1,
                        itemId TEXT NOT NULL,
                        itemName TEXT NOT NULL,
                        itemType TEXT NOT NULL,
                        itemRarity TEXT NOT NULL,
                        iconEmoji TEXT NOT NULL,
                        description TEXT NOT NULL,
                        quantity INTEGER NOT NULL DEFAULT 1,
                        isUsable INTEGER NOT NULL DEFAULT 1,
                        effectValue INTEGER NOT NULL DEFAULT 0,
                        purchasePrice INTEGER NOT NULL DEFAULT 0,
                        obtainedTime INTEGER NOT NULL
                    )
                """)
            }
        }
        
        // 🔥 新增：版本27到28 - 添加转盘结算面板皮肤
        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE user_preferences 
                    ADD COLUMN spinResultPanelSkin TEXT NOT NULL DEFAULT 'js_1'
                """)
            }
        }
        
        // 🔥 新增：版本28到29 - 添加分数操作记录表
        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS score_operations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        gameSessionId INTEGER NOT NULL,
                        playerId INTEGER NOT NULL,
                        playerName TEXT NOT NULL,
                        playerAvatar TEXT NOT NULL,
                        operation INTEGER NOT NULL,
                        scoreBefore INTEGER NOT NULL,
                        scoreAfter INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """)
            }
        }
        
        // 🔥 新增：版本29到30 - 添加转盘按钮皮肤
        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE user_preferences 
                    ADD COLUMN spinButtonSkin TEXT NOT NULL DEFAULT 'pf_1'
                """)
            }
        }
        
        // 🔥 新增：版本30到31 - 添加转盘旋转音量
        private val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE user_preferences 
                    ADD COLUMN spinRotationVolume REAL NOT NULL DEFAULT 0.7
                """)
            }
        }
        
        // 🔥 新增：版本31到32 - 添加纪念日相框字段
        private val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE anniversaries 
                    ADD COLUMN frameId TEXT NOT NULL DEFAULT 'jinian_card_1'
                """)
            }
        }
        
        // 🔥 新增：版本32到33 - 添加VIP系统
        private val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建用户VIP表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_vip (
                        userId INTEGER PRIMARY KEY NOT NULL,
                        vipLevel INTEGER NOT NULL DEFAULT 0,
                        expireDate TEXT,
                        lastDailyClaimDate TEXT,
                        totalDaysActive INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // 创建兑换码表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS redeem_codes (
                        code TEXT PRIMARY KEY NOT NULL,
                        type TEXT NOT NULL,
                        value TEXT NOT NULL,
                        maxUses INTEGER NOT NULL DEFAULT -1,
                        currentUses INTEGER NOT NULL DEFAULT 0,
                        expiryDate TEXT,
                        isActive INTEGER NOT NULL DEFAULT 1
                    )
                """)
                
                // 创建用户兑换历史表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_redeem_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        code TEXT NOT NULL,
                        redeemDate TEXT NOT NULL,
                        reward TEXT NOT NULL
                    )
                """)
                
                // 🔒 安全清理：移除历史硬编码兑换码（之前曾插入 '223498' 终身VIP，
                // 已知会被反编译泄漏，此处不再写入新数据）
            }
        }
        
        // 🔥 新增：版本33到34 - 添加VIP安全签名
        private val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为user_vip表添加signature字段（防篡改签名）
                database.execSQL("""
                    ALTER TABLE user_vip ADD COLUMN signature TEXT
                """)
            }
        }
        
        // 🔥 新增：版本34到35 - 添加VIP个人主页系统
        private val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建用户头像信息表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_avatars (
                        userId INTEGER PRIMARY KEY NOT NULL,
                        avatarUri TEXT,
                        frameId TEXT,
                        backgroundId TEXT,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                
                // 创建头像框表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS avatar_frames (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        price INTEGER NOT NULL,
                        requiredVipLevel INTEGER NOT NULL,
                        animationType TEXT NOT NULL,
                        category TEXT NOT NULL DEFAULT 'basic',
                        description TEXT NOT NULL DEFAULT '',
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // 创建背景主题表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS profile_backgrounds (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        preview TEXT NOT NULL,
                        price INTEGER NOT NULL,
                        requiredVipLevel INTEGER NOT NULL,
                        gradientColors TEXT NOT NULL,
                        particleType TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // 创建用户拥有的头像框表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_owned_frames (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        frameId TEXT NOT NULL,
                        purchasedAt INTEGER NOT NULL
                    )
                """)
                
                // 创建用户拥有的背景表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_owned_backgrounds (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        backgroundId TEXT NOT NULL,
                        purchasedAt INTEGER NOT NULL
                    )
                """)
                
                // 创建索引
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_user_owned_frames_userId 
                    ON user_owned_frames(userId)
                """)
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_user_owned_backgrounds_userId 
                    ON user_owned_backgrounds(userId)
                """)
                
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_user_owned_frames_userId_frameId 
                    ON user_owned_frames(userId, frameId)
                """)
                
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_user_owned_backgrounds_userId_backgroundId 
                    ON user_owned_backgrounds(userId, backgroundId)
                """)
            }
        }
        
        // 🔥 新增：版本35到36 - 添加头像框商城系统
        private val MIGRATION_35_36 = object : Migration(35, 36) {
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
        
        // 🔥 新增：版本36到37 - 添加头像框商城kapian设计字段
        private val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 占位迁移，确保版本号连续
                // 版本37的schema变更已在版本36中完成
            }
        }
        
        // 🔥 新增：版本37到38 - 修复AvatarFrameInitializer字段
        private val MIGRATION_37_38 = object : Migration(37, 38) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 占位迁移，确保版本号连续
                // 此版本主要修复了AvatarFrameInitializer中移除的不存在字段
                // 数据库schema没有实际变更
            }
        }
        
        // 🔥 新增：版本38到39 - 添加商城积分系统（商品转盘）
        private val MIGRATION_38_39 = object : Migration(38, 39) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE user_coins ADD COLUMN shopPoints INTEGER NOT NULL DEFAULT 0
                """)
            }
        }
        
        // 🔥 新增：版本39到40 - 聊天记账系统
        private val MIGRATION_39_40 = object : Migration(39, 40) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS bills (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        category TEXT NOT NULL,
                        note TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        linkedMessageId INTEGER
                    )
                """)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        personaId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        billId INTEGER,
                        timestamp INTEGER NOT NULL
                    )
                """)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_personas (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        avatar TEXT NOT NULL,
                        bubbleColor INTEGER NOT NULL,
                        systemPrompt TEXT NOT NULL,
                        isBuiltin INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                """)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_persona_state (
                        personaId TEXT PRIMARY KEY NOT NULL,
                        userId INTEGER NOT NULL,
                        affection INTEGER NOT NULL,
                        mood TEXT NOT NULL,
                        interactionCount INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_bills_userId ON bills(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_messages_userId ON chat_messages(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_messages_timestamp ON chat_messages(timestamp DESC)")
            }
        }
        
        // 🔥 新增：版本41到42 - 人格自定义头像
        private val MIGRATION_41_42 = object : Migration(41, 42) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chat_personas ADD COLUMN customAvatarUri TEXT")
            }
        }

        // 🔥 新增：版本42到43 - 添加equippedAvatarFrame字段
        private val MIGRATION_42_43 = object : Migration(42, 43) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 🛡️ 防御性迁移：先检查列是否已存在（旧版本可能已通过其它路径添加过），避免 duplicate column 崩溃
                val cursor = database.query("PRAGMA table_info(user_preferences)")
                var alreadyHasColumn = false
                cursor.use {
                    val nameIdx = it.getColumnIndex("name")
                    while (it.moveToNext()) {
                        if (nameIdx >= 0 && it.getString(nameIdx) == "equippedAvatarFrame") {
                            alreadyHasColumn = true
                            break
                        }
                    }
                }
                if (!alreadyHasColumn) {
                    database.execSQL("ALTER TABLE user_preferences ADD COLUMN equippedAvatarFrame TEXT")
                }
            }
        }

        // 🔥 新增：版本43到44 - 修复shop_items表defaultValue不匹配
        private val MIGRATION_43_44 = object : Migration(43, 44) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. 创建临时表，使用正确的schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shop_items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        price INTEGER NOT NULL,
                        vipPrice INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        value INTEGER NOT NULL,
                        isAvailable INTEGER NOT NULL,
                        assetPath TEXT,
                        rarity TEXT NOT NULL,
                        isAnimated INTEGER NOT NULL,
                        category TEXT,
                        sortOrder INTEGER NOT NULL
                    )
                """)
                
                // 2. 复制数据
                database.execSQL("""
                    INSERT INTO shop_items_new (
                        id, name, description, icon, price, vipPrice, type, value, 
                        isAvailable, assetPath, rarity, isAnimated, category, sortOrder
                    )
                    SELECT 
                        id, name, description, icon, price, 
                        COALESCE(vipPrice, price) as vipPrice,
                        type, 
                        COALESCE(value, 1) as value,
                        COALESCE(isAvailable, 1) as isAvailable,
                        assetPath,
                        COALESCE(rarity, 'COMMON') as rarity,
                        COALESCE(isAnimated, 0) as isAnimated,
                        category,
                        COALESCE(sortOrder, 0) as sortOrder
                    FROM shop_items
                """)
                
                // 3. 删除旧表
                database.execSQL("DROP TABLE shop_items")
                
                // 4. 重命名新表
                database.execSQL("ALTER TABLE shop_items_new RENAME TO shop_items")

                // 🛡️ 注意：ShopItem 实体未声明 @Entity(indices=...)，Room 期望 indices=[]
                // 不能在迁移里建索引，否则 Room schema 校验失败导致启动崩溃
                // 如需索引，要在 ShopItem 实体上加 indices 声明并升数据库版本
            }
        }

        // 🔥 新增：版本40到41 - 修复聊天记账表DEFAULT不匹配
        private val MIGRATION_40_41 = object : Migration(40, 41) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS bills")
                database.execSQL("DROP TABLE IF EXISTS chat_messages")
                database.execSQL("DROP TABLE IF EXISTS chat_personas")
                database.execSQL("DROP TABLE IF EXISTS chat_persona_state")
                database.execSQL("DROP INDEX IF EXISTS idx_bills_userId")
                database.execSQL("DROP INDEX IF EXISTS idx_chat_messages_userId")
                database.execSQL("DROP INDEX IF EXISTS idx_chat_messages_timestamp")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS bills (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        category TEXT NOT NULL,
                        note TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        linkedMessageId INTEGER
                    )
                """)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        personaId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        billId INTEGER,
                        timestamp INTEGER NOT NULL
                    )
                """)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_personas (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        avatar TEXT NOT NULL,
                        bubbleColor INTEGER NOT NULL,
                        systemPrompt TEXT NOT NULL,
                        isBuiltin INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                """)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_persona_state (
                        personaId TEXT PRIMARY KEY NOT NULL,
                        userId INTEGER NOT NULL,
                        affection INTEGER NOT NULL,
                        mood TEXT NOT NULL,
                        interactionCount INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_bills_userId ON bills(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_messages_userId ON chat_messages(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_messages_timestamp ON chat_messages(timestamp DESC)")
            }
        }

        // 🔒 新增：版本45到46 - 计分页面表加 userId 多账号隔离
        private val MIGRATION_45_46 = object : Migration(45, 46) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. score_operations 加 userId 列与索引
                database.execSQL("ALTER TABLE score_operations ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_score_operations_userId ON score_operations(userId)")

                // 2. player_victory_records 加 userId 列与索引
                database.execSQL("ALTER TABLE player_victory_records ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_player_victory_records_userId ON player_victory_records(userId)")
                // 旧数据 userId 默认 0（系统级 / 游客），等用户首次登录后属于谁就归谁
                // 不主动迁移到具体 userId，避免误归属错账号
            }
        }

        // 🔒 v46 → v47：补救 v46 漏加的 players 表 userId 列
        //   v46 时 Player Entity 已声明 userId，但 MIGRATION_45_46 只改了
        //   score_operations / player_victory_records，遗漏了 players 表本身，
        //   导致 Room schema 校验失败抛 "Migration didn't properly handle: players"。
        //   本迁移幂等：ALTER 包 try-catch（重复添加列时 SQLite 抛 SQLException，吞掉）。
        private val MIGRATION_46_47 = object : Migration(46, 47) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE players ADD COLUMN userId INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 列已存在 → 忽略
                    android.util.Log.w("AppDatabase", "MIGRATION_46_47 ADD COLUMN userId on players: ${e.message}")
                }
                try {
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_players_userId ON players(userId)")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "MIGRATION_46_47 CREATE INDEX: ${e.message}")
                }
            }
        }

        // 🔒 新增：版本44到45 - 清除历史硬编码万能兑换码（安全修复）
        private val MIGRATION_44_45 = object : Migration(44, 45) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DELETE FROM redeem_codes WHERE code IN ('223498','HZ223498','VIP2024','SUPERVIP','COINS1000','TESTCODE')")
            }
        }

        // 🆕 v47 → v48：聊天记账多账户系统
        //   1. 新建 accounts 表（含 @Index("userId") + (userId, systemKey) 复合索引）
        //   2. bills 表加 accountId 列（可空，旧账单 = null）
        //
        //   全部 ALTER 走 PRAGMA 检查（DEVELOPMENT_PRINCIPLES §5.1）；
        //   CREATE INDEX 与 Entity 的 indices 一一对应（防 "Migration didn't properly handle"）。
        private val MIGRATION_47_48 = object : Migration(47, 48) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // ── (1) 创建 accounts 表 ──
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `accounts` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `userId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `icon` TEXT NOT NULL DEFAULT '💰',
                        `color` INTEGER NOT NULL DEFAULT 4294934656,
                        `initialBalance` REAL NOT NULL DEFAULT 0.0,
                        `balance` REAL NOT NULL DEFAULT 0.0,
                        `isArchived` INTEGER NOT NULL DEFAULT 0,
                        `sortOrder` INTEGER NOT NULL DEFAULT 100,
                        `systemKey` TEXT,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_userId` ON `accounts`(`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_accounts_userId_systemKey` ON `accounts`(`userId`, `systemKey`)")

                // ── (2) 给 bills 加 accountId 列（防御性 PRAGMA 检查） ──
                val cursor = database.query("PRAGMA table_info(bills)")
                var hasAccountId = false
                cursor.use {
                    val nameIdx = it.getColumnIndex("name")
                    while (it.moveToNext()) {
                        if (nameIdx >= 0 && it.getString(nameIdx) == "accountId") {
                            hasAccountId = true; break
                        }
                    }
                }
                if (!hasAccountId) {
                    try {
                        database.execSQL("ALTER TABLE bills ADD COLUMN accountId INTEGER")
                    } catch (e: Exception) {
                        android.util.Log.w("AppDatabase", "MIGRATION_47_48 ADD bills.accountId: ${e.message}")
                    }
                }
            }
        }

        // 🆕 v49 → v50：定期账单
        //   新建 recurring_bills 表，与 Entity 索引一致： index_recurring_bills_userId
        private val MIGRATION_49_50 = object : Migration(49, 50) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recurring_bills` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `userId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `accountId` INTEGER,
                        `period` TEXT NOT NULL DEFAULT 'MONTHLY',
                        `dayOfPeriod` INTEGER NOT NULL DEFAULT 1,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `startDate` INTEGER NOT NULL DEFAULT 0,
                        `lastGeneratedAt` INTEGER NOT NULL DEFAULT 0,
                        `color` INTEGER NOT NULL DEFAULT 4286137030,
                        `sortOrder` INTEGER NOT NULL DEFAULT 100,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_bills_userId` ON `recurring_bills`(`userId`)")
            }
        }

        // 🆕 v50 → v51：时光信箱（letter_recipients + letters）
        //   字段顺序、类型、默认值、NOT NULL 与 Entity 严格一致；
        //   索引名严格匹配 Room 自动生成：
        //     - index_letter_recipients_userId
        //     - index_letters_userId / index_letters_recipientId / index_letters_deliveryAt
        //   content 字段存的是 AES-GCM 加密后的 base64，由 Repository 层透明解密。
        // 🆕 v52 人生书架：books 表，含 (userId), (userId, finishedAt) 两个索引
        private val MIGRATION_51_52 = object : Migration(51, 52) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `books` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `userId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `author` TEXT NOT NULL,
                        `rating` INTEGER NOT NULL,
                        `finishedAt` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `favoriteQuote` TEXT NOT NULL,
                        `tags` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_books_userId` ON `books` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_books_userId_finishedAt` ON `books` (`userId`, `finishedAt`)")
            }
        }

        // 🆕 v52 → v53 阅光书房：
        //   1) books 扩 5 列：totalPages / currentPage / openingLetter / openingMood / finishedMood
        //   2) 新建 reading_sessions（阅读时长打卡）
        //   3) 新建 quotes（摘抄 + 时光胶囊）
        //   4) 新建 reader_dna_cards（读者 DNA）
        //   5) 新建 morning_herald_log（晨光信使日志，复合主键 userId+dateYmd）
        //   6) 新建 system_quota_used（系统赠送配额，复合主键 userId+quotaKey+monthYm）
        // 注意：所有表强制 userId 索引，DAO 查询全部带 userId 过滤；多用户数据隔离前置约束。
        private val MIGRATION_52_53 = object : Migration(52, 53) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // (1) books 加列
                database.execSQL("ALTER TABLE `books` ADD COLUMN `totalPages` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `books` ADD COLUMN `currentPage` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `books` ADD COLUMN `openingLetter` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `books` ADD COLUMN `openingMood` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `books` ADD COLUMN `finishedMood` TEXT NOT NULL DEFAULT ''")

                // (2) reading_sessions
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `reading_sessions` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `userId` INTEGER NOT NULL,
                        `bookId` INTEGER,
                        `minutes` INTEGER NOT NULL,
                        `dateYmd` INTEGER NOT NULL,
                        `atPage` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_sessions_userId` ON `reading_sessions` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_sessions_userId_dateYmd` ON `reading_sessions` (`userId`, `dateYmd`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_sessions_userId_bookId` ON `reading_sessions` (`userId`, `bookId`)")

                // (3) quotes
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `quotes` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `userId` INTEGER NOT NULL,
                        `bookId` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `page` INTEGER NOT NULL DEFAULT 0,
                        `rating` INTEGER NOT NULL DEFAULT 0,
                        `pinned` INTEGER NOT NULL DEFAULT 0,
                        `capsuleDeliveryAt` INTEGER NOT NULL DEFAULT 0,
                        `capsuleDelivered` INTEGER NOT NULL DEFAULT 0,
                        `publishedToGalaxy` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_quotes_userId` ON `quotes` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_quotes_userId_bookId` ON `quotes` (`userId`, `bookId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_quotes_userId_capsuleDeliveryAt` ON `quotes` (`userId`, `capsuleDeliveryAt`)")

                // (4) reader_dna_cards
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `reader_dna_cards` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `userId` INTEGER NOT NULL,
                        `generatedAt` INTEGER NOT NULL,
                        `vectorJson` TEXT NOT NULL,
                        `tagline` TEXT NOT NULL,
                        `basedOnBookCount` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_reader_dna_cards_userId` ON `reader_dna_cards` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_reader_dna_cards_userId_generatedAt` ON `reader_dna_cards` (`userId`, `generatedAt`)")

                // (5) morning_herald_log（复合主键）
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `morning_herald_log` (
                        `userId` INTEGER NOT NULL,
                        `dateYmd` INTEGER NOT NULL,
                        `contentType` TEXT NOT NULL,
                        `payloadSummary` TEXT NOT NULL DEFAULT '',
                        `sentAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `dateYmd`)
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_morning_herald_log_userId` ON `morning_herald_log` (`userId`)")

                // (6) system_quota_used（复合主键）
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `system_quota_used` (
                        `userId` INTEGER NOT NULL,
                        `quotaKey` TEXT NOT NULL,
                        `monthYm` INTEGER NOT NULL,
                        `count` INTEGER NOT NULL DEFAULT 1,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `quotaKey`, `monthYm`)
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_system_quota_used_userId` ON `system_quota_used` (`userId`)")
            }
        }

        // ════════════════════════════════════════════════════════════════
        // 🆕 v54 阅光书房 v53.2 · 长对话存档（VIP3）
        //   新建 book_chat_sessions：单条记录承载一次完整 AI 读书对话
        //   userId 强制索引；(userId, bookId) 索引让"按本书查档案"O(log n)
        //   (userId, lastMessageAt) 索引支持按最近时间倒序展示档案列表
        // ════════════════════════════════════════════════════════════════
        // ═══════════════════════════════════════════════════════════════
        // v55 古籍日记本：diary_entries 表
        //   userId 强制索引 + (userId, date) 唯一索引避免一日多篇
        // ═══════════════════════════════════════════════════════════════
        private val MIGRATION_54_55 = object : Migration(54, 55) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `diary_entries` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `userId` INTEGER NOT NULL,
                        `date` TEXT NOT NULL,
                        `title` TEXT NOT NULL DEFAULT '',
                        `content` TEXT NOT NULL DEFAULT '',
                        `weather` TEXT,
                        `temperature` REAL,
                        `moodEmoji` TEXT,
                        `location` TEXT,
                        `bookmarked` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_userId` ON `diary_entries` (`userId`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_diary_entries_userId_date` ON `diary_entries` (`userId`, `date`)")
            }
        }

        // v56 日记本分册：bookSkinId + pageSlot，按页槽位存储，皮肤间隔离
        private val MIGRATION_66_67 = object : Migration(66, 67) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `platformer_clip_cache` (
                        `id` TEXT NOT NULL,
                        `catalogId` TEXT NOT NULL,
                        `clipFolder` TEXT NOT NULL,
                        `frameCount` INTEGER NOT NULL,
                        `decodeTag` TEXT NOT NULL,
                        `bundleVersion` INTEGER NOT NULL,
                        `format` TEXT NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_platformer_clip_cache_catalogId` ON `platformer_clip_cache` (`catalogId`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_platformer_clip_cache_decodeTag_bundleVersion` ON `platformer_clip_cache` (`decodeTag`, `bundleVersion`)",
                )
            }
        }

        private val MIGRATION_65_66 = object : Migration(65, 66) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `social_game_room_cache` ADD COLUMN `hostReady` INTEGER NOT NULL DEFAULT 1",
                )
                database.execSQL(
                    "ALTER TABLE `social_game_room_cache` ADD COLUMN `guestReady` INTEGER NOT NULL DEFAULT 0",
                )
                database.execSQL(
                    "ALTER TABLE `social_game_room_cache` ADD COLUMN `pacMazeJson` TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        private val MIGRATION_64_65 = object : Migration(64, 65) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `pac_maze_progress` ADD COLUMN `endlessBestScore` INTEGER NOT NULL DEFAULT 0",
                )
                database.execSQL(
                    "ALTER TABLE `pac_maze_progress` ADD COLUMN `endlessBestWave` INTEGER NOT NULL DEFAULT 0",
                )
                database.execSQL(
                    "ALTER TABLE `pac_maze_progress` ADD COLUMN `mazeBestTimeMs` INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        private val MIGRATION_63_64 = object : Migration(63, 64) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pac_maze_progress` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` INTEGER NOT NULL,
                        `maxLevelReached` INTEGER NOT NULL,
                        `highScore` INTEGER NOT NULL,
                        `starsBitmask` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_pac_maze_progress_userId` ON `pac_maze_progress` (`userId`)",
                )
            }
        }

        private val MIGRATION_62_63 = object : Migration(62, 63) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `social_friend_cache` ADD COLUMN `online` INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        private val MIGRATION_61_62 = object : Migration(61, 62) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `social_game_room_cache` ADD COLUMN `declinedByPbId` TEXT NOT NULL DEFAULT ''",
                )
                database.execSQL(
                    "ALTER TABLE `social_game_room_cache` ADD COLUMN `membersJson` TEXT NOT NULL DEFAULT '[]'",
                )
                database.execSQL(
                    "ALTER TABLE `social_game_room_cache` ADD COLUMN `maxPlayers` INTEGER NOT NULL DEFAULT 2",
                )
                database.execSQL(
                    "ALTER TABLE `social_game_room_cache` ADD COLUMN `minPlayers` INTEGER NOT NULL DEFAULT 2",
                )
                database.execSQL(
                    "ALTER TABLE `social_game_room_cache` ADD COLUMN `pendingInvitePbId` TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        private val MIGRATION_60_61 = object : Migration(60, 61) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `social_game_room_cache` ADD COLUMN `hostDisplayName` TEXT NOT NULL DEFAULT ''",
                )
                database.execSQL(
                    "ALTER TABLE `social_game_room_cache` ADD COLUMN `hostAvatarUrl` TEXT NOT NULL DEFAULT ''",
                )
                database.execSQL(
                    "ALTER TABLE `social_game_room_cache` ADD COLUMN `guestProfileName` TEXT NOT NULL DEFAULT ''",
                )
                database.execSQL(
                    "ALTER TABLE `social_game_room_cache` ADD COLUMN `guestProfileAvatar` TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        private val MIGRATION_59_60 = object : Migration(59, 60) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `social_game_room_cache` ADD COLUMN `peerAvatarUrl` TEXT NOT NULL DEFAULT ''",
                )
                database.execSQL(
                    "ALTER TABLE `social_game_room_cache` ADD COLUMN `declinedByGuest` INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        private val MIGRATION_58_59 = object : Migration(58, 59) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `social_game_room_cache` (
                        `userId` INTEGER NOT NULL,
                        `roomId` TEXT NOT NULL,
                        `gameType` TEXT NOT NULL,
                        `inviteMode` TEXT NOT NULL,
                        `roomCode` TEXT NOT NULL,
                        `hostPbId` TEXT NOT NULL,
                        `guestPbId` TEXT,
                        `guestDisplayName` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `inviteMessage` TEXT NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `roomId`)
                    )
                """.trimIndent())
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_social_game_room_cache_userId` ON `social_game_room_cache` (`userId`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_social_game_room_cache_userId_updatedAtMs` ON `social_game_room_cache` (`userId`, `updatedAtMs`)",
                )
            }
        }

        private val MIGRATION_57_58 = object : Migration(57, 58) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `social_conversation_cache` (
                        `userId` INTEGER NOT NULL,
                        `conversationId` TEXT NOT NULL,
                        `peerPbId` TEXT NOT NULL,
                        `peerUsername` TEXT NOT NULL,
                        `peerDisplayName` TEXT NOT NULL,
                        `peerAvatarUrl` TEXT,
                        `lastPreview` TEXT NOT NULL,
                        `lastMessageAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `conversationId`)
                    )
                """.trimIndent())
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_social_conversation_cache_userId` ON `social_conversation_cache` (`userId`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_social_conversation_cache_userId_lastMessageAt` ON `social_conversation_cache` (`userId`, `lastMessageAt`)",
                )
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `social_message_cache` (
                        `userId` INTEGER NOT NULL,
                        `conversationId` TEXT NOT NULL,
                        `messageId` TEXT NOT NULL,
                        `senderPbId` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `messageId`)
                    )
                """.trimIndent())
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_social_message_cache_userId` ON `social_message_cache` (`userId`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_social_message_cache_userId_conversationId_createdAt` ON `social_message_cache` (`userId`, `conversationId`, `createdAt`)",
                )
            }
        }

        private val MIGRATION_56_57 = object : Migration(56, 57) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `social_pb_links` (
                        `userId` INTEGER NOT NULL,
                        `pbRecordId` TEXT NOT NULL,
                        `pbIdentity` TEXT NOT NULL,
                        `linkedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`)
                    )
                """.trimIndent())
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_social_pb_links_userId` ON `social_pb_links` (`userId`)",
                )
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `social_friend_cache` (
                        `userId` INTEGER NOT NULL,
                        `friendPbId` TEXT NOT NULL,
                        `funlifeUsername` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `avatarUrl` TEXT,
                        `friendshipId` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `remark` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `friendPbId`)
                    )
                """.trimIndent())
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_social_friend_cache_userId` ON `social_friend_cache` (`userId`)",
                )
            }
        }

        private val MIGRATION_55_56 = object : Migration(55, 56) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `diary_entries` ADD COLUMN `bookSkinId` TEXT NOT NULL DEFAULT 'builtin::hengwu'"
                )
                database.execSQL(
                    "ALTER TABLE `diary_entries` ADD COLUMN `pageSlot` INTEGER NOT NULL DEFAULT 2"
                )
                database.execSQL("""
                    UPDATE `diary_entries` SET `pageSlot` = 2 + (
                        SELECT COUNT(*) FROM `diary_entries` AS e2
                        WHERE e2.`userId` = `diary_entries`.`userId`
                          AND (e2.`date` < `diary_entries`.`date`
                               OR (e2.`date` = `diary_entries`.`date` AND e2.`id` < `diary_entries`.`id`))
                    )
                """.trimIndent())
                database.execSQL("DROP INDEX IF EXISTS `index_diary_entries_userId_date`")
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_diary_entries_bookSkinId` ON `diary_entries` (`bookSkinId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_diary_entries_userId_bookSkinId` ON `diary_entries` (`userId`, `bookSkinId`)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_diary_entries_userId_bookSkinId_pageSlot` ON `diary_entries` (`userId`, `bookSkinId`, `pageSlot`)"
                )
            }
        }

        private val MIGRATION_53_54 = object : Migration(53, 54) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `book_chat_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` INTEGER NOT NULL,
                        `bookId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL DEFAULT '',
                        `messagesJson` TEXT NOT NULL DEFAULT '[]',
                        `turnCount` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `lastMessageAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_book_chat_sessions_userId` ON `book_chat_sessions` (`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_book_chat_sessions_userId_bookId` ON `book_chat_sessions` (`userId`, `bookId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_book_chat_sessions_userId_lastMessageAt` ON `book_chat_sessions` (`userId`, `lastMessageAt`)")
            }
        }

        private val MIGRATION_50_51 = object : Migration(50, 51) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // ── (1) letter_recipients ──
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `letter_recipients` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `userId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `avatar` TEXT NOT NULL DEFAULT '✉️',
                        `customAvatarUri` TEXT,
                        `relation` TEXT NOT NULL DEFAULT 'custom',
                        `persona` TEXT NOT NULL DEFAULT '',
                        `timeAnchor` INTEGER,
                        `sortOrder` INTEGER NOT NULL DEFAULT 100,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_letter_recipients_userId` ON `letter_recipients`(`userId`)")

                // ── (2) letters ──
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `letters` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `userId` INTEGER NOT NULL,
                        `recipientId` INTEGER NOT NULL,
                        `direction` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `mood` TEXT,
                        `sentAt` INTEGER NOT NULL DEFAULT 0,
                        `deliveryAt` INTEGER NOT NULL DEFAULT 0,
                        `deliveredAt` INTEGER,
                        `status` TEXT NOT NULL DEFAULT 'pending',
                        `isRead` INTEGER NOT NULL DEFAULT 0,
                        `parentLetterId` INTEGER,
                        `failureReason` TEXT,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_letters_userId` ON `letters`(`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_letters_recipientId` ON `letters`(`recipientId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_letters_deliveryAt` ON `letters`(`deliveryAt`)")
            }
        }

        // 🆕 v48 → v49：预算系统
        //   新建 budgets 表，与 Entity 索引严格一致：
        //     - index_budgets_userId
        //     - index_budgets_userId_scope_period
        private val MIGRATION_48_49 = object : Migration(48, 49) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `userId` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `scope` TEXT NOT NULL DEFAULT 'TOTAL',
                        `targetKey` TEXT,
                        `period` TEXT NOT NULL DEFAULT 'MONTHLY',
                        `amount` REAL NOT NULL,
                        `startDate` INTEGER NOT NULL DEFAULT 0,
                        `rollover` INTEGER NOT NULL DEFAULT 0,
                        `warnThreshold` REAL NOT NULL DEFAULT 0.8,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `color` INTEGER NOT NULL DEFAULT 4294934656,
                        `sortOrder` INTEGER NOT NULL DEFAULT 100,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `updatedAt` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_userId` ON `budgets`(`userId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_userId_scope_period` ON `budgets`(`userId`, `scope`, `period`)")
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "funlife_database"
            )
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
                MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21,
                MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25,
                MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29,
                MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33,
                MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37,
                MIGRATION_37_38, MIGRATION_38_39, MIGRATION_39_40, MIGRATION_40_41,
                MIGRATION_41_42, MIGRATION_42_43, MIGRATION_43_44, MIGRATION_44_45,
                MIGRATION_45_46, MIGRATION_46_47, MIGRATION_47_48,
                MIGRATION_48_49, MIGRATION_49_50, MIGRATION_50_51,
                MIGRATION_51_52, MIGRATION_52_53, MIGRATION_53_54, MIGRATION_54_55, MIGRATION_55_56,
                MIGRATION_56_57,
                MIGRATION_57_58,
                MIGRATION_58_59,
                MIGRATION_59_60,
                MIGRATION_60_61,
                MIGRATION_61_62,
                MIGRATION_62_63,
                MIGRATION_63_64,
                MIGRATION_64_65,
                MIGRATION_65_66,
                MIGRATION_66_67,
            )
            // Release 禁止 destructive migration，避免升级误删全库
            if (BuildConfig.DEBUG) {
                builder.fallbackToDestructiveMigration()
            }
            return builder.build()
        }

        private fun deleteDatabaseFiles(context: Context) {
            try {
                context.getDatabasePath("funlife_database").delete()
                context.getDatabasePath("funlife_database-shm").delete()
                context.getDatabasePath("funlife_database-wal").delete()
            } catch (_: Exception) {
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val instance = try {
                        val db = buildDatabase(context)
                        db.openHelper.writableDatabase
                        db
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            android.util.Log.e(
                                "AppDatabase",
                                "DB open failed (debug recreate). Cause: ${e.message}",
                                e,
                            )
                            deleteDatabaseFiles(context)
                            buildDatabase(context)
                        } else {
                            android.util.Log.e(
                                "AppDatabase",
                                "DB migration failed — user data preserved, fix migration. Cause: ${e.message}",
                                e,
                            )
                            throw e
                        }
                    }
                    INSTANCE = instance
                    instance
                }
            }
        }
    }
}
