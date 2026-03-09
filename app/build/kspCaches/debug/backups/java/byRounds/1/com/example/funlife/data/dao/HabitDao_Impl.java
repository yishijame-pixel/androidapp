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
import com.example.funlife.data.model.Habit;
import com.example.funlife.data.model.HabitRecord;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class HabitDao_Impl implements HabitDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Habit> __insertionAdapterOfHabit;

  private final EntityInsertionAdapter<HabitRecord> __insertionAdapterOfHabitRecord;

  private final EntityDeletionOrUpdateAdapter<Habit> __deletionAdapterOfHabit;

  private final EntityDeletionOrUpdateAdapter<HabitRecord> __deletionAdapterOfHabitRecord;

  private final EntityDeletionOrUpdateAdapter<Habit> __updateAdapterOfHabit;

  private final SharedSQLiteStatement __preparedStmtOfUpdateMakeupCards;

  public HabitDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHabit = new EntityInsertionAdapter<Habit>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `habits` (`id`,`name`,`icon`,`color`,`targetDays`,`createdAt`,`isActive`,`makeupCards`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Habit entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getIcon());
        statement.bindString(4, entity.getColor());
        statement.bindLong(5, entity.getTargetDays());
        statement.bindString(6, entity.getCreatedAt());
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getMakeupCards());
      }
    };
    this.__insertionAdapterOfHabitRecord = new EntityInsertionAdapter<HabitRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `habit_records` (`id`,`habitId`,`date`,`note`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HabitRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getHabitId());
        statement.bindString(3, entity.getDate());
        statement.bindString(4, entity.getNote());
        statement.bindString(5, entity.getTimestamp());
      }
    };
    this.__deletionAdapterOfHabit = new EntityDeletionOrUpdateAdapter<Habit>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `habits` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Habit entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__deletionAdapterOfHabitRecord = new EntityDeletionOrUpdateAdapter<HabitRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `habit_records` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HabitRecord entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfHabit = new EntityDeletionOrUpdateAdapter<Habit>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `habits` SET `id` = ?,`name` = ?,`icon` = ?,`color` = ?,`targetDays` = ?,`createdAt` = ?,`isActive` = ?,`makeupCards` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Habit entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getIcon());
        statement.bindString(4, entity.getColor());
        statement.bindLong(5, entity.getTargetDays());
        statement.bindString(6, entity.getCreatedAt());
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getMakeupCards());
        statement.bindLong(9, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateMakeupCards = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE habits SET makeupCards = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertHabit(final Habit habit, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHabit.insert(habit);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertRecord(final HabitRecord record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHabitRecord.insert(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteHabit(final Habit habit, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfHabit.handle(habit);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRecord(final HabitRecord record,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfHabitRecord.handle(record);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateHabit(final Habit habit, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfHabit.handle(habit);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateMakeupCards(final int habitId, final int cards,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateMakeupCards.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, cards);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, habitId);
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
          __preparedStmtOfUpdateMakeupCards.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Habit>> getAllActiveHabits() {
    final String _sql = "SELECT * FROM habits WHERE isActive = 1 ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"habits"}, new Callable<List<Habit>>() {
      @Override
      @NonNull
      public List<Habit> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfTargetDays = CursorUtil.getColumnIndexOrThrow(_cursor, "targetDays");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfMakeupCards = CursorUtil.getColumnIndexOrThrow(_cursor, "makeupCards");
          final List<Habit> _result = new ArrayList<Habit>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Habit _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpIcon;
            _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            final String _tmpColor;
            _tmpColor = _cursor.getString(_cursorIndexOfColor);
            final int _tmpTargetDays;
            _tmpTargetDays = _cursor.getInt(_cursorIndexOfTargetDays);
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final int _tmpMakeupCards;
            _tmpMakeupCards = _cursor.getInt(_cursorIndexOfMakeupCards);
            _item = new Habit(_tmpId,_tmpName,_tmpIcon,_tmpColor,_tmpTargetDays,_tmpCreatedAt,_tmpIsActive,_tmpMakeupCards);
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
  public Flow<List<HabitRecord>> getHabitRecords(final int habitId) {
    final String _sql = "SELECT * FROM habit_records WHERE habitId = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, habitId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"habit_records"}, new Callable<List<HabitRecord>>() {
      @Override
      @NonNull
      public List<HabitRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHabitId = CursorUtil.getColumnIndexOrThrow(_cursor, "habitId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<HabitRecord> _result = new ArrayList<HabitRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HabitRecord _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpHabitId;
            _tmpHabitId = _cursor.getInt(_cursorIndexOfHabitId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpTimestamp;
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp);
            _item = new HabitRecord(_tmpId,_tmpHabitId,_tmpDate,_tmpNote,_tmpTimestamp);
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
  public Object getRecordByDate(final int habitId, final String date,
      final Continuation<? super HabitRecord> $completion) {
    final String _sql = "SELECT * FROM habit_records WHERE habitId = ? AND date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, habitId);
    _argIndex = 2;
    _statement.bindString(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<HabitRecord>() {
      @Override
      @Nullable
      public HabitRecord call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHabitId = CursorUtil.getColumnIndexOrThrow(_cursor, "habitId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final HabitRecord _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpHabitId;
            _tmpHabitId = _cursor.getInt(_cursorIndexOfHabitId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpTimestamp;
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp);
            _result = new HabitRecord(_tmpId,_tmpHabitId,_tmpDate,_tmpNote,_tmpTimestamp);
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
  public Object getHabitRecordCount(final int habitId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM habit_records WHERE habitId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, habitId);
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
  public Object getMakeupCards(final int habitId, final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT makeupCards FROM habits WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, habitId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _result;
          if (_cursor.moveToFirst()) {
            _result = _cursor.getInt(0);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
