package com.example.funlife.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.funlife.data.model.GameHistory;
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
public final class GameHistoryDao_Impl implements GameHistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GameHistory> __insertionAdapterOfGameHistory;

  private final EntityDeletionOrUpdateAdapter<GameHistory> __deletionAdapterOfGameHistory;

  private final SharedSQLiteStatement __preparedStmtOfClearAllHistory;

  private final SharedSQLiteStatement __preparedStmtOfDeleteHistoryBefore;

  public GameHistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGameHistory = new EntityInsertionAdapter<GameHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `game_history` (`id`,`gameType`,`playerName`,`score`,`result`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GameHistory entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getGameType());
        statement.bindString(3, entity.getPlayerName());
        statement.bindLong(4, entity.getScore());
        statement.bindString(5, entity.getResult());
        statement.bindString(6, entity.getTimestamp());
      }
    };
    this.__deletionAdapterOfGameHistory = new EntityDeletionOrUpdateAdapter<GameHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `game_history` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GameHistory entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfClearAllHistory = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM game_history";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteHistoryBefore = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM game_history WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertHistory(final GameHistory history,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGameHistory.insert(history);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteHistory(final GameHistory history,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfGameHistory.handle(history);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAllHistory(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllHistory.acquire();
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
          __preparedStmtOfClearAllHistory.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteHistoryBefore(final String beforeDate,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteHistoryBefore.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, beforeDate);
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
          __preparedStmtOfDeleteHistoryBefore.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GameHistory>> getAllHistory() {
    final String _sql = "SELECT * FROM game_history ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"game_history"}, new Callable<List<GameHistory>>() {
      @Override
      @NonNull
      public List<GameHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGameType = CursorUtil.getColumnIndexOrThrow(_cursor, "gameType");
          final int _cursorIndexOfPlayerName = CursorUtil.getColumnIndexOrThrow(_cursor, "playerName");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<GameHistory> _result = new ArrayList<GameHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GameHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpGameType;
            _tmpGameType = _cursor.getString(_cursorIndexOfGameType);
            final String _tmpPlayerName;
            _tmpPlayerName = _cursor.getString(_cursorIndexOfPlayerName);
            final int _tmpScore;
            _tmpScore = _cursor.getInt(_cursorIndexOfScore);
            final String _tmpResult;
            _tmpResult = _cursor.getString(_cursorIndexOfResult);
            final String _tmpTimestamp;
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp);
            _item = new GameHistory(_tmpId,_tmpGameType,_tmpPlayerName,_tmpScore,_tmpResult,_tmpTimestamp);
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
  public Flow<List<GameHistory>> getHistoryByType(final String gameType) {
    final String _sql = "SELECT * FROM game_history WHERE gameType = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, gameType);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"game_history"}, new Callable<List<GameHistory>>() {
      @Override
      @NonNull
      public List<GameHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGameType = CursorUtil.getColumnIndexOrThrow(_cursor, "gameType");
          final int _cursorIndexOfPlayerName = CursorUtil.getColumnIndexOrThrow(_cursor, "playerName");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<GameHistory> _result = new ArrayList<GameHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GameHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpGameType;
            _tmpGameType = _cursor.getString(_cursorIndexOfGameType);
            final String _tmpPlayerName;
            _tmpPlayerName = _cursor.getString(_cursorIndexOfPlayerName);
            final int _tmpScore;
            _tmpScore = _cursor.getInt(_cursorIndexOfScore);
            final String _tmpResult;
            _tmpResult = _cursor.getString(_cursorIndexOfResult);
            final String _tmpTimestamp;
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp);
            _item = new GameHistory(_tmpId,_tmpGameType,_tmpPlayerName,_tmpScore,_tmpResult,_tmpTimestamp);
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
  public Flow<List<GameHistory>> getHistoryByPlayer(final String playerName) {
    final String _sql = "SELECT * FROM game_history WHERE playerName = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, playerName);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"game_history"}, new Callable<List<GameHistory>>() {
      @Override
      @NonNull
      public List<GameHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGameType = CursorUtil.getColumnIndexOrThrow(_cursor, "gameType");
          final int _cursorIndexOfPlayerName = CursorUtil.getColumnIndexOrThrow(_cursor, "playerName");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<GameHistory> _result = new ArrayList<GameHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GameHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpGameType;
            _tmpGameType = _cursor.getString(_cursorIndexOfGameType);
            final String _tmpPlayerName;
            _tmpPlayerName = _cursor.getString(_cursorIndexOfPlayerName);
            final int _tmpScore;
            _tmpScore = _cursor.getInt(_cursorIndexOfScore);
            final String _tmpResult;
            _tmpResult = _cursor.getString(_cursorIndexOfResult);
            final String _tmpTimestamp;
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp);
            _item = new GameHistory(_tmpId,_tmpGameType,_tmpPlayerName,_tmpScore,_tmpResult,_tmpTimestamp);
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
  public Flow<List<GameHistory>> getRecentHistory(final int limit) {
    final String _sql = "SELECT * FROM game_history ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"game_history"}, new Callable<List<GameHistory>>() {
      @Override
      @NonNull
      public List<GameHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGameType = CursorUtil.getColumnIndexOrThrow(_cursor, "gameType");
          final int _cursorIndexOfPlayerName = CursorUtil.getColumnIndexOrThrow(_cursor, "playerName");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfResult = CursorUtil.getColumnIndexOrThrow(_cursor, "result");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<GameHistory> _result = new ArrayList<GameHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GameHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpGameType;
            _tmpGameType = _cursor.getString(_cursorIndexOfGameType);
            final String _tmpPlayerName;
            _tmpPlayerName = _cursor.getString(_cursorIndexOfPlayerName);
            final int _tmpScore;
            _tmpScore = _cursor.getInt(_cursorIndexOfScore);
            final String _tmpResult;
            _tmpResult = _cursor.getString(_cursorIndexOfResult);
            final String _tmpTimestamp;
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp);
            _item = new GameHistory(_tmpId,_tmpGameType,_tmpPlayerName,_tmpScore,_tmpResult,_tmpTimestamp);
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
  public Object getCountByType(final String gameType,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM game_history WHERE gameType = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, gameType);
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
  public Object getTotalPlayers(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(DISTINCT playerName) FROM game_history";
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
