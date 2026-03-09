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
import com.example.funlife.data.model.SpinWheelTemplate;
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
public final class SpinWheelTemplateDao_Impl implements SpinWheelTemplateDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SpinWheelTemplate> __insertionAdapterOfSpinWheelTemplate;

  private final EntityDeletionOrUpdateAdapter<SpinWheelTemplate> __deletionAdapterOfSpinWheelTemplate;

  private final EntityDeletionOrUpdateAdapter<SpinWheelTemplate> __updateAdapterOfSpinWheelTemplate;

  private final SharedSQLiteStatement __preparedStmtOfIncrementUsageCount;

  public SpinWheelTemplateDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSpinWheelTemplate = new EntityInsertionAdapter<SpinWheelTemplate>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `spin_wheel_templates` (`id`,`name`,`options`,`weights`,`category`,`isDefault`,`usageCount`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SpinWheelTemplate entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getOptions());
        statement.bindString(4, entity.getWeights());
        statement.bindString(5, entity.getCategory());
        final int _tmp = entity.isDefault() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindLong(7, entity.getUsageCount());
        statement.bindString(8, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfSpinWheelTemplate = new EntityDeletionOrUpdateAdapter<SpinWheelTemplate>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `spin_wheel_templates` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SpinWheelTemplate entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfSpinWheelTemplate = new EntityDeletionOrUpdateAdapter<SpinWheelTemplate>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `spin_wheel_templates` SET `id` = ?,`name` = ?,`options` = ?,`weights` = ?,`category` = ?,`isDefault` = ?,`usageCount` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SpinWheelTemplate entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getOptions());
        statement.bindString(4, entity.getWeights());
        statement.bindString(5, entity.getCategory());
        final int _tmp = entity.isDefault() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindLong(7, entity.getUsageCount());
        statement.bindString(8, entity.getCreatedAt());
        statement.bindLong(9, entity.getId());
      }
    };
    this.__preparedStmtOfIncrementUsageCount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE spin_wheel_templates SET usageCount = usageCount + 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertTemplate(final SpinWheelTemplate template,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSpinWheelTemplate.insert(template);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTemplate(final SpinWheelTemplate template,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSpinWheelTemplate.handle(template);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTemplate(final SpinWheelTemplate template,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSpinWheelTemplate.handle(template);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementUsageCount(final int id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementUsageCount.acquire();
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
          __preparedStmtOfIncrementUsageCount.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SpinWheelTemplate>> getAllTemplates() {
    final String _sql = "SELECT * FROM spin_wheel_templates ORDER BY usageCount DESC, name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spin_wheel_templates"}, new Callable<List<SpinWheelTemplate>>() {
      @Override
      @NonNull
      public List<SpinWheelTemplate> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "options");
          final int _cursorIndexOfWeights = CursorUtil.getColumnIndexOrThrow(_cursor, "weights");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfIsDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isDefault");
          final int _cursorIndexOfUsageCount = CursorUtil.getColumnIndexOrThrow(_cursor, "usageCount");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SpinWheelTemplate> _result = new ArrayList<SpinWheelTemplate>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpinWheelTemplate _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpOptions;
            _tmpOptions = _cursor.getString(_cursorIndexOfOptions);
            final String _tmpWeights;
            _tmpWeights = _cursor.getString(_cursorIndexOfWeights);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final boolean _tmpIsDefault;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDefault);
            _tmpIsDefault = _tmp != 0;
            final int _tmpUsageCount;
            _tmpUsageCount = _cursor.getInt(_cursorIndexOfUsageCount);
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _item = new SpinWheelTemplate(_tmpId,_tmpName,_tmpOptions,_tmpWeights,_tmpCategory,_tmpIsDefault,_tmpUsageCount,_tmpCreatedAt);
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
  public Flow<List<SpinWheelTemplate>> getTemplatesByCategory(final String category) {
    final String _sql = "SELECT * FROM spin_wheel_templates WHERE category = ? ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, category);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spin_wheel_templates"}, new Callable<List<SpinWheelTemplate>>() {
      @Override
      @NonNull
      public List<SpinWheelTemplate> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "options");
          final int _cursorIndexOfWeights = CursorUtil.getColumnIndexOrThrow(_cursor, "weights");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfIsDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isDefault");
          final int _cursorIndexOfUsageCount = CursorUtil.getColumnIndexOrThrow(_cursor, "usageCount");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SpinWheelTemplate> _result = new ArrayList<SpinWheelTemplate>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpinWheelTemplate _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpOptions;
            _tmpOptions = _cursor.getString(_cursorIndexOfOptions);
            final String _tmpWeights;
            _tmpWeights = _cursor.getString(_cursorIndexOfWeights);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final boolean _tmpIsDefault;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDefault);
            _tmpIsDefault = _tmp != 0;
            final int _tmpUsageCount;
            _tmpUsageCount = _cursor.getInt(_cursorIndexOfUsageCount);
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _item = new SpinWheelTemplate(_tmpId,_tmpName,_tmpOptions,_tmpWeights,_tmpCategory,_tmpIsDefault,_tmpUsageCount,_tmpCreatedAt);
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
  public Flow<List<SpinWheelTemplate>> getDefaultTemplates() {
    final String _sql = "SELECT * FROM spin_wheel_templates WHERE isDefault = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spin_wheel_templates"}, new Callable<List<SpinWheelTemplate>>() {
      @Override
      @NonNull
      public List<SpinWheelTemplate> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "options");
          final int _cursorIndexOfWeights = CursorUtil.getColumnIndexOrThrow(_cursor, "weights");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfIsDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isDefault");
          final int _cursorIndexOfUsageCount = CursorUtil.getColumnIndexOrThrow(_cursor, "usageCount");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SpinWheelTemplate> _result = new ArrayList<SpinWheelTemplate>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpinWheelTemplate _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpOptions;
            _tmpOptions = _cursor.getString(_cursorIndexOfOptions);
            final String _tmpWeights;
            _tmpWeights = _cursor.getString(_cursorIndexOfWeights);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final boolean _tmpIsDefault;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDefault);
            _tmpIsDefault = _tmp != 0;
            final int _tmpUsageCount;
            _tmpUsageCount = _cursor.getInt(_cursorIndexOfUsageCount);
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _item = new SpinWheelTemplate(_tmpId,_tmpName,_tmpOptions,_tmpWeights,_tmpCategory,_tmpIsDefault,_tmpUsageCount,_tmpCreatedAt);
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
  public Object getTemplateById(final int id,
      final Continuation<? super SpinWheelTemplate> $completion) {
    final String _sql = "SELECT * FROM spin_wheel_templates WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SpinWheelTemplate>() {
      @Override
      @Nullable
      public SpinWheelTemplate call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "options");
          final int _cursorIndexOfWeights = CursorUtil.getColumnIndexOrThrow(_cursor, "weights");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfIsDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isDefault");
          final int _cursorIndexOfUsageCount = CursorUtil.getColumnIndexOrThrow(_cursor, "usageCount");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final SpinWheelTemplate _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpOptions;
            _tmpOptions = _cursor.getString(_cursorIndexOfOptions);
            final String _tmpWeights;
            _tmpWeights = _cursor.getString(_cursorIndexOfWeights);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final boolean _tmpIsDefault;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDefault);
            _tmpIsDefault = _tmp != 0;
            final int _tmpUsageCount;
            _tmpUsageCount = _cursor.getInt(_cursorIndexOfUsageCount);
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _result = new SpinWheelTemplate(_tmpId,_tmpName,_tmpOptions,_tmpWeights,_tmpCategory,_tmpIsDefault,_tmpUsageCount,_tmpCreatedAt);
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
  public Flow<List<SpinWheelTemplate>> searchTemplates(final String query) {
    final String _sql = "SELECT * FROM spin_wheel_templates WHERE name LIKE '%' || ? || '%' ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spin_wheel_templates"}, new Callable<List<SpinWheelTemplate>>() {
      @Override
      @NonNull
      public List<SpinWheelTemplate> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfOptions = CursorUtil.getColumnIndexOrThrow(_cursor, "options");
          final int _cursorIndexOfWeights = CursorUtil.getColumnIndexOrThrow(_cursor, "weights");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfIsDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isDefault");
          final int _cursorIndexOfUsageCount = CursorUtil.getColumnIndexOrThrow(_cursor, "usageCount");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SpinWheelTemplate> _result = new ArrayList<SpinWheelTemplate>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpinWheelTemplate _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpOptions;
            _tmpOptions = _cursor.getString(_cursorIndexOfOptions);
            final String _tmpWeights;
            _tmpWeights = _cursor.getString(_cursorIndexOfWeights);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final boolean _tmpIsDefault;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDefault);
            _tmpIsDefault = _tmp != 0;
            final int _tmpUsageCount;
            _tmpUsageCount = _cursor.getInt(_cursorIndexOfUsageCount);
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _item = new SpinWheelTemplate(_tmpId,_tmpName,_tmpOptions,_tmpWeights,_tmpCategory,_tmpIsDefault,_tmpUsageCount,_tmpCreatedAt);
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
