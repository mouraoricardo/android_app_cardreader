package com.rlfm.mifarereader.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rlfm.mifarereader.CardEntry
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Card operations
 */
@Dao
interface CardDao {

    /**
     * Insert a new card entry
     * @param card The card to insert
     * @return The row ID of the inserted card
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntry): Long

    /**
     * Get all cards ordered by timestamp descending (newest first)
     * @return Flow of all cards
     */
    @Query("SELECT * FROM cards ORDER BY timestamp DESC")
    fun getAllCards(): Flow<List<CardEntry>>

    /**
     * Get all cards as a list (one-time query)
     * @return List of all cards
     */
    @Query("SELECT * FROM cards ORDER BY timestamp DESC")
    suspend fun getAllCardsList(): List<CardEntry>

    /**
     * Delete all cards
     */
    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()

    /**
     * Delete a specific card
     * @param card The card to delete
     */
    @Delete
    suspend fun deleteCard(card: CardEntry)

    /**
     * Get the total count of cards
     * @return Flow of card count
     */
    @Query("SELECT COUNT(*) FROM cards")
    fun getCardCount(): Flow<Int>

    /**
     * Get a card by UID and timestamp (for duplicate checking)
     * @param uid The card UID
     * @param timestamp The timestamp
     * @return The card if found, null otherwise
     */
    @Query("SELECT * FROM cards WHERE uid = :uid AND timestamp = :timestamp LIMIT 1")
    suspend fun getCardByUidAndTimestamp(uid: String, timestamp: Long): CardEntry?
}
