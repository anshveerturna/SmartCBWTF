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
import com.smartcbwtf.mobile.database.entity.BagEventEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class BagEventDao_Impl implements BagEventDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BagEventEntity> __insertionAdapterOfBagEventEntity;

  private final DatabaseConverters __databaseConverters = new DatabaseConverters();

  private final EntityDeletionOrUpdateAdapter<BagEventEntity> __updateAdapterOfBagEventEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public BagEventDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBagEventEntity = new EntityInsertionAdapter<BagEventEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `bag_events` (`id`,`qrCode`,`eventType`,`eventTs`,`gpsLat`,`gpsLon`,`weightKg`,`hcfId`,`facilityId`,`synced`,`deviceId`,`driverId`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BagEventEntity entity) {
        final String _tmp = __databaseConverters.fromUuid(entity.getId());
        if (_tmp == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, _tmp);
        }
        if (entity.getQrCode() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getQrCode());
        }
        if (entity.getEventType() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getEventType());
        }
        statement.bindLong(4, entity.getEventTs());
        statement.bindDouble(5, entity.getGpsLat());
        statement.bindDouble(6, entity.getGpsLon());
        statement.bindDouble(7, entity.getWeightKg());
        if (entity.getHcfId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getHcfId());
        }
        if (entity.getFacilityId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getFacilityId());
        }
        final int _tmp_1 = entity.getSynced() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        if (entity.getDeviceId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getDeviceId());
        }
        if (entity.getDriverId() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getDriverId());
        }
      }
    };
    this.__updateAdapterOfBagEventEntity = new EntityDeletionOrUpdateAdapter<BagEventEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `bag_events` SET `id` = ?,`qrCode` = ?,`eventType` = ?,`eventTs` = ?,`gpsLat` = ?,`gpsLon` = ?,`weightKg` = ?,`hcfId` = ?,`facilityId` = ?,`synced` = ?,`deviceId` = ?,`driverId` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BagEventEntity entity) {
        final String _tmp = __databaseConverters.fromUuid(entity.getId());
        if (_tmp == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, _tmp);
        }
        if (entity.getQrCode() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getQrCode());
        }
        if (entity.getEventType() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getEventType());
        }
        statement.bindLong(4, entity.getEventTs());
        statement.bindDouble(5, entity.getGpsLat());
        statement.bindDouble(6, entity.getGpsLon());
        statement.bindDouble(7, entity.getWeightKg());
        if (entity.getHcfId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getHcfId());
        }
        if (entity.getFacilityId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getFacilityId());
        }
        final int _tmp_1 = entity.getSynced() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
        if (entity.getDeviceId() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getDeviceId());
        }
        if (entity.getDriverId() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getDriverId());
        }
        final String _tmp_2 = __databaseConverters.fromUuid(entity.getId());
        if (_tmp_2 == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, _tmp_2);
        }
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM bag_events WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsertAll(final List<BagEventEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBagEventEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final BagEventEntity item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBagEventEntity.insert(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final BagEventEntity entity, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfBagEventEntity.handle(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
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
  public Flow<List<BagEventEntity>> getPending() {
    final String _sql = "SELECT * FROM bag_events WHERE synced = 0 ORDER BY eventTs ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"bag_events"}, new Callable<List<BagEventEntity>>() {
      @Override
      @NonNull
      public List<BagEventEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfQrCode = CursorUtil.getColumnIndexOrThrow(_cursor, "qrCode");
          final int _cursorIndexOfEventType = CursorUtil.getColumnIndexOrThrow(_cursor, "eventType");
          final int _cursorIndexOfEventTs = CursorUtil.getColumnIndexOrThrow(_cursor, "eventTs");
          final int _cursorIndexOfGpsLat = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsLat");
          final int _cursorIndexOfGpsLon = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsLon");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfHcfId = CursorUtil.getColumnIndexOrThrow(_cursor, "hcfId");
          final int _cursorIndexOfFacilityId = CursorUtil.getColumnIndexOrThrow(_cursor, "facilityId");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfDriverId = CursorUtil.getColumnIndexOrThrow(_cursor, "driverId");
          final List<BagEventEntity> _result = new ArrayList<BagEventEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BagEventEntity _item;
            final UUID _tmpId;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfId);
            }
            _tmpId = __databaseConverters.toUuid(_tmp);
            final String _tmpQrCode;
            if (_cursor.isNull(_cursorIndexOfQrCode)) {
              _tmpQrCode = null;
            } else {
              _tmpQrCode = _cursor.getString(_cursorIndexOfQrCode);
            }
            final String _tmpEventType;
            if (_cursor.isNull(_cursorIndexOfEventType)) {
              _tmpEventType = null;
            } else {
              _tmpEventType = _cursor.getString(_cursorIndexOfEventType);
            }
            final long _tmpEventTs;
            _tmpEventTs = _cursor.getLong(_cursorIndexOfEventTs);
            final double _tmpGpsLat;
            _tmpGpsLat = _cursor.getDouble(_cursorIndexOfGpsLat);
            final double _tmpGpsLon;
            _tmpGpsLon = _cursor.getDouble(_cursorIndexOfGpsLon);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final String _tmpHcfId;
            if (_cursor.isNull(_cursorIndexOfHcfId)) {
              _tmpHcfId = null;
            } else {
              _tmpHcfId = _cursor.getString(_cursorIndexOfHcfId);
            }
            final String _tmpFacilityId;
            if (_cursor.isNull(_cursorIndexOfFacilityId)) {
              _tmpFacilityId = null;
            } else {
              _tmpFacilityId = _cursor.getString(_cursorIndexOfFacilityId);
            }
            final boolean _tmpSynced;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp_1 != 0;
            final String _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            }
            final String _tmpDriverId;
            if (_cursor.isNull(_cursorIndexOfDriverId)) {
              _tmpDriverId = null;
            } else {
              _tmpDriverId = _cursor.getString(_cursorIndexOfDriverId);
            }
            _item = new BagEventEntity(_tmpId,_tmpQrCode,_tmpEventType,_tmpEventTs,_tmpGpsLat,_tmpGpsLon,_tmpWeightKg,_tmpHcfId,_tmpFacilityId,_tmpSynced,_tmpDeviceId,_tmpDriverId);
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
  public Flow<Integer> pendingCount() {
    final String _sql = "SELECT COUNT(*) FROM bag_events WHERE synced = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"bag_events"}, new Callable<Integer>() {
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
  public Object findById(final UUID id, final Continuation<? super BagEventEntity> $completion) {
    final String _sql = "SELECT * FROM bag_events WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final String _tmp = __databaseConverters.fromUuid(id);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BagEventEntity>() {
      @Override
      @Nullable
      public BagEventEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfQrCode = CursorUtil.getColumnIndexOrThrow(_cursor, "qrCode");
          final int _cursorIndexOfEventType = CursorUtil.getColumnIndexOrThrow(_cursor, "eventType");
          final int _cursorIndexOfEventTs = CursorUtil.getColumnIndexOrThrow(_cursor, "eventTs");
          final int _cursorIndexOfGpsLat = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsLat");
          final int _cursorIndexOfGpsLon = CursorUtil.getColumnIndexOrThrow(_cursor, "gpsLon");
          final int _cursorIndexOfWeightKg = CursorUtil.getColumnIndexOrThrow(_cursor, "weightKg");
          final int _cursorIndexOfHcfId = CursorUtil.getColumnIndexOrThrow(_cursor, "hcfId");
          final int _cursorIndexOfFacilityId = CursorUtil.getColumnIndexOrThrow(_cursor, "facilityId");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final int _cursorIndexOfDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceId");
          final int _cursorIndexOfDriverId = CursorUtil.getColumnIndexOrThrow(_cursor, "driverId");
          final BagEventEntity _result;
          if (_cursor.moveToFirst()) {
            final UUID _tmpId;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfId);
            }
            _tmpId = __databaseConverters.toUuid(_tmp_1);
            final String _tmpQrCode;
            if (_cursor.isNull(_cursorIndexOfQrCode)) {
              _tmpQrCode = null;
            } else {
              _tmpQrCode = _cursor.getString(_cursorIndexOfQrCode);
            }
            final String _tmpEventType;
            if (_cursor.isNull(_cursorIndexOfEventType)) {
              _tmpEventType = null;
            } else {
              _tmpEventType = _cursor.getString(_cursorIndexOfEventType);
            }
            final long _tmpEventTs;
            _tmpEventTs = _cursor.getLong(_cursorIndexOfEventTs);
            final double _tmpGpsLat;
            _tmpGpsLat = _cursor.getDouble(_cursorIndexOfGpsLat);
            final double _tmpGpsLon;
            _tmpGpsLon = _cursor.getDouble(_cursorIndexOfGpsLon);
            final double _tmpWeightKg;
            _tmpWeightKg = _cursor.getDouble(_cursorIndexOfWeightKg);
            final String _tmpHcfId;
            if (_cursor.isNull(_cursorIndexOfHcfId)) {
              _tmpHcfId = null;
            } else {
              _tmpHcfId = _cursor.getString(_cursorIndexOfHcfId);
            }
            final String _tmpFacilityId;
            if (_cursor.isNull(_cursorIndexOfFacilityId)) {
              _tmpFacilityId = null;
            } else {
              _tmpFacilityId = _cursor.getString(_cursorIndexOfFacilityId);
            }
            final boolean _tmpSynced;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp_2 != 0;
            final String _tmpDeviceId;
            if (_cursor.isNull(_cursorIndexOfDeviceId)) {
              _tmpDeviceId = null;
            } else {
              _tmpDeviceId = _cursor.getString(_cursorIndexOfDeviceId);
            }
            final String _tmpDriverId;
            if (_cursor.isNull(_cursorIndexOfDriverId)) {
              _tmpDriverId = null;
            } else {
              _tmpDriverId = _cursor.getString(_cursorIndexOfDriverId);
            }
            _result = new BagEventEntity(_tmpId,_tmpQrCode,_tmpEventType,_tmpEventTs,_tmpGpsLat,_tmpGpsLon,_tmpWeightKg,_tmpHcfId,_tmpFacilityId,_tmpSynced,_tmpDeviceId,_tmpDriverId);
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
  public Object markSynced(final List<UUID> ids, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("UPDATE bag_events SET synced = 1 WHERE id IN (");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
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
