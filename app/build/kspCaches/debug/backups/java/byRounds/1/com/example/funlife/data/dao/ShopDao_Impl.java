package com.example.funlife.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.funlife.data.model.PurchaseHistory;
import com.example.funlife.data.model.ShopItem;
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
public final class ShopDao_Impl implements ShopDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ShopItem> __insertionAdapterOfShopItem;

  private final EntityInsertionAdapter<PurchaseHistory> __insertionAdapterOfPurchaseHistory;

  public ShopDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfShopItem = new EntityInsertionAdapter<ShopItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `shop_items` (`id`,`name`,`description`,`icon`,`price`,`type`,`value`,`isAvailable`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShopItem entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getDescription());
        statement.bindString(4, entity.getIcon());
        statement.bindLong(5, entity.getPrice());
        statement.bindString(6, entity.getType());
        statement.bindLong(7, entity.getValue());
        final int _tmp = entity.isAvailable() ? 1 : 0;
        statement.bindLong(8, _tmp);
      }
    };
    this.__insertionAdapterOfPurchaseHistory = new EntityInsertionAdapter<PurchaseHistory>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `purchase_history` (`id`,`itemId`,`itemName`,`price`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PurchaseHistory entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getItemId());
        statement.bindString(3, entity.getItemName());
        statement.bindLong(4, entity.getPrice());
        statement.bindString(5, entity.getTimestamp());
      }
    };
  }

  @Override
  public Object insertShopItem(final ShopItem item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfShopItem.insert(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertPurchaseHistory(final PurchaseHistory purchase,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPurchaseHistory.insert(purchase);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ShopItem>> getAllShopItems() {
    final String _sql = "SELECT * FROM shop_items WHERE isAvailable = 1 ORDER BY price ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"shop_items"}, new Callable<List<ShopItem>>() {
      @Override
      @NonNull
      public List<ShopItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfIsAvailable = CursorUtil.getColumnIndexOrThrow(_cursor, "isAvailable");
          final List<ShopItem> _result = new ArrayList<ShopItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShopItem _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpIcon;
            _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            final int _tmpPrice;
            _tmpPrice = _cursor.getInt(_cursorIndexOfPrice);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final int _tmpValue;
            _tmpValue = _cursor.getInt(_cursorIndexOfValue);
            final boolean _tmpIsAvailable;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAvailable);
            _tmpIsAvailable = _tmp != 0;
            _item = new ShopItem(_tmpId,_tmpName,_tmpDescription,_tmpIcon,_tmpPrice,_tmpType,_tmpValue,_tmpIsAvailable);
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
  public Object getShopItem(final int itemId, final Continuation<? super ShopItem> $completion) {
    final String _sql = "SELECT * FROM shop_items WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, itemId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ShopItem>() {
      @Override
      @Nullable
      public ShopItem call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfIsAvailable = CursorUtil.getColumnIndexOrThrow(_cursor, "isAvailable");
          final ShopItem _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpIcon;
            _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            final int _tmpPrice;
            _tmpPrice = _cursor.getInt(_cursorIndexOfPrice);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final int _tmpValue;
            _tmpValue = _cursor.getInt(_cursorIndexOfValue);
            final boolean _tmpIsAvailable;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsAvailable);
            _tmpIsAvailable = _tmp != 0;
            _result = new ShopItem(_tmpId,_tmpName,_tmpDescription,_tmpIcon,_tmpPrice,_tmpType,_tmpValue,_tmpIsAvailable);
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
  public Flow<List<PurchaseHistory>> getPurchaseHistory() {
    final String _sql = "SELECT * FROM purchase_history ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"purchase_history"}, new Callable<List<PurchaseHistory>>() {
      @Override
      @NonNull
      public List<PurchaseHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfItemId = CursorUtil.getColumnIndexOrThrow(_cursor, "itemId");
          final int _cursorIndexOfItemName = CursorUtil.getColumnIndexOrThrow(_cursor, "itemName");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<PurchaseHistory> _result = new ArrayList<PurchaseHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PurchaseHistory _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpItemId;
            _tmpItemId = _cursor.getInt(_cursorIndexOfItemId);
            final String _tmpItemName;
            _tmpItemName = _cursor.getString(_cursorIndexOfItemName);
            final int _tmpPrice;
            _tmpPrice = _cursor.getInt(_cursorIndexOfPrice);
            final String _tmpTimestamp;
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp);
            _item = new PurchaseHistory(_tmpId,_tmpItemId,_tmpItemName,_tmpPrice,_tmpTimestamp);
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
  public Object getPurchaseCount(final int itemId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM purchase_history WHERE itemId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, itemId);
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
