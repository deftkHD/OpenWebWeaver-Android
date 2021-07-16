package de.deftk.openww.android.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RoomQuickMessage::class, IgnoredTask::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun quickMessageDao(): QuickMessageDao
    abstract fun ignoredTaskDao(): IgnoredTaskDao

}