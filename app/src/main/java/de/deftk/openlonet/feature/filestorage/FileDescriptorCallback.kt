package de.deftk.openlonet.feature.filestorage

import android.os.Build
import android.os.CancellationSignal
import android.os.ProxyFileDescriptorCallback
import androidx.annotation.RequiresApi
import de.deftk.lonet.api.model.feature.files.FileUrl
import java.io.InputStream
import java.net.URL
import kotlin.math.min

@RequiresApi(Build.VERSION_CODES.O)
class FileDescriptorCallback(private val cancellationSignal: CancellationSignal?, private val createDownloadUrl: () -> FileUrl): ProxyFileDescriptorCallback() {

    private val downloadUrl by lazy { createDownloadUrl() }

    private var inputStream: InputStream? = null
    private var offset = 0L

    override fun onGetSize(): Long {
        return downloadUrl.size
    }

    override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
        if (inputStream == null || offset < this.offset)
            initStream()

        while (this.offset < offset)
            this.offset += inputStream!!.skip(offset - this.offset)

        var actualRead = 0
        val destinationRead = min(size, data.size)
        while (actualRead != destinationRead) {
            if (cancellationSignal?.isCanceled == true)
                return -1
            val currentRead = inputStream!!.read(data, actualRead, destinationRead - actualRead)
            if (currentRead <= 0)
                return actualRead
            actualRead += currentRead
            this.offset += currentRead
        }
        return actualRead
    }

    override fun onWrite(offset: Long, size: Int, data: ByteArray?): Int {
        TODO("Not yet implemented")
    }

    override fun onRelease() {
        if (inputStream != null)
            inputStream?.close()
    }

    private fun initStream() {
        inputStream?.close()
        inputStream = URL(downloadUrl.url).openStream()
        offset = 0L
    }

}