package com.example.iptvplayer.di

import com.example.iptvplayer.retrofit.FlussonicService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@InstallIn(SingletonComponent::class)
@Module
class RetrofitModule {
    @Provides
    fun provideRetrofit() =
        Retrofit.Builder()
            ***REMOVED***
            .baseUrl("http://193.176.212.58:8080/")
            ***REMOVED***
            ***REMOVED***
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    fun provideFlussonicService(retrofit: Retrofit) =
        retrofit.create(FlussonicService::class.java)

}