package com.rlfm.mifarereader

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.rlfm.mifarereader.databinding.ActivityMainBinding
import com.rlfm.mifarereader.utils.CsvExporter
import kotlinx.coroutines.launch

/**
 * Main Activity for NFC Card Reading with card list management
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var nfcReader: NfcReader
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
     * Setup NFC reader
     */
    private fun setupNfc() {
        nfcReader = NfcReader(this)

        // Set callbacks
        nfcReader.setOnCardDetectedListener { uid, type, timestamp ->
            onCardDetected(uid, type, timestamp)
        }

        nfcReader.setOnNfcNotSupportedListener {
            showNfcNotSupported()
        }

        nfcReader.setOnNfcDisabledListener {
            updateNfcStatus()
            showEnableNfcDialog()
        }

        // Initialize NFC
        if (!nfcReader.initialize()) {
            return
        }
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
     * Handle card detection from NFC reader
     */
    private fun onCardDetected(uid: String, type: String, timestamp: Long) {
        // Add card to list
        val cardEntry = CardEntry(
            uid = uid,
            type = type,
            timestamp = timestamp
        )

        cardList.add(0, cardEntry)
        updateUI()

        // Update status
        binding.tvNfcStatus.text = getString(R.string.card_read_success)

        // Show snackbar with card info
        showCardDetectedSnackbar(uid, type)

        // Scroll to top
        binding.recyclerViewCards.smoothScrollToPosition(0)

        // Reset status after 3 seconds
        binding.tvNfcStatus.postDelayed({
            binding.tvNfcStatus.text = getString(R.string.nfc_ready)
        }, 3000)
    }

    /**
     * Show snackbar when card is detected
     */
    private fun showCardDetectedSnackbar(uid: String, type: String) {
        Snackbar.make(
            binding.root,
            getString(R.string.card_detected_snackbar, uid),
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.view)) {
            // Scroll to top to show the card
            binding.recyclerViewCards.smoothScrollToPosition(0)
        }.setAnchorView(binding.buttonContainer)
            .show()
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
            !nfcReader.isNfcAvailable() -> {
                binding.tvNfcPrompt.text = getString(R.string.nfc_not_supported)
                binding.tvNfcStatus.text = ""
            }
            !nfcReader.isNfcEnabled() -> {
                binding.tvNfcPrompt.text = getString(R.string.nfc_disabled)
                binding.tvNfcStatus.text = getString(R.string.open_nfc_settings)
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

        // Clear NFC reader debounce state
        nfcReader.clearDebounceState()

        Snackbar.make(
            binding.root,
            getString(R.string.list_cleared),
            Snackbar.LENGTH_SHORT
        ).setAnchorView(binding.buttonContainer)
            .show()
    }

    override fun onResume() {
        super.onResume()
        updateNfcStatus()
        nfcReader.enableForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        nfcReader.disableForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        nfcReader.handleIntent(intent)
    }

    /**
     * Export all cards to CSV
     */
    private fun exportToCSV() {
        if (cardList.isEmpty()) {
            Snackbar.make(
                binding.root,
                getString(R.string.no_data_to_export),
                Snackbar.LENGTH_SHORT
            ).setAnchorView(binding.buttonContainer)
                .show()
            return
        }

        lifecycleScope.launch {
            val result = csvExporter.exportCardList(cardList)

            result.onSuccess { filePath ->
                Snackbar.make(
                    binding.root,
                    getString(R.string.export_success_short),
                    Snackbar.LENGTH_LONG
                ).setAction(getString(R.string.details)) {
                    showExportDetailsDialog(filePath)
                }.setAnchorView(binding.buttonContainer)
                    .show()
            }

            result.onFailure { error ->
                Snackbar.make(
                    binding.root,
                    getString(R.string.export_error_with_message, error.message ?: "Unknown error"),
                    Snackbar.LENGTH_LONG
                ).setAnchorView(binding.buttonContainer)
                    .show()
            }
        }
    }

    /**
     * Show export details dialog
     */
    private fun showExportDetailsDialog(filePath: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.export_to_csv))
            .setMessage(getString(R.string.export_success, filePath))
            .setPositiveButton("OK", null)
            .show()
    }
}
