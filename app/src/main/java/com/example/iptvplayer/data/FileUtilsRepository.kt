package com.example.iptvplayer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class FileUtilsRepository @Inject constructor (

) {
    suspend fun readFile(fileUrl: String): List<String> =
        withContext(Dispatchers.IO) {
            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            val inputStream = connection.inputStream
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))

            val content = mutableListOf<String>()
            var line: String? = bufferedReader.readLine()

            while (line != null) {
                content += line
                line = bufferedReader.readLine()
            }

            inputStream.close()
            bufferedReader.close()
            connection.disconnect()
            content
        }
}