package com.example.iptvplayer.di

import com.example.iptvplayer.data.DefaultIjkPlayerFactory
import com.example.iptvplayer.data.DefaultInputStreamProvider
import com.example.iptvplayer.data.IjkPlayerFactory
import com.example.iptvplayer.data.InputStreamProvider
import com.example.iptvplayer.data.repositories.MediaDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tv.danmaku.ijk.media.player.misc.IMediaDataSource

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaBindingModule {
    @Binds
    abstract fun bindIMediaDataSource(mediaDataSource: MediaDataSource): IMediaDataSource

    @Binds
    abstract fun bindIjkPlayerFactory(defaultIjkPlayerFactory: DefaultIjkPlayerFactory): IjkPlayerFactory

    @Binds
    abstract fun bindInputStreamProvider(defaultInputStreamProvider: DefaultInputStreamProvider): InputStreamProvider
}