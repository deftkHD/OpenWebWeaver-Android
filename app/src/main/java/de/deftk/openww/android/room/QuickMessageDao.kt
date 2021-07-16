package de.deftk.openww.android.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface QuickMessageDao {

    @Query("SELECT * FROM roomquickmessage WHERE account = (:account) AND (fromlogin = (:login) OR tologin = (:login))")
    suspend fun getHistoryWith(account: String, login: String): List<RoomQuickMessage>

    @Insert
    suspend fun insertMessages(messages: List<RoomQuickMessage>)

    @Delete
    suspend fun deleteMessages(messages: List<RoomQuickMessage>)

}