package com.example.iptvplayer.data

import android.provider.MediaStore.Audio.Media
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.danmaku.ijk.media.player.misc.IMediaDataSource
import java.io.InputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

class MediaDataSource @Inject constructor(

): IMediaDataSource {
    private var inputStream: InputStream? = null
    private var onBufferingFinish: suspend () -> Unit = {}

    private var newSegmentRequested = false

    private var totalBytes = 0

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
        totalBytes += bytesRead

        if (bytesRead == -1 && !newSegmentRequested) {
            CoroutineScope(Dispatchers.IO).launch {
                onBufferingFinish()
                newSegmentRequested = true
            }
        }

        if (bytesRead != -1) {
            newSegmentRequested = false
            Log.i("LOL", " $this total bytes: $totalBytes bytes read: $bytesRead $inputStream")
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
                val connection = URL(url).openConnection()
                inputStream = connection.getInputStream()
                Log.i("inputStream", inputStream.toString())
                onUrlSet(this@MediaDataSource)
                Log.i("lel", "setMediaUrl $url ${this@MediaDataSource}")
            } catch (e: Exception) {
                Log.i("exception", e.message.toString())
            }
        }
    }

    fun setOnBufferingFinishCallback(callback: suspend () -> Unit) {
        onBufferingFinish = callback
    }
}

@Singleton
class MediaRepository @Inject constructor(
    private val mediaDataSource: MediaDataSource,
) {

    fun getMediaDataSource(onBufferingFinish: suspend () -> Unit): MediaDataSource {
        mediaDataSource.setOnBufferingFinishCallback(onBufferingFinish)
        return mediaDataSource
    }

    suspend fun setMediaUrl(url: String, onUrlSet: (MediaDataSource) -> Unit) {
        mediaDataSource.setMediaUrl(url, onUrlSet)
    }
}