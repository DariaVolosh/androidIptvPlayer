package com.example.iptvplayer.data

import android.util.Log
import com.instacart.library.truetime.TrueTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

interface NtpTimeClient {
    suspend fun getGmtTime(): Long
}

// implementations

class TrueTimeClient @Inject constructor(): NtpTimeClient {
    override suspend fun getGmtTime(): Long =
        withContext(Dispatchers.IO) {
            try {
                TrueTime.build().withNtpHost("pool.ntp.org").initialize()
                Log.i("I SEE YOU", TrueTime.now().time.toString())
                TrueTime.now().time / 1000
            } catch (e: IOException) {
                0L
            }
        }
}