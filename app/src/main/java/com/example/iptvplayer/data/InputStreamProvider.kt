package com.example.iptvplayer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL
import javax.inject.Inject

interface InputStreamProvider {
    suspend fun getStream(url: String): InputStream?
}

class DefaultInputStreamProvider @Inject constructor() : InputStreamProvider {
    override suspend fun getStream(url: String): InputStream? {
        return withContext(Dispatchers.IO) {
            try {
                println("get stream $url")
                val connection = URL(url).openConnection()
                connection.getInputStream()
            } catch (e: Exception) {
                println("get stream exception $e")
                null
            }
        }
    }
}