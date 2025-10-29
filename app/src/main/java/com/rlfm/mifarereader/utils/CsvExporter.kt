package com.rlfm.mifarereader.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.opencsv.CSVWriter
import com.rlfm.mifarereader.CardEntry
import java.io.File
import java.io.FileWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Result of CSV export containing file path and URI for sharing
 */
data class CsvExportResult(
    val filePath: String,
    val fileUri: Uri?,
    val fileName: String
)

/**
 * Utility class for exporting card data to CSV
 * Exports to Downloads folder with proper Android 10+ support using MediaStore API
 */
class CsvExporter(private val context: Context) {

    companion object {
        private const val CSV_DIRECTORY = "RFIDCardReader"
        private const val MIME_TYPE_CSV = "text/csv"
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

    /**
     * Export list of card entries to CSV file in Downloads folder
     * Format: UID, Data/Hora
     * For Android 10+ (API 29+): Uses MediaStore API
     * For Android 9- (API < 29): Uses legacy external storage
     * @return Result with CsvExportResult containing file path, URI for sharing, and filename
     */
    fun exportCardList(cardList: List<CardEntry>): Result<CsvExportResult> {
        try {
            if (cardList.isEmpty()) {
                return Result.failure(IllegalArgumentException("Lista de cartões vazia"))
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "mifare_cards_$timestamp.csv"

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (API 29+): Use MediaStore
                exportUsingMediaStore(fileName, cardList)
            } else {
                // Android 9- (API < 29): Use legacy method
                exportUsingLegacyStorage(fileName, cardList)
            }

        } catch (e: Exception) {
            return Result.failure(Exception("Erro ao exportar CSV: ${e.message}", e))
        }
    }

    /**
     * Export using MediaStore API (Android 10+)
     * Returns URI from MediaStore which can be used for sharing
     */
    private fun exportUsingMediaStore(fileName: String, cardList: List<CardEntry>): Result<CsvExportResult> {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE_CSV)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return Result.failure(Exception("Falha ao criar ficheiro no MediaStore"))

            resolver.openOutputStream(uri)?.use { outputStream ->
                writeCardListToCsv(outputStream, cardList)
            } ?: return Result.failure(Exception("Falha ao abrir stream de saída"))

            // Get the actual file path for display purposes
            val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
            val displayPath = "$downloadsPath/$fileName"

            // Return result with MediaStore URI for sharing
            val result = CsvExportResult(
                filePath = displayPath,
                fileUri = uri,
                fileName = fileName
            )

            return Result.success(result)

        } catch (e: Exception) {
            return Result.failure(Exception("Erro ao exportar com MediaStore: ${e.message}", e))
        }
    }

    /**
     * Export using legacy external storage (Android 9-)
     * Uses FileProvider to get shareable URI
     */
    private fun exportUsingLegacyStorage(fileName: String, cardList: List<CardEntry>): Result<CsvExportResult> {
        try {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, fileName)
            file.outputStream().use { outputStream ->
                writeCardListToCsv(outputStream, cardList)
            }

            // Get FileProvider URI for sharing
            val fileUri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                null // If FileProvider fails, we'll still have the file path
            }

            val result = CsvExportResult(
                filePath = file.absolutePath,
                fileUri = fileUri,
                fileName = fileName
            )

            return Result.success(result)

        } catch (e: Exception) {
            return Result.failure(Exception("Erro ao exportar ficheiro: ${e.message}", e))
        }
    }

    /**
     * Write card list data to CSV output stream
     * CSV Format: UID, Data/Hora (compatible with Excel)
     */
    private fun writeCardListToCsv(outputStream: OutputStream, cardList: List<CardEntry>) {
        CSVWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->
            // Write header (column names)
            writer.writeNext(arrayOf("UID", "Data/Hora"))

            // Write all cards
            cardList.forEach { card ->
                writer.writeNext(
                    arrayOf(
                        card.uid,
                        card.getFormattedDate()
                    )
                )
            }
        }
    }
}
