package com.example.iptvplayer.data.repositories

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import javax.inject.Inject

class FileUtilsRepository @Inject constructor (

) {

    suspend fun getFileInputStream(fileUrl: String): InputStream =
        withContext(Dispatchers.IO) {
            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            connection.inputStream
        }

    suspend fun readFile(fileUrl: String): List<String> =
        withContext(Dispatchers.IO) {
            val inputStream = getFileInputStream(fileUrl)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))

            val content = mutableListOf<String>()
            var line: String? = bufferedReader.readLine()

            while (line != null) {
                content += line
                line = bufferedReader.readLine()
            }

            inputStream.close()
            bufferedReader.close()
            content
        }

    suspend fun isLinkAccessible(url: String): Boolean =
        withContext(Dispatchers.IO) {
            Log.i("accessible called", "$url")
            val isAccessible = CompletableDeferred<Boolean>()
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()

                isAccessible.complete(connection.responseCode == HttpURLConnection.HTTP_OK)
            } catch (e: Exception) {
                isAccessible.complete(false)
            }

            isAccessible.await()
        }

    fun unzipGzip(inputStream: InputStream): GZIPInputStream {
        return GZIPInputStream(inputStream)
    }
}