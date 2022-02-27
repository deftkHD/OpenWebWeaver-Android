package de.deftk.openww.android.feature.filestorage

import java.util.*

sealed class NetworkTransfer(val workerId: UUID, val id: String) {

    var progressValue = 0
    var maxProgress = 1

    class DownloadOpen(workerId: UUID, id: String) : NetworkTransfer(workerId, id)
    class DownloadSave(workerId: UUID, id: String) : NetworkTransfer(workerId, id)
    class Upload(workerId: UUID, id: String) : NetworkTransfer(workerId, id)

}