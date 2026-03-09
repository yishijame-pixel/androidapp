package com.example.funlife.data.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.example.funlife.data.dao.AnniversaryDao;
import com.example.funlife.data.dao.AnniversaryDao_Impl;
import com.example.funlife.data.dao.CoinDao;
import com.example.funlife.data.dao.CoinDao_Impl;
import com.example.funlife.data.dao.CustomSpinModeDao;
import com.example.funlife.data.dao.CustomSpinModeDao_Impl;
import com.example.funlife.data.dao.GameHistoryDao;
import com.example.funlife.data.dao.GameHistoryDao_Impl;
import com.example.funlife.data.dao.GoalDao;
import com.example.funlife.data.dao.GoalDao_Impl;
import com.example.funlife.data.dao.GuaranteeCounterDao;
import com.example.funlife.data.dao.GuaranteeCounterDao_Impl;
import com.example.funlife.data.dao.HabitDao;
import com.example.funlife.data.dao.HabitDao_Impl;
import com.example.funlife.data.dao.MoodDao;
import com.example.funlife.data.dao.MoodDao_Impl;
import com.example.funlife.data.dao.PlayerDao;
import com.example.funlife.data.dao.PlayerDao_Impl;
import com.example.funlife.data.dao.ShopDao;
import com.example.funlife.data.dao.ShopDao_Impl;
import com.example.funlife.data.dao.SpinWheelHistoryDao;
import com.example.funlife.data.dao.SpinWheelHistoryDao_Impl;
import com.example.funlife.data.dao.SpinWheelTemplateDao;
import com.example.funlife.data.dao.SpinWheelTemplateDao_Impl;
import com.example.funlife.data.dao.UserPreferencesDao;
import com.example.funlife.data.dao.UserPreferencesDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile AnniversaryDao _anniversaryDao;

  private volatile PlayerDao _playerDao;

  private volatile GameHistoryDao _gameHistoryDao;

  private volatile UserPreferencesDao _userPreferencesDao;

  private volatile SpinWheelTemplateDao _spinWheelTemplateDao;

  private volatile SpinWheelHistoryDao _spinWheelHistoryDao;

  private volatile HabitDao _habitDao;

  private volatile MoodDao _moodDao;

  private volatile GoalDao _goalDao;

  private volatile CoinDao _coinDao;

  private volatile ShopDao _shopDao;

  private volatile GuaranteeCounterDao _guaranteeCounterDao;

  private volatile CustomSpinModeDao _customSpinModeDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(11) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `anniversaries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `date` TEXT NOT NULL, `imageUri` TEXT, `isPinned` INTEGER NOT NULL, `type` TEXT NOT NULL, `isYearly` INTEGER NOT NULL, `note` TEXT, `importance` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `players` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `score` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `game_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `gameType` TEXT NOT NULL, `playerName` TEXT NOT NULL, `score` INTEGER NOT NULL, `result` TEXT NOT NULL, `timestamp` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_preferences` (`id` INTEGER NOT NULL, `isDarkMode` INTEGER NOT NULL, `enableNotifications` INTEGER NOT NULL, `notificationDaysBefore` INTEGER NOT NULL, `defaultScoreIncrement` INTEGER NOT NULL, `enableSound` INTEGER NOT NULL, `enableVibration` INTEGER NOT NULL, `autoBackup` INTEGER NOT NULL, `language` TEXT NOT NULL, `sortOrder` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `spin_wheel_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `options` TEXT NOT NULL, `weights` TEXT NOT NULL, `category` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, `usageCount` INTEGER NOT NULL, `createdAt` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `spin_wheel_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `templateId` INTEGER, `templateName` TEXT NOT NULL, `result` TEXT NOT NULL, `allOptions` TEXT NOT NULL, `mode` TEXT NOT NULL, `coinCost` INTEGER NOT NULL, `coinReward` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `color` TEXT NOT NULL, `targetDays` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `makeupCards` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `habit_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `habitId` INTEGER NOT NULL, `date` TEXT NOT NULL, `note` TEXT NOT NULL, `timestamp` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `mood_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `mood` TEXT NOT NULL, `moodLevel` INTEGER NOT NULL, `note` TEXT NOT NULL, `tags` TEXT NOT NULL, `timestamp` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `category` TEXT NOT NULL, `targetDate` TEXT, `progress` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `completedAt` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `countdowns` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `targetDate` TEXT NOT NULL, `category` TEXT NOT NULL, `icon` TEXT NOT NULL, `color` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_coins` (`id` INTEGER NOT NULL, `coins` INTEGER NOT NULL, `totalEarned` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `shop_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `icon` TEXT NOT NULL, `price` INTEGER NOT NULL, `type` TEXT NOT NULL, `value` INTEGER NOT NULL, `isAvailable` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `purchase_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `itemId` INTEGER NOT NULL, `itemName` TEXT NOT NULL, `price` INTEGER NOT NULL, `timestamp` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `guarantee_counter` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `optionText` TEXT NOT NULL, `currentCount` INTEGER NOT NULL, `guaranteeThreshold` INTEGER NOT NULL, `isEnabled` INTEGER NOT NULL, `lastUpdated` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `custom_spin_modes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `emoji` TEXT NOT NULL, `description` TEXT NOT NULL, `costPerSpin` INTEGER NOT NULL, `hasReward` INTEGER NOT NULL, `rewardMultiplier` REAL NOT NULL, `primaryColor` TEXT NOT NULL, `secondaryColor` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `usageCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '435e5a637697122c4bee64e0e6059060')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `anniversaries`");
        db.execSQL("DROP TABLE IF EXISTS `players`");
        db.execSQL("DROP TABLE IF EXISTS `game_history`");
        db.execSQL("DROP TABLE IF EXISTS `user_preferences`");
        db.execSQL("DROP TABLE IF EXISTS `spin_wheel_templates`");
        db.execSQL("DROP TABLE IF EXISTS `spin_wheel_history`");
        db.execSQL("DROP TABLE IF EXISTS `habits`");
        db.execSQL("DROP TABLE IF EXISTS `habit_records`");
        db.execSQL("DROP TABLE IF EXISTS `mood_entries`");
        db.execSQL("DROP TABLE IF EXISTS `goals`");
        db.execSQL("DROP TABLE IF EXISTS `countdowns`");
        db.execSQL("DROP TABLE IF EXISTS `user_coins`");
        db.execSQL("DROP TABLE IF EXISTS `shop_items`");
        db.execSQL("DROP TABLE IF EXISTS `purchase_history`");
        db.execSQL("DROP TABLE IF EXISTS `guarantee_counter`");
        db.execSQL("DROP TABLE IF EXISTS `custom_spin_modes`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsAnniversaries = new HashMap<String, TableInfo.Column>(9);
        _columnsAnniversaries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnniversaries.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnniversaries.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnniversaries.put("imageUri", new TableInfo.Column("imageUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnniversaries.put("isPinned", new TableInfo.Column("isPinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnniversaries.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnniversaries.put("isYearly", new TableInfo.Column("isYearly", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnniversaries.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnniversaries.put("importance", new TableInfo.Column("importance", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAnniversaries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAnniversaries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAnniversaries = new TableInfo("anniversaries", _columnsAnniversaries, _foreignKeysAnniversaries, _indicesAnniversaries);
        final TableInfo _existingAnniversaries = TableInfo.read(db, "anniversaries");
        if (!_infoAnniversaries.equals(_existingAnniversaries)) {
          return new RoomOpenHelper.ValidationResult(false, "anniversaries(com.example.funlife.data.model.Anniversary).\n"
                  + " Expected:\n" + _infoAnniversaries + "\n"
                  + " Found:\n" + _existingAnniversaries);
        }
        final HashMap<String, TableInfo.Column> _columnsPlayers = new HashMap<String, TableInfo.Column>(3);
        _columnsPlayers.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("score", new TableInfo.Column("score", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlayers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlayers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlayers = new TableInfo("players", _columnsPlayers, _foreignKeysPlayers, _indicesPlayers);
        final TableInfo _existingPlayers = TableInfo.read(db, "players");
        if (!_infoPlayers.equals(_existingPlayers)) {
          return new RoomOpenHelper.ValidationResult(false, "players(com.example.funlife.data.model.Player).\n"
                  + " Expected:\n" + _infoPlayers + "\n"
                  + " Found:\n" + _existingPlayers);
        }
        final HashMap<String, TableInfo.Column> _columnsGameHistory = new HashMap<String, TableInfo.Column>(6);
        _columnsGameHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameHistory.put("gameType", new TableInfo.Column("gameType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameHistory.put("playerName", new TableInfo.Column("playerName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameHistory.put("score", new TableInfo.Column("score", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameHistory.put("result", new TableInfo.Column("result", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGameHistory.put("timestamp", new TableInfo.Column("timestamp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGameHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGameHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGameHistory = new TableInfo("game_history", _columnsGameHistory, _foreignKeysGameHistory, _indicesGameHistory);
        final TableInfo _existingGameHistory = TableInfo.read(db, "game_history");
        if (!_infoGameHistory.equals(_existingGameHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "game_history(com.example.funlife.data.model.GameHistory).\n"
                  + " Expected:\n" + _infoGameHistory + "\n"
                  + " Found:\n" + _existingGameHistory);
        }
        final HashMap<String, TableInfo.Column> _columnsUserPreferences = new HashMap<String, TableInfo.Column>(10);
        _columnsUserPreferences.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserPreferences.put("isDarkMode", new TableInfo.Column("isDarkMode", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserPreferences.put("enableNotifications", new TableInfo.Column("enableNotifications", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserPreferences.put("notificationDaysBefore", new TableInfo.Column("notificationDaysBefore", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserPreferences.put("defaultScoreIncrement", new TableInfo.Column("defaultScoreIncrement", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserPreferences.put("enableSound", new TableInfo.Column("enableSound", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserPreferences.put("enableVibration", new TableInfo.Column("enableVibration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserPreferences.put("autoBackup", new TableInfo.Column("autoBackup", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserPreferences.put("language", new TableInfo.Column("language", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserPreferences.put("sortOrder", new TableInfo.Column("sortOrder", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserPreferences = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserPreferences = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserPreferences = new TableInfo("user_preferences", _columnsUserPreferences, _foreignKeysUserPreferences, _indicesUserPreferences);
        final TableInfo _existingUserPreferences = TableInfo.read(db, "user_preferences");
        if (!_infoUserPreferences.equals(_existingUserPreferences)) {
          return new RoomOpenHelper.ValidationResult(false, "user_preferences(com.example.funlife.data.model.UserPreferences).\n"
                  + " Expected:\n" + _infoUserPreferences + "\n"
                  + " Found:\n" + _existingUserPreferences);
        }
        final HashMap<String, TableInfo.Column> _columnsSpinWheelTemplates = new HashMap<String, TableInfo.Column>(8);
        _columnsSpinWheelTemplates.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelTemplates.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelTemplates.put("options", new TableInfo.Column("options", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelTemplates.put("weights", new TableInfo.Column("weights", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelTemplates.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelTemplates.put("isDefault", new TableInfo.Column("isDefault", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelTemplates.put("usageCount", new TableInfo.Column("usageCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelTemplates.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSpinWheelTemplates = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSpinWheelTemplates = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSpinWheelTemplates = new TableInfo("spin_wheel_templates", _columnsSpinWheelTemplates, _foreignKeysSpinWheelTemplates, _indicesSpinWheelTemplates);
        final TableInfo _existingSpinWheelTemplates = TableInfo.read(db, "spin_wheel_templates");
        if (!_infoSpinWheelTemplates.equals(_existingSpinWheelTemplates)) {
          return new RoomOpenHelper.ValidationResult(false, "spin_wheel_templates(com.example.funlife.data.model.SpinWheelTemplate).\n"
                  + " Expected:\n" + _infoSpinWheelTemplates + "\n"
                  + " Found:\n" + _existingSpinWheelTemplates);
        }
        final HashMap<String, TableInfo.Column> _columnsSpinWheelHistory = new HashMap<String, TableInfo.Column>(9);
        _columnsSpinWheelHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelHistory.put("templateId", new TableInfo.Column("templateId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelHistory.put("templateName", new TableInfo.Column("templateName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelHistory.put("result", new TableInfo.Column("result", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelHistory.put("allOptions", new TableInfo.Column("allOptions", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelHistory.put("mode", new TableInfo.Column("mode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelHistory.put("coinCost", new TableInfo.Column("coinCost", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelHistory.put("coinReward", new TableInfo.Column("coinReward", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpinWheelHistory.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSpinWheelHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSpinWheelHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSpinWheelHistory = new TableInfo("spin_wheel_history", _columnsSpinWheelHistory, _foreignKeysSpinWheelHistory, _indicesSpinWheelHistory);
        final TableInfo _existingSpinWheelHistory = TableInfo.read(db, "spin_wheel_history");
        if (!_infoSpinWheelHistory.equals(_existingSpinWheelHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "spin_wheel_history(com.example.funlife.data.model.SpinWheelHistory).\n"
                  + " Expected:\n" + _infoSpinWheelHistory + "\n"
                  + " Found:\n" + _existingSpinWheelHistory);
        }
        final HashMap<String, TableInfo.Column> _columnsHabits = new HashMap<String, TableInfo.Column>(8);
        _columnsHabits.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("icon", new TableInfo.Column("icon", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("color", new TableInfo.Column("color", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("targetDays", new TableInfo.Column("targetDays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabits.put("makeupCards", new TableInfo.Column("makeupCards", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHabits = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHabits = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHabits = new TableInfo("habits", _columnsHabits, _foreignKeysHabits, _indicesHabits);
        final TableInfo _existingHabits = TableInfo.read(db, "habits");
        if (!_infoHabits.equals(_existingHabits)) {
          return new RoomOpenHelper.ValidationResult(false, "habits(com.example.funlife.data.model.Habit).\n"
                  + " Expected:\n" + _infoHabits + "\n"
                  + " Found:\n" + _existingHabits);
        }
        final HashMap<String, TableInfo.Column> _columnsHabitRecords = new HashMap<String, TableInfo.Column>(5);
        _columnsHabitRecords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabitRecords.put("habitId", new TableInfo.Column("habitId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabitRecords.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabitRecords.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHabitRecords.put("timestamp", new TableInfo.Column("timestamp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHabitRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHabitRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHabitRecords = new TableInfo("habit_records", _columnsHabitRecords, _foreignKeysHabitRecords, _indicesHabitRecords);
        final TableInfo _existingHabitRecords = TableInfo.read(db, "habit_records");
        if (!_infoHabitRecords.equals(_existingHabitRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "habit_records(com.example.funlife.data.model.HabitRecord).\n"
                  + " Expected:\n" + _infoHabitRecords + "\n"
                  + " Found:\n" + _existingHabitRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsMoodEntries = new HashMap<String, TableInfo.Column>(7);
        _columnsMoodEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMoodEntries.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMoodEntries.put("mood", new TableInfo.Column("mood", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMoodEntries.put("moodLevel", new TableInfo.Column("moodLevel", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMoodEntries.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMoodEntries.put("tags", new TableInfo.Column("tags", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMoodEntries.put("timestamp", new TableInfo.Column("timestamp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMoodEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMoodEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMoodEntries = new TableInfo("mood_entries", _columnsMoodEntries, _foreignKeysMoodEntries, _indicesMoodEntries);
        final TableInfo _existingMoodEntries = TableInfo.read(db, "mood_entries");
        if (!_infoMoodEntries.equals(_existingMoodEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "mood_entries(com.example.funlife.data.model.MoodEntry).\n"
                  + " Expected:\n" + _infoMoodEntries + "\n"
                  + " Found:\n" + _existingMoodEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsGoals = new HashMap<String, TableInfo.Column>(9);
        _columnsGoals.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("targetDate", new TableInfo.Column("targetDate", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("progress", new TableInfo.Column("progress", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("isCompleted", new TableInfo.Column("isCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("completedAt", new TableInfo.Column("completedAt", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGoals = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGoals = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGoals = new TableInfo("goals", _columnsGoals, _foreignKeysGoals, _indicesGoals);
        final TableInfo _existingGoals = TableInfo.read(db, "goals");
        if (!_infoGoals.equals(_existingGoals)) {
          return new RoomOpenHelper.ValidationResult(false, "goals(com.example.funlife.data.model.Goal).\n"
                  + " Expected:\n" + _infoGoals + "\n"
                  + " Found:\n" + _existingGoals);
        }
        final HashMap<String, TableInfo.Column> _columnsCountdowns = new HashMap<String, TableInfo.Column>(8);
        _columnsCountdowns.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCountdowns.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCountdowns.put("targetDate", new TableInfo.Column("targetDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCountdowns.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCountdowns.put("icon", new TableInfo.Column("icon", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCountdowns.put("color", new TableInfo.Column("color", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCountdowns.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCountdowns.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCountdowns = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCountdowns = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCountdowns = new TableInfo("countdowns", _columnsCountdowns, _foreignKeysCountdowns, _indicesCountdowns);
        final TableInfo _existingCountdowns = TableInfo.read(db, "countdowns");
        if (!_infoCountdowns.equals(_existingCountdowns)) {
          return new RoomOpenHelper.ValidationResult(false, "countdowns(com.example.funlife.data.model.Countdown).\n"
                  + " Expected:\n" + _infoCountdowns + "\n"
                  + " Found:\n" + _existingCountdowns);
        }
        final HashMap<String, TableInfo.Column> _columnsUserCoins = new HashMap<String, TableInfo.Column>(3);
        _columnsUserCoins.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserCoins.put("coins", new TableInfo.Column("coins", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserCoins.put("totalEarned", new TableInfo.Column("totalEarned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserCoins = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserCoins = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserCoins = new TableInfo("user_coins", _columnsUserCoins, _foreignKeysUserCoins, _indicesUserCoins);
        final TableInfo _existingUserCoins = TableInfo.read(db, "user_coins");
        if (!_infoUserCoins.equals(_existingUserCoins)) {
          return new RoomOpenHelper.ValidationResult(false, "user_coins(com.example.funlife.data.model.UserCoins).\n"
                  + " Expected:\n" + _infoUserCoins + "\n"
                  + " Found:\n" + _existingUserCoins);
        }
        final HashMap<String, TableInfo.Column> _columnsShopItems = new HashMap<String, TableInfo.Column>(8);
        _columnsShopItems.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShopItems.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShopItems.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShopItems.put("icon", new TableInfo.Column("icon", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShopItems.put("price", new TableInfo.Column("price", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShopItems.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShopItems.put("value", new TableInfo.Column("value", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsShopItems.put("isAvailable", new TableInfo.Column("isAvailable", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysShopItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesShopItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoShopItems = new TableInfo("shop_items", _columnsShopItems, _foreignKeysShopItems, _indicesShopItems);
        final TableInfo _existingShopItems = TableInfo.read(db, "shop_items");
        if (!_infoShopItems.equals(_existingShopItems)) {
          return new RoomOpenHelper.ValidationResult(false, "shop_items(com.example.funlife.data.model.ShopItem).\n"
                  + " Expected:\n" + _infoShopItems + "\n"
                  + " Found:\n" + _existingShopItems);
        }
        final HashMap<String, TableInfo.Column> _columnsPurchaseHistory = new HashMap<String, TableInfo.Column>(5);
        _columnsPurchaseHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPurchaseHistory.put("itemId", new TableInfo.Column("itemId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPurchaseHistory.put("itemName", new TableInfo.Column("itemName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPurchaseHistory.put("price", new TableInfo.Column("price", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPurchaseHistory.put("timestamp", new TableInfo.Column("timestamp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPurchaseHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPurchaseHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPurchaseHistory = new TableInfo("purchase_history", _columnsPurchaseHistory, _foreignKeysPurchaseHistory, _indicesPurchaseHistory);
        final TableInfo _existingPurchaseHistory = TableInfo.read(db, "purchase_history");
        if (!_infoPurchaseHistory.equals(_existingPurchaseHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "purchase_history(com.example.funlife.data.model.PurchaseHistory).\n"
                  + " Expected:\n" + _infoPurchaseHistory + "\n"
                  + " Found:\n" + _existingPurchaseHistory);
        }
        final HashMap<String, TableInfo.Column> _columnsGuaranteeCounter = new HashMap<String, TableInfo.Column>(6);
        _columnsGuaranteeCounter.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGuaranteeCounter.put("optionText", new TableInfo.Column("optionText", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGuaranteeCounter.put("currentCount", new TableInfo.Column("currentCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGuaranteeCounter.put("guaranteeThreshold", new TableInfo.Column("guaranteeThreshold", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGuaranteeCounter.put("isEnabled", new TableInfo.Column("isEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGuaranteeCounter.put("lastUpdated", new TableInfo.Column("lastUpdated", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGuaranteeCounter = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGuaranteeCounter = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGuaranteeCounter = new TableInfo("guarantee_counter", _columnsGuaranteeCounter, _foreignKeysGuaranteeCounter, _indicesGuaranteeCounter);
        final TableInfo _existingGuaranteeCounter = TableInfo.read(db, "guarantee_counter");
        if (!_infoGuaranteeCounter.equals(_existingGuaranteeCounter)) {
          return new RoomOpenHelper.ValidationResult(false, "guarantee_counter(com.example.funlife.data.model.GuaranteeCounter).\n"
                  + " Expected:\n" + _infoGuaranteeCounter + "\n"
                  + " Found:\n" + _existingGuaranteeCounter);
        }
        final HashMap<String, TableInfo.Column> _columnsCustomSpinModes = new HashMap<String, TableInfo.Column>(14);
        _columnsCustomSpinModes.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomSpinModes.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomSpinModes.put("emoji", new TableInfo.Column("emoji", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomSpinModes.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomSpinModes.put("costPerSpin", new TableInfo.Column("costPerSpin", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomSpinModes.put("hasReward", new TableInfo.Column("hasReward", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomSpinModes.put("rewardMultiplier", new TableInfo.Column("rewardMultiplier", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomSpinModes.put("primaryColor", new TableInfo.Column("primaryColor", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomSpinModes.put("secondaryColor", new TableInfo.Column("secondaryColor", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomSpinModes.put("isDefault", new TableInfo.Column("isDefault", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomSpinModes.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomSpinModes.put("usageCount", new TableInfo.Column("usageCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomSpinModes.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomSpinModes.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCustomSpinModes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCustomSpinModes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCustomSpinModes = new TableInfo("custom_spin_modes", _columnsCustomSpinModes, _foreignKeysCustomSpinModes, _indicesCustomSpinModes);
        final TableInfo _existingCustomSpinModes = TableInfo.read(db, "custom_spin_modes");
        if (!_infoCustomSpinModes.equals(_existingCustomSpinModes)) {
          return new RoomOpenHelper.ValidationResult(false, "custom_spin_modes(com.example.funlife.data.model.CustomSpinMode).\n"
                  + " Expected:\n" + _infoCustomSpinModes + "\n"
                  + " Found:\n" + _existingCustomSpinModes);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "435e5a637697122c4bee64e0e6059060", "2a1eb71a7c47582ad5d3d184e756d29e");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "anniversaries","players","game_history","user_preferences","spin_wheel_templates","spin_wheel_history","habits","habit_records","mood_entries","goals","countdowns","user_coins","shop_items","purchase_history","guarantee_counter","custom_spin_modes");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `anniversaries`");
      _db.execSQL("DELETE FROM `players`");
      _db.execSQL("DELETE FROM `game_history`");
      _db.execSQL("DELETE FROM `user_preferences`");
      _db.execSQL("DELETE FROM `spin_wheel_templates`");
      _db.execSQL("DELETE FROM `spin_wheel_history`");
      _db.execSQL("DELETE FROM `habits`");
      _db.execSQL("DELETE FROM `habit_records`");
      _db.execSQL("DELETE FROM `mood_entries`");
      _db.execSQL("DELETE FROM `goals`");
      _db.execSQL("DELETE FROM `countdowns`");
      _db.execSQL("DELETE FROM `user_coins`");
      _db.execSQL("DELETE FROM `shop_items`");
      _db.execSQL("DELETE FROM `purchase_history`");
      _db.execSQL("DELETE FROM `guarantee_counter`");
      _db.execSQL("DELETE FROM `custom_spin_modes`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(AnniversaryDao.class, AnniversaryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PlayerDao.class, PlayerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GameHistoryDao.class, GameHistoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(UserPreferencesDao.class, UserPreferencesDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SpinWheelTemplateDao.class, SpinWheelTemplateDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SpinWheelHistoryDao.class, SpinWheelHistoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HabitDao.class, HabitDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MoodDao.class, MoodDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GoalDao.class, GoalDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CoinDao.class, CoinDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ShopDao.class, ShopDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GuaranteeCounterDao.class, GuaranteeCounterDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CustomSpinModeDao.class, CustomSpinModeDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public AnniversaryDao anniversaryDao() {
    if (_anniversaryDao != null) {
      return _anniversaryDao;
    } else {
      synchronized(this) {
        if(_anniversaryDao == null) {
          _anniversaryDao = new AnniversaryDao_Impl(this);
        }
        return _anniversaryDao;
      }
    }
  }

  @Override
  public PlayerDao playerDao() {
    if (_playerDao != null) {
      return _playerDao;
    } else {
      synchronized(this) {
        if(_playerDao == null) {
          _playerDao = new PlayerDao_Impl(this);
        }
        return _playerDao;
      }
    }
  }

  @Override
  public GameHistoryDao gameHistoryDao() {
    if (_gameHistoryDao != null) {
      return _gameHistoryDao;
    } else {
      synchronized(this) {
        if(_gameHistoryDao == null) {
          _gameHistoryDao = new GameHistoryDao_Impl(this);
        }
        return _gameHistoryDao;
      }
    }
  }

  @Override
  public UserPreferencesDao userPreferencesDao() {
    if (_userPreferencesDao != null) {
      return _userPreferencesDao;
    } else {
      synchronized(this) {
        if(_userPreferencesDao == null) {
          _userPreferencesDao = new UserPreferencesDao_Impl(this);
        }
        return _userPreferencesDao;
      }
    }
  }

  @Override
  public SpinWheelTemplateDao spinWheelTemplateDao() {
    if (_spinWheelTemplateDao != null) {
      return _spinWheelTemplateDao;
    } else {
      synchronized(this) {
        if(_spinWheelTemplateDao == null) {
          _spinWheelTemplateDao = new SpinWheelTemplateDao_Impl(this);
        }
        return _spinWheelTemplateDao;
      }
    }
  }

  @Override
  public SpinWheelHistoryDao spinWheelHistoryDao() {
    if (_spinWheelHistoryDao != null) {
      return _spinWheelHistoryDao;
    } else {
      synchronized(this) {
        if(_spinWheelHistoryDao == null) {
          _spinWheelHistoryDao = new SpinWheelHistoryDao_Impl(this);
        }
        return _spinWheelHistoryDao;
      }
    }
  }

  @Override
  public HabitDao habitDao() {
    if (_habitDao != null) {
      return _habitDao;
    } else {
      synchronized(this) {
        if(_habitDao == null) {
          _habitDao = new HabitDao_Impl(this);
        }
        return _habitDao;
      }
    }
  }

  @Override
  public MoodDao moodDao() {
    if (_moodDao != null) {
      return _moodDao;
    } else {
      synchronized(this) {
        if(_moodDao == null) {
          _moodDao = new MoodDao_Impl(this);
        }
        return _moodDao;
      }
    }
  }

  @Override
  public GoalDao goalDao() {
    if (_goalDao != null) {
      return _goalDao;
    } else {
      synchronized(this) {
        if(_goalDao == null) {
          _goalDao = new GoalDao_Impl(this);
        }
        return _goalDao;
      }
    }
  }

  @Override
  public CoinDao coinDao() {
    if (_coinDao != null) {
      return _coinDao;
    } else {
      synchronized(this) {
        if(_coinDao == null) {
          _coinDao = new CoinDao_Impl(this);
        }
        return _coinDao;
      }
    }
  }

  @Override
  public ShopDao shopDao() {
    if (_shopDao != null) {
      return _shopDao;
    } else {
      synchronized(this) {
        if(_shopDao == null) {
          _shopDao = new ShopDao_Impl(this);
        }
        return _shopDao;
      }
    }
  }

  @Override
  public GuaranteeCounterDao guaranteeCounterDao() {
    if (_guaranteeCounterDao != null) {
      return _guaranteeCounterDao;
    } else {
      synchronized(this) {
        if(_guaranteeCounterDao == null) {
          _guaranteeCounterDao = new GuaranteeCounterDao_Impl(this);
        }
        return _guaranteeCounterDao;
      }
    }
  }

  @Override
  public CustomSpinModeDao customSpinModeDao() {
    if (_customSpinModeDao != null) {
      return _customSpinModeDao;
    } else {
      synchronized(this) {
        if(_customSpinModeDao == null) {
          _customSpinModeDao = new CustomSpinModeDao_Impl(this);
        }
        return _customSpinModeDao;
      }
    }
  }
}
