package com.rlfm.mifarereader.utils

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Data class representing a Mifare Classic card
 */
data class MifareCardData(
    val uid: String,
    val type: String,
    val size: Int,
    val sectorCount: Int,
    val blockCount: Int,
    val sectors: List<SectorData>,
    val rawData: String
)

/**
 * Data class representing a sector in a Mifare Classic card
 */
data class SectorData(
    val sectorIndex: Int,
    val blocks: List<BlockData>,
    val authenticated: Boolean
)

/**
 * Data class representing a block in a sector
 */
data class BlockData(
    val blockIndex: Int,
    val data: ByteArray,
    val hexData: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockData

        if (blockIndex != other.blockIndex) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = blockIndex
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Reader class for Mifare Classic cards
 */
class MifareClassicReader {

    companion object {
        private const val TAG = "MifareClassicReader"

        // Default keys to try for authentication
        private val DEFAULT_KEYS = listOf(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
            byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte()),
            byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        )
    }

    /**
     * Read all data from a Mifare Classic card
     */
    suspend fun readCard(tag: Tag): Result<MifareCardData> = withContext(Dispatchers.IO) {
        try {
            val mifareClassic = MifareClassic.get(tag)

            if (mifareClassic == null) {
                return@withContext Result.failure(Exception("Tag is not a Mifare Classic card"))
            }

            mifareClassic.connect()

            try {
                val uid = NfcUtils.getTagUid(tag)
                val type = NfcUtils.getMifareClassicTypeName(mifareClassic.type)
                val size = mifareClassic.size
                val sectorCount = mifareClassic.sectorCount
                val blockCount = mifareClassic.blockCount

                Log.d(TAG, "Reading card - UID: $uid, Type: $type, Sectors: $sectorCount")

                val sectors = mutableListOf<SectorData>()
                val rawDataBuilder = StringBuilder()

                // Read each sector
                for (sectorIndex in 0 until sectorCount) {
                    val sectorData = readSector(mifareClassic, sectorIndex)
                    sectors.add(sectorData)

                    // Build raw data string
                    rawDataBuilder.append("Sector $sectorIndex:\n")
                    sectorData.blocks.forEach { block ->
                        rawDataBuilder.append("  Block ${block.blockIndex}: ${block.hexData}\n")
                    }
                    rawDataBuilder.append("\n")
                }

                val cardData = MifareCardData(
                    uid = uid,
                    type = type,
                    size = size,
                    sectorCount = sectorCount,
                    blockCount = blockCount,
                    sectors = sectors,
                    rawData = rawDataBuilder.toString()
                )

                Result.success(cardData)

            } finally {
                try {
                    mifareClassic.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing connection", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error reading card", e)
            Result.failure(e)
        }
    }

    /**
     * Read a single sector from the card
     */
    private fun readSector(mifareClassic: MifareClassic, sectorIndex: Int): SectorData {
        var authenticated = false

        // Try to authenticate with default keys
        for (key in DEFAULT_KEYS) {
            try {
                if (mifareClassic.authenticateSectorWithKeyA(sectorIndex, key)) {
                    authenticated = true
                    Log.d(TAG, "Sector $sectorIndex authenticated with key A: ${NfcUtils.formatHex(key)}")
                    break
                }
            } catch (e: IOException) {
                Log.w(TAG, "Authentication failed for sector $sectorIndex with key A", e)
            }

            try {
                if (mifareClassic.authenticateSectorWithKeyB(sectorIndex, key)) {
                    authenticated = true
                    Log.d(TAG, "Sector $sectorIndex authenticated with key B: ${NfcUtils.formatHex(key)}")
                    break
                }
            } catch (e: IOException) {
                Log.w(TAG, "Authentication failed for sector $sectorIndex with key B", e)
            }
        }

        val blocks = mutableListOf<BlockData>()

        if (authenticated) {
            // Read all blocks in the sector
            val firstBlock = mifareClassic.sectorToBlock(sectorIndex)
            val blockCount = mifareClassic.getBlockCountInSector(sectorIndex)

            for (i in 0 until blockCount) {
                val blockIndex = firstBlock + i
                try {
                    val blockData = mifareClassic.readBlock(blockIndex)
                    blocks.add(
                        BlockData(
                            blockIndex = blockIndex,
                            data = blockData,
                            hexData = NfcUtils.formatHex(blockData)
                        )
                    )
                } catch (e: IOException) {
                    Log.w(TAG, "Error reading block $blockIndex", e)
                    // Add empty block on error
                    blocks.add(
                        BlockData(
                            blockIndex = blockIndex,
                            data = ByteArray(16),
                            hexData = "Error reading block"
                        )
                    )
                }
            }
        } else {
            Log.w(TAG, "Could not authenticate sector $sectorIndex")
        }

        return SectorData(
            sectorIndex = sectorIndex,
            blocks = blocks,
            authenticated = authenticated
        )
    }
}
