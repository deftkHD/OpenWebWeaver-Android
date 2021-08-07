package de.deftk.openww.android.feature.filestorage

import de.deftk.openww.api.model.feature.FilePreviewUrl
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile

data class FileCacheElement(val file: IRemoteFile, var previewUrl: FilePreviewUrl? = null)