package de.deftk.openlonet.utils

import android.content.Intent
import de.deftk.lonet.api.LoNetClient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

inline fun <reified T> Intent.getJsonExtra(key: String): T? {
    return if (hasExtra(key))
        LoNetClient.json.decodeFromString(getStringExtra(key)!!)
    else null
}

inline fun <reified T> Intent.putJsonExtra(key: String, obj: T) {
    putExtra(key, LoNetClient.json.encodeToString(obj))
}