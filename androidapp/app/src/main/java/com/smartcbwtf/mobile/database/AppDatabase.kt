package com.smartcbwtf.mobile.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartcbwtf.mobile.database.dao.AttendanceDao
import com.smartcbwtf.mobile.database.dao.BagEventDao
import com.smartcbwtf.mobile.database.dao.HcfDao
import com.smartcbwtf.mobile.database.dao.UserProfileDao
import com.smartcbwtf.mobile.database.entity.AttendanceEventEntity
import com.smartcbwtf.mobile.database.entity.BagEventEntity
import com.smartcbwtf.mobile.database.entity.HcfEntity
import com.smartcbwtf.mobile.database.entity.UserProfileEntity

@Database(
    entities = [
        BagEventEntity::class, 
        HcfEntity::class, 
        AttendanceEventEntity::class,
        UserProfileEntity::class
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bagEventDao(): BagEventDao
    abstract fun hcfDao(): HcfDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bag_events ADD COLUMN gpsAccuracyM REAL")
            }
        }
    }
}
