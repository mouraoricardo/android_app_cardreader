package com.rlfm.mifarereader

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Room Entity representing a card entry in the database
 */
@Entity(tableName = "cards")
data class CardEntry(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "uid")
    val uid: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Get formatted time string (HH:mm:ss)
     * Used for displaying time in the card list RecyclerView
     * @return Time string in 24-hour format (e.g., "14:35:22")
     */
    fun getFormattedTime(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    /**
     * Get formatted date and time string (dd/MM/yyyy HH:mm:ss)
     * Used for CSV export with full date and time information
     * @return Date and time string (e.g., "30/10/2025 14:35:22")
     */
    fun getFormattedDate(): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}
