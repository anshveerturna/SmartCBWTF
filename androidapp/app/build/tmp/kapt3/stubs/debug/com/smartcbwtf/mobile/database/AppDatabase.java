package com.smartcbwtf.mobile.database;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\'\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&J\b\u0010\u0005\u001a\u00020\u0006H&J\b\u0010\u0007\u001a\u00020\bH&\u00a8\u0006\t"}, d2 = {"Lcom/smartcbwtf/mobile/database/AppDatabase;", "Landroidx/room/RoomDatabase;", "()V", "attendanceDao", "Lcom/smartcbwtf/mobile/database/dao/AttendanceDao;", "bagEventDao", "Lcom/smartcbwtf/mobile/database/dao/BagEventDao;", "hcfDao", "Lcom/smartcbwtf/mobile/database/dao/HcfDao;", "app_debug"})
@androidx.room.Database(entities = {com.smartcbwtf.mobile.database.entity.BagEventEntity.class, com.smartcbwtf.mobile.database.entity.HcfEntity.class, com.smartcbwtf.mobile.database.entity.AttendanceEventEntity.class}, version = 3, exportSchema = true)
@androidx.room.TypeConverters(value = {com.smartcbwtf.mobile.database.DatabaseConverters.class})
public abstract class AppDatabase extends androidx.room.RoomDatabase {
    
    public AppDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.smartcbwtf.mobile.database.dao.BagEventDao bagEventDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.smartcbwtf.mobile.database.dao.HcfDao hcfDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.smartcbwtf.mobile.database.dao.AttendanceDao attendanceDao();
}