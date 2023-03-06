package de.deftk.openww.android.feature.board

import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.feature.board.IBoardNotification

data class BoardNotification(val notification: IBoardNotification, val group: IGroup)