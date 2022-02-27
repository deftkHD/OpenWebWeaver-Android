package de.deftk.openww.android.room

import androidx.room.*
import de.deftk.openww.api.model.RemoteScope

@Dao
interface QuickMessageDao {

    @Query("SELECT * FROM roomquickmessage WHERE account = (:account) AND (fromlogin = (:login) OR tologin = (:login))")
    suspend fun getHistoryWith(account: String, login: String): List<RoomQuickMessage>

    @Query("SELECT DISTINCT fromlogin AS `login`, fromname AS `name`, fromtype AS `type`, fromonline AS `online`, fromalias AS `alias`, fromminiature AS 'miniature' FROM roomquickmessage WHERE account = (:account)")
    suspend fun getAllSenders(account: String): List<RemoteScope>

    @Query("SELECT DISTINCT tologin AS `login`, toname AS `name`, totype AS `type`, toonline AS `online`, toalias AS `alias`, tominiature AS 'miniature' FROM roomquickmessage WHERE account = (:account)")
    suspend fun getAllRecipients(account: String): List<RemoteScope>

    @Insert
    suspend fun insertMessages(messages: List<RoomQuickMessage>)

    @Delete
    suspend fun deleteMessages(messages: List<RoomQuickMessage>)

}