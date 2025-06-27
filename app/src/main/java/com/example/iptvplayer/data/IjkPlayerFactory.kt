package com.example.iptvplayer.data

import tv.danmaku.ijk.media.player.IjkMediaPlayer
import javax.inject.Inject

interface IjkPlayerFactory {
    fun create(): IjkMediaPlayer
}

class DefaultIjkPlayerFactory @Inject constructor(): IjkPlayerFactory {
    override fun create(): IjkMediaPlayer {
        return IjkMediaPlayer()
    }
}