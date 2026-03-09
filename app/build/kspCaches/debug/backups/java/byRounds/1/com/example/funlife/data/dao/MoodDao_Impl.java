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
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.funlife.data.model.MoodEntry;
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
public final class MoodDao_Impl implements MoodDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MoodEntry> __insertionAdapterOfMoodEntry;

  private final EntityDeletionOrUpdateAdapter<MoodEntry> __deletionAdapterOfMoodEntry;

  private final EntityDeletionOrUpdateAdapter<MoodEntry> __updateAdapterOfMoodEntry;

  public MoodDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMoodEntry = new EntityInsertionAdapter<MoodEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `mood_entries` (`id`,`date`,`mood`,`moodLevel`,`note`,`tags`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MoodEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getDate());
        statement.bindString(3, entity.getMood());
        statement.bindLong(4, entity.getMoodLevel());
        statement.bindString(5, entity.getNote());
        statement.bindString(6, entity.getTags());
        statement.bindString(7, entity.getTimestamp());
      }
    };
    this.__deletionAdapterOfMoodEntry = new EntityDeletionOrUpdateAdapter<MoodEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `mood_entries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MoodEntry entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfMoodEntry = new EntityDeletionOrUpdateAdapter<MoodEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `mood_entries` SET `id` = ?,`date` = ?,`mood` = ?,`moodLevel` = ?,`note` = ?,`tags` = ?,`timestamp` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MoodEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getDate());
        statement.bindString(3, entity.getMood());
        statement.bindLong(4, entity.getMoodLevel());
        statement.bindString(5, entity.getNote());
        statement.bindString(6, entity.getTags());
        statement.bindString(7, entity.getTimestamp());
        statement.bindLong(8, entity.getId());
      }
    };
  }

  @Override
  public Object insertMood(final MoodEntry mood, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMoodEntry.insert(mood);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteMood(final MoodEntry mood, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfMoodEntry.handle(mood);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateMood(final MoodEntry mood, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMoodEntry.handle(mood);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MoodEntry>> getAllMoodEntries() {
    final String _sql = "SELECT * FROM mood_entries ORDER BY date DESC, timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"mood_entries"}, new Callable<List<MoodEntry>>() {
      @Override
      @NonNull
      public List<MoodEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfMood = CursorUtil.getColumnIndexOrThrow(_cursor, "mood");
          final int _cursorIndexOfMoodLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "moodLevel");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<MoodEntry> _result = new ArrayList<MoodEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MoodEntry _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpMood;
            _tmpMood = _cursor.getString(_cursorIndexOfMood);
            final int _tmpMoodLevel;
            _tmpMoodLevel = _cursor.getInt(_cursorIndexOfMoodLevel);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final String _tmpTimestamp;
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp);
            _item = new MoodEntry(_tmpId,_tmpDate,_tmpMood,_tmpMoodLevel,_tmpNote,_tmpTags,_tmpTimestamp);
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
  public Object getMoodByDate(final String date,
      final Continuation<? super MoodEntry> $completion) {
    final String _sql = "SELECT * FROM mood_entries WHERE date = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, date);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MoodEntry>() {
      @Override
      @Nullable
      public MoodEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfMood = CursorUtil.getColumnIndexOrThrow(_cursor, "mood");
          final int _cursorIndexOfMoodLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "moodLevel");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final MoodEntry _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpMood;
            _tmpMood = _cursor.getString(_cursorIndexOfMood);
            final int _tmpMoodLevel;
            _tmpMoodLevel = _cursor.getInt(_cursorIndexOfMoodLevel);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final String _tmpTimestamp;
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp);
            _result = new MoodEntry(_tmpId,_tmpDate,_tmpMood,_tmpMoodLevel,_tmpNote,_tmpTags,_tmpTimestamp);
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
  public Flow<List<MoodEntry>> getRecentMoods(final int limit) {
    final String _sql = "SELECT * FROM mood_entries ORDER BY date DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"mood_entries"}, new Callable<List<MoodEntry>>() {
      @Override
      @NonNull
      public List<MoodEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfMood = CursorUtil.getColumnIndexOrThrow(_cursor, "mood");
          final int _cursorIndexOfMoodLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "moodLevel");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<MoodEntry> _result = new ArrayList<MoodEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MoodEntry _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final String _tmpMood;
            _tmpMood = _cursor.getString(_cursorIndexOfMood);
            final int _tmpMoodLevel;
            _tmpMoodLevel = _cursor.getInt(_cursorIndexOfMoodLevel);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            final String _tmpTags;
            _tmpTags = _cursor.getString(_cursorIndexOfTags);
            final String _tmpTimestamp;
            _tmpTimestamp = _cursor.getString(_cursorIndexOfTimestamp);
            _item = new MoodEntry(_tmpId,_tmpDate,_tmpMood,_tmpMoodLevel,_tmpNote,_tmpTags,_tmpTimestamp);
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
