package de.deftk.openlonet.feature.filestorage

import android.net.Uri
import de.deftk.lonet.api.model.feature.abstract.IFilePrimitive
import java.util.concurrent.ConcurrentHashMap

class DocumentCache {

    private val cache = ConcurrentHashMap<Uri, List<IFilePrimitive>>()

    fun put(uri: Uri, documents: List<IFilePrimitive>) {
        cache[uri] = documents
    }

    fun get(uri: Uri?): List<IFilePrimitive>? {
        if (uri == null)
            return emptyList()
        return cache[uri]
    }

    fun clear() {
        cache.clear()
    }

}