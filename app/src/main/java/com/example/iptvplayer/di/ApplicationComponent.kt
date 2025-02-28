package com.example.iptvplayer.di

import com.example.iptvplayer.MainActivity
import dagger.Component
import javax.inject.Singleton

@Component(modules = [XmlPullParserModule::class, FirebaseModule::class])
@Singleton
interface ApplicationComponent {
    fun inject(activity: MainActivity)
}