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
import com.example.funlife.data.model.Anniversary;
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
public final class AnniversaryDao_Impl implements AnniversaryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Anniversary> __insertionAdapterOfAnniversary;

  private final EntityDeletionOrUpdateAdapter<Anniversary> __deletionAdapterOfAnniversary;

  private final EntityDeletionOrUpdateAdapter<Anniversary> __updateAdapterOfAnniversary;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfUnpinAll;

  public AnniversaryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAnniversary = new EntityInsertionAdapter<Anniversary>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `anniversaries` (`id`,`name`,`date`,`imageUri`,`isPinned`,`type`,`isYearly`,`note`,`importance`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Anniversary entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getDate());
        if (entity.getImageUri() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getImageUri());
        }
        final int _tmp = entity.isPinned() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindString(6, entity.getType());
        final int _tmp_1 = entity.isYearly() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        if (entity.getNote() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getNote());
        }
        statement.bindLong(9, entity.getImportance());
      }
    };
    this.__deletionAdapterOfAnniversary = new EntityDeletionOrUpdateAdapter<Anniversary>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `anniversaries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Anniversary entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfAnniversary = new EntityDeletionOrUpdateAdapter<Anniversary>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `anniversaries` SET `id` = ?,`name` = ?,`date` = ?,`imageUri` = ?,`isPinned` = ?,`type` = ?,`isYearly` = ?,`note` = ?,`importance` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Anniversary entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getDate());
        if (entity.getImageUri() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getImageUri());
        }
        final int _tmp = entity.isPinned() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindString(6, entity.getType());
        final int _tmp_1 = entity.isYearly() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        if (entity.getNote() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getNote());
        }
        statement.bindLong(9, entity.getImportance());
        statement.bindLong(10, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM anniversaries WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUnpinAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE anniversaries SET isPinned = 0";
        return _query;
      }
    };
  }

  @Override
  public Object insertAnniversary(final Anniversary anniversary,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAnniversary.insert(anniversary);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAnniversary(final Anniversary anniversary,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfAnniversary.handle(anniversary);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateAnniversary(final Anniversary anniversary,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfAnniversary.handle(anniversary);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final int id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object unpinAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUnpinAll.acquire();
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
          __preparedStmtOfUnpinAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Anniversary>> getAllAnniversaries() {
    final String _sql = "SELECT * FROM anniversaries ORDER BY isPinned DESC, date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"anniversaries"}, new Callable<List<Anniversary>>() {
      @Override
      @NonNull
      public List<Anniversary> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUri");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsYearly = CursorUtil.getColumnIndexOrThrow(_cursor, "isYearly");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfImportance = CursorUtil.getColumnIndexOrThrow(_cursor, "importance");
          final List<Anniversary> _result = new ArrayList<Anniversary>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Anniversary _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpImageUri;
            if (_cursor.isNull(_cursorIndexOfImageUri)) {
              _tmpImageUri = null;
            } else {
              _tmpImageUri = _cursor.getString(_cursorIndexOfImageUri);
            }
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final boolean _tmpIsYearly;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsYearly);
            _tmpIsYearly = _tmp_1 != 0;
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final int _tmpImportance;
            _tmpImportance = _cursor.getInt(_cursorIndexOfImportance);
            _item = new Anniversary(_tmpId,_tmpName,_tmpDate,_tmpImageUri,_tmpIsPinned,_tmpType,_tmpIsYearly,_tmpNote,_tmpImportance);
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
  public Flow<Anniversary> getPinnedAnniversary() {
    final String _sql = "SELECT * FROM anniversaries WHERE isPinned = 1 ORDER BY date ASC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"anniversaries"}, new Callable<Anniversary>() {
      @Override
      @Nullable
      public Anniversary call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfImageUri = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUri");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsYearly = CursorUtil.getColumnIndexOrThrow(_cursor, "isYearly");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfImportance = CursorUtil.getColumnIndexOrThrow(_cursor, "importance");
          final Anniversary _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpImageUri;
            if (_cursor.isNull(_cursorIndexOfImageUri)) {
              _tmpImageUri = null;
            } else {
              _tmpImageUri = _cursor.getString(_cursorIndexOfImageUri);
            }
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final boolean _tmpIsYearly;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsYearly);
            _tmpIsYearly = _tmp_1 != 0;
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            final int _tmpImportance;
            _tmpImportance = _cursor.getInt(_cursorIndexOfImportance);
            _result = new Anniversary(_tmpId,_tmpName,_tmpDate,_tmpImageUri,_tmpIsPinned,_tmpType,_tmpIsYearly,_tmpNote,_tmpImportance);
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
