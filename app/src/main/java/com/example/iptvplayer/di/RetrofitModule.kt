package com.example.iptvplayer.di

import com.example.iptvplayer.retrofit.services.ChannelsAndEpgService
import com.example.iptvplayer.retrofit.services.FlussonicService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class FlussonicRetrofit

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ChannelsBackendRetrofit

@InstallIn(SingletonComponent::class)
@Module
class RetrofitModule {
    @Provides
    @FlussonicRetrofit
    fun provideFlussonicRetrofit() =
        Retrofit.Builder()
            ***REMOVED***
            ***REMOVED***
            ***REMOVED***
            ***REMOVED***
            // lasha flussonic
            .baseUrl("http://185.15.115.246/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @ChannelsBackendRetrofit
    fun provideChannelsAndEpgRetrofit() =
        Retrofit.Builder()
            .baseUrl("https://airnet.admi.ge/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    fun provideFlussonicService(@FlussonicRetrofit retrofit: Retrofit) =
        retrofit.create(FlussonicService::class.java)


    @Provides
    fun provideChannelsAndEpgService(@ChannelsBackendRetrofit retrofit: Retrofit) =
        retrofit.create(ChannelsAndEpgService::class.java)
}