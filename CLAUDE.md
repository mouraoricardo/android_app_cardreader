# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**RFID Card Reader** - An Android application for reading Mifare Classic RFID/NFC cards.

- **Package**: com.rlfm.mifarereader
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34
- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL
- **UI**: ViewBinding enabled

The app reads Mifare Classic cards via NFC, maintains a list of scanned cards with timestamps, and exports the list to CSV format. Features a modern Material Design 3 interface with RecyclerView-based card list management.

## Build Commands

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install debug build on connected device
./gradlew installDebug

# Clean build artifacts
./gradlew clean
```

## Testing

```bash
# Run all tests
./gradlew test

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Run tests for a specific module
./gradlew :app:test
```

## Code Quality

```bash
# Run lint checks
./gradlew lint

# Run lint and generate HTML report
./gradlew lintDebug

# Check for dependency updates
./gradlew dependencyUpdates
```

## Project Structure

```
app/
├── src/main/
│   ├── java/com/rlfm/mifarereader/
│   │   ├── MainActivity.kt              # Main activity with UI & list management
│   │   ├── NfcReader.kt                 # NFC communication manager (NEW)
│   │   ├── CardEntry.kt                 # Data class for card list items
│   │   ├── CardAdapter.kt               # RecyclerView adapter with DiffUtil
│   │   └── utils/
│   │       ├── MifareClassicReader.kt   # Core NFC reading logic
│   │       ├── NfcUtils.kt              # NFC utility functions
│   │       └── CsvExporter.kt           # CSV export (single card & list)
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml        # Main UI with RecyclerView
│   │   │   └── item_card.xml            # RecyclerView item layout
│   │   ├── drawable/
│   │   │   ├── ic_nfc.xml               # NFC icon (Material Design)
│   │   │   ├── ic_credit_card.xml       # Card icon
│   │   │   ├── ic_badge.xml             # Counter badge icon
│   │   │   ├── ic_save.xml              # Export icon
│   │   │   └── ic_delete.xml            # Clear list icon
│   │   ├── values/
│   │   │   ├── strings.xml              # String resources (Portuguese)
│   │   │   ├── colors.xml               # Modern color palette
│   │   │   └── themes.xml               # Material 3 theme
│   │   └── xml/
│   │       ├── nfc_tech_filter.xml      # NFC tech list filter
│   │       ├── backup_rules.xml         # Backup configuration
│   │       └── data_extraction_rules.xml
│   └── AndroidManifest.xml              # App configuration with NFC setup
└── build.gradle.kts                     # Module dependencies
```

## Architecture

### NFC Integration - NfcReader Class
The `NfcReader` class is a dedicated NFC communication manager that handles all NFC operations:

**Core Features:**
- **Foreground Dispatch Management**: Automatically enables/disables foreground dispatch
- **UID Extraction**: Extracts and converts card UID to hexadecimal format
- **Card Type Detection**: Identifies Mifare Classic (1K/2K/4K), Plus, Pro, Ultralight, and generic NFC tags
- **Callback System**: Event-driven architecture with listeners for card detection, NFC disabled, etc.

**Debounce Logic (2 seconds):**
- Prevents duplicate reads of the same card within 2 seconds
- Tracks last read UID and timestamp
- Can be cleared manually (e.g., when clearing the list)

**Haptic Feedback:**
- Vibrates device for 200ms on successful card read
- Uses modern VibrationEffect API (Android 8.0+) with fallback for older versions
- Supports both Vibrator and VibratorManager (Android 12+)

**Tech List Filters:**
- Mifare Classic (priority)
- NFC-A tags
- NDEF tags

**Intent Handling:**
- `ACTION_TECH_DISCOVERED`
- `ACTION_TAG_DISCOVERED`
- `ACTION_NDEF_DISCOVERED`

### Mifare Classic Reading
The `MifareClassicReader` class handles card authentication and data reading:
- Attempts authentication with multiple default keys (0xFFFFFFFFFFFF, 0xA0A1A2A3A4A5, etc.)
- Tries both Key A and Key B for each sector
- Reads all accessible sectors and blocks
- Gracefully handles authentication failures

### Data Model
- `MifareCardData`: Complete card information (from NFC read)
- `SectorData`: Sector-level data with authentication status
- `BlockData`: Individual block data with hex representation
- `CardEntry`: List item with UID, type, and timestamp for RecyclerView

### CSV Export
The app supports two export modes:
1. **Single Card Export**: Full sector/block data
   - Filename: `card_[UID]_[timestamp].csv`
   - Contains complete sector and block information in hex and ASCII

2. **Card List Export**: Summary of all scanned cards
   - Filename: `card_list_[timestamp].csv`
   - Contains: No., UID, Type, Date & Time for each card
   - Triggered by "Exportar CSV" button

All files saved to: `Documents/RFIDCardReader/` directory
Uses OpenCSV library for CSV writing

### Security Considerations
- NFC permission required in manifest
- VIBRATE permission for haptic feedback
- Default Mifare keys are industry-standard (not secret)
- Raw card data is displayed and exported - ensure proper handling
- No network transmission of card data
- Backup rules exclude sensitive shared preferences
- Debounce mechanism prevents rapid duplicate reads

## Key Dependencies

```kotlin
// Core Android
androidx.core:core-ktx:1.12.0
androidx.appcompat:appcompat:1.6.1
com.google.android.material:material:1.11.0
androidx.constraintlayout:constraintlayout:2.1.4

// Lifecycle & ViewModel
androidx.lifecycle:lifecycle-runtime-ktx:2.7.0
androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0

// Coroutines (for async NFC operations)
kotlinx-coroutines-android:1.7.3

// CSV handling
com.opencsv:opencsv:5.9
```

## Development Notes

### Testing NFC
- **Must use physical device** - Android emulators do not support NFC hardware
- Requires actual Mifare Classic cards for testing
- Test with different card types (1K, 4K) and manufacturers
- Some sectors may be locked with non-default keys

### Common Mifare Classic Keys
The app tries these default keys for authentication:
- `FF FF FF FF FF FF` (factory default)
- `A0 A1 A2 A3 A4 A5` (common alternative)
- `D3 F7 D3 F7 D3 F7` (MAD key)
- `00 00 00 00 00 00` (null key)

To add custom keys, edit `MifareClassicReader.kt:DEFAULT_KEYS`

### NFC Best Practices
- Keep card steady during reading (typically 1-2 seconds)
- MainActivity uses `launchMode="singleTop"` to handle NFC intents
- Foreground dispatch ensures app receives NFC events when in foreground
- Connection is opened/closed properly to avoid resource leaks

### Debounce Implementation
The 2-second debounce prevents duplicate reads:
```kotlin
private var lastReadUid: String? = null
private var lastReadTimestamp: Long = 0

fun shouldDebounce(uid: String, currentTime: Long): Boolean {
    if (lastReadUid != uid) return false
    val timeSinceLastRead = currentTime - lastReadTimestamp
    return timeSinceLastRead < 2000L // 2 seconds
}
```

**Behavior:**
- Same card re-read within 2 seconds: **Ignored**
- Different card: **Accepted immediately**
- Same card after 2 seconds: **Accepted**
- Cleared when list is cleared via `clearDebounceState()`

**Benefits:**
- Prevents accidental duplicate entries
- Allows intentional re-reading after short wait
- Tracks per-card, not globally

### UI/UX - Modern Interface
The app features a modern Material Design 3 interface with:

**Main Screen Layout:**
1. **NFC Prompt Card** (top)
   - Large NFC icon (64dp)
   - "Aproxime o cartão Mifare" message
   - Real-time NFC status updates
   - Blue primary color background

2. **Counter Card**
   - Badge icon with counter
   - "Cartões lidos: X" dynamic count

3. **RecyclerView Card List**
   - Shows all scanned cards in chronological order (newest first)
   - Each item displays: UID (monospace), card type, timestamp
   - Auto-scrolls to top when new card added
   - Empty state with icon and "Nenhum cartão lido ainda" message

4. **Bottom Action Buttons**
   - "Limpar Lista" (outlined) - Clears list with confirmation dialog
   - "Exportar CSV" (filled) - Exports entire list to CSV
   - Both buttons disabled when list is empty

**RecyclerView Implementation:**
- Uses `CardAdapter` with `DiffUtil` for efficient updates
- `LinearLayoutManager` for vertical scrolling
- Item animations handled automatically
- ViewBinding for type-safe view access

**Color Scheme:**
- Primary: Modern Blue (#2196F3)
- Secondary: Teal Accent (#00BCD4)
- Background: Light Gray (#F5F7FA)
- Surface: White (#FFFFFF)

**Icons:**
- All Material Design vector drawables
- NFC, credit card, badge, save, delete icons included
- Consistent 24dp size with proper tinting

**Portuguese Localization:**
- All strings in Portuguese (Portugal)
- Proper plurals and formatting
- Confirmation dialogs with clear messaging

**Visual Feedback:**
- Snackbar notifications for all actions:
  - Card detected with UID
  - List cleared
  - CSV exported
  - Errors
- Snackbars anchored to bottom button container
- Action buttons in snackbars (e.g., "Ver" to scroll to card, "Detalhes" for export path)

**Haptic Feedback:**
- Short vibration (200ms) on successful card read
- Provides tactile confirmation without visual distraction

### File Locations
- CSV exports: `/storage/emulated/0/Android/data/com.rlfm.mifarereader/files/Documents/RFIDCardReader/`
- On Android 10+, files are scoped to app directory (no permission needed)
- On Android 9-, uses legacy external storage (requires WRITE_EXTERNAL_STORAGE)

## Key Features

### Card List Management
- **Persistent Session List**: Cards remain in list until cleared
- **No Duplicates Filtering**: Each scan adds a new entry (allows tracking multiple reads of same card)
- **Chronological Order**: Newest cards appear at top
- **Counter**: Real-time count of scanned cards
- **Clear All**: Button to clear entire list with confirmation dialog

### User Workflow
1. App opens → Shows NFC prompt and empty list
2. User taps card → Card reads and appears at top of list
3. Counter updates automatically
4. Repeat for multiple cards
5. Export entire list to CSV when ready
6. Clear list to start fresh session

### Data Flow

**Quick Read Flow (UID Only):**
```
NFC Tag → NfcReader → Extract UID → Debounce Check → Vibrate → CardEntry → RecyclerView
                                                                    ↓
                                                              Snackbar feedback
```

**Detailed Read Flow (Full Card Data):**
```
NFC Tag → MifareClassicReader → MifareCardData → CardEntry → RecyclerView
                                                              ↓
                                                    CsvExporter (list export)
```

**NfcReader Responsibilities:**
- Intent handling and tag extraction
- UID to hex conversion
- Card type identification
- Debounce logic
- Vibration feedback
- Callback notifications

**MainActivity Responsibilities:**
- UI updates
- List management
- Snackbar notifications
- CSV export coordination

## License

This project is licensed under the Apache License 2.0.
