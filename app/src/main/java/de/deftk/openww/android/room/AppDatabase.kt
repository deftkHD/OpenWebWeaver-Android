package de.deftk.openww.android.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [RoomQuickMessage::class, IgnoredTask::class], version = 4)
@TypeConverters(MiniatureConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun quickMessageDao(): QuickMessageDao
    abstract fun ignoredTaskDao(): IgnoredTaskDao

}