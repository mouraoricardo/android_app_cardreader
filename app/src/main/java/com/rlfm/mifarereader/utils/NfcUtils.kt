package com.rlfm.mifarereader.utils

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import java.io.IOException

/**
 * Utility class for NFC operations
 */
object NfcUtils {

    /**
     * Convert byte array to hex string
     */
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach { byte ->
            val value = byte.toInt()
            result.append(hexChars[value shr 4 and 0x0F])
            result.append(hexChars[value and 0x0F])
        }

        return result.toString()
    }

    /**
     * Convert hex string to byte array
     */
    fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").replace(":", "")
        val result = ByteArray(cleanHex.length / 2)

        for (i in result.indices) {
            val index = i * 2
            val value = cleanHex.substring(index, index + 2).toInt(16)
            result[i] = value.toByte()
        }

        return result
    }

    /**
     * Format byte array to readable hex string with separators
     */
    fun formatHex(bytes: ByteArray, separator: String = " "): String {
        return bytes.joinToString(separator) { byte ->
            String.format("%02X", byte)
        }
    }

    /**
     * Get tag UID as hex string
     */
    fun getTagUid(tag: Tag): String {
        return formatHex(tag.id, ":")
    }

    /**
     * Check if tag supports Mifare Classic
     */
    fun isMifareClassic(tag: Tag): Boolean {
        return tag.techList.contains(MifareClassic::class.java.name)
    }

    /**
     * Get Mifare Classic type name
     */
    fun getMifareClassicTypeName(type: Int): String {
        return when (type) {
            MifareClassic.TYPE_CLASSIC -> "Mifare Classic"
            MifareClassic.TYPE_PLUS -> "Mifare Plus"
            MifareClassic.TYPE_PRO -> "Mifare Pro"
            else -> "Unknown"
        }
    }

    /**
     * Get Mifare Classic size name
     */
    fun getMifareClassicSizeName(size: Int): String {
        return when (size) {
            MifareClassic.SIZE_1K -> "1K (1024 bytes)"
            MifareClassic.SIZE_2K -> "2K (2048 bytes)"
            MifareClassic.SIZE_4K -> "4K (4096 bytes)"
            else -> "Unknown ($size bytes)"
        }
    }
}
