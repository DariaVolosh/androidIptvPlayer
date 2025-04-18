package com.example.iptvplayer.di

import com.example.iptvplayer.view.mainScreen.MainActivity
import dagger.Component
import javax.inject.Singleton

@Component(modules = [XmlPullParserModule::class, FirebaseModule::class, RetrofitModule::class])
@Singleton
interface ApplicationComponent {
    fun inject(activity: MainActivity)
}