package com.example.iptvplayer.di

import com.example.iptvplayer.data.NtpTimeClient
import com.example.iptvplayer.data.TrueTimeClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NtpModule {
    @Binds
    abstract fun bindNtpClient(timeClient: TrueTimeClient): NtpTimeClient
}