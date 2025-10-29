# Guia de Início Rápido

## Primeiros Passos

### 1. Abrir o Projeto no Android Studio

```bash
# Navegue até o diretório do projeto
cd /mnt/c/Users/rmourao/StudioProjects/android_app_cardreader

# Abra no Android Studio
# File → Open → Selecione esta pasta
```

### 2. Sincronizar Dependências

Ao abrir o projeto, o Android Studio irá:
- Baixar o Gradle automaticamente
- Sincronizar todas as dependências
- Indexar o código

Aguarde até "Gradle sync" completar (veja a barra de progresso no rodapé).

### 3. Configurar Dispositivo

Você **precisa** de um dispositivo físico com NFC (emuladores não suportam NFC).

#### Opção A: Dispositivo USB
1. Ative "Opções de Programador" no dispositivo
2. Ative "Depuração USB"
3. Conecte via USB
4. Autorize o computador quando solicitado

#### Opção B: Wireless ADB (Android 11+)
1. Dispositivo e PC na mesma rede Wi-Fi
2. Android Studio → Pair Device Using Wi-Fi
3. Siga as instruções na tela

### 4. Verificar NFC

No seu dispositivo:
1. Configurações → Conexões → NFC
2. Certifique-se de que está **ativado**

### 5. Build e Executar

```bash
# Via linha de comando
./gradlew installDebug

# Ou no Android Studio
# Clique no botão "Run" (▶️) ou Shift+F10
```

## Testar a Aplicação

1. Abra a app no dispositivo
2. Aproxime um cartão Mifare Classic do leitor NFC
3. A app irá ler e exibir os dados automaticamente
4. Clique em "Exportar para CSV" para guardar os dados

## Estrutura de Pastas

```
android_app_cardreader/
├── app/
│   ├── build.gradle.kts          # Configuração do módulo
│   └── src/main/
│       ├── AndroidManifest.xml   # Configuração da app
│       ├── java/com/rlfm/mifarereader/
│       │   ├── MainActivity.kt
│       │   └── utils/
│       │       ├── MifareClassicReader.kt
│       │       ├── NfcUtils.kt
│       │       └── CsvExporter.kt
│       └── res/
│           ├── layout/           # Layouts XML
│           ├── values/           # Strings, cores, temas
│           └── xml/              # Configurações NFC
├── build.gradle.kts              # Configuração root
├── settings.gradle.kts           # Módulos do projeto
└── gradle.properties             # Propriedades Gradle
```

## Problemas Comuns

### "NFC não suportado"
- Verifique se o dispositivo tem chip NFC
- Alguns emuladores/tablets não têm NFC

### "NFC desativado"
- Vá em Configurações → NFC
- Ative o NFC

### "Erro ao ler cartão"
- Mantenha o cartão próximo por 1-2 segundos
- Tente diferentes posições do cartão
- Alguns sectores podem estar protegidos com chaves não-padrão

### "Gradle sync failed"
- Verifique sua conexão com a internet
- File → Invalidate Caches → Restart

### Erro de permissão
- As permissões NFC são declaradas no AndroidManifest
- No Android 10+, permissões de armazenamento não são necessárias (usa scoped storage)

## Próximos Passos

1. **Personalizar chaves Mifare**: Edite `MifareClassicReader.kt:DEFAULT_KEYS`
2. **Adicionar ícones personalizados**: Use Image Asset no Android Studio
3. **Testar com diferentes cartões**: Mifare Classic 1K, 4K, etc.
4. **Explorar dados exportados**: Verifique a pasta `Documents/RFIDCardReader/`

## Comandos Úteis

```bash
# Limpar build
./gradlew clean

# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease

# Run tests
./gradlew test

# Lint code
./gradlew lint

# Ver dispositivos conectados
adb devices
```

## Recursos

- [Documentação Android NFC](https://developer.android.com/guide/topics/connectivity/nfc)
- [Mifare Classic Datasheet](https://www.nxp.com/docs/en/data-sheet/MF1S50YYX_V1.pdf)
- [CLAUDE.md](CLAUDE.md) - Arquitetura detalhada
- [README.md](README.md) - Visão geral do projeto
