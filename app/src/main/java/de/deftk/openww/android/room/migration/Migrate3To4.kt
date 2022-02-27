package de.deftk.openww.android.room.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migrate3To4: Migration(3, 4) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE RoomQuickMessage RENAME COLUMN toisOnline TO toonline")
        database.execSQL("ALTER TABLE RoomQuickMessage RENAME COLUMN fromisOnline TO fromonline")
        database.execSQL("ALTER TABLE RoomQuickMessage ADD COLUMN tominiature TEXT")
        database.execSQL("ALTER TABLE RoomQuickMessage ADD COLUMN fromminiature TEXT")

        // copy data into clone table with nullability fixed and replace original table (nullability cannot be altered afterwards)
        database.execSQL("CREATE TABLE `RoomQuickMessage2` (`account` TEXT NOT NULL, `id` INTEGER NOT NULL, `text` TEXT, `date` INTEGER NOT NULL, `flags` TEXT NOT NULL, `file_name` TEXT, `fromlogin` TEXT NOT NULL, `fromname` TEXT NOT NULL, `fromtype` INTEGER NOT NULL, `fromonline` INTEGER, `fromalias` TEXT, `tologin` TEXT NOT NULL, `toname` TEXT NOT NULL, `totype` INTEGER NOT NULL, `toonline` INTEGER, `toalias` TEXT, `downloadid` TEXT, `downloadname` TEXT, `downloadsize` INTEGER, `downloadurl` TEXT, `tominiature` TEXT, `fromminiature` TEXT, PRIMARY KEY(`account`, `id`))")
        database.execSQL("INSERT INTO RoomQuickMessage2 SELECT * from RoomQuickMessage")
        database.execSQL("DROP TABLE RoomQuickMessage")
        database.execSQL("ALTER TABLE RoomQuickMessage2 RENAME TO RoomQuickMessage")
    }
}