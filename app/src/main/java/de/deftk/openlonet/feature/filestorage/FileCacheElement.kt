package de.deftk.openlonet.feature.filestorage

import androidx.lifecycle.MutableLiveData
import de.deftk.lonet.api.model.feature.FilePreviewUrl
import de.deftk.lonet.api.model.feature.filestorage.IRemoteFile
import de.deftk.openlonet.api.Response

data class FileCacheElement(val file: IRemoteFile) {

    val children = MutableLiveData<Response<List<FileCacheElement>>>()
    var previewUrl: FilePreviewUrl? = null

}
