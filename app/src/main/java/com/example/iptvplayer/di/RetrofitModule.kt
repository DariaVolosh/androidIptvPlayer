package com.example.iptvplayer.di

import com.example.iptvplayer.data.FLUSSONIC_BASE_URL
import com.example.iptvplayer.data.auth.AuthInterceptor
import com.example.iptvplayer.domain.auth.AuthManager
import com.example.iptvplayer.retrofit.services.AuthService
import com.example.iptvplayer.retrofit.services.ChannelsAndEpgService
import com.example.iptvplayer.retrofit.services.FlussonicService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class FlussonicRetrofit

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ChannelsBackendRetrofit

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UnauthenticatedRetrofit

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthenticatedOkHttpClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UnauthenticatedOkHttpClient

@InstallIn(SingletonComponent::class)
@Module
class RetrofitModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(authManager: AuthManager): AuthInterceptor {
        return AuthInterceptor(authManager)
    }

    @Provides
    @Singleton
    @AuthenticatedOkHttpClient
    fun provideAuthenticatedOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @UnauthenticatedOkHttpClient
    fun provideUnauthenticatedOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .build()
    }

    @Provides
    @Singleton
    @FlussonicRetrofit
    fun provideFlussonicRetrofit(@UnauthenticatedOkHttpClient okHttpClient: OkHttpClient) =
        Retrofit.Builder()
            .client(okHttpClient)
            ***REMOVED***
            ***REMOVED***
            ***REMOVED***
            ***REMOVED***
            // lasha flussonic
            .baseUrl(FLUSSONIC_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @ChannelsBackendRetrofit
    fun provideChannelsAndEpgRetrofit(@AuthenticatedOkHttpClient okHttpClient: OkHttpClient) =
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("https://airnet.admi.ge/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @UnauthenticatedRetrofit
    fun provideUnauthenticatedRetrofit(@UnauthenticatedOkHttpClient okHttpClient: OkHttpClient) =
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("https://airnet.admi.ge/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideFlussonicService(@FlussonicRetrofit retrofit: Retrofit) =
        retrofit.create(FlussonicService::class.java)


    @Provides
    @Singleton
    fun provideChannelsAndEpgService(@ChannelsBackendRetrofit retrofit: Retrofit) =
        retrofit.create(ChannelsAndEpgService::class.java)

    @Provides
    @Singleton
    fun provideAuthService(@UnauthenticatedRetrofit retrofit: Retrofit)=
        retrofit.create(AuthService::class.java)
}