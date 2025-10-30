# RFID Card Reader

Uma aplicação Android moderna para leitura de cartões Mifare Classic via NFC com persistência de dados, exportação CSV e partilha de ficheiros.

## Características

- 📱 Interface moderna com Material Design 3
- 🔖 Leitura de cartões Mifare Classic (1K, 2K, 4K)
- 💾 Persistência de dados com Room Database
- 📋 Lista de cartões lidos com RecyclerView
- 📊 Exportação para CSV (lista completa ou cartão individual)
- 🔄 Partilha de ficheiros via email, WhatsApp, Bluetooth
- 🎨 Suporte para modo escuro automático
- ✨ Animações suaves e feedback visual
- 🔔 Feedback háptico ao ler cartões
- 🔄 SwipeRefreshLayout para atualizar lista
- ⏱️ Anti-duplicação com debounce de 2 segundos
- 🌐 Interface totalmente em Português

## Requisitos Mínimos

| Requisito | Especificação |
|-----------|--------------|
| **Android** | 5.0 (API 21) ou superior |
| **Hardware NFC** | Obrigatório |
| **RAM** | 1 GB mínimo, 2 GB recomendado |
| **Armazenamento** | 50 MB livres |
| **Permissões** | NFC, Vibração, Armazenamento* |

\* Armazenamento apenas necessário em Android 6-9 para exportação CSV

## Instalação

### Opção 1: Instalar APK (Utilizadores)

1. Descarregue o ficheiro APK da [página de releases](../../releases)
2. Ative "Fontes desconhecidas" nas definições de segurança do Android
3. Abra o ficheiro APK e siga as instruções
4. Conceda as permissões de NFC quando solicitado

### Opção 2: Compilar do Código Fonte (Programadores)

```bash
# Clone o repositório
git clone https://github.com/yourusername/android_app_cardreader.git
cd android_app_cardreader

# Build debug APK
./gradlew assembleDebug

# Instalar no dispositivo conectado
./gradlew installDebug

# Ou build release APK (requer keystore)
./gradlew assembleRelease
```

## Como Usar

### Primeira Utilização

1. **Ativar NFC**
   - Vá a Definições → Ligações → NFC
   - Ative o interruptor NFC
   - A app mostrará uma mensagem se o NFC estiver desativado

2. **Abrir a Aplicação**
   - Verá um ícone NFC animado no topo
   - Mensagem: "Aproxime o cartão Mifare"
   - Contador: "Cartões lidos: 0"

### Ler Cartões

1. **Aproximar o Cartão**
   - Coloque o cartão Mifare na parte traseira do telemóvel
   - Mantenha estável durante 1-2 segundos
   - Sentirá uma vibração breve ao ler com sucesso

2. **Visualizar Informações**
   - O cartão aparece automaticamente no topo da lista
   - Informações mostradas: UID, tipo de cartão, data/hora
   - Contador atualiza automaticamente
   - Snackbar mostra confirmação com botão "Ver"

3. **Anti-duplicação**
   - Ler o mesmo cartão 2 vezes em menos de 2 segundos: ignorado
   - Ler cartões diferentes: aceite imediatamente
   - Ler o mesmo cartão após 2 segundos: aceite

### Exportar e Partilhar

1. **Exportar Lista Completa**
   - Pressione o botão "Exportar CSV"
   - Ficheiro guardado automaticamente na pasta Downloads
   - Diálogo de partilha abre automaticamente

2. **Partilhar Ficheiro**
   - Escolha a app de destino (Gmail, WhatsApp, Drive, etc.)
   - Ficheiro é enviado como anexo
   - Nome do ficheiro: `mifare_cards_[timestamp].csv`

3. **Formato CSV**
   ```csv
   UID,Data/Hora
   04:A1:B2:C3:D4:E5,30/10/2025 14:35:22
   04:F6:E7:D8:C9:BA,30/10/2025 14:36:15
   ```

### Gerir Lista

1. **Atualizar Lista**
   - Deslize para baixo (pull-to-refresh) na lista
   - Feedback visual com animação de carregamento

2. **Limpar Lista**
   - Pressione botão "Limpar Lista"
   - Confirme no diálogo que aparece
   - Todos os cartões são removidos da base de dados
   - Estado de debounce é limpo

### Modo Escuro

- **Ativação Automática**: Segue as definições do sistema
- **Ativar Manualmente**:
  1. Definições → Ecrã → Tema escuro
  2. Ou use o botão de ação rápida nas notificações
- **Cores adaptativas**: Paleta completa para modo claro e escuro

## Permissões

| Permissão | Android | Quando é Pedida | Obrigatória |
|-----------|---------|-----------------|-------------|
| **NFC** | Todos | Instalação | ✅ Sim |
| **VIBRATE** | Todos | Instalação | ⚠️ Opcional* |
| **WRITE_EXTERNAL_STORAGE** | 6.0 - 9.0 | Exportar CSV | ⚠️ Opcional** |

\* Sem vibração, a app funciona mas sem feedback háptico
\*\* Android 10+ não precisa de permissão (usa MediaStore API)

### Detalhes por Versão Android

- **Android 5.0 - 5.1** (API 21-22): NFC + Vibração
- **Android 6.0 - 9.0** (API 23-28): NFC + Vibração + Armazenamento (pedida em runtime)
- **Android 10+** (API 29+): NFC + Vibração (armazenamento scoped, sem permissão)

## Tecnologias Utilizadas

### Core
- **Linguagem**: Kotlin 1.9.24
- **Min SDK**: 21 (Android 5.0 Lollipop)
- **Target SDK**: 34 (Android 14)
- **Build System**: Gradle 8.7.3 com Kotlin DSL
- **JVM Target**: 17

### UI/UX
- **UI Framework**: ViewBinding + Material Design 3
- **RecyclerView**: Listas com DiffUtil para eficiência
- **Animações**: XML animations (slide-in, pulse, rotate)
- **Temas**: Material 3 com suporte a dark mode
- **Componentes**: MaterialCardView, MaterialButton, Snackbar
- **Swipe Refresh**: SwipeRefreshLayout 1.1.0

### Persistência
- **Database**: Room 2.6.1 (SQLite abstraction)
- **Padrão**: Repository Pattern
- **Reatividade**: Kotlin Flow para updates automáticos
- **Processador**: KSP 1.9.24-1.0.20 (Kotlin Symbol Processing)

### Async & Concorrência
- **Coroutines**: kotlinx-coroutines-android 1.7.3
- **Lifecycle**: lifecycle-runtime-ktx 2.7.0
- **ViewModel**: lifecycle-viewmodel-ktx 2.7.0

### Bibliotecas
- **CSV**: OpenCSV 5.9
- **AndroidX Core**: core-ktx 1.12.0
- **AppCompat**: 1.6.1
- **ConstraintLayout**: 2.1.4

## Estrutura do Projeto

```
app/
├── src/main/
│   ├── java/com/rlfm/mifarereader/
│   │   ├── MainActivity.kt              # Atividade principal com UI & gestão de lista
│   │   ├── NfcReader.kt                 # Gestor de comunicação NFC
│   │   ├── CardEntry.kt                 # Data class (Room Entity)
│   │   ├── CardAdapter.kt               # RecyclerView adapter com animações
│   │   ├── data/
│   │   │   ├── CardDao.kt               # Data Access Object (Room)
│   │   │   ├── CardDatabase.kt          # Room Database singleton
│   │   │   └── CardRepository.kt        # Repository pattern
│   │   └── utils/
│   │       ├── MifareClassicReader.kt   # Lógica de leitura NFC detalhada
│   │       ├── NfcUtils.kt              # Utilitários NFC
│   │       └── CsvExporter.kt           # Exportação CSV (lista & cartão)
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml        # UI principal com RecyclerView
│   │   │   └── item_card.xml            # Layout de item da lista
│   │   ├── anim/
│   │   │   ├── slide_in_right.xml       # Animação de entrada de items
│   │   │   ├── pulse.xml                # Animação de pulso do ícone NFC
│   │   │   └── rotate_infinite.xml      # Rotação contínua
│   │   ├── drawable/
│   │   │   ├── ic_nfc.xml               # Ícone NFC (Material)
│   │   │   ├── ic_credit_card.xml       # Ícone de cartão
│   │   │   ├── ic_badge.xml             # Ícone de contador
│   │   │   ├── ic_save.xml              # Ícone de exportar
│   │   │   └── ic_delete.xml            # Ícone de limpar
│   │   ├── values/
│   │   │   ├── strings.xml              # Strings em Português
│   │   │   ├── colors.xml               # Paleta de cores (claro)
│   │   │   └── themes.xml               # Tema Material 3
│   │   ├── values-night/
│   │   │   └── colors.xml               # Paleta de cores (escuro)
│   │   └── xml/
│   │       ├── nfc_tech_filter.xml      # Filtro de tecnologias NFC
│   │       ├── file_paths.xml           # FileProvider paths
│   │       ├── backup_rules.xml         # Regras de backup
│   │       └── data_extraction_rules.xml
│   └── AndroidManifest.xml              # Configuração da app + NFC + FileProvider
└── build.gradle.kts                     # Dependências do módulo
```

## Arquitetura

### Fluxo de Leitura NFC

```
Tag NFC → NfcReader → Extrair UID → Verificar Debounce
                          ↓
                    Vibrar (200ms)
                          ↓
        CardRepository.insertCard(uid, type, timestamp)
                          ↓
                    Room Database
                          ↓
                    Flow<List<CardEntry>>
                          ↓
              MainActivity observa Flow
                          ↓
                CardAdapter.submitList()
                          ↓
                  RecyclerView atualiza
                          ↓
                Animação slide-in + Scroll para topo
```

### Padrão Repository

```
MainActivity → CardRepository → CardDao → Room Database (SQLite)
      ↑                                          ↓
      └──────── Flow<List<CardEntry>> ──────────┘
```

### Exportação e Partilha CSV

```
Botão Export → performCsvExport() → CardRepository.getAllCardsList()
                                            ↓
                          CsvExporter.exportCardList(cardList)
                                            ↓
                    ┌──────────────────────┴──────────────────────┐
                    ↓                                              ↓
          Android 10+ (API 29+)                        Android 6-9 (API 23-28)
        MediaStore Downloads API                    Legacy External Storage
       (sem permissão necessária)              (pede WRITE_EXTERNAL_STORAGE)
                    ↓                                              ↓
                    └──────────────────────┬──────────────────────┘
                                           ↓
                              CsvExportResult (URI, path, filename)
                                           ↓
                            shareExportedFile() → Intent.ACTION_SEND
                                           ↓
                              Diálogo de partilha do Android
```

## Chaves Mifare Padrão

A aplicação tenta autenticar sectores com as seguintes chaves (ordem de tentativa):

1. `FF FF FF FF FF FF` - Chave de fábrica padrão
2. `A0 A1 A2 A3 A4 A5` - Alternativa comum
3. `D3 F7 D3 F7 D3 F7` - Chave MAD (Mifare Application Directory)
4. `00 00 00 00 00 00` - Chave nula

Para cada sector, tenta:
- Key A com todas as chaves acima
- Key B com todas as chaves acima

**Adicionar chaves personalizadas**: Editar `MifareClassicReader.kt:DEFAULT_KEYS`

## Localização de Ficheiros

### Android 10+ (API 29+)
```
/storage/emulated/0/Download/mifare_cards_[timestamp].csv
```
- Acessível via app Ficheiros → Downloads
- Visível imediatamente após exportação
- Não requer permissão de armazenamento

### Android 6-9 (API 23-28)
```
/storage/emulated/0/Download/mifare_cards_[timestamp].csv
```
- Requer permissão `WRITE_EXTERNAL_STORAGE`
- Pedida apenas quando exporta pela primeira vez
- Ficheiro acessível via qualquer gestor de ficheiros

### Formato de Nomes
- **Lista de cartões**: `mifare_cards_20251030_143522.csv`
- **Cartão individual**: `card_04A1B2C3D4E5_20251030_143522.csv`

## Build e Deployment

### Build Debug
```bash
./gradlew assembleDebug
# APK gerado em: app/build/outputs/apk/debug/app-debug.apk
```

### Build Release (Produção)
```bash
# 1. Criar keystore (apenas primeira vez)
keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mifare-reader

# 2. Configurar signing em app/build.gradle.kts
# 3. Build release
./gradlew assembleRelease

# APK gerado em: app/build/outputs/apk/release/app-release.apk
```

### Testes
```bash
# Todos os testes
./gradlew test

# Testes unitários apenas
./gradlew testDebugUnitTest

# Testes instrumentados (requer dispositivo)
./gradlew connectedAndroidTest
```

### Qualidade de Código
```bash
# Lint checks
./gradlew lint

# Relatório HTML de lint
./gradlew lintDebug
# Abrir: app/build/reports/lint-results-debug.html
```

## Resolução de Problemas

### NFC não funciona

**Problema**: App não lê cartões

**Soluções**:
1. Verificar se o dispositivo tem NFC:
   - Definições → Ligações → procurar "NFC"
   - Se não existir, o dispositivo não tem hardware NFC
2. Ativar NFC nas definições
3. Remover capas grossas do telemóvel
4. Experimentar diferentes posições do cartão (geralmente no centro traseiro)
5. Reiniciar a app e tentar novamente

### Permissão de Armazenamento Negada

**Problema**: Não consegue exportar CSV em Android 6-9

**Solução**:
1. Pressionar "Abrir Definições" no snackbar que aparece
2. Ou ir a: Definições → Apps → RFID Card Reader → Permissões
3. Ativar permissão de "Armazenamento"
4. Voltar à app e tentar exportar novamente

### Ficheiro CSV não aparece

**Problema**: Exportou mas não encontra o ficheiro

**Soluções Android 10+**:
1. Abrir app "Ficheiros" (Files)
2. Ir para "Downloads"
3. Procurar por "mifare_cards_"
4. Se não aparecer, aguardar 10-20 segundos (indexação)

**Soluções Android 6-9**:
1. Usar qualquer gestor de ficheiros
2. Navegar para `/Download/` ou `/Downloads/`
3. Procurar por "mifare_cards_"

### Cartões Duplicados

**Problema**: Mesmo cartão aparece várias vezes rapidamente

**Explicação**:
- Comportamento esperado: cada leitura cria uma nova entrada
- Debounce de 2 segundos previne duplicações acidentais
- Se ler o mesmo cartão 3x em 10 segundos: 3 entradas criadas

**Solução**:
- Se não deseja duplicados: aguardar antes de re-ler
- Se deseja limpar: botão "Limpar Lista"

### App Lenta com Muitos Cartões

**Problema**: App fica lenta com 1000+ cartões

**Solução**:
1. Exportar lista para CSV
2. Limpar lista antiga
3. Começar nova sessão de leitura
4. Room Database é eficiente até ~10.000 entradas

### Vibração não funciona

**Problema**: Não sente vibração ao ler cartões

**Causas possíveis**:
1. Permissão VIBRATE não concedida (verificar AndroidManifest)
2. Modo "Não Incomodar" ativado
3. Vibração desativada nas definições do sistema
4. Hardware não suporta vibração (raro)

**Nota**: App funciona normalmente sem vibração, apenas sem feedback háptico

## Segurança e Privacidade

### Dados Armazenados Localmente
- Todos os dados de cartões ficam apenas no dispositivo
- Nenhuma transmissão de rede
- Não há servidores remotos
- Base de dados Room armazenada em:
  ```
  /data/data/com.rlfm.mifarereader/databases/card_database
  ```

### Backup e Restauro
- **Backup Automático Android**: Excluído (por segurança)
- **Backup Manual**: Exportar para CSV e guardar ficheiro
- **Restauro**: Não implementado (seria necessário importação CSV)

### Partilha Segura
- Usa `FileProvider` com URIs `content://` (não `file://`)
- URIs temporários com permissão `FLAG_GRANT_READ_URI_PERMISSION`
- Ficheiros partilhados são apenas leitura
- Permissão revogada após partilha completa

### Chaves de Autenticação
- Chaves padrão são públicas (não secretas)
- Se cartão tem chaves personalizadas, sectores ficam inacessíveis
- App não tenta ataques de força bruta
- Não armazena chaves descobertas

## Limitações Conhecidas

1. **Apenas Mifare Classic**: Não lê outros tipos (DESFire, Ultralight, etc.)
2. **Chaves Padrão**: Apenas tenta chaves conhecidas publicamente
3. **Sectores Protegidos**: Sectores com chaves não-padrão ficam inacessíveis
4. **Sem Escrita**: App apenas lê, não escreve em cartões
5. **Sem Cloud**: Dados não sincronizam entre dispositivos
6. **Import CSV**: Não suportado (apenas export)

## Contribuir

### Reportar Bugs
1. Ir para [Issues](../../issues)
2. Criar novo issue com:
   - Versão Android
   - Modelo do dispositivo
   - Passos para reproduzir
   - Screenshots se possível

### Sugerir Funcionalidades
1. Verificar se já existe issue semelhante
2. Criar novo issue com tag "enhancement"
3. Descrever use case e benefícios

### Pull Requests
1. Fork do repositório
2. Criar branch: `git checkout -b feature/nova-funcionalidade`
3. Commit: `git commit -m "Adicionar nova funcionalidade"`
4. Push: `git push origin feature/nova-funcionalidade`
5. Criar Pull Request

## Roadmap Futuro

- [ ] Suporte para outros tipos de cartões NFC (Ultralight, DESFire)
- [ ] Importação de ficheiros CSV
- [ ] Exportação para outros formatos (JSON, XML)
- [ ] Estatísticas de leitura (gráficos)
- [ ] Categorização de cartões com tags
- [ ] Pesquisa e filtros na lista
- [ ] Widget de leitura rápida
- [ ] Backup para cloud (Google Drive)
- [ ] Modo multi-utilizador
- [ ] Escrita em cartões (requer cuidados de segurança)

## Licença

Este projeto está licenciado sob a **Apache License 2.0**.

```
Copyright 2025 RLFM

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

Consulte o ficheiro [LICENSE](LICENSE) para o texto completo da licença.

## Desenvolvimento

Para informações técnicas detalhadas sobre arquitetura, convenções de código, estrutura interna e guias de desenvolvimento, consulte o ficheiro [CLAUDE.md](CLAUDE.md).

## Contacto e Suporte

- **Issues**: [GitHub Issues](../../issues)
- **Documentação**: Este README e [CLAUDE.md](CLAUDE.md)
- **Versão**: 1.0
- **Última Atualização**: 30 de Outubro de 2025

---

Desenvolvido com ❤️ em Kotlin para Android
