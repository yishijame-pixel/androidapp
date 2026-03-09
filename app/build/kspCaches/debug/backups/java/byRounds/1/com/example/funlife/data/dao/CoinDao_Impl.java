package com.example.funlife.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.funlife.data.model.UserCoins;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class CoinDao_Impl implements CoinDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserCoins> __insertionAdapterOfUserCoins;

  private final SharedSQLiteStatement __preparedStmtOfAddCoins;

  private final SharedSQLiteStatement __preparedStmtOfSpendCoins;

  private final SharedSQLiteStatement __preparedStmtOfInitializeCoins;

  public CoinDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserCoins = new EntityInsertionAdapter<UserCoins>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_coins` (`id`,`coins`,`totalEarned`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserCoins entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getCoins());
        statement.bindLong(3, entity.getTotalEarned());
      }
    };
    this.__preparedStmtOfAddCoins = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_coins SET coins = coins + ?, totalEarned = totalEarned + ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfSpendCoins = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_coins SET coins = coins - ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfInitializeCoins = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "INSERT OR IGNORE INTO user_coins (id, coins, totalEarned) VALUES (1, 0, 0)";
        return _query;
      }
    };
  }

  @Override
  public Object insertUserCoins(final UserCoins userCoins,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserCoins.insert(userCoins);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object addCoins(final int amount, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfAddCoins.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, amount);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, amount);
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
          __preparedStmtOfAddCoins.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object spendCoins(final int amount, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSpendCoins.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, amount);
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
          __preparedStmtOfSpendCoins.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object initializeCoins(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfInitializeCoins.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeInsert();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfInitializeCoins.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<UserCoins> getUserCoins() {
    final String _sql = "SELECT * FROM user_coins WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_coins"}, new Callable<UserCoins>() {
      @Override
      @Nullable
      public UserCoins call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCoins = CursorUtil.getColumnIndexOrThrow(_cursor, "coins");
          final int _cursorIndexOfTotalEarned = CursorUtil.getColumnIndexOrThrow(_cursor, "totalEarned");
          final UserCoins _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpCoins;
            _tmpCoins = _cursor.getInt(_cursorIndexOfCoins);
            final int _tmpTotalEarned;
            _tmpTotalEarned = _cursor.getInt(_cursorIndexOfTotalEarned);
            _result = new UserCoins(_tmpId,_tmpCoins,_tmpTotalEarned);
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

  @Override
  public Object getCoinsAmount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT coins FROM user_coins WHERE id = 1";
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
            if (_cursor.isNull(0)) {
              _result = null;
            } else {
              _result = _cursor.getInt(0);
            }
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
