package de.deftk.openww.android.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface IgnoredTaskDao {

    @Query("SELECT * FROM ignoredtask")
    suspend fun getIgnoredTasks(): List<IgnoredTask>

    @Insert
    suspend fun ignoreTasks(tasks: List<IgnoredTask>)

    @Delete
    suspend fun unignoreTasks(tasks: List<IgnoredTask>)

}