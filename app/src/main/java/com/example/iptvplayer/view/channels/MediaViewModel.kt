package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.MediaDataSource
import com.example.iptvplayer.domain.GetMediaDataSourceUseCase
import com.example.iptvplayer.domain.GetTsSegmentsUseCase
import com.example.iptvplayer.domain.HandleNextSegmentRequestedUseCase
import com.example.iptvplayer.domain.SetMediaUrlUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.util.LinkedList
import javax.inject.Inject

class MediaViewModel @Inject constructor(
    private val getTsSegmentsUseCase: GetTsSegmentsUseCase,
    private val getMediaDataSourceUseCase: GetMediaDataSourceUseCase,
    private val setMediaUrlUseCase: SetMediaUrlUseCase,
    private val handleNextSegmentRequestedUseCase: HandleNextSegmentRequestedUseCase
): ViewModel() {
    var ijkPlayer: IjkMediaPlayer? = null

    private val _isDataSourceSet: MutableLiveData<Boolean> = MutableLiveData(false)
    val isDataSourceSet: LiveData<Boolean> = _isDataSourceSet

    private val urlQueue = LinkedList<String>()
    private var isPlayerReset = true
    private var firstSegmentRead = false

    private var tsJob: Job? = null
    private var segmentRequestJob: Job? = null

    private suspend fun setNextUrl(url: String) {
        setMediaUrlUseCase.setMediaUrl(url) { mediaSource ->
            if (!firstSegmentRead) startPlayback(mediaSource)
        }
    }

    private fun startPlayback(mediaSource: MediaDataSource) {
        try {
            Log.i("lel", "startPlayback $mediaSource")
            ijkPlayer?.prepareAsync()
            firstSegmentRead = true
        } catch (e: Exception) {
            Log.i("lel", "startPlayback exception ${e.message}")
        }
    }

    private fun setOnSegmentRequestCallback() {
        handleNextSegmentRequestedUseCase.setOnNextSegmentRequestedCallback {
            segmentRequestJob?.cancel()
            segmentRequestJob = viewModelScope.launch {
                while (true) {
                    urlQueue.poll()?.let { url ->
                        setNextUrl(url)
                        segmentRequestJob?.cancel()
                    }

                    delay(2000)
                }
            }
        }
    }

    fun setMediaUrl(url: String) {
        if (ijkPlayer != null) {
            ijkPlayer?.reset()
            isPlayerReset = true
            firstSegmentRead = false
            _isDataSourceSet.postValue(false)
        }

        tsJob?.cancel()
        urlQueue.clear()

        tsJob = viewModelScope.launch {
            getTsSegmentsUseCase.extractTsSegments(url).collect { u ->
                Log.i("emission", "collected $u")
                if (ijkPlayer == null || isPlayerReset) {
                    ijkPlayer = IjkMediaPlayer()
                    ijkPlayer?.setDataSource(getMediaDataSourceUseCase.getMediaDataSource())
                    IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG)

                    setOnSegmentRequestCallback()
                    setNextUrl(u)

                    ijkPlayer?.setOnPreparedListener {
                        _isDataSourceSet.postValue(true)
                    }

                    isPlayerReset = false

                } else {
                    urlQueue.add(u)
                }
            }
        }
    }



    /*@OptIn(UnstableApi::class)
    fun playMedia(url: String, context: Context) {
        if (exoPlayer == null) {
            val rendererFactory = DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

            exoPlayer = ExoPlayer.Builder(context).setRenderersFactory(rendererFactory).build()
            exoPlayer?.addListener(object: Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (exoPlayer?.isPlaying == true) {
                        _isPlaying.postValue(true)
                    }
                }
            })
        }

        exoPlayer?.setMediaItem(MediaItem.fromUri(url))
        exoPlayer?.prepare()
        exoPlayer?.play()
    } */

}