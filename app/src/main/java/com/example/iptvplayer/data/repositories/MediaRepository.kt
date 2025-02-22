package com.example.iptvplayer.data.repositories

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tv.danmaku.ijk.media.player.misc.IMediaDataSource
import java.io.InputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

class MediaDataSource @Inject constructor(

): IMediaDataSource {
    private var inputStream: InputStream? = null
    private var onNextSegmentRequest: () -> Unit = {}
    private var nextSegmentRequested = true

    //private var totalBytes = 0

    override fun readAt(
        position: Long,
        buffer: ByteArray?,
        offset: Int,
        size: Int
    ): Int {
        var bytesRead = 0
       try {
           bytesRead = inputStream?.read(buffer, offset, size) ?: 0
       } catch (e: Exception) {
           Log.i("lel", "readAt exception $e")
       }

        if (bytesRead == -1 && !nextSegmentRequested) {
            nextSegmentRequested = true
            onNextSegmentRequest()
        }
        if (bytesRead != -1) {
            Log.i("LOL", "bytes read: $bytesRead $inputStream")
        }
        return if (bytesRead == -1) 0 else bytesRead
    }

    override fun getSize(): Long {
        return -1
    }

    override fun close() {

    }

    suspend fun setMediaUrl(url: String, onUrlSet: (MediaDataSource) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                Log.i("emission", "setMediaUrl $url")
                val connection = URL(url).openConnection()
                inputStream = connection.getInputStream()
                Log.i("inputStream", inputStream.toString())
                onUrlSet(this@MediaDataSource)
                Log.i("lel", "setMediaUrl $url ${this@MediaDataSource}")
                nextSegmentRequested = false
            } catch (e: Exception) {
                //Log.i("exception", e.message.toString())
            }
        }
    }

    fun setOnNextSegmentRequestedCallback(callback: () -> Unit) {
        onNextSegmentRequest = callback
    }
}

@Singleton
class MediaRepository @Inject constructor(
    private val mediaDataSource: MediaDataSource,
) {

    fun setOnNextSegmentRequestedCallback(callback: () -> Unit) {
        mediaDataSource.setOnNextSegmentRequestedCallback(callback)
    }

    fun getMediaDataSource() = mediaDataSource

    suspend fun setMediaUrl(url: String, onUrlSet: (MediaDataSource) -> Unit) {
        mediaDataSource.setMediaUrl(url, onUrlSet)
    }
}