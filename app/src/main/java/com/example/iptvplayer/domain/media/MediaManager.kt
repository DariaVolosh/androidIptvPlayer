package com.example.iptvplayer.domain.media

import android.view.Surface
import com.example.iptvplayer.data.IjkPlayerFactory
import com.example.iptvplayer.data.repositories.MediaDataSource
import com.example.iptvplayer.di.IoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IMediaPlayer.OnPreparedListener
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaManager @Inject constructor(
    ijkPlayerFactory: IjkPlayerFactory,
    @IoDispatcher managerScope: CoroutineScope
) {
    // player instance
    private val _ijkPlayer = MutableStateFlow<IjkMediaPlayer?>(null)
    val ijkPlayer: StateFlow<IjkMediaPlayer?> = _ijkPlayer

    init {
        managerScope.launch {
            delay(1)
            _ijkPlayer.value = ijkPlayerFactory.create()
            println("player instantiated")
        }
    }

    fun play() {
        ijkPlayer.value?.start()
    }

    fun pause() {
        ijkPlayer.value?.pause()
    }

    fun setOnPreparedListener(onPreparedListener: OnPreparedListener) {
        ijkPlayer.value?.setOnPreparedListener(onPreparedListener)
    }

    fun setOnInfoListener(infoListener: (IMediaPlayer, Int, Int) -> Boolean) {
        ijkPlayer.value?.setOnInfoListener(infoListener)
    }

    fun setDataSource(dataSource: MediaDataSource) {
        println("set data source $dataSource")
        println("set data source ${ijkPlayer.value}")
        ijkPlayer.value?.setDataSource(dataSource)
        ijkPlayer.value?.prepareAsync()
    }

    fun resetPlayer() {
        println("player reset")
        ijkPlayer.value?.reset()
    }

    fun setPlayerSurface(surface: Surface) {
        _ijkPlayer.value?.setSurface(surface)
    }
}