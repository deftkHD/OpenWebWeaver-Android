package de.deftk.openww.android.repository.board

import de.deftk.openww.android.feature.board.BoardNotification
import de.deftk.openww.android.repository.CacheDataSource

class BoardCacheDataSource: CacheDataSource() {

    private val notifications = mutableListOf<BoardNotification>()

    fun addNotification(notification: BoardNotification) {
        notifications.add(notification)
    }

    fun deleteNotification(notification: BoardNotification) {
        notifications.removeAll { it.group.login == notification.group.login && it.notification.id == notification.notification.id }
    }

    fun setData(notifications: List<BoardNotification>) {
        this.notifications.clear()
        this.notifications.addAll(notifications)
    }

    override fun cleanCache() {
        notifications.clear()
    }


}