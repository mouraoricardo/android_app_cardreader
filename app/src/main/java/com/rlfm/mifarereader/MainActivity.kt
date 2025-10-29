package com.rlfm.mifarereader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.rlfm.mifarereader.data.CardRepository
import com.rlfm.mifarereader.databinding.ActivityMainBinding
import com.rlfm.mifarereader.utils.CsvExporter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Main Activity for NFC Card Reading with card list management
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var nfcReader: NfcReader
    private lateinit var csvExporter: CsvExporter
    private lateinit var cardRepository: CardRepository

    private val cardAdapter = CardAdapter()
    private var currentCardList = listOf<CardEntry>()

    // Permission launcher for storage (Android 6-9 only)
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, proceed with export
            performCsvExport()
        } else {
            // Permission denied
            showPermissionDeniedMessage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        csvExporter = CsvExporter(this)
        cardRepository = CardRepository(this)

        setupNfc()
        setupRecyclerView()
        setupUI()
        observeCards()
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
     * Observe cards from repository and update UI
     */
    private fun observeCards() {
        lifecycleScope.launch {
            cardRepository.allCards.collectLatest { cards ->
                currentCardList = cards
                updateUI()
            }
        }
    }

    /**
     * Handle card detection from NFC reader
     */
    private fun onCardDetected(uid: String, type: String, timestamp: Long) {
        // Insert card into database
        lifecycleScope.launch {
            cardRepository.insertCard(uid, type)
        }

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
        val hasCards = currentCardList.isNotEmpty()

        // Update counter
        binding.tvCounter.text = getString(R.string.cards_read_count, currentCardList.size)

        // Update empty state
        binding.emptyState.visibility = if (hasCards) View.GONE else View.VISIBLE
        binding.recyclerViewCards.visibility = if (hasCards) View.VISIBLE else View.GONE

        // Update buttons
        binding.btnExportCsv.isEnabled = hasCards
        binding.btnClearList.isEnabled = hasCards

        // Update adapter
        cardAdapter.submitList(currentCardList)
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
        lifecycleScope.launch {
            cardRepository.deleteAllCards()
        }

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
     * Checks storage permissions for Android 6-9 (API 23-28)
     * Android 10+ doesn't need permissions for Downloads via MediaStore
     */
    private fun exportToCSV() {
        if (currentCardList.isEmpty()) {
            Snackbar.make(
                binding.root,
                getString(R.string.no_data_to_export),
                Snackbar.LENGTH_SHORT
            ).setAnchorView(binding.buttonContainer)
                .show()
            return
        }

        // Check if we need storage permission (Android 6-9 only)
        if (needsStoragePermission() && !hasStoragePermission()) {
            requestStoragePermission()
            return
        }

        // Proceed with export
        performCsvExport()
    }

    /**
     * Check if storage permission is needed
     * Only needed for Android 6-9 (API 23-28)
     */
    private fun needsStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT in Build.VERSION_CODES.M until Build.VERSION_CODES.Q
    }

    /**
     * Check if storage permission is granted
     */
    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request storage permission
     */
    private fun requestStoragePermission() {
        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    /**
     * Show message when permission is denied
     */
    private fun showPermissionDeniedMessage() {
        Snackbar.make(
            binding.root,
            getString(R.string.storage_permission_denied),
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.open_nfc_settings)) {
            // Open app settings
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.fromParts("package", packageName, null)
            startActivity(intent)
        }.setAnchorView(binding.buttonContainer)
            .show()
    }

    /**
     * Perform the actual CSV export
     */
    private fun performCsvExport() {
        lifecycleScope.launch {
            val cardList = cardRepository.getAllCardsList()
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
