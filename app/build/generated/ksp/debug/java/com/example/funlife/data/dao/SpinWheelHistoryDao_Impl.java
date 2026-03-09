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
import com.example.funlife.data.model.SpinWheelHistory;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
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
public final class SpinWheelHistoryDao_Impl implements SpinWheelHistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SpinWheelHistory> __insertionAdapterOfSpinWheelHistory;

  private final EntityDeletionOrUpdateAdapter<SpinWheelHistory> __deletionAdapterOfSpinWheelHistory;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public SpinWheelHistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSpinWheelHistory = new EntityInsertionAdapter<SpinWheelHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `spin_wheel_history` (`id`,`templateId`,`templateName`,`result`,`allOptions`,`mode`,`coinCost`,`coinReward`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SpinWheelHistory entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getTemplateId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getTemplateId());
        }
        statement.bindString(3, entity.getTemplateName());
        statement.bindString(4, entity.getResult());
        statement.bindString(5, entity.getAllOptions());
        statement.bindString(6, entity.getMode());
        statement.bindLong(7, entity.getCoinCost());
        statement.bindLong(8, entity.getCoinReward());
        statement.bindLong(9, entity.getTimestamp());
      }
    };
    this.__deletionAdapterOfSpinWheelHistory = new EntityDeletionOrUpdateAdapter<SpinWheelHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `spin_wheel_history` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SpinWheelHistory entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM spin_wheel_history";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final SpinWheelHistory history,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSpinWheelHistory.insertAndReturnId(history);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final SpinWheelHistory history,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSpinWheelHistory.handle(history);
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
  public Flow<List<SpinWheelHistory>> getAllHistory() {
    final String _sql = "SELECT * FROM spin_wheel_history ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spin_wheel_history"}, new Callable<List<SpinWheelHistory>>() {
      @Override
      @NonNull
      public List<SpinWheelHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTemplateId = CursorUtil.getColumnIndexOrThrow(_cursor, "templateId");
          final int _cursorIndexOfTemplateName = CursorUtil.getColumnIndexOrThrow(_cursor, "templateName");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfAllOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "allOptions");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfCoinCost = CursorUtil.getColumnIndexOrThrow(_cursor, "coinCost");
          final int _cursorIndexOfCoinReward = CursorUtil.getColumnIndexOrThrow(_cursor, "coinReward");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<SpinWheelHistory> _result = new ArrayList<SpinWheelHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpinWheelHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final Integer _tmpTemplateId;
            if (_cursor.isNull(_cursorIndexOfTemplateId)) {
              _tmpTemplateId = null;
            } else {
              _tmpTemplateId = _cursor.getInt(_cursorIndexOfTemplateId);
            }
            final String _tmpTemplateName;
            _tmpTemplateName = _cursor.getString(_cursorIndexOfTemplateName);
            final String _tmpResult;
            _tmpResult = _cursor.getString(_cursorIndexOfResult);
            final String _tmpAllOptions;
            _tmpAllOptions = _cursor.getString(_cursorIndexOfAllOptions);
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            final int _tmpCoinCost;
            _tmpCoinCost = _cursor.getInt(_cursorIndexOfCoinCost);
            final int _tmpCoinReward;
            _tmpCoinReward = _cursor.getInt(_cursorIndexOfCoinReward);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new SpinWheelHistory(_tmpId,_tmpTemplateId,_tmpTemplateName,_tmpResult,_tmpAllOptions,_tmpMode,_tmpCoinCost,_tmpCoinReward,_tmpTimestamp);
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
  public Flow<List<SpinWheelHistory>> getHistoryByTemplate(final int templateId) {
    final String _sql = "SELECT * FROM spin_wheel_history WHERE templateId = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, templateId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spin_wheel_history"}, new Callable<List<SpinWheelHistory>>() {
      @Override
      @NonNull
      public List<SpinWheelHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTemplateId = CursorUtil.getColumnIndexOrThrow(_cursor, "templateId");
          final int _cursorIndexOfTemplateName = CursorUtil.getColumnIndexOrThrow(_cursor, "templateName");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfAllOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "allOptions");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfCoinCost = CursorUtil.getColumnIndexOrThrow(_cursor, "coinCost");
          final int _cursorIndexOfCoinReward = CursorUtil.getColumnIndexOrThrow(_cursor, "coinReward");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<SpinWheelHistory> _result = new ArrayList<SpinWheelHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpinWheelHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final Integer _tmpTemplateId;
            if (_cursor.isNull(_cursorIndexOfTemplateId)) {
              _tmpTemplateId = null;
            } else {
              _tmpTemplateId = _cursor.getInt(_cursorIndexOfTemplateId);
            }
            final String _tmpTemplateName;
            _tmpTemplateName = _cursor.getString(_cursorIndexOfTemplateName);
            final String _tmpResult;
            _tmpResult = _cursor.getString(_cursorIndexOfResult);
            final String _tmpAllOptions;
            _tmpAllOptions = _cursor.getString(_cursorIndexOfAllOptions);
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            final int _tmpCoinCost;
            _tmpCoinCost = _cursor.getInt(_cursorIndexOfCoinCost);
            final int _tmpCoinReward;
            _tmpCoinReward = _cursor.getInt(_cursorIndexOfCoinReward);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new SpinWheelHistory(_tmpId,_tmpTemplateId,_tmpTemplateName,_tmpResult,_tmpAllOptions,_tmpMode,_tmpCoinCost,_tmpCoinReward,_tmpTimestamp);
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
  public Flow<List<SpinWheelHistory>> getRecentHistory(final int limit) {
    final String _sql = "SELECT * FROM spin_wheel_history ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spin_wheel_history"}, new Callable<List<SpinWheelHistory>>() {
      @Override
      @NonNull
      public List<SpinWheelHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTemplateId = CursorUtil.getColumnIndexOrThrow(_cursor, "templateId");
          final int _cursorIndexOfTemplateName = CursorUtil.getColumnIndexOrThrow(_cursor, "templateName");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfAllOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "allOptions");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfCoinCost = CursorUtil.getColumnIndexOrThrow(_cursor, "coinCost");
          final int _cursorIndexOfCoinReward = CursorUtil.getColumnIndexOrThrow(_cursor, "coinReward");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<SpinWheelHistory> _result = new ArrayList<SpinWheelHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpinWheelHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final Integer _tmpTemplateId;
            if (_cursor.isNull(_cursorIndexOfTemplateId)) {
              _tmpTemplateId = null;
            } else {
              _tmpTemplateId = _cursor.getInt(_cursorIndexOfTemplateId);
            }
            final String _tmpTemplateName;
            _tmpTemplateName = _cursor.getString(_cursorIndexOfTemplateName);
            final String _tmpResult;
            _tmpResult = _cursor.getString(_cursorIndexOfResult);
            final String _tmpAllOptions;
            _tmpAllOptions = _cursor.getString(_cursorIndexOfAllOptions);
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            final int _tmpCoinCost;
            _tmpCoinCost = _cursor.getInt(_cursorIndexOfCoinCost);
            final int _tmpCoinReward;
            _tmpCoinReward = _cursor.getInt(_cursorIndexOfCoinReward);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new SpinWheelHistory(_tmpId,_tmpTemplateId,_tmpTemplateName,_tmpResult,_tmpAllOptions,_tmpMode,_tmpCoinCost,_tmpCoinReward,_tmpTimestamp);
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
  public Object getCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM spin_wheel_history";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTotalCoinCost(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT SUM(coinCost) FROM spin_wheel_history";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @Nullable
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
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

  @Override
  public Object getTotalCoinReward(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT SUM(coinReward) FROM spin_wheel_history";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @Nullable
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
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

  @Override
  public Object getOptionHitCount(final String option,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM spin_wheel_history WHERE result = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, option);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTemplateUsageCount(final int templateId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM spin_wheel_history WHERE templateId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, templateId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SpinWheelHistory>> getHistoryByDateRange(final long startTime,
      final long endTime) {
    final String _sql = "SELECT * FROM spin_wheel_history WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startTime);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endTime);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spin_wheel_history"}, new Callable<List<SpinWheelHistory>>() {
      @Override
      @NonNull
      public List<SpinWheelHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTemplateId = CursorUtil.getColumnIndexOrThrow(_cursor, "templateId");
          final int _cursorIndexOfTemplateName = CursorUtil.getColumnIndexOrThrow(_cursor, "templateName");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfAllOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "allOptions");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfCoinCost = CursorUtil.getColumnIndexOrThrow(_cursor, "coinCost");
          final int _cursorIndexOfCoinReward = CursorUtil.getColumnIndexOrThrow(_cursor, "coinReward");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<SpinWheelHistory> _result = new ArrayList<SpinWheelHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpinWheelHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final Integer _tmpTemplateId;
            if (_cursor.isNull(_cursorIndexOfTemplateId)) {
              _tmpTemplateId = null;
            } else {
              _tmpTemplateId = _cursor.getInt(_cursorIndexOfTemplateId);
            }
            final String _tmpTemplateName;
            _tmpTemplateName = _cursor.getString(_cursorIndexOfTemplateName);
            final String _tmpResult;
            _tmpResult = _cursor.getString(_cursorIndexOfResult);
            final String _tmpAllOptions;
            _tmpAllOptions = _cursor.getString(_cursorIndexOfAllOptions);
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            final int _tmpCoinCost;
            _tmpCoinCost = _cursor.getInt(_cursorIndexOfCoinCost);
            final int _tmpCoinReward;
            _tmpCoinReward = _cursor.getInt(_cursorIndexOfCoinReward);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new SpinWheelHistory(_tmpId,_tmpTemplateId,_tmpTemplateName,_tmpResult,_tmpAllOptions,_tmpMode,_tmpCoinCost,_tmpCoinReward,_tmpTimestamp);
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
  public Flow<List<SpinWheelHistory>> getHistoryByMode(final String mode) {
    final String _sql = "SELECT * FROM spin_wheel_history WHERE mode = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, mode);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spin_wheel_history"}, new Callable<List<SpinWheelHistory>>() {
      @Override
      @NonNull
      public List<SpinWheelHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTemplateId = CursorUtil.getColumnIndexOrThrow(_cursor, "templateId");
          final int _cursorIndexOfTemplateName = CursorUtil.getColumnIndexOrThrow(_cursor, "templateName");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfAllOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "allOptions");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfCoinCost = CursorUtil.getColumnIndexOrThrow(_cursor, "coinCost");
          final int _cursorIndexOfCoinReward = CursorUtil.getColumnIndexOrThrow(_cursor, "coinReward");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<SpinWheelHistory> _result = new ArrayList<SpinWheelHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpinWheelHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final Integer _tmpTemplateId;
            if (_cursor.isNull(_cursorIndexOfTemplateId)) {
              _tmpTemplateId = null;
            } else {
              _tmpTemplateId = _cursor.getInt(_cursorIndexOfTemplateId);
            }
            final String _tmpTemplateName;
            _tmpTemplateName = _cursor.getString(_cursorIndexOfTemplateName);
            final String _tmpResult;
            _tmpResult = _cursor.getString(_cursorIndexOfResult);
            final String _tmpAllOptions;
            _tmpAllOptions = _cursor.getString(_cursorIndexOfAllOptions);
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            final int _tmpCoinCost;
            _tmpCoinCost = _cursor.getInt(_cursorIndexOfCoinCost);
            final int _tmpCoinReward;
            _tmpCoinReward = _cursor.getInt(_cursorIndexOfCoinReward);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new SpinWheelHistory(_tmpId,_tmpTemplateId,_tmpTemplateName,_tmpResult,_tmpAllOptions,_tmpMode,_tmpCoinCost,_tmpCoinReward,_tmpTimestamp);
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
  public Flow<List<SpinWheelHistory>> getHistoryByDateRangeAndMode(final long startTime,
      final long endTime, final String mode) {
    final String _sql = "SELECT * FROM spin_wheel_history WHERE timestamp BETWEEN ? AND ? AND mode = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startTime);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endTime);
    _argIndex = 3;
    _statement.bindString(_argIndex, mode);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spin_wheel_history"}, new Callable<List<SpinWheelHistory>>() {
      @Override
      @NonNull
      public List<SpinWheelHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTemplateId = CursorUtil.getColumnIndexOrThrow(_cursor, "templateId");
          final int _cursorIndexOfTemplateName = CursorUtil.getColumnIndexOrThrow(_cursor, "templateName");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfAllOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "allOptions");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfCoinCost = CursorUtil.getColumnIndexOrThrow(_cursor, "coinCost");
          final int _cursorIndexOfCoinReward = CursorUtil.getColumnIndexOrThrow(_cursor, "coinReward");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<SpinWheelHistory> _result = new ArrayList<SpinWheelHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpinWheelHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final Integer _tmpTemplateId;
            if (_cursor.isNull(_cursorIndexOfTemplateId)) {
              _tmpTemplateId = null;
            } else {
              _tmpTemplateId = _cursor.getInt(_cursorIndexOfTemplateId);
            }
            final String _tmpTemplateName;
            _tmpTemplateName = _cursor.getString(_cursorIndexOfTemplateName);
            final String _tmpResult;
            _tmpResult = _cursor.getString(_cursorIndexOfResult);
            final String _tmpAllOptions;
            _tmpAllOptions = _cursor.getString(_cursorIndexOfAllOptions);
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            final int _tmpCoinCost;
            _tmpCoinCost = _cursor.getInt(_cursorIndexOfCoinCost);
            final int _tmpCoinReward;
            _tmpCoinReward = _cursor.getInt(_cursorIndexOfCoinReward);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new SpinWheelHistory(_tmpId,_tmpTemplateId,_tmpTemplateName,_tmpResult,_tmpAllOptions,_tmpMode,_tmpCoinCost,_tmpCoinReward,_tmpTimestamp);
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
  public Flow<List<SpinWheelHistory>> searchHistoryByResult(final String searchQuery) {
    final String _sql = "SELECT * FROM spin_wheel_history WHERE result LIKE '%' || ? || '%' ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, searchQuery);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spin_wheel_history"}, new Callable<List<SpinWheelHistory>>() {
      @Override
      @NonNull
      public List<SpinWheelHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTemplateId = CursorUtil.getColumnIndexOrThrow(_cursor, "templateId");
          final int _cursorIndexOfTemplateName = CursorUtil.getColumnIndexOrThrow(_cursor, "templateName");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfAllOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "allOptions");
          final int _cursorIndexOfMode = CursorUtil.getColumnIndexOrThrow(_cursor, "mode");
          final int _cursorIndexOfCoinCost = CursorUtil.getColumnIndexOrThrow(_cursor, "coinCost");
          final int _cursorIndexOfCoinReward = CursorUtil.getColumnIndexOrThrow(_cursor, "coinReward");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<SpinWheelHistory> _result = new ArrayList<SpinWheelHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpinWheelHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final Integer _tmpTemplateId;
            if (_cursor.isNull(_cursorIndexOfTemplateId)) {
              _tmpTemplateId = null;
            } else {
              _tmpTemplateId = _cursor.getInt(_cursorIndexOfTemplateId);
            }
            final String _tmpTemplateName;
            _tmpTemplateName = _cursor.getString(_cursorIndexOfTemplateName);
            final String _tmpResult;
            _tmpResult = _cursor.getString(_cursorIndexOfResult);
            final String _tmpAllOptions;
            _tmpAllOptions = _cursor.getString(_cursorIndexOfAllOptions);
            final String _tmpMode;
            _tmpMode = _cursor.getString(_cursorIndexOfMode);
            final int _tmpCoinCost;
            _tmpCoinCost = _cursor.getInt(_cursorIndexOfCoinCost);
            final int _tmpCoinReward;
            _tmpCoinReward = _cursor.getInt(_cursorIndexOfCoinReward);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new SpinWheelHistory(_tmpId,_tmpTemplateId,_tmpTemplateName,_tmpResult,_tmpAllOptions,_tmpMode,_tmpCoinCost,_tmpCoinReward,_tmpTimestamp);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
