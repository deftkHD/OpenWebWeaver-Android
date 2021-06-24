package de.deftk.openww.android.feature.filestorage.integration

import android.net.Uri
import java.util.concurrent.ConcurrentHashMap

class ProviderCache {

    private val cache = ConcurrentHashMap<Uri, List<ProviderCacheElement>>()

    fun put(uri: Uri, documents: List<ProviderCacheElement>) {
        cache[uri] = documents
    }

    fun get(uri: Uri?): List<ProviderCacheElement>? {
        if (uri == null)
            return emptyList()
        return cache[uri]
    }

    fun clear() {
        cache.clear()
    }

    fun dump() {
        cache.forEach { (uri, children) ->
            println(uri)
            children.forEach { child ->
                print("    ")
                println(child.provider.name)
                print("        ")
                println(child.children?.valueOrNull()?.size ?: -1)
                print("        ")
                println(child.scope.login)
            }
        }
    }

}