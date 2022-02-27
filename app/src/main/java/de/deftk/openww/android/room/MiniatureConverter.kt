package de.deftk.openww.android.room

import androidx.room.TypeConverter
import de.deftk.openww.api.WebWeaverClient
import de.deftk.openww.api.model.ProfileMiniature
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class MiniatureConverter {

    @TypeConverter
    fun fromMiniature(miniature: ProfileMiniature?): String? {
        miniature ?: return null
        return WebWeaverClient.json.encodeToString(miniature)
    }

    @TypeConverter
    fun toMiniature(miniature: String?): ProfileMiniature? {
        miniature ?: return null
        return WebWeaverClient.json.decodeFromString(miniature)
    }

}