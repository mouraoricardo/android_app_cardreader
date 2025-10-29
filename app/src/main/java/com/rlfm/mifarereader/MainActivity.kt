package com.rlfm.mifarereader

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rlfm.mifarereader.databinding.ActivityMainBinding
import com.rlfm.mifarereader.utils.CsvExporter
import com.rlfm.mifarereader.utils.MifareCardData
import com.rlfm.mifarereader.utils.MifareClassicReader
import kotlinx.coroutines.launch

/**
 * Main Activity for NFC Card Reading
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var techLists: Array<Array<String>>

    private val mifareReader = MifareClassicReader()
    private lateinit var csvExporter: CsvExporter

    private var currentCardData: MifareCardData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        csvExporter = CsvExporter(this)

        setupNfc()
        setupUI()
    }

    /**
     * Setup NFC adapter and intent filters
     */
    private fun setupNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            showNfcNotSupported()
            return
        }

        // Create pending intent for NFC
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
        )

        // Setup intent filters for NFC discovery
        intentFilters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )

        // Setup tech lists for Mifare Classic
        techLists = arrayOf(
            arrayOf(MifareClassic::class.java.name),
            arrayOf(android.nfc.tech.NfcA::class.java.name)
        )
    }

    /**
     * Setup UI components and listeners
     */
    private fun setupUI() {
        binding.btnExportCsv.setOnClickListener {
            exportCurrentCard()
        }

        updateNfcStatus()
    }

    /**
     * Update NFC status message
     */
    private fun updateNfcStatus() {
        when {
            nfcAdapter == null -> {
                binding.tvNfcStatus.text = getString(R.string.nfc_status)
                binding.tvNfcMessage.text = getString(R.string.nfc_not_supported)
            }
            !nfcAdapter!!.isEnabled -> {
                binding.tvNfcStatus.text = getString(R.string.nfc_status)
                binding.tvNfcMessage.text = getString(R.string.nfc_disabled)
                showEnableNfcDialog()
            }
            else -> {
                binding.tvNfcStatus.text = getString(R.string.nfc_status)
                binding.tvNfcMessage.text = getString(R.string.waiting_for_card)
            }
        }
    }

    /**
     * Show dialog when NFC is not supported
     */
    private fun showNfcNotSupported() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.nfc_not_supported))
            .setMessage(getString(R.string.nfc_not_supported))
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Show dialog to enable NFC
     */
    private fun showEnableNfcDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.nfc_disabled))
            .setMessage(getString(R.string.nfc_disabled))
            .setPositiveButton(getString(R.string.open_nfc_settings)) { _, _ ->
                startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateNfcStatus()

        // Enable foreground dispatch for NFC
        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            intentFilters,
            techLists
        )
    }

    override fun onPause() {
        super.onPause()

        // Disable foreground dispatch
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action
        ) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let {
                handleNfcTag(it)
            }
        }
    }

    /**
     * Handle NFC tag detection
     */
    private fun handleNfcTag(tag: Tag) {
        binding.tvNfcMessage.text = getString(R.string.reading_card)

        lifecycleScope.launch {
            val result = mifareReader.readCard(tag)

            result.onSuccess { cardData ->
                currentCardData = cardData
                displayCardData(cardData)
                binding.tvNfcMessage.text = getString(R.string.card_read_success)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.card_read_success),
                    Toast.LENGTH_SHORT
                ).show()
            }

            result.onFailure { error ->
                binding.tvNfcMessage.text = "${getString(R.string.card_read_error)}: ${error.message}"
                Toast.makeText(
                    this@MainActivity,
                    "${getString(R.string.card_read_error)}: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Display card data in UI
     */
    private fun displayCardData(cardData: MifareCardData) {
        binding.cardInfo.visibility = View.VISIBLE
        binding.btnExportCsv.isEnabled = true

        binding.tvCardUid.text = getString(R.string.uid_label, cardData.uid)
        binding.tvCardType.text = getString(R.string.type_label, cardData.type)
        binding.tvCardSize.text = getString(R.string.size_label, cardData.size)
        binding.tvCardSectors.text = getString(R.string.sectors_label, cardData.sectorCount)
        binding.tvRawData.text = cardData.rawData
    }

    /**
     * Export current card data to CSV
     */
    private fun exportCurrentCard() {
        val cardData = currentCardData ?: run {
            Toast.makeText(
                this,
                getString(R.string.no_data_to_export),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {
            val result = csvExporter.exportToCSV(cardData)

            result.onSuccess { filePath ->
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.export_success, filePath),
                    Toast.LENGTH_LONG
                ).show()

                // Show dialog with file location
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(getString(R.string.export_to_csv))
                    .setMessage(getString(R.string.export_success, filePath))
                    .setPositiveButton("OK", null)
                    .show()
            }

            result.onFailure { error ->
                Toast.makeText(
                    this@MainActivity,
                    "${getString(R.string.export_error)}: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
