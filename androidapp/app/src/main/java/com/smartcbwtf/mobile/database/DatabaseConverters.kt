package com.smartcbwtf.mobile.database

import androidx.room.TypeConverter
import java.util.UUID

class DatabaseConverters {
    @TypeConverter
    fun fromUuid(uuid: UUID?): String? = uuid?.toString()

    @TypeConverter
    fun toUuid(value: String?): UUID? = value?.let(UUID::fromString)
}
