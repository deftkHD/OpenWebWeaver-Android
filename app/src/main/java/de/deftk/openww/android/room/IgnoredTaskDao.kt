package de.deftk.openww.android.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface IgnoredTaskDao {

    @Query("SELECT * FROM ignoredtask WHERE account = (:account)")
    suspend fun getIgnoredTasks(account: String): List<IgnoredTask>

    @Insert
    suspend fun ignoreTasks(tasks: List<IgnoredTask>)

    @Delete
    suspend fun unignoreTasks(tasks: List<IgnoredTask>)

}