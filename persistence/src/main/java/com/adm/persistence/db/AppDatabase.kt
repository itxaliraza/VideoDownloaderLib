package com.adm.persistence.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.adm.domain.InProgressVideoDB
import com.adm.persistence.inprogress.InProgressVideoDao
import com.adm.persistence.inprogress.MapTypeConverter

@Database(entities = [InProgressVideoDB::class], version = 1)
@TypeConverters(MapTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getInProgressDao(): InProgressVideoDao
}