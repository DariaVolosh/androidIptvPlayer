package com.example.iptvplayer.di

import android.content.Context
import androidx.room.Room
import com.example.iptvplayer.room.AppDatabase
import com.example.iptvplayer.room.ChannelDao
import com.example.iptvplayer.room.EpgDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class RoomModule {

    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "iptv-database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideChannelDao(appDatabase: AppDatabase): ChannelDao {
        return appDatabase.channelDao()
    }

    @Provides
    fun provideEpgDao(appDatabase: AppDatabase): EpgDao {
        return appDatabase.epgDao()
    }
}