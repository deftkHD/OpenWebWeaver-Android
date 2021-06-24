package de.deftk.openww.android.feature.filestorage

import androidx.lifecycle.MutableLiveData
import de.deftk.openww.api.model.feature.FilePreviewUrl
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import de.deftk.openww.android.api.Response

data class FileCacheElement(val file: IRemoteFile) {

    val children = MutableLiveData<Response<List<FileCacheElement>>>()
    var previewUrl: FilePreviewUrl? = null

}
