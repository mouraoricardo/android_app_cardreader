package com.rlfm.mifarereader.utils

import android.content.Context
import android.os.Environment
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for exporting card data to CSV
 */
class CsvExporter(private val context: Context) {

    companion object {
        private const val CSV_DIRECTORY = "RFIDCardReader"
    }

    /**
     * Export card data to CSV file
     */
    fun exportToCSV(cardData: MifareCardData): Result<String> {
        try {
            val file = createCsvFile(cardData.uid)

            CSVWriter(FileWriter(file)).use { writer ->
                // Write header
                writer.writeNext(arrayOf("Card Information"))
                writer.writeNext(arrayOf("UID", cardData.uid))
                writer.writeNext(arrayOf("Type", cardData.type))
                writer.writeNext(arrayOf("Size", "${cardData.size} bytes"))
                writer.writeNext(arrayOf("Sectors", cardData.sectorCount.toString()))
                writer.writeNext(arrayOf("Blocks", cardData.blockCount.toString()))
                writer.writeNext(arrayOf(""))

                // Write sector data header
                writer.writeNext(arrayOf("Sector Data"))
                writer.writeNext(arrayOf("Sector", "Block", "Hex Data", "ASCII"))

                // Write all blocks
                cardData.sectors.forEach { sector ->
                    sector.blocks.forEach { block ->
                        val ascii = bytesToAscii(block.data)
                        writer.writeNext(
                            arrayOf(
                                sector.sectorIndex.toString(),
                                block.blockIndex.toString(),
                                block.hexData,
                                ascii
                            )
                        )
                    }
                }

                // Write timestamp
                writer.writeNext(arrayOf(""))
                writer.writeNext(arrayOf("Exported at", getCurrentTimestamp()))
            }

            return Result.success(file.absolutePath)

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Create CSV file in the appropriate directory
     */
    private fun createCsvFile(uid: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "card_${uid.replace(":", "")}_$timestamp.csv"

        // Try to use external storage first (for API < 29)
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val directory = if (externalDir != null) {
            File(externalDir, CSV_DIRECTORY)
        } else {
            // Fallback to internal storage
            File(context.filesDir, CSV_DIRECTORY)
        }

        if (!directory.exists()) {
            directory.mkdirs()
        }

        return File(directory, fileName)
    }

    /**
     * Convert bytes to ASCII string (printable characters only)
     */
    private fun bytesToAscii(bytes: ByteArray): String {
        return bytes.joinToString("") { byte ->
            val char = byte.toInt() and 0xFF
            if (char in 32..126) {
                char.toChar().toString()
            } else {
                "."
            }
        }
    }

    /**
     * Get current timestamp as formatted string
     */
    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }
}
