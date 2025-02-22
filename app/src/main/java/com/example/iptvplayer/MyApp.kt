package com.example.iptvplayer

import android.app.Application
import com.example.iptvplayer.di.DaggerApplicationComponent
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp: Application() {
    val appComponent = DaggerApplicationComponent.create()
}