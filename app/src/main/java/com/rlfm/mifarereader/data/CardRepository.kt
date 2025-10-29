package com.rlfm.mifarereader.data

import android.content.Context
import com.rlfm.mifarereader.CardEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository class for managing card data operations
 * Provides a clean API for data access and abstracts the data source
 */
class CardRepository(context: Context) {

    private val cardDao: CardDao = CardDatabase.getDatabase(context).cardDao()

    /**
     * Get all cards as a Flow (reactive stream)
     * Automatically updates when database changes
     */
    val allCards: Flow<List<CardEntry>> = cardDao.getAllCards()

    /**
     * Get card count as a Flow
     */
    val cardCount: Flow<Int> = cardDao.getCardCount()

    /**
     * Insert a new card
     * @param card The card to insert
     * @return The row ID of the inserted card
     */
    suspend fun insertCard(card: CardEntry): Long {
        return cardDao.insertCard(card)
    }

    /**
     * Insert a new card with UID, type and timestamp
     * Creates a CardEntry with the provided timestamp and auto-generated ID
     * @param uid The card UID
     * @param type The card type
     * @param timestamp The timestamp when the card was detected (from NFC event)
     * @return The row ID of the inserted card
     */
    suspend fun insertCard(uid: String, type: String, timestamp: Long): Long {
        val card = CardEntry(
            uid = uid,
            type = type,
            timestamp = timestamp
        )
        return cardDao.insertCard(card)
    }

    /**
     * Get all cards as a list (one-time query)
     * Useful for CSV export
     * @return List of all cards
     */
    suspend fun getAllCardsList(): List<CardEntry> {
        return cardDao.getAllCardsList()
    }

    /**
     * Delete all cards
     */
    suspend fun deleteAllCards() {
        cardDao.deleteAllCards()
    }

    /**
     * Delete a specific card
     * @param card The card to delete
     */
    suspend fun deleteCard(card: CardEntry) {
        cardDao.deleteCard(card)
    }

    /**
     * Check if a card already exists by UID and timestamp
     * Useful for preventing exact duplicates
     * @param uid The card UID
     * @param timestamp The timestamp
     * @return True if card exists, false otherwise
     */
    suspend fun cardExists(uid: String, timestamp: Long): Boolean {
        return cardDao.getCardByUidAndTimestamp(uid, timestamp) != null
    }
}
