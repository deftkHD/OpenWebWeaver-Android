package de.deftk.openww.android.feature.filestorage

import java.util.*

sealed class NetworkTransfer(val workerId: UUID, val id: String, val filename: String) {

    var progressValue = 0
    var maxProgress = 1

    class DownloadOpen(workerId: UUID, id: String, filename: String) : NetworkTransfer(workerId, id, filename)
    class DownloadSave(workerId: UUID, id: String, filename: String) : NetworkTransfer(workerId, id, filename)
    class Upload(workerId: UUID, id: String, filename: String) : NetworkTransfer(workerId, id, filename)

}