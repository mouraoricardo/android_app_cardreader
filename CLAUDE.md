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

The app reads Mifare Classic cards via NFC, displays card information (UID, type, sectors, raw data), and exports data to CSV format.

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
│   │   ├── MainActivity.kt              # Main activity with NFC handling
│   │   └── utils/
│   │       ├── MifareClassicReader.kt   # Core NFC reading logic
│   │       ├── NfcUtils.kt              # NFC utility functions
│   │       └── CsvExporter.kt           # CSV export functionality
│   ├── res/
│   │   ├── layout/
│   │   │   └── activity_main.xml        # Main UI layout
│   │   ├── values/
│   │   │   ├── strings.xml              # String resources (Portuguese)
│   │   │   ├── colors.xml               # Color definitions
│   │   │   └── themes.xml               # Material 3 theme
│   │   └── xml/
│   │       ├── nfc_tech_filter.xml      # NFC tech list filter
│   │       ├── backup_rules.xml         # Backup configuration
│   │       └── data_extraction_rules.xml
│   └── AndroidManifest.xml              # App configuration with NFC setup
└── build.gradle.kts                     # Module dependencies
```

## Architecture

### NFC Integration
- Uses Android's NFC API for Mifare Classic card reading
- Implements foreground dispatch system for NFC tag discovery
- Supports `ACTION_TECH_DISCOVERED` and `ACTION_TAG_DISCOVERED` intents
- Tech list filters configured for NfcA and MifareClassic technologies

### Mifare Classic Reading
The `MifareClassicReader` class handles card authentication and data reading:
- Attempts authentication with multiple default keys (0xFFFFFFFFFFFF, 0xA0A1A2A3A4A5, etc.)
- Tries both Key A and Key B for each sector
- Reads all accessible sectors and blocks
- Gracefully handles authentication failures

### Data Model
- `MifareCardData`: Complete card information
- `SectorData`: Sector-level data with authentication status
- `BlockData`: Individual block data with hex representation

### CSV Export
- Exports to `Documents/RFIDCardReader/` directory
- Filename format: `card_[UID]_[timestamp].csv`
- Includes card metadata, sector/block data in hex and ASCII
- Uses OpenCSV library for CSV writing

### Security Considerations
- NFC permission required in manifest
- Default Mifare keys are industry-standard (not secret)
- Raw card data is displayed and exported - ensure proper handling
- No network transmission of card data
- Backup rules exclude sensitive shared preferences

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

### UI/UX
- Material 3 Design with MaterialCardView components
- Portuguese string resources
- Displays real-time NFC status
- Shows authentication failures per sector in logs
- Export button enabled only after successful card read

### File Locations
- CSV exports: `/storage/emulated/0/Android/data/com.rlfm.mifarereader/files/Documents/RFIDCardReader/`
- On Android 10+, files are scoped to app directory (no permission needed)
- On Android 9-, uses legacy external storage (requires WRITE_EXTERNAL_STORAGE)

## License

This project is licensed under the Apache License 2.0.
