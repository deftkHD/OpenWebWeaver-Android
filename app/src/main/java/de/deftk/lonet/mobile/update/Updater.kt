package de.deftk.lonet.mobile.update

import android.os.AsyncTask
import android.util.Base64
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.channels.Channels
import java.security.MessageDigest

class Updater(private val targetFile: File, private val callback: IUpdateCallback): AsyncTask<String, Void, Pair<String, String>>() {

    companion object {
        const val VERSION_URL = "http://<server address>/get_version"
        const val DOWNLOAD_URL = "http://<server address>/download"
        const val CHECKSUM_URL = "http://<server address>/checksum"

        const val MODE_CHECK_VERSION = "check_version"
        const val MODE_DOWNLOAD_UPDATE = "download_update"

        const val EXCEPTION = "exception"
        const val CHECKSUM_FAILED = "CHECKSUM_FAILED"
    }

    override fun doInBackground(vararg params: String?): Pair<String, String> {
        check(params.size == 1)
        when (params[0]) {
            MODE_CHECK_VERSION -> {
                return try {
                    val url = URL(VERSION_URL)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 1000
                    connection.readTimeout = 10000
                    val version = BufferedReader(InputStreamReader(connection.inputStream)).readLine()
                    connection.disconnect()
                    Pair(MODE_CHECK_VERSION, version)
                } catch (e: Exception) {
                    Pair(EXCEPTION, e.toString())
                }
            }
            MODE_DOWNLOAD_UPDATE -> {
                val url = URL(DOWNLOAD_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.addRequestProperty("Allow-Download", "Yes, sir")
                if (targetFile.exists()) targetFile.delete() // whatever
                if (targetFile.parentFile?.exists() != true) targetFile.parentFile?.mkdirs()
                targetFile.createNewFile()
                targetFile.deleteOnExit()
                val fout = FileOutputStream(targetFile)
                fout.channel.transferFrom(Channels.newChannel(connection.inputStream), 0, connection.contentLength.toLong())

                if (downloadChecksum() != calculateChecksum(targetFile))
                    return Pair(EXCEPTION, CHECKSUM_FAILED)
                return Pair(MODE_DOWNLOAD_UPDATE, targetFile.absolutePath)
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun onPostExecute(result: Pair<String, String>) {
        when (result.first) {
            MODE_CHECK_VERSION -> {
                callback.onVersionCheckResult(result.second)
            }
            MODE_DOWNLOAD_UPDATE -> {
                callback.onUpdateDownloaded(File(result.second))
            }
            EXCEPTION -> {
                callback.onUpdateException(result.second)
            }
        }
    }

    private fun downloadChecksum(): String {
        val url = URL(CHECKSUM_URL)
        val connection = url.openConnection() as HttpURLConnection
        return BufferedReader(InputStreamReader(connection.inputStream)).readLine()
    }

    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        val bos = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        val stream = FileInputStream(file)
        while (true) {
            val size = stream.read(buffer)
            if (size == -1) break
            bos.write(buffer, 0, size)
        }
        digest.update(bos.toByteArray())
        return Base64.encodeToString(digest.digest(), 0).replace("\n", "") // output ends with \n
    }

    interface IUpdateCallback {
        fun onVersionCheckResult(onlineVersion: String)
        fun onUpdateDownloaded(file: File)
        fun onUpdateException(message: String)
    }
}