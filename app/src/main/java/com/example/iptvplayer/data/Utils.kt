package com.example.iptvplayer.data

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.instacart.library.truetime.TrueTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Utils {
    @RequiresApi(Build.VERSION_CODES.O)
    fun convertToGmt4(date: Long): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tbilisi"))
        calendar.time = Date(date * 1000)
        return calendar.timeInMillis / 1000
    }

    fun formatDate(date: Long, pattern: String): String {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(Date(date * 1000))
    }

    fun compareDates(date1: String, date2: String): Int {
        val formatter = SimpleDateFormat("dd MMMM", Locale.getDefault())
        val dateObj1 = formatter.parse(date1)
        val dateObj2 = formatter.parse(date2)
        return dateObj1.compareTo(dateObj2)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getCurrentTime(): Long =
        withContext(Dispatchers.IO) {
            TrueTime.build().withNtpHost("pool.ntp.org").initialize()
            Log.i("I SEE YOU", TrueTime.now().time.toString())
            TrueTime.now().time / 1000
        }
}