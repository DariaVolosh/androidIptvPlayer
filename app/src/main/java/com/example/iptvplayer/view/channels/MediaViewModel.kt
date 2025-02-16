package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.MediaDataSource
import com.example.iptvplayer.domain.GetMediaDataSourceUseCase
import com.example.iptvplayer.domain.GetTsSegmentsUseCase
import com.example.iptvplayer.domain.SetMediaUrlUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.util.LinkedList
import javax.inject.Inject

class MediaViewModel @Inject constructor(
    private val getTsSegmentsUseCase: GetTsSegmentsUseCase,
    private val getMediaDataSourceUseCase: GetMediaDataSourceUseCase,
    private val setMediaUrlUseCase: SetMediaUrlUseCase
): ViewModel() {
    var ijkPlayer: IjkMediaPlayer? = null

    private val _isDataSourceSet: MutableLiveData<Boolean> = MutableLiveData(false)
    val isDataSourceSet: LiveData<Boolean> = _isDataSourceSet

    private val urlQueue = LinkedList<String>()
    private var isPlayerReset = true
    private var tsJob: Job? = null

    private suspend fun setNextUrl(url: String, isFirstSegment: Boolean) {
        setMediaUrlUseCase.setMediaUrl(url) { mediaSource ->
            if (isFirstSegment) startPlayback(mediaSource)
        }
    }

    private fun startPlayback(mediaSource: MediaDataSource) {
        try {
            Log.i("lel", "startPlayback $mediaSource")
            ijkPlayer?.prepareAsync()
        } catch (e: Exception) {
            Log.i("lel", "startPlayback exception ${e.message}")
        }
    }

    fun setMediaUrl(url: String) {
        if (ijkPlayer != null) {
            ijkPlayer?.reset()
            isPlayerReset = true
            _isDataSourceSet.postValue(false)
        }

        tsJob?.cancel()

        tsJob = viewModelScope.launch {
            getTsSegmentsUseCase.extractTsSegments(url).collect { u ->
                Log.i("emission", "collected $u")
                if (ijkPlayer == null || isPlayerReset) {
                    ijkPlayer = IjkMediaPlayer()
                    isPlayerReset = false
                    IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);

                    ijkPlayer?.setDataSource(getMediaDataSourceUseCase.getMediaDataSource({
                        urlQueue.poll()?.let {
                            url -> setNextUrl(url, false)
                            true
                        }

                        false
                    }))

                    setNextUrl(u, true)

                    ijkPlayer?.setOnPreparedListener {
                        _isDataSourceSet.postValue(true)
                    }
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