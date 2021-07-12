package de.deftk.openww.android.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RoomQuickMessage::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun quickMessageDao(): QuickMessageDao

}