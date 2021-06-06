package de.deftk.openlonet.feature.filestorage

import java.util.*

sealed class NetworkTransfer(val workerId: UUID, val id: String) {

    var progress = 0

    class DownloadOpen(workerId: UUID, id: String) : NetworkTransfer(workerId, id) {

    }

    class DownloadSave(workerId: UUID, id: String) : NetworkTransfer(workerId, id) {

    }

    class Upload(workerId: UUID, id: String) : NetworkTransfer(workerId, id) {

    }

}