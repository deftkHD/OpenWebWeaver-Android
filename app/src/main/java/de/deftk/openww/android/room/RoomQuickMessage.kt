package de.deftk.openww.android.room

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import de.deftk.openww.api.implementation.feature.messenger.QuickMessage
import de.deftk.openww.api.model.RemoteScope
import de.deftk.openww.api.model.feature.FileDownloadUrl
import de.deftk.openww.api.model.feature.messenger.IQuickMessage
import java.util.*

@Entity(primaryKeys = ["account", "id"])
data class RoomQuickMessage(
    val account: String,
    val id: Int,
    @Embedded(prefix = "from") val from: RemoteScope,
    @Embedded(prefix = "to") val to: RemoteScope,
    val text: String?,
    val date: Long,
    val flags: String,
    @ColumnInfo(name = "file_name") val fileName: String?,
    @Embedded(prefix = "download") val file: FileDownloadUrl?
) {

    companion object {
        fun from(account: String, quickMessage: IQuickMessage): RoomQuickMessage {
            return RoomQuickMessage(
                account,
                quickMessage.id,
                quickMessage.from,
                quickMessage.to,
                quickMessage.text,
                quickMessage.date.time,
                quickMessage.flags,
                quickMessage.fileName,
                quickMessage.file
            )
        }
    }

    fun toQuickMessage(): QuickMessage {
        return QuickMessage(
            id,
            from,
            to,
            text,
            Date(date),
            flags,
            fileName,
            file
        )
    }

}