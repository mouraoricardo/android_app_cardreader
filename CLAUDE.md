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
│   │   ├── NfcReader.kt                 # NFC communication manager
│   │   ├── CardEntry.kt                 # Room Entity for card data
│   │   ├── CardAdapter.kt               # RecyclerView adapter with DiffUtil
│   │   ├── data/
│   │   │   ├── CardDao.kt               # Room DAO for database operations
│   │   │   ├── CardDatabase.kt          # Room Database singleton
│   │   │   └── CardRepository.kt        # Repository pattern for data access
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

### Data Persistence - Room Database
The app uses Room Database for persistent storage of card data across app sessions:

**Architecture Components:**

1. **CardEntry (Entity)**
   - Room `@Entity` with table name "cards"
   - Fields: `id` (auto-generated primary key), `uid`, `type`, `timestamp`
   - Includes formatting methods: `getFormattedTime()`, `getFormattedDate()`

2. **CardDao (Data Access Object)**
   - Interface defining database operations
   - `insertCard()`: Add new card (suspend function)
   - `getAllCards()`: Returns `Flow<List<CardEntry>>` for reactive updates
   - `getAllCardsList()`: One-time query for CSV export
   - `deleteAllCards()`: Clear entire database
   - `deleteCard()`: Remove specific card
   - `getCardCount()`: Returns `Flow<Int>` for counter
   - All queries ordered by timestamp DESC (newest first)

3. **CardDatabase**
   - Room Database singleton
   - Version 1 with `fallbackToDestructiveMigration()`
   - Database name: "card_database"
   - Thread-safe instance using `synchronized` block

4. **CardRepository**
   - Repository pattern for clean data access
   - Exposes `allCards` Flow for reactive UI updates
   - Exposes `cardCount` Flow for counter updates
   - Provides convenience method: `insertCard(uid, type)` with auto-generated ID
   - All database operations are suspend functions

**Data Persistence Flow:**
```
Card Scanned → CardRepository.insertCard() → CardDao → Room Database → Flow Updates → UI Auto-Updates
                                                                           ↓
                                                                     RecyclerView refreshes
```

**Key Benefits:**
- **Automatic Persistence**: All cards saved to SQLite database
- **Survives App Restart**: Data persists across sessions
- **Reactive Updates**: UI automatically refreshes when data changes via Flow
- **Thread-Safe**: All database operations on background threads via coroutines
- **Type-Safe**: Room compile-time verification of SQL queries

**Database Location:**
- `/data/data/com.rlfm.mifarereader/databases/card_database`
- Automatically managed by Room
- Scoped to app (cleared on uninstall)

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
- `CardEntry`: Room Entity (database table) with auto-generated ID, UID, type, and timestamp
  - Used for persistent storage and RecyclerView display
  - Automatically synchronized between database and UI via Flow

### CSV Export
The app exports scanned cards to CSV format compatible with Excel.

**Export Details:**
- **Filename**: `mifare_cards_[timestamp].csv` (e.g., `mifare_cards_20250129_143022.csv`)
- **Location**: Downloads folder (accessible via Files app)
- **Format**: CSV with comma separator, UTF-8 encoding
- **Columns**:
  - `UID` - Card unique identifier (e.g., "04:A3:B2:C1:D4:E5:F6")
  - `Data/Hora` - Scan date and time (format: "dd/MM/yyyy HH:mm:ss")
- **Triggered by**: "Exportar CSV" button in main screen

**Platform-Specific Implementation:**

1. **Android 10+ (API 29+)**: Uses MediaStore API
   - No runtime permissions needed
   - Files saved to public Downloads folder
   - Accessible from any file manager
   - Path: `/storage/emulated/0/Download/mifare_cards_[timestamp].csv`

2. **Android 6-9 (API 23-28)**: Uses legacy external storage
   - Requires `WRITE_EXTERNAL_STORAGE` runtime permission
   - Permission requested automatically on first export
   - User can grant/deny permission
   - If denied, Snackbar shows with link to app settings

3. **Android 5 and below (API < 23)**: Uses legacy external storage
   - No runtime permissions (granted at install time)
   - Manifest permission: `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion="28"`

**Error Handling:**
- Empty list check before export
- IOException handling with user-friendly error messages
- Permission denial handling with settings redirect
- MediaStore insert failure handling

**CSV Format Example:**
```csv
UID,Data/Hora
04:A3:B2:C1:D4:E5:F6,29/01/2025 14:30:22
08:B4:C3:D2:E1:F0:A7,29/01/2025 14:31:15
0C:C5:D4:E3:F2:A1:B8,29/01/2025 14:32:08
```

**File Sharing:**
After export, the app automatically shows Android's share dialog allowing users to:
- **Email**: Send CSV as attachment via Gmail, Outlook, etc.
- **Messaging**: Share via WhatsApp, Telegram, Signal, etc.
- **Bluetooth**: Transfer to nearby devices
- **Cloud Storage**: Upload to Google Drive, Dropbox, OneDrive
- **Other Apps**: Any app that accepts CSV/text files

**Share Implementation:**
- Uses `FileProvider` for secure file sharing
- Android 10+: Shares MediaStore URI directly (no FileProvider needed)
- Android 9-: Uses FileProvider to create shareable URI
- Share intent includes:
  - `EXTRA_STREAM`: File URI
  - `EXTRA_SUBJECT`: "Exportação de Cartões RFID"
  - `EXTRA_TEXT`: Filename for context
  - `FLAG_GRANT_READ_URI_PERMISSION`: Temporary read access
- Shows share dialog automatically after export (500ms delay)
- Snackbar with "Partilhar" action button for manual sharing

**FileProvider Configuration:**
- Authority: `com.rlfm.mifarereader.fileprovider`
- Paths configured in `res/xml/file_paths.xml`:
  - `external_downloads`: Public Downloads folder
  - `external_files`: App external files directory
  - `cache`: Cache directory for temporary files

**Technical Details:**
- Uses OpenCSV library for CSV writing
- UTF-8 encoding for international characters
- Proper CSV escaping for special characters
- Excel-compatible format (RFC 4180 compliant)
- Secure file sharing via FileProvider (prevents file:// URI vulnerabilities)

### Security Considerations
- **NFC permission** required in manifest
- **VIBRATE permission** for haptic feedback
- **WRITE_EXTERNAL_STORAGE** (Android 6-9 only, `maxSdkVersion="28"`):
  - Requested at runtime when exporting CSV
  - Not needed for Android 10+ (uses MediaStore)
  - User can deny permission (graceful handling)
- **READ_EXTERNAL_STORAGE** (`maxSdkVersion="32"`) - declared but not actively used
- Default Mifare keys are industry-standard (not secret)
- Raw card data is displayed and exported - ensure proper handling
- **CSV files saved to public Downloads folder** - accessible by other apps
- No network transmission of card data
- Backup rules exclude sensitive shared preferences
- Debounce mechanism prevents rapid duplicate reads
- **Scoped Storage compliance** for Android 10+ (API 29+)

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

// Coroutines (for async operations)
kotlinx-coroutines-android:1.7.3

// Room Database (for persistent storage)
androidx.room:room-runtime:2.6.1
androidx.room:room-ktx:2.6.1
ksp: androidx.room:room-compiler:2.6.1

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
- **Smooth slide-in animations** when adding new cards
- ViewBinding for type-safe view access
- Wrapped in `SwipeRefreshLayout` for pull-to-refresh gesture

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

**Animations & UX Enhancements:**

1. **RecyclerView Item Animations**
   - Smooth slide-in-right animation (300ms) when cards are added
   - Fade-in effect combined with translation
   - Animation defined in `res/anim/slide_in_right.xml`
   - Prevents re-animation on scroll with `lastPosition` tracking

2. **NFC Icon Pulse Animation**
   - Continuous pulse animation on NFC icon when waiting for cards
   - Scale + alpha animation (800ms cycle)
   - Indicates app is ready and actively listening
   - Animation stops when app goes to background
   - Restarts automatically on resume
   - Defined in `res/anim/pulse.xml`

3. **SwipeRefreshLayout**
   - Pull-to-refresh gesture for visual feedback
   - Uses theme colors (primary, secondary, primary_light)
   - 800ms refresh duration
   - Data updates automatically from database via Flow
   - Provides tactile feedback that app is responsive

4. **Confirmation Dialogs**
   - Clear list: Shows confirmation before deleting all cards
   - NFC disabled: Offers direct link to NFC settings
   - NFC not supported: Clear message with app exit option

5. **Dark Mode Support**
   - Automatic dark theme based on system settings
   - Custom colors for dark mode in `values-night/colors.xml`
   - Dark background (#121212) with elevated surfaces (#1E1E1E)
   - High contrast text colors for readability
   - Theme switches automatically when system dark mode changes

### File Locations

**CSV Exports:**
- **Android 10+** (API 29+): `/storage/emulated/0/Download/mifare_cards_[timestamp].csv`
  - Uses MediaStore API for Downloads folder
  - Files are in public storage (accessible by other apps)
  - No permissions needed

- **Android 6-9** (API 23-28): `/storage/emulated/0/Download/mifare_cards_[timestamp].csv`
  - Uses legacy Environment.getExternalStoragePublicDirectory()
  - Requires WRITE_EXTERNAL_STORAGE runtime permission
  - Files in public Downloads folder

- **Android 5 and below** (API < 23): Same as Android 6-9
  - No runtime permissions (granted at install)

**Room Database:**
- `/data/data/com.rlfm.mifarereader/databases/card_database`
- Private app storage (cleared on uninstall)
- Automatically managed by Room

## Key Features

### Card List Management
- **Persistent Storage**: Cards saved to Room Database, survive app restarts
- **Reactive UI**: Automatic UI updates via Flow when data changes
- **No Duplicates Filtering**: Each scan adds a new entry (allows tracking multiple reads of same card)
- **Chronological Order**: Newest cards appear at top (sorted by timestamp DESC)
- **Counter**: Real-time count of scanned cards
- **Clear All**: Button to clear entire database with confirmation dialog
- **Unique IDs**: Auto-generated primary keys for each card entry

### User Workflow
1. App opens → Loads previously scanned cards from database (if any)
2. User taps card → Card saved to database and appears at top of list
3. Counter updates automatically via Flow
4. Repeat for multiple cards
5. Close app → All data persists in database
6. Reopen app → Previously scanned cards are restored
7. Export entire list to CSV when ready
8. Clear list to delete all cards from database

### Data Flow

**Card Reading with Persistence:**
```
NFC Tag → NfcReader → Extract UID → Debounce Check → Vibrate → Callback
                                                                    ↓
MainActivity.onCardDetected() → CardRepository.insertCard() → Room Database
                                                                    ↓
                                              Flow<List<CardEntry>> emits update
                                                                    ↓
                                              MainActivity.observeCards() collects
                                                                    ↓
                                              RecyclerView auto-updates + Snackbar
```

**App Startup Flow:**
```
App Launch → MainActivity.onCreate() → CardRepository initialization
                                             ↓
                                    observeCards() starts collecting
                                             ↓
                          Flow emits existing cards from database
                                             ↓
                                    UI displays saved cards
```

**Export Flow:**
```
User clicks Export → CardRepository.getAllCardsList() → One-time query
                                                              ↓
                                                      CsvExporter writes file
                                                              ↓
                                                      Snackbar with file path
```

**Clear List Flow:**
```
User clicks Clear (with confirmation) → CardRepository.deleteAllCards()
                                                    ↓
                                          Room deletes all rows
                                                    ↓
                                   Flow emits empty list automatically
                                                    ↓
                                          UI shows empty state
```

**Component Responsibilities:**

**NfcReader:**
- Intent handling and tag extraction
- UID to hex conversion
- Card type identification
- Debounce logic (2 seconds)
- Vibration feedback
- Callback notifications

**CardRepository:**
- Database access abstraction
- Exposes reactive Flows
- Manages CRUD operations
- Thread-safe database calls

**MainActivity:**
- UI updates via Flow collection
- Lifecycle-aware coroutines
- User interaction handling
- Snackbar notifications
- Coordinates repository and UI

## License

This project is licensed under the Apache License 2.0.
