package com.example.iptvplayer

import android.app.Application
import com.example.iptvplayer.di.DaggerApplicationComponent

class MyApp: Application() {
    val appComponent = DaggerApplicationComponent.create()
}