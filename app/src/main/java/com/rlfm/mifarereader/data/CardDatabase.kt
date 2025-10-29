package com.rlfm.mifarereader.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rlfm.mifarereader.CardEntry

/**
 * Room Database for storing card entries
 */
@Database(
    entities = [CardEntry::class],
    version = 1,
    exportSchema = false
)
abstract class CardDatabase : RoomDatabase() {

    /**
     * Get the CardDao instance
     */
    abstract fun cardDao(): CardDao

    companion object {
        @Volatile
        private var INSTANCE: CardDatabase? = null

        /**
         * Get the singleton database instance
         * @param context Application context
         * @return CardDatabase instance
         */
        fun getDatabase(context: Context): CardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CardDatabase::class.java,
                    "card_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
