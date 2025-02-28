package com.example.iptvplayer.data

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {
    @RequiresApi(Build.VERSION_CODES.O)
    fun convertToGmt4(date: Long): String {
        val formatter = SimpleDateFormat("dd MMMM HH:mm", Locale.getDefault())
        return formatter.format(Date(date * 1000))
    }

    fun compareDates(date1: String, date2: String): Int {
        val formatter = SimpleDateFormat("dd MMMM", Locale.getDefault())
        val dateObj1 = formatter.parse(date1)
        val dateObj2 = formatter.parse(date2)
        return dateObj1.compareTo(dateObj2)
    }
}