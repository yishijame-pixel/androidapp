package com.example.funlife.data.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.funlife.data.model.UserPreferences;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class UserPreferencesDao_Impl implements UserPreferencesDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserPreferences> __insertionAdapterOfUserPreferences;

  private final EntityDeletionOrUpdateAdapter<UserPreferences> __updateAdapterOfUserPreferences;

  private final SharedSQLiteStatement __preparedStmtOfUpdateDarkMode;

  private final SharedSQLiteStatement __preparedStmtOfUpdateNotifications;

  private final SharedSQLiteStatement __preparedStmtOfUpdateScoreIncrement;

  public UserPreferencesDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserPreferences = new EntityInsertionAdapter<UserPreferences>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_preferences` (`id`,`isDarkMode`,`enableNotifications`,`notificationDaysBefore`,`defaultScoreIncrement`,`enableSound`,`enableVibration`,`autoBackup`,`language`,`sortOrder`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserPreferences entity) {
        statement.bindLong(1, entity.getId());
        final int _tmp = entity.isDarkMode() ? 1 : 0;
        statement.bindLong(2, _tmp);
        final int _tmp_1 = entity.getEnableNotifications() ? 1 : 0;
        statement.bindLong(3, _tmp_1);
        statement.bindLong(4, entity.getNotificationDaysBefore());
        statement.bindLong(5, entity.getDefaultScoreIncrement());
        final int _tmp_2 = entity.getEnableSound() ? 1 : 0;
        statement.bindLong(6, _tmp_2);
        final int _tmp_3 = entity.getEnableVibration() ? 1 : 0;
        statement.bindLong(7, _tmp_3);
        final int _tmp_4 = entity.getAutoBackup() ? 1 : 0;
        statement.bindLong(8, _tmp_4);
        statement.bindString(9, entity.getLanguage());
        statement.bindString(10, entity.getSortOrder());
      }
    };
    this.__updateAdapterOfUserPreferences = new EntityDeletionOrUpdateAdapter<UserPreferences>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `user_preferences` SET `id` = ?,`isDarkMode` = ?,`enableNotifications` = ?,`notificationDaysBefore` = ?,`defaultScoreIncrement` = ?,`enableSound` = ?,`enableVibration` = ?,`autoBackup` = ?,`language` = ?,`sortOrder` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserPreferences entity) {
        statement.bindLong(1, entity.getId());
        final int _tmp = entity.isDarkMode() ? 1 : 0;
        statement.bindLong(2, _tmp);
        final int _tmp_1 = entity.getEnableNotifications() ? 1 : 0;
        statement.bindLong(3, _tmp_1);
        statement.bindLong(4, entity.getNotificationDaysBefore());
        statement.bindLong(5, entity.getDefaultScoreIncrement());
        final int _tmp_2 = entity.getEnableSound() ? 1 : 0;
        statement.bindLong(6, _tmp_2);
        final int _tmp_3 = entity.getEnableVibration() ? 1 : 0;
        statement.bindLong(7, _tmp_3);
        final int _tmp_4 = entity.getAutoBackup() ? 1 : 0;
        statement.bindLong(8, _tmp_4);
        statement.bindString(9, entity.getLanguage());
        statement.bindString(10, entity.getSortOrder());
        statement.bindLong(11, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateDarkMode = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_preferences SET isDarkMode = ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateNotifications = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_preferences SET enableNotifications = ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateScoreIncrement = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_preferences SET defaultScoreIncrement = ? WHERE id = 1";
        return _query;
      }
    };
  }

  @Override
  public Object insertPreferences(final UserPreferences preferences,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserPreferences.insert(preferences);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePreferences(final UserPreferences preferences,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfUserPreferences.handle(preferences);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateDarkMode(final boolean isDarkMode,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateDarkMode.acquire();
        int _argIndex = 1;
        final int _tmp = isDarkMode ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateDarkMode.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateNotifications(final boolean enable,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateNotifications.acquire();
        int _argIndex = 1;
        final int _tmp = enable ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateNotifications.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateScoreIncrement(final int increment,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateScoreIncrement.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, increment);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateScoreIncrement.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<UserPreferences> getPreferences() {
    final String _sql = "SELECT * FROM user_preferences WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_preferences"}, new Callable<UserPreferences>() {
      @Override
      @Nullable
      public UserPreferences call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfIsDarkMode = CursorUtil.getColumnIndexOrThrow(_cursor, "isDarkMode");
          final int _cursorIndexOfEnableNotifications = CursorUtil.getColumnIndexOrThrow(_cursor, "enableNotifications");
          final int _cursorIndexOfNotificationDaysBefore = CursorUtil.getColumnIndexOrThrow(_cursor, "notificationDaysBefore");
          final int _cursorIndexOfDefaultScoreIncrement = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultScoreIncrement");
          final int _cursorIndexOfEnableSound = CursorUtil.getColumnIndexOrThrow(_cursor, "enableSound");
          final int _cursorIndexOfEnableVibration = CursorUtil.getColumnIndexOrThrow(_cursor, "enableVibration");
          final int _cursorIndexOfAutoBackup = CursorUtil.getColumnIndexOrThrow(_cursor, "autoBackup");
          final int _cursorIndexOfLanguage = CursorUtil.getColumnIndexOrThrow(_cursor, "language");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final UserPreferences _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final boolean _tmpIsDarkMode;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDarkMode);
            _tmpIsDarkMode = _tmp != 0;
            final boolean _tmpEnableNotifications;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfEnableNotifications);
            _tmpEnableNotifications = _tmp_1 != 0;
            final int _tmpNotificationDaysBefore;
            _tmpNotificationDaysBefore = _cursor.getInt(_cursorIndexOfNotificationDaysBefore);
            final int _tmpDefaultScoreIncrement;
            _tmpDefaultScoreIncrement = _cursor.getInt(_cursorIndexOfDefaultScoreIncrement);
            final boolean _tmpEnableSound;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfEnableSound);
            _tmpEnableSound = _tmp_2 != 0;
            final boolean _tmpEnableVibration;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfEnableVibration);
            _tmpEnableVibration = _tmp_3 != 0;
            final boolean _tmpAutoBackup;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfAutoBackup);
            _tmpAutoBackup = _tmp_4 != 0;
            final String _tmpLanguage;
            _tmpLanguage = _cursor.getString(_cursorIndexOfLanguage);
            final String _tmpSortOrder;
            _tmpSortOrder = _cursor.getString(_cursorIndexOfSortOrder);
            _result = new UserPreferences(_tmpId,_tmpIsDarkMode,_tmpEnableNotifications,_tmpNotificationDaysBefore,_tmpDefaultScoreIncrement,_tmpEnableSound,_tmpEnableVibration,_tmpAutoBackup,_tmpLanguage,_tmpSortOrder);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
