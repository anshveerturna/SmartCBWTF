package com.smartcbwtf.mobile.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartcbwtf.mobile.database.dao.AttendanceDao
import com.smartcbwtf.mobile.database.dao.BagEventDao
import com.smartcbwtf.mobile.database.dao.HcfDao
import com.smartcbwtf.mobile.database.entity.AttendanceEventEntity
import com.smartcbwtf.mobile.database.entity.BagEventEntity
import com.smartcbwtf.mobile.database.entity.HcfEntity

@Database(
    entities = [BagEventEntity::class, HcfEntity::class, AttendanceEventEntity::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bagEventDao(): BagEventDao
    abstract fun hcfDao(): HcfDao
    abstract fun attendanceDao(): AttendanceDao
}
