package com.smartcbwtf.mobile.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.smartcbwtf.mobile.database.dao.BagEventDao;
import com.smartcbwtf.mobile.database.dao.BagEventDao_Impl;
import com.smartcbwtf.mobile.database.dao.HcfDao;
import com.smartcbwtf.mobile.database.dao.HcfDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile BagEventDao _bagEventDao;

  private volatile HcfDao _hcfDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `bag_events` (`id` TEXT NOT NULL, `qrCode` TEXT NOT NULL, `eventType` TEXT NOT NULL, `eventTs` INTEGER NOT NULL, `gpsLat` REAL NOT NULL, `gpsLon` REAL NOT NULL, `weightKg` REAL NOT NULL, `hcfId` TEXT NOT NULL, `facilityId` TEXT, `synced` INTEGER NOT NULL, `deviceId` TEXT, `driverId` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `hcfs` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `address` TEXT, `city` TEXT, `state` TEXT, `postalCode` TEXT, `phone` TEXT, `latitude` REAL, `longitude` REAL, `approved` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b84be3333c9c8db0d6e6b5a27f27b1ff')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `bag_events`");
        db.execSQL("DROP TABLE IF EXISTS `hcfs`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsBagEvents = new HashMap<String, TableInfo.Column>(12);
        _columnsBagEvents.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBagEvents.put("qrCode", new TableInfo.Column("qrCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBagEvents.put("eventType", new TableInfo.Column("eventType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBagEvents.put("eventTs", new TableInfo.Column("eventTs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBagEvents.put("gpsLat", new TableInfo.Column("gpsLat", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBagEvents.put("gpsLon", new TableInfo.Column("gpsLon", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBagEvents.put("weightKg", new TableInfo.Column("weightKg", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBagEvents.put("hcfId", new TableInfo.Column("hcfId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBagEvents.put("facilityId", new TableInfo.Column("facilityId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBagEvents.put("synced", new TableInfo.Column("synced", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBagEvents.put("deviceId", new TableInfo.Column("deviceId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBagEvents.put("driverId", new TableInfo.Column("driverId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBagEvents = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBagEvents = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBagEvents = new TableInfo("bag_events", _columnsBagEvents, _foreignKeysBagEvents, _indicesBagEvents);
        final TableInfo _existingBagEvents = TableInfo.read(db, "bag_events");
        if (!_infoBagEvents.equals(_existingBagEvents)) {
          return new RoomOpenHelper.ValidationResult(false, "bag_events(com.smartcbwtf.mobile.database.entity.BagEventEntity).\n"
                  + " Expected:\n" + _infoBagEvents + "\n"
                  + " Found:\n" + _existingBagEvents);
        }
        final HashMap<String, TableInfo.Column> _columnsHcfs = new HashMap<String, TableInfo.Column>(10);
        _columnsHcfs.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHcfs.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHcfs.put("address", new TableInfo.Column("address", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHcfs.put("city", new TableInfo.Column("city", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHcfs.put("state", new TableInfo.Column("state", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHcfs.put("postalCode", new TableInfo.Column("postalCode", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHcfs.put("phone", new TableInfo.Column("phone", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHcfs.put("latitude", new TableInfo.Column("latitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHcfs.put("longitude", new TableInfo.Column("longitude", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHcfs.put("approved", new TableInfo.Column("approved", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHcfs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHcfs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHcfs = new TableInfo("hcfs", _columnsHcfs, _foreignKeysHcfs, _indicesHcfs);
        final TableInfo _existingHcfs = TableInfo.read(db, "hcfs");
        if (!_infoHcfs.equals(_existingHcfs)) {
          return new RoomOpenHelper.ValidationResult(false, "hcfs(com.smartcbwtf.mobile.database.entity.HcfEntity).\n"
                  + " Expected:\n" + _infoHcfs + "\n"
                  + " Found:\n" + _existingHcfs);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "b84be3333c9c8db0d6e6b5a27f27b1ff", "aed4666d7989130db5483337770f0c3c");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "bag_events","hcfs");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `bag_events`");
      _db.execSQL("DELETE FROM `hcfs`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(BagEventDao.class, BagEventDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HcfDao.class, HcfDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public BagEventDao bagEventDao() {
    if (_bagEventDao != null) {
      return _bagEventDao;
    } else {
      synchronized(this) {
        if(_bagEventDao == null) {
          _bagEventDao = new BagEventDao_Impl(this);
        }
        return _bagEventDao;
      }
    }
  }

  @Override
  public HcfDao hcfDao() {
    if (_hcfDao != null) {
      return _hcfDao;
    } else {
      synchronized(this) {
        if(_hcfDao == null) {
          _hcfDao = new HcfDao_Impl(this);
        }
        return _hcfDao;
      }
    }
  }
}
