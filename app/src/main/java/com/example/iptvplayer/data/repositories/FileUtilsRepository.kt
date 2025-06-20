package com.example.iptvplayer.data.repositories

import android.content.Context
import android.util.Log
import com.example.iptvplayer.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

class FileUtilsRepository @Inject constructor (
    @ApplicationContext val context: Context
) {
    private suspend fun getFileInputStream(
        fileUrl: String,
        onErrorCallback: (String, String) -> Unit
    ): InputStream? =
        withContext(Dispatchers.IO) {
            Log.i("file url read", fileUrl)
            try {
                val url = URL(fileUrl)
                Log.i("url created", "true")
                val connection = url.openConnection() as HttpsURLConnection
                connection.connectTimeout = 1000
                Log.i("connection open", "true")
                connection.connect()
                connection.inputStream
            } catch (e: IOException) {
                Log.i("exception?", e.localizedMessage)
                when (e) {
                    is FileNotFoundException, is SocketTimeoutException -> {
                        if (isFileDvrStream(fileUrl)) {
                            onErrorCallback(
                                context.getString(R.string.no_archive),
                                context.getString(R.string.no_archive_descr)
                            )
                        } else {
                            onErrorCallback(
                                context.getString(R.string.no_live),
                                context.getString(R.string.no_live_descr)
                            )
                        }
                    }

                    else -> onErrorCallback(
                        context.getString(R.string.playback_failed),
                        context.getString(R.string.unknown_error)
                    )
                }

                null
            }
        }

    suspend fun readFile(
        fileUrl: String,
        onErrorCallback: (String, String) -> Unit
    ): List<String> =
        withContext(Dispatchers.IO) {
            val inputStream = getFileInputStream(fileUrl, onErrorCallback)
            val content = mutableListOf<String>()

            Log.i("content", "content read?")

            inputStream?.let { stream ->
                val bufferedReader = BufferedReader(InputStreamReader(stream))

                var line: String? = bufferedReader.readLine()

                while (line != null) {
                    content += line
                    line = bufferedReader.readLine()
                }

                stream.close()
                bufferedReader.close()
                content
            } ?: content
        }

    suspend fun isLinkAccessible(url: String): Boolean =
        withContext(Dispatchers.IO) {
            Log.i("accessible called", url)
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

    fun isFileDvrStream(url: String): Boolean =
        url.contains("dvr")
}