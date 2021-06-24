package de.deftk.openww.android.feature.filestorage.integration

import de.deftk.openww.android.api.Response
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.FilePreviewUrl
import de.deftk.openww.api.model.feature.filestorage.IRemoteFileProvider

data class ProviderCacheElement(val scope: IOperatingScope, val provider: IRemoteFileProvider) {

    var children: Response<List<ProviderCacheElement>>? = null
    var previewUrl: FilePreviewUrl? = null

}