# RFID Card Reader

Uma aplicação Android para leitura de cartões Mifare Classic via NFC.

## Características

- ✅ Leitura de cartões Mifare Classic (1K, 4K)
- ✅ Exibição de informações do cartão (UID, tipo, tamanho, sectores)
- ✅ Visualização de dados brutos em hexadecimal
- ✅ Exportação para CSV
- ✅ Interface em Material Design 3
- ✅ Suporte para múltiplas chaves de autenticação padrão

## Requisitos

- Android 5.0 (API 21) ou superior
- Dispositivo com suporte NFC
- NFC ativado nas configurações do dispositivo

## Como Usar

1. Abra a aplicação
2. Certifique-se de que o NFC está ativado
3. Aproxime um cartão Mifare Classic do leitor NFC do dispositivo
4. A aplicação irá ler automaticamente os dados do cartão
5. Use o botão "Exportar para CSV" para guardar os dados

## Estrutura do Projeto

```
app/
├── src/main/
│   ├── java/com/rlfm/mifarereader/
│   │   ├── MainActivity.kt              # Atividade principal
│   │   └── utils/
│   │       ├── MifareClassicReader.kt   # Lógica de leitura NFC
│   │       ├── NfcUtils.kt              # Utilitários NFC
│   │       └── CsvExporter.kt           # Exportação CSV
│   └── res/
│       ├── layout/                       # Layouts XML
│       ├── values/                       # Recursos (strings, cores, temas)
│       └── xml/                          # Configurações NFC
```

## Tecnologias

- **Linguagem**: Kotlin
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34
- **Build System**: Gradle com Kotlin DSL
- **UI**: ViewBinding + Material Design 3
- **Async**: Kotlin Coroutines
- **CSV**: OpenCSV

## Build

```bash
# Build debug APK
./gradlew assembleDebug

# Instalar no dispositivo conectado
./gradlew installDebug

# Run tests
./gradlew test
```

## Configuração de Ícones

⚠️ **Nota**: Os ícones do launcher estão configurados como vetores XML. Para produção, considere:
1. Usar Android Studio → Image Asset para gerar ícones PNG
2. Criar ícones personalizados para todas as densidades (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)

## Chaves Mifare Padrão

A aplicação tenta autenticar com as seguintes chaves:
- `FF FF FF FF FF FF` (padrão de fábrica)
- `A0 A1 A2 A3 A4 A5` (alternativa comum)
- `D3 F7 D3 F7 D3 F7` (chave MAD)
- `00 00 00 00 00 00` (chave nula)

Para adicionar chaves personalizadas, edite `MifareClassicReader.kt`.

## Exportação de Dados

Os ficheiros CSV são guardados em:
```
/storage/emulated/0/Android/data/com.rlfm.mifarereader/files/Documents/RFIDCardReader/
```

Formato do nome: `card_[UID]_[timestamp].csv`

## Licença

Este projeto está licenciado sob a Apache License 2.0 - veja o ficheiro [LICENSE](LICENSE) para detalhes.

## Desenvolvimento

Consulte o ficheiro [CLAUDE.md](CLAUDE.md) para informações detalhadas sobre arquitetura e desenvolvimento.
