package com.example.iptvplayer.data.repositories

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

    fun unzipGzip(inputStream: InputStream): GZIPInputStream {
        return GZIPInputStream(inputStream)
    }
}