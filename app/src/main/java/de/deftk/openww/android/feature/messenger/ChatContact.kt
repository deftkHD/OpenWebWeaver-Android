package de.deftk.openww.android.feature.messenger

import de.deftk.openww.api.model.RemoteScope

data class ChatContact(val user: RemoteScope, val isLocal: Boolean)