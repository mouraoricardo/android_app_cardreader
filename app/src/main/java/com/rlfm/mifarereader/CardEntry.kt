package com.rlfm.mifarereader

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data class representing a card entry in the list
 */
data class CardEntry(
    val uid: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedTime(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun getFormattedDate(): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}
