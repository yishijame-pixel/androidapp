package com.example.funlife.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
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
import com.example.funlife.data.model.GuaranteeCounter;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GuaranteeCounterDao_Impl implements GuaranteeCounterDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GuaranteeCounter> __insertionAdapterOfGuaranteeCounter;

  private final EntityDeletionOrUpdateAdapter<GuaranteeCounter> __deletionAdapterOfGuaranteeCounter;

  private final EntityDeletionOrUpdateAdapter<GuaranteeCounter> __updateAdapterOfGuaranteeCounter;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  private final SharedSQLiteStatement __preparedStmtOfIncrementCounter;

  private final SharedSQLiteStatement __preparedStmtOfResetCounter;

  private final SharedSQLiteStatement __preparedStmtOfResetAllCounters;

  public GuaranteeCounterDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGuaranteeCounter = new EntityInsertionAdapter<GuaranteeCounter>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `guarantee_counter` (`id`,`optionText`,`currentCount`,`guaranteeThreshold`,`isEnabled`,`lastUpdated`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GuaranteeCounter entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getOptionText());
        statement.bindLong(3, entity.getCurrentCount());
        statement.bindLong(4, entity.getGuaranteeThreshold());
        final int _tmp = entity.isEnabled() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getLastUpdated());
      }
    };
    this.__deletionAdapterOfGuaranteeCounter = new EntityDeletionOrUpdateAdapter<GuaranteeCounter>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `guarantee_counter` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GuaranteeCounter entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfGuaranteeCounter = new EntityDeletionOrUpdateAdapter<GuaranteeCounter>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `guarantee_counter` SET `id` = ?,`optionText` = ?,`currentCount` = ?,`guaranteeThreshold` = ?,`isEnabled` = ?,`lastUpdated` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GuaranteeCounter entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getOptionText());
        statement.bindLong(3, entity.getCurrentCount());
        statement.bindLong(4, entity.getGuaranteeThreshold());
        final int _tmp = entity.isEnabled() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getLastUpdated());
        statement.bindLong(7, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM guarantee_counter";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementCounter = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE guarantee_counter SET currentCount = currentCount + 1, lastUpdated = ? WHERE optionText = ?";
        return _query;
      }
    };
    this.__preparedStmtOfResetCounter = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE guarantee_counter SET currentCount = 0, lastUpdated = ? WHERE optionText = ?";
        return _query;
      }
    };
    this.__preparedStmtOfResetAllCounters = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE guarantee_counter SET currentCount = 0, lastUpdated = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final GuaranteeCounter counter,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGuaranteeCounter.insert(counter);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final GuaranteeCounter counter,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfGuaranteeCounter.handle(counter);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final GuaranteeCounter counter,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfGuaranteeCounter.handle(counter);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
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
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementCounter(final String optionText, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementCounter.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, optionText);
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
          __preparedStmtOfIncrementCounter.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object resetCounter(final String optionText, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfResetCounter.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, optionText);
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
          __preparedStmtOfResetCounter.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object resetAllCounters(final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfResetAllCounters.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
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
          __preparedStmtOfResetAllCounters.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GuaranteeCounter>> getAllEnabledCounters() {
    final String _sql = "SELECT * FROM guarantee_counter WHERE isEnabled = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"guarantee_counter"}, new Callable<List<GuaranteeCounter>>() {
      @Override
      @NonNull
      public List<GuaranteeCounter> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfOptionText = CursorUtil.getColumnIndexOrThrow(_cursor, "optionText");
          final int _cursorIndexOfCurrentCount = CursorUtil.getColumnIndexOrThrow(_cursor, "currentCount");
          final int _cursorIndexOfGuaranteeThreshold = CursorUtil.getColumnIndexOrThrow(_cursor, "guaranteeThreshold");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final List<GuaranteeCounter> _result = new ArrayList<GuaranteeCounter>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GuaranteeCounter _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpOptionText;
            _tmpOptionText = _cursor.getString(_cursorIndexOfOptionText);
            final int _tmpCurrentCount;
            _tmpCurrentCount = _cursor.getInt(_cursorIndexOfCurrentCount);
            final int _tmpGuaranteeThreshold;
            _tmpGuaranteeThreshold = _cursor.getInt(_cursorIndexOfGuaranteeThreshold);
            final boolean _tmpIsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp != 0;
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _item = new GuaranteeCounter(_tmpId,_tmpOptionText,_tmpCurrentCount,_tmpGuaranteeThreshold,_tmpIsEnabled,_tmpLastUpdated);
            _result.add(_item);
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

  @Override
  public Object getCounterByOption(final String optionText,
      final Continuation<? super GuaranteeCounter> $completion) {
    final String _sql = "SELECT * FROM guarantee_counter WHERE optionText = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, optionText);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<GuaranteeCounter>() {
      @Override
      @Nullable
      public GuaranteeCounter call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfOptionText = CursorUtil.getColumnIndexOrThrow(_cursor, "optionText");
          final int _cursorIndexOfCurrentCount = CursorUtil.getColumnIndexOrThrow(_cursor, "currentCount");
          final int _cursorIndexOfGuaranteeThreshold = CursorUtil.getColumnIndexOrThrow(_cursor, "guaranteeThreshold");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUpdated");
          final GuaranteeCounter _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpOptionText;
            _tmpOptionText = _cursor.getString(_cursorIndexOfOptionText);
            final int _tmpCurrentCount;
            _tmpCurrentCount = _cursor.getInt(_cursorIndexOfCurrentCount);
            final int _tmpGuaranteeThreshold;
            _tmpGuaranteeThreshold = _cursor.getInt(_cursorIndexOfGuaranteeThreshold);
            final boolean _tmpIsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp != 0;
            final long _tmpLastUpdated;
            _tmpLastUpdated = _cursor.getLong(_cursorIndexOfLastUpdated);
            _result = new GuaranteeCounter(_tmpId,_tmpOptionText,_tmpCurrentCount,_tmpGuaranteeThreshold,_tmpIsEnabled,_tmpLastUpdated);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
