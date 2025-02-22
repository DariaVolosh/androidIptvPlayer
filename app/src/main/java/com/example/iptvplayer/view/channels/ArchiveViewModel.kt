package com.example.iptvplayer.view.channels

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

fun secondsSinceEpoch() = System.currentTimeMillis() / 1000

@HiltViewModel
class ArchiveViewModel @Inject constructor(

): ViewModel() {
    private var seekSeconds = 5

    fun seekBack() {
        Log.i("seek", seekSeconds.toString())
        seekSeconds *= 2
    }

    fun getArchiveUrl(url: String): String {
        val startTime = secondsSinceEpoch() - seekSeconds
        val baseUrl = url.substring(0, url.lastIndexOf("/") + 1)
        val archiveUrl = baseUrl + "index-$startTime-$seekSeconds.m3u8"
        Log.i("seek", seekSeconds.toString())
        return archiveUrl
    }
}