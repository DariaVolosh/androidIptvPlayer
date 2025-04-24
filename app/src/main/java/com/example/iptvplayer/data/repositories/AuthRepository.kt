package com.example.iptvplayer.data.repositories

import android.util.Log
import com.example.iptvplayer.retrofit.data.ChannelsAndEpgAuthResponse
import com.example.iptvplayer.retrofit.services.ChannelsAndEpgService
import kotlinx.coroutines.CompletableDeferred
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val channelsAndEpgService: ChannelsAndEpgService
){
    suspend fun getAuthToken(requestBody: RequestBody): String  {
        Log.i("get auth token", requestBody.contentType().toString())
        val tokenDeferred = CompletableDeferred<String>()
        channelsAndEpgService.getAuthToken(requestBody)
            .enqueue(
                object: Callback<ChannelsAndEpgAuthResponse> {
                    override fun onResponse(
                        call: Call<ChannelsAndEpgAuthResponse>,
                        response: Response<ChannelsAndEpgAuthResponse>
                    ) {
                        Log.i("token on response", response.code().toString())
                        if (response.isSuccessful) {
                            if (response.code() == 200) {
                                response.body()?.data?.token?.let { token ->
                                    tokenDeferred.complete(token)
                                    return
                                }
                            }
                        }

                        tokenDeferred.complete("")
                    }

                    override fun onFailure(call: Call<ChannelsAndEpgAuthResponse>, throwable: Throwable) {
                        Log.i("token on response", throwable.toString())
                        tokenDeferred.complete("")
                    }
                }
            )

        return tokenDeferred.await()
    }
}