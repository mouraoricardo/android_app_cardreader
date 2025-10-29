package com.rlfm.mifarereader

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.rlfm.mifarereader.utils.NfcUtils

/**
 * NFC Reader class to manage NFC communication and card reading
 */
class NfcReader(private val activity: Activity) {

    companion object {
        private const val TAG = "NfcReader"
        private const val DEBOUNCE_INTERVAL_MS = 2000L // 2 seconds
        private const val VIBRATION_DURATION_MS = 200L // 200ms
    }

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var techLists: Array<Array<String>>

    // Debounce tracking
    private var lastReadUid: String? = null
    private var lastReadTimestamp: Long = 0

    // Callbacks
    private var onCardDetected: ((String, String, Long) -> Unit)? = null
    private var onNfcNotSupported: (() -> Unit)? = null
    private var onNfcDisabled: (() -> Unit)? = null

    /**
     * Initialize NFC adapter and configurations
     */
    fun initialize(): Boolean {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)

        if (nfcAdapter == null) {
            Log.e(TAG, "NFC not supported on this device")
            onNfcNotSupported?.invoke()
            return false
        }

        setupNfcIntent()
        return true
    }

    /**
     * Setup NFC intent filters and tech lists
     */
    private fun setupNfcIntent() {
        val intent = Intent(activity, activity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        pendingIntent = PendingIntent.getActivity(
            activity,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
        )

        // Intent filters for NFC tag discovery
        intentFilters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        )

        // Tech lists - prioritize Mifare Classic
        techLists = arrayOf(
            arrayOf(MifareClassic::class.java.name),
            arrayOf(android.nfc.tech.NfcA::class.java.name),
            arrayOf(android.nfc.tech.Ndef::class.java.name)
        )
    }

    /**
     * Enable NFC foreground dispatch
     */
    fun enableForegroundDispatch() {
        if (!isNfcEnabled()) {
            onNfcDisabled?.invoke()
            return
        }

        nfcAdapter?.enableForegroundDispatch(
            activity,
            pendingIntent,
            intentFilters,
            techLists
        )
        Log.d(TAG, "NFC foreground dispatch enabled")
    }

    /**
     * Disable NFC foreground dispatch
     */
    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
        Log.d(TAG, "NFC foreground dispatch disabled")
    }

    /**
     * Handle NFC intent when a tag is detected
     */
    fun handleIntent(intent: Intent): Boolean {
        val action = intent.action

        if (action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED &&
            action != NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            return false
        }

        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (tag != null) {
            processTag(tag)
            return true
        }

        return false
    }

    /**
     * Process NFC tag and extract information
     */
    private fun processTag(tag: Tag) {
        try {
            // Extract UID
            val uid = NfcUtils.getTagUid(tag)
            val currentTime = System.currentTimeMillis()

            // Debounce check - prevent duplicate reads
            if (shouldDebounce(uid, currentTime)) {
                Log.d(TAG, "Debouncing read for UID: $uid")
                return
            }

            // Update debounce tracking
            lastReadUid = uid
            lastReadTimestamp = currentTime

            // Determine card type
            val cardType = determineCardType(tag)

            Log.i(TAG, "Card detected - UID: $uid, Type: $cardType")

            // Vibrate on successful read
            vibrateOnSuccess()

            // Notify listener
            onCardDetected?.invoke(uid, cardType, currentTime)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing tag", e)
        }
    }

    /**
     * Check if should debounce this read
     */
    private fun shouldDebounce(uid: String, currentTime: Long): Boolean {
        if (lastReadUid != uid) {
            return false
        }

        val timeSinceLastRead = currentTime - lastReadTimestamp
        return timeSinceLastRead < DEBOUNCE_INTERVAL_MS
    }

    /**
     * Determine the type of card from tag technologies
     */
    private fun determineCardType(tag: Tag): String {
        val techList = tag.techList

        return when {
            techList.contains(MifareClassic::class.java.name) -> {
                val mifareClassic = MifareClassic.get(tag)
                when (mifareClassic?.type) {
                    MifareClassic.TYPE_CLASSIC -> {
                        when (mifareClassic.size) {
                            MifareClassic.SIZE_1K -> "Mifare Classic 1K"
                            MifareClassic.SIZE_2K -> "Mifare Classic 2K"
                            MifareClassic.SIZE_4K -> "Mifare Classic 4K"
                            else -> "Mifare Classic"
                        }
                    }
                    MifareClassic.TYPE_PLUS -> "Mifare Plus"
                    MifareClassic.TYPE_PRO -> "Mifare Pro"
                    else -> "Mifare Classic"
                }
            }
            techList.contains(android.nfc.tech.MifareUltralight::class.java.name) -> {
                "Mifare Ultralight"
            }
            techList.contains(android.nfc.tech.NfcA::class.java.name) -> {
                "NFC-A Tag"
            }
            techList.contains(android.nfc.tech.NfcB::class.java.name) -> {
                "NFC-B Tag"
            }
            else -> "Unknown NFC Tag"
        }
    }

    /**
     * Vibrate device on successful card read
     */
    private fun vibrateOnSuccess() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            VIBRATION_DURATION_MS,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(VIBRATION_DURATION_MS)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating", e)
        }
    }

    /**
     * Check if NFC is available
     */
    fun isNfcAvailable(): Boolean {
        return nfcAdapter != null
    }

    /**
     * Check if NFC is enabled
     */
    fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }

    /**
     * Get NFC adapter
     */
    fun getNfcAdapter(): NfcAdapter? {
        return nfcAdapter
    }

    /**
     * Set callback for when a card is detected
     */
    fun setOnCardDetectedListener(listener: (uid: String, type: String, timestamp: Long) -> Unit) {
        this.onCardDetected = listener
    }

    /**
     * Set callback for when NFC is not supported
     */
    fun setOnNfcNotSupportedListener(listener: () -> Unit) {
        this.onNfcNotSupported = listener
    }

    /**
     * Set callback for when NFC is disabled
     */
    fun setOnNfcDisabledListener(listener: () -> Unit) {
        this.onNfcDisabled = listener
    }

    /**
     * Clear debounce state (useful when clearing the list)
     */
    fun clearDebounceState() {
        lastReadUid = null
        lastReadTimestamp = 0
        Log.d(TAG, "Debounce state cleared")
    }
}
