package de.deftk.openlonet.feature.filestorage

import android.net.Uri
import de.deftk.lonet.api.implementation.OperatingScope
import de.deftk.lonet.api.model.feature.filestorage.IRemoteFileProvider
import java.util.concurrent.ConcurrentHashMap

class DocumentCache {

    private val cache = ConcurrentHashMap<Uri, List<Pair<IRemoteFileProvider, OperatingScope>>>()

    fun put(uri: Uri, documents: List<Pair<IRemoteFileProvider, OperatingScope>>) {
        cache[uri] = documents
    }

    fun get(uri: Uri?): List<Pair<IRemoteFileProvider, OperatingScope>>? {
        if (uri == null)
            return emptyList()
        return cache[uri]
    }

    fun clear() {
        cache.clear()
    }

}