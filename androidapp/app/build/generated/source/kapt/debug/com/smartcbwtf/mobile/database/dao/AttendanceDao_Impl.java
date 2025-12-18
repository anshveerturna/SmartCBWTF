package com.smartcbwtf.mobile.database.dao;

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
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.smartcbwtf.mobile.database.DatabaseConverters;
import com.smartcbwtf.mobile.database.entity.AttendanceEventEntity;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AttendanceDao_Impl implements AttendanceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AttendanceEventEntity> __insertionAdapterOfAttendanceEventEntity;

  private final DatabaseConverters __databaseConverters = new DatabaseConverters();

  private final EntityDeletionOrUpdateAdapter<AttendanceEventEntity> __updateAdapterOfAttendanceEventEntity;

  private final SharedSQLiteStatement __preparedStmtOfMarkSyncError;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public AttendanceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAttendanceEventEntity = new EntityInsertionAdapter<AttendanceEventEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `attendance_events` (`id`,`hcfId`,`hcfName`,`eventTs`,`gpsLat`,`gpsLon`,`gpsAccuracyM`,`distanceFromHcfM`,`deviceId`,`synced`,`syncedAt`,`syncError`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AttendanceEventEntity entity) {
        final String _tmp = __databaseConverters.fromUuid(entity.getId());
        if (_tmp == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, _tmp);
        }
        if (entity.getHcfId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getHcfId());
        }
        if (entity.getHcfName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getHcfName());
        }
        statement.bindLong(4, entity.getEventTs());
        statement.bindDouble(5, entity.getGpsLat());
        statement.bindDouble(6, entity.getGpsLon());
        if (entity.getGpsAccuracyM() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getGpsAccuracyM());
        }
        statement.bindDouble(8, entity.getDistanceFromHcfM());
        if (entity.getDeviceId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getDeviceId());
        }
        final int _tmp_1 = entity.getSynced() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        if (entity.getSyncedAt() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getSyncedAt());
        }
        if (entity.getSyncError() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getSyncError());
        }
        statement.bindLong(13, entity.getCreatedAt());
      }
    };
    this.__updateAdapterOfAttendanceEventEntity = new EntityDeletionOrUpdateAdapter<AttendanceEventEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `attendance_events` SET `id` = ?,`hcfId` = ?,`hcfName` = ?,`eventTs` = ?,`gpsLat` = ?,`gpsLon` = ?,`gpsAccuracyM` = ?,`distanceFromHcfM` = ?,`deviceId` = ?,`synced` = ?,`syncedAt` = ?,`syncError` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AttendanceEventEntity entity) {
        final String _tmp = __databaseConverters.fromUuid(entity.getId());
        if (_tmp == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, _tmp);
        }
        if (entity.getHcfId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getHcfId());
        }
        if (entity.getHcfName() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getHcfName());
        }
        statement.bindLong(4, entity.getEventTs());
        statement.bindDouble(5, entity.getGpsLat());
        statement.bindDouble(6, entity.getGpsLon());
        if (entity.getGpsAccuracyM() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getGpsAccuracyM());
        }
        statement.bindDouble(8, entity.getDistanceFromHcfM());
        if (entity.getDeviceId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getDeviceId());
        }
        final int _tmp_1 = entity.getSynced() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        if (entity.getSyncedAt() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getSyncedAt());
        }
        if (entity.getSyncError() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getSyncError());
        }
        statement.bindLong(13, entity.getCreatedAt());
        final String _tmp_2 = __databaseConverters.fromUuid(entity.getId());
        if (_tmp_2 == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, _tmp_2);
        }
      }
    };
    this.__preparedStmtOfMarkSyncError = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE attendance_events SET syncError = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM attendance_events WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final AttendanceEventEntity event,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAttendanceEventEntity.insert(event);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<AttendanceEventEntity> events,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAttendanceEventEntity.insert(events);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final AttendanceEventEntity event,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfAttendanceEventEntity.handle(event);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markSyncError(final UUID id, final String error,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSyncError.acquire();
        int _argIndex = 1;
        if (error == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, error);
        }
        _argIndex = 2;
        final String _tmp = __databaseConverters.fromUuid(id);
        if (_tmp == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, _tmp);
        }
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
          __preparedStmtOfMarkSyncError.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final UUID id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        final String _tmp = __databaseConverters.fromUuid(id);
        if (_tmp == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, _tmp);
        }
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
  public Flow<List<AttendanceEventEntity>> getPending() {
    final String _sql = "SELECT * FROM attendance_events WHERE synced = 0 ORDER BY eventTs ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance_events"}, new Callable<List<AttendanceEventEntity>>() {
      @Override
      @NonNull
      public List<AttendanceEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHcfId = CursorUtil.getColumnIndexOrThrow(_cursor, "hcfId");
          final int _cursorIndexOfHcfName = CursorUtil.getColumnIndexOrThrow(_cursor, "hcfName");
          final int _cursorIndexOfEventTs = CursorUtil.getColumnIndexOrThrow(_cursor, "eventTs");
          final int _cursorIndexOfGpsLat = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsLat");
          final int _cursorIndexOfGpsLon = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsLon");
          final int _cursorIndexOfGpsAccuracyM = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsAccuracyM");
          final int _cursorIndexOfDistanceFromHcfM = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceFromHcfM");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final int _cursorIndexOfSyncError = CursorUtil.getColumnIndexOrThrow(_cursor, "syncError");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AttendanceEventEntity> _result = new ArrayList<AttendanceEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceEventEntity _item;
            final UUID _tmpId;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfId);
            }
            _tmpId = __databaseConverters.toUuid(_tmp);
            final String _tmpHcfId;
            if (_cursor.isNull(_cursorIndexOfHcfId)) {
              _tmpHcfId = null;
            } else {
              _tmpHcfId = _cursor.getString(_cursorIndexOfHcfId);
            }
            final String _tmpHcfName;
            if (_cursor.isNull(_cursorIndexOfHcfName)) {
              _tmpHcfName = null;
            } else {
              _tmpHcfName = _cursor.getString(_cursorIndexOfHcfName);
            }
            final long _tmpEventTs;
            _tmpEventTs = _cursor.getLong(_cursorIndexOfEventTs);
            final double _tmpGpsLat;
            _tmpGpsLat = _cursor.getDouble(_cursorIndexOfGpsLat);
            final double _tmpGpsLon;
            _tmpGpsLon = _cursor.getDouble(_cursorIndexOfGpsLon);
            final Float _tmpGpsAccuracyM;
            if (_cursor.isNull(_cursorIndexOfGpsAccuracyM)) {
              _tmpGpsAccuracyM = null;
            } else {
              _tmpGpsAccuracyM = _cursor.getFloat(_cursorIndexOfGpsAccuracyM);
            }
            final double _tmpDistanceFromHcfM;
            _tmpDistanceFromHcfM = _cursor.getDouble(_cursorIndexOfDistanceFromHcfM);
            final String _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            }
            final boolean _tmpSynced;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp_1 != 0;
            final Long _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getLong(_cursorIndexOfSyncedAt);
            }
            final String _tmpSyncError;
            if (_cursor.isNull(_cursorIndexOfSyncError)) {
              _tmpSyncError = null;
            } else {
              _tmpSyncError = _cursor.getString(_cursorIndexOfSyncError);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AttendanceEventEntity(_tmpId,_tmpHcfId,_tmpHcfName,_tmpEventTs,_tmpGpsLat,_tmpGpsLon,_tmpGpsAccuracyM,_tmpDistanceFromHcfM,_tmpDeviceId,_tmpSynced,_tmpSyncedAt,_tmpSyncError,_tmpCreatedAt);
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
  public Object getPendingList(
      final Continuation<? super List<AttendanceEventEntity>> $completion) {
    final String _sql = "SELECT * FROM attendance_events WHERE synced = 0 ORDER BY eventTs ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AttendanceEventEntity>>() {
      @Override
      @NonNull
      public List<AttendanceEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHcfId = CursorUtil.getColumnIndexOrThrow(_cursor, "hcfId");
          final int _cursorIndexOfHcfName = CursorUtil.getColumnIndexOrThrow(_cursor, "hcfName");
          final int _cursorIndexOfEventTs = CursorUtil.getColumnIndexOrThrow(_cursor, "eventTs");
          final int _cursorIndexOfGpsLat = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsLat");
          final int _cursorIndexOfGpsLon = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsLon");
          final int _cursorIndexOfGpsAccuracyM = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsAccuracyM");
          final int _cursorIndexOfDistanceFromHcfM = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceFromHcfM");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final int _cursorIndexOfSyncError = CursorUtil.getColumnIndexOrThrow(_cursor, "syncError");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AttendanceEventEntity> _result = new ArrayList<AttendanceEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceEventEntity _item;
            final UUID _tmpId;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfId);
            }
            _tmpId = __databaseConverters.toUuid(_tmp);
            final String _tmpHcfId;
            if (_cursor.isNull(_cursorIndexOfHcfId)) {
              _tmpHcfId = null;
            } else {
              _tmpHcfId = _cursor.getString(_cursorIndexOfHcfId);
            }
            final String _tmpHcfName;
            if (_cursor.isNull(_cursorIndexOfHcfName)) {
              _tmpHcfName = null;
            } else {
              _tmpHcfName = _cursor.getString(_cursorIndexOfHcfName);
            }
            final long _tmpEventTs;
            _tmpEventTs = _cursor.getLong(_cursorIndexOfEventTs);
            final double _tmpGpsLat;
            _tmpGpsLat = _cursor.getDouble(_cursorIndexOfGpsLat);
            final double _tmpGpsLon;
            _tmpGpsLon = _cursor.getDouble(_cursorIndexOfGpsLon);
            final Float _tmpGpsAccuracyM;
            if (_cursor.isNull(_cursorIndexOfGpsAccuracyM)) {
              _tmpGpsAccuracyM = null;
            } else {
              _tmpGpsAccuracyM = _cursor.getFloat(_cursorIndexOfGpsAccuracyM);
            }
            final double _tmpDistanceFromHcfM;
            _tmpDistanceFromHcfM = _cursor.getDouble(_cursorIndexOfDistanceFromHcfM);
            final String _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            }
            final boolean _tmpSynced;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp_1 != 0;
            final Long _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getLong(_cursorIndexOfSyncedAt);
            }
            final String _tmpSyncError;
            if (_cursor.isNull(_cursorIndexOfSyncError)) {
              _tmpSyncError = null;
            } else {
              _tmpSyncError = _cursor.getString(_cursorIndexOfSyncError);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AttendanceEventEntity(_tmpId,_tmpHcfId,_tmpHcfName,_tmpEventTs,_tmpGpsLat,_tmpGpsLon,_tmpGpsAccuracyM,_tmpDistanceFromHcfM,_tmpDeviceId,_tmpSynced,_tmpSyncedAt,_tmpSyncError,_tmpCreatedAt);
            _result.add(_item);
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
  public Flow<Integer> pendingCount() {
    final String _sql = "SELECT COUNT(*) FROM attendance_events WHERE synced = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance_events"}, new Callable<Integer>() {
      @Override
      @NonNull
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
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object findById(final UUID id,
      final Continuation<? super AttendanceEventEntity> $completion) {
    final String _sql = "SELECT * FROM attendance_events WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final String _tmp = __databaseConverters.fromUuid(id);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AttendanceEventEntity>() {
      @Override
      @Nullable
      public AttendanceEventEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHcfId = CursorUtil.getColumnIndexOrThrow(_cursor, "hcfId");
          final int _cursorIndexOfHcfName = CursorUtil.getColumnIndexOrThrow(_cursor, "hcfName");
          final int _cursorIndexOfEventTs = CursorUtil.getColumnIndexOrThrow(_cursor, "eventTs");
          final int _cursorIndexOfGpsLat = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsLat");
          final int _cursorIndexOfGpsLon = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsLon");
          final int _cursorIndexOfGpsAccuracyM = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsAccuracyM");
          final int _cursorIndexOfDistanceFromHcfM = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceFromHcfM");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final int _cursorIndexOfSyncError = CursorUtil.getColumnIndexOrThrow(_cursor, "syncError");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final AttendanceEventEntity _result;
          if (_cursor.moveToFirst()) {
            final UUID _tmpId;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfId);
            }
            _tmpId = __databaseConverters.toUuid(_tmp_1);
            final String _tmpHcfId;
            if (_cursor.isNull(_cursorIndexOfHcfId)) {
              _tmpHcfId = null;
            } else {
              _tmpHcfId = _cursor.getString(_cursorIndexOfHcfId);
            }
            final String _tmpHcfName;
            if (_cursor.isNull(_cursorIndexOfHcfName)) {
              _tmpHcfName = null;
            } else {
              _tmpHcfName = _cursor.getString(_cursorIndexOfHcfName);
            }
            final long _tmpEventTs;
            _tmpEventTs = _cursor.getLong(_cursorIndexOfEventTs);
            final double _tmpGpsLat;
            _tmpGpsLat = _cursor.getDouble(_cursorIndexOfGpsLat);
            final double _tmpGpsLon;
            _tmpGpsLon = _cursor.getDouble(_cursorIndexOfGpsLon);
            final Float _tmpGpsAccuracyM;
            if (_cursor.isNull(_cursorIndexOfGpsAccuracyM)) {
              _tmpGpsAccuracyM = null;
            } else {
              _tmpGpsAccuracyM = _cursor.getFloat(_cursorIndexOfGpsAccuracyM);
            }
            final double _tmpDistanceFromHcfM;
            _tmpDistanceFromHcfM = _cursor.getDouble(_cursorIndexOfDistanceFromHcfM);
            final String _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            }
            final boolean _tmpSynced;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp_2 != 0;
            final Long _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getLong(_cursorIndexOfSyncedAt);
            }
            final String _tmpSyncError;
            if (_cursor.isNull(_cursorIndexOfSyncError)) {
              _tmpSyncError = null;
            } else {
              _tmpSyncError = _cursor.getString(_cursorIndexOfSyncError);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new AttendanceEventEntity(_tmpId,_tmpHcfId,_tmpHcfName,_tmpEventTs,_tmpGpsLat,_tmpGpsLon,_tmpGpsAccuracyM,_tmpDistanceFromHcfM,_tmpDeviceId,_tmpSynced,_tmpSyncedAt,_tmpSyncError,_tmpCreatedAt);
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
  public Object getLatest(final Continuation<? super AttendanceEventEntity> $completion) {
    final String _sql = "SELECT * FROM attendance_events ORDER BY eventTs DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AttendanceEventEntity>() {
      @Override
      @Nullable
      public AttendanceEventEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHcfId = CursorUtil.getColumnIndexOrThrow(_cursor, "hcfId");
          final int _cursorIndexOfHcfName = CursorUtil.getColumnIndexOrThrow(_cursor, "hcfName");
          final int _cursorIndexOfEventTs = CursorUtil.getColumnIndexOrThrow(_cursor, "eventTs");
          final int _cursorIndexOfGpsLat = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsLat");
          final int _cursorIndexOfGpsLon = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsLon");
          final int _cursorIndexOfGpsAccuracyM = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsAccuracyM");
          final int _cursorIndexOfDistanceFromHcfM = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceFromHcfM");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final int _cursorIndexOfSyncError = CursorUtil.getColumnIndexOrThrow(_cursor, "syncError");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final AttendanceEventEntity _result;
          if (_cursor.moveToFirst()) {
            final UUID _tmpId;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfId);
            }
            _tmpId = __databaseConverters.toUuid(_tmp);
            final String _tmpHcfId;
            if (_cursor.isNull(_cursorIndexOfHcfId)) {
              _tmpHcfId = null;
            } else {
              _tmpHcfId = _cursor.getString(_cursorIndexOfHcfId);
            }
            final String _tmpHcfName;
            if (_cursor.isNull(_cursorIndexOfHcfName)) {
              _tmpHcfName = null;
            } else {
              _tmpHcfName = _cursor.getString(_cursorIndexOfHcfName);
            }
            final long _tmpEventTs;
            _tmpEventTs = _cursor.getLong(_cursorIndexOfEventTs);
            final double _tmpGpsLat;
            _tmpGpsLat = _cursor.getDouble(_cursorIndexOfGpsLat);
            final double _tmpGpsLon;
            _tmpGpsLon = _cursor.getDouble(_cursorIndexOfGpsLon);
            final Float _tmpGpsAccuracyM;
            if (_cursor.isNull(_cursorIndexOfGpsAccuracyM)) {
              _tmpGpsAccuracyM = null;
            } else {
              _tmpGpsAccuracyM = _cursor.getFloat(_cursorIndexOfGpsAccuracyM);
            }
            final double _tmpDistanceFromHcfM;
            _tmpDistanceFromHcfM = _cursor.getDouble(_cursorIndexOfDistanceFromHcfM);
            final String _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            }
            final boolean _tmpSynced;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp_1 != 0;
            final Long _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getLong(_cursorIndexOfSyncedAt);
            }
            final String _tmpSyncError;
            if (_cursor.isNull(_cursorIndexOfSyncError)) {
              _tmpSyncError = null;
            } else {
              _tmpSyncError = _cursor.getString(_cursorIndexOfSyncError);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new AttendanceEventEntity(_tmpId,_tmpHcfId,_tmpHcfName,_tmpEventTs,_tmpGpsLat,_tmpGpsLon,_tmpGpsAccuracyM,_tmpDistanceFromHcfM,_tmpDeviceId,_tmpSynced,_tmpSyncedAt,_tmpSyncError,_tmpCreatedAt);
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
  public Flow<List<AttendanceEventEntity>> getHistory(final int limit) {
    final String _sql = "SELECT * FROM attendance_events ORDER BY eventTs DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance_events"}, new Callable<List<AttendanceEventEntity>>() {
      @Override
      @NonNull
      public List<AttendanceEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHcfId = CursorUtil.getColumnIndexOrThrow(_cursor, "hcfId");
          final int _cursorIndexOfHcfName = CursorUtil.getColumnIndexOrThrow(_cursor, "hcfName");
          final int _cursorIndexOfEventTs = CursorUtil.getColumnIndexOrThrow(_cursor, "eventTs");
          final int _cursorIndexOfGpsLat = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsLat");
          final int _cursorIndexOfGpsLon = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsLon");
          final int _cursorIndexOfGpsAccuracyM = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsAccuracyM");
          final int _cursorIndexOfDistanceFromHcfM = CursorUtil.getColumnIndexOrThrow(_cursor, "distanceFromHcfM");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final int _cursorIndexOfSyncedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedAt");
          final int _cursorIndexOfSyncError = CursorUtil.getColumnIndexOrThrow(_cursor, "syncError");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<AttendanceEventEntity> _result = new ArrayList<AttendanceEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceEventEntity _item;
            final UUID _tmpId;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfId);
            }
            _tmpId = __databaseConverters.toUuid(_tmp);
            final String _tmpHcfId;
            if (_cursor.isNull(_cursorIndexOfHcfId)) {
              _tmpHcfId = null;
            } else {
              _tmpHcfId = _cursor.getString(_cursorIndexOfHcfId);
            }
            final String _tmpHcfName;
            if (_cursor.isNull(_cursorIndexOfHcfName)) {
              _tmpHcfName = null;
            } else {
              _tmpHcfName = _cursor.getString(_cursorIndexOfHcfName);
            }
            final long _tmpEventTs;
            _tmpEventTs = _cursor.getLong(_cursorIndexOfEventTs);
            final double _tmpGpsLat;
            _tmpGpsLat = _cursor.getDouble(_cursorIndexOfGpsLat);
            final double _tmpGpsLon;
            _tmpGpsLon = _cursor.getDouble(_cursorIndexOfGpsLon);
            final Float _tmpGpsAccuracyM;
            if (_cursor.isNull(_cursorIndexOfGpsAccuracyM)) {
              _tmpGpsAccuracyM = null;
            } else {
              _tmpGpsAccuracyM = _cursor.getFloat(_cursorIndexOfGpsAccuracyM);
            }
            final double _tmpDistanceFromHcfM;
            _tmpDistanceFromHcfM = _cursor.getDouble(_cursorIndexOfDistanceFromHcfM);
            final String _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            }
            final boolean _tmpSynced;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp_1 != 0;
            final Long _tmpSyncedAt;
            if (_cursor.isNull(_cursorIndexOfSyncedAt)) {
              _tmpSyncedAt = null;
            } else {
              _tmpSyncedAt = _cursor.getLong(_cursorIndexOfSyncedAt);
            }
            final String _tmpSyncError;
            if (_cursor.isNull(_cursorIndexOfSyncError)) {
              _tmpSyncError = null;
            } else {
              _tmpSyncError = _cursor.getString(_cursorIndexOfSyncError);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new AttendanceEventEntity(_tmpId,_tmpHcfId,_tmpHcfName,_tmpEventTs,_tmpGpsLat,_tmpGpsLon,_tmpGpsAccuracyM,_tmpDistanceFromHcfM,_tmpDeviceId,_tmpSynced,_tmpSyncedAt,_tmpSyncError,_tmpCreatedAt);
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
  public Object isCooldownActive(final long cooldownStartMs,
      final Continuation<? super Boolean> $completion) {
    final String _sql = "SELECT COUNT(*) > 0 FROM attendance_events WHERE eventTs > ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, cooldownStartMs);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Boolean>() {
      @Override
      @NonNull
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp == null ? null : _tmp != 0;
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
  public Object markSynced(final List<UUID> ids, final long syncedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("UPDATE attendance_events SET synced = 1, syncedAt = ");
        _stringBuilder.append("?");
        _stringBuilder.append(", syncError = NULL WHERE id IN (");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, syncedAt);
        _argIndex = 2;
        for (UUID _item : ids) {
          final String _tmp = __databaseConverters.fromUuid(_item);
          if (_tmp == null) {
            _stmt.bindNull(_argIndex);
          } else {
            _stmt.bindString(_argIndex, _tmp);
          }
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
