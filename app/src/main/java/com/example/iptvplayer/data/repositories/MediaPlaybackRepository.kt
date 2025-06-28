package com.example.iptvplayer.data.repositories

import com.example.iptvplayer.data.DefaultInputStreamProvider
import com.example.iptvplayer.data.InputStreamProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tv.danmaku.ijk.media.player.misc.IMediaDataSource
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaDataSource @Inject constructor(
    private val inputStreamProvider: InputStreamProvider
): IMediaDataSource {
    private var inputStream: InputStream? = null
    private var onNextSegmentRequest: () -> Unit = {}
    private var nextSegmentRequested = false

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
        } catch (e: Exception) { }

        println("bytes read $bytesRead")

        if (bytesRead <= 0 && !nextSegmentRequested) {
            nextSegmentRequested = true
            onNextSegmentRequest()
        }

        return if (bytesRead == -1) 0 else bytesRead
    }

    override fun getSize(): Long {
        return -1
    }

    override fun close() {

    }

    suspend fun setMediaUrl(url: String) {
        withContext(Dispatchers.IO) {
            try {
                println("set media url in data source $url ${this@MediaDataSource}")
                inputStream = inputStreamProvider.getStream(url)
                nextSegmentRequested = false
            } catch (e: Exception) {

            }
        }
    }

    fun setOnNextSegmentRequestedCallback(callback: () -> Unit) {
        onNextSegmentRequest = callback
    }
}

@Singleton
class MediaPlaybackRepository @Inject constructor(

) {
    fun getMediaDataSource(): MediaDataSource {
        return MediaDataSource(DefaultInputStreamProvider())
    }
}