package com.rlfm.mifarereader

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.rlfm.mifarereader.databinding.ActivityMainBinding
import com.rlfm.mifarereader.utils.CsvExporter
import com.rlfm.mifarereader.utils.MifareCardData
import com.rlfm.mifarereader.utils.MifareClassicReader
import kotlinx.coroutines.launch

/**
 * Main Activity for NFC Card Reading with card list management
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private lateinit var techLists: Array<Array<String>>

    private val mifareReader = MifareClassicReader()
    private lateinit var csvExporter: CsvExporter

    private val cardAdapter = CardAdapter()
    private val cardList = mutableListOf<CardEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        csvExporter = CsvExporter(this)

        setupNfc()
        setupRecyclerView()
        setupUI()
        updateUI()
    }

    /**
     * Setup RecyclerView
     */
    private fun setupRecyclerView() {
        binding.recyclerViewCards.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = cardAdapter
        }
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
            exportToCSV()
        }

        binding.btnClearList.setOnClickListener {
            showClearListDialog()
        }

        updateNfcStatus()
    }

    /**
     * Update UI based on current state
     */
    private fun updateUI() {
        val hasCards = cardList.isNotEmpty()

        // Update counter
        binding.tvCounter.text = getString(R.string.cards_read_count, cardList.size)

        // Update empty state
        binding.emptyState.visibility = if (hasCards) View.GONE else View.VISIBLE
        binding.recyclerViewCards.visibility = if (hasCards) View.VISIBLE else View.GONE

        // Update buttons
        binding.btnExportCsv.isEnabled = hasCards
        binding.btnClearList.isEnabled = hasCards

        // Update adapter
        cardAdapter.submitList(cardList.toList())
    }

    /**
     * Update NFC status message
     */
    private fun updateNfcStatus() {
        when {
            nfcAdapter == null -> {
                binding.tvNfcPrompt.text = getString(R.string.nfc_not_supported)
                binding.tvNfcStatus.text = ""
            }
            !nfcAdapter!!.isEnabled -> {
                binding.tvNfcPrompt.text = getString(R.string.nfc_disabled)
                binding.tvNfcStatus.text = getString(R.string.open_nfc_settings)
                showEnableNfcDialog()
            }
            else -> {
                binding.tvNfcPrompt.text = getString(R.string.approach_mifare_card)
                binding.tvNfcStatus.text = getString(R.string.nfc_ready)
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
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /**
     * Show confirmation dialog before clearing list
     */
    private fun showClearListDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_list_title))
            .setMessage(getString(R.string.clear_list_message))
            .setPositiveButton(getString(R.string.clear)) { _, _ ->
                clearList()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /**
     * Clear the card list
     */
    private fun clearList() {
        cardList.clear()
        updateUI()
        Toast.makeText(this, getString(R.string.clear_list), Toast.LENGTH_SHORT).show()
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
            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            tag?.let {
                handleNfcTag(it)
            }
        }
    }

    /**
     * Handle NFC tag detection
     */
    private fun handleNfcTag(tag: Tag) {
        binding.tvNfcStatus.text = getString(R.string.reading_card)

        lifecycleScope.launch {
            val result = mifareReader.readCard(tag)

            result.onSuccess { cardData ->
                addCardToList(cardData)
                binding.tvNfcStatus.text = getString(R.string.card_read_success)
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.card_read_success),
                    Toast.LENGTH_SHORT
                ).show()
            }

            result.onFailure { error ->
                binding.tvNfcStatus.text = getString(R.string.card_read_error_with_message, error.message ?: "Unknown error")
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.card_read_error_with_message, error.message ?: "Unknown error"),
                    Toast.LENGTH_LONG
                ).show()
            }

            // Reset status after 3 seconds
            binding.tvNfcStatus.postDelayed({
                binding.tvNfcStatus.text = getString(R.string.nfc_ready)
            }, 3000)
        }
    }

    /**
     * Add card to the list
     */
    private fun addCardToList(cardData: MifareCardData) {
        val cardEntry = CardEntry(
            uid = cardData.uid,
            type = cardData.type,
            timestamp = System.currentTimeMillis()
        )

        // Add to beginning of list
        cardList.add(0, cardEntry)
        updateUI()

        // Scroll to top
        binding.recyclerViewCards.smoothScrollToPosition(0)
    }

    /**
     * Export all cards to CSV
     */
    private fun exportToCSV() {
        if (cardList.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.no_data_to_export),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {
            val result = csvExporter.exportCardList(cardList)

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
                    getString(R.string.export_error_with_message, error.message ?: "Unknown error"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
