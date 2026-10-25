# STREAMHUB

[![Build APK](https://github.com/StillSalad/streamhub/actions/workflows/android.yml/badge.svg)](https://github.com/StillSalad/streamhub/actions/workflows/android.yml)

Aggregatore personale di film e serie TV per **Android TV, Google TV, Xiaomi TV Stick e Amazon Fire TV Stick**.
Cerchi un titolo, vedi dove è disponibile in Italia (abbonamento, noleggio, acquisto) e premi **GUARDA**:
STREAMHUB apre l'app ufficiale del servizio con una breve transizione cinematografica.

STREAMHUB **non** riproduce, scarica o redistribuisce contenuti, **non** chiede password e **non** contiene
pubblicità o tracciamento. Tutti i dati personali (lista, servizi, filtri, ricerche) restano sul dispositivo.

---

## Stato reale del progetto (leggi prima)

| Cosa | Stato |
|---|---|
| Codice sorgente completo, Gradle, manifest, asset | ✅ Presente |
| APK compilato | ❌ **Non compilato**: l'ambiente in cui è stato scritto non ha l'Android SDK. Va compilato in Android Studio (istruzioni sotto). |
| Test della logica (servizi, avvio app, fallback, preferiti, ricerca, ordinamento offerte) | ✅ 22 test **eseguiti e superati** con il compilatore Kotlin e un runner minimale (non con Gradle) |
| Test di parsing API ed errori di rete (`TmdbParsingTest`, `TmdbClientTest`) | ⚠️ Scritti, **non eseguiti**: richiedono Gradle |
| Test telecomando e tasto Indietro (`RemoteNavigationTest`) | ⚠️ Scritti, **non eseguiti**: richiedono emulatore o dispositivo |
| Prova su Xiaomi TV Stick / Fire TV Stick | ❌ Non effettuata |

Alla prima compilazione potrebbero emergere piccoli errori (il codice UI non è stato compilato contro le librerie Android).
Se succede, copia l'errore e chiedi aiuto: di solito si sistemano in pochi minuti.

---

## 0. GitHub: compilare l'APK senza Android Studio

Il repository include `.github/workflows/android.yml`: a ogni push su `main` GitHub compila l'APK di debug
ed esegue i test unitari sui propri server (che hanno già l'Android SDK).

**Caricare il progetto** (una volta sola), dalla cartella `StreamHub`:

```
git init
git add .
git commit -m "STREAMHUB 1.0.0"
git branch -M main
git remote add origin https://github.com/StillSalad/streamhub.git
git push -u origin main
```

Prima crea il repository vuoto `streamhub` su github.com (senza README, senza .gitignore).
In alternativa usa **GitHub Desktop**: *File → Add local repository* → *Publish repository*.

**Scaricare l'APK:** tab **Actions** → ultima esecuzione "Build APK" → sezione *Artifacts* → `streamhub-debug-apk`
(è uno ZIP che contiene l'APK).

**Release con APK allegato:**

```
git tag v1.0.0
git push origin v1.0.0
```

L'APK comparirà in **Releases**: dal TV stick puoi scaricarlo con l'app *Downloader* usando il link della release.

> 🔒 **Non caricare mai la chiave TMDb su GitHub.** `local.properties` è già escluso da `.gitignore`.
> Non metterla nemmeno nei *Secrets* per la build: finirebbe dentro l'APK scaricabile da chiunque.
> Inseriscila dall'app, in Impostazioni → *Configura chiave*.

## 1. Requisiti

- **Android Studio** (versione Ladybug 2024.2 o successiva) — https://developer.android.com/studio
- Connessione a Internet (al primo avvio scarica Gradle e le librerie)
- Un account gratuito **TMDb** per la chiave API
- Il TV stick e il PC sulla **stessa rete Wi‑Fi** (per l'installazione via ADB)

## 2. Aprire il progetto

1. Estrai lo ZIP.
2. Android Studio → **File → Open** → seleziona la cartella `StreamHub`.
3. Attendi la fine della sincronizzazione Gradle (barra in basso). Se chiede di installare SDK 35 o build tools, accetta.

## 3. Configurare la chiave TMDb

STREAMHUB usa TMDb per titoli, locandine, trame, cast e disponibilità (dati JustWatch).

1. Crea un account su https://www.themoviedb.org
2. Profilo → **Impostazioni → API** → richiedi una chiave per uso personale.
3. Hai due opzioni:
   - **Consigliata — dall'app**: avvia STREAMHUB → Impostazioni → *Configura chiave* → incolla la "Chiave API" (v3) o il "Token di accesso in lettura". Viene cifrata con l'Android Keystore.
   - **Per lo sviluppo**: aggiungi al file `local.properties` (nella cartella del progetto) la riga
     `TMDB_API_KEY=la_tua_chiave` e ricompila.

> ⚠️ Una chiave incorporata in un APK **non è segreta**: chiunque abbia l'APK può estrarla.
> Va bene per uso personale; non pubblicare mai un APK o un repository con la tua chiave dentro.
> `local.properties` è già escluso da Git.

Prima di distribuire l'app ad altri, leggi i termini TMDb (https://www.themoviedb.org/api-terms-of-use).
L'attribuzione TMDb e JustWatch richiesta è già presente nell'interfaccia.

## 4. Compilare l'APK di debug

**Da Android Studio:** menu **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
Al termine clicca *locate*: il file è `app/build/outputs/apk/debug/app-debug.apk`.

**Da terminale** (nella cartella del progetto):

```
./gradlew assembleDebug          # macOS / Linux
gradlew.bat assembleDebug        # Windows
```

L'APK di debug si installa accanto a una futura versione release (ha il suffisso `.debug`).

## 5. Installare su Xiaomi TV Stick (Android TV / Google TV)

1. Sul TV stick: **Impostazioni → Preferenze dispositivo → Informazioni** → premi 7 volte su **Build** finché appare "Sei uno sviluppatore".
   (Su Google TV: *Impostazioni → Sistema → Informazioni → Build del sistema operativo Android TV*.)
2. **Impostazioni → Preferenze dispositivo → Opzioni sviluppatore** → attiva **Debug USB** (e "Debug di rete" se presente).
3. Trova l'indirizzo IP: *Impostazioni → Rete e Internet → (la tua rete)*.
4. Dal PC (ADB è in `Android/Sdk/platform-tools`):
   ```
   adb connect 192.168.1.XX:5555
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   Alla prima connessione conferma sulla TV "Consenti debug".
5. STREAMHUB compare tra le app (banner con il logo).

*Alternativa senza PC:* app "Send Files to TV" o una chiavetta USB + un file manager, poi consenti l'installazione da origini sconosciute per quel file manager.

## 6. Installare su Amazon Fire TV Stick

1. **Impostazioni → La mia Fire TV → Informazioni** → premi 7 volte sul nome del dispositivo per sbloccare le **Opzioni sviluppatore**.
2. **Impostazioni → La mia Fire TV → Opzioni sviluppatore** → attiva **Debug ADB** e **App da origini sconosciute** (o abilitala per l'app che userai per installare).
3. IP: *Impostazioni → La mia Fire TV → Informazioni → Rete*.
4. Dal PC:
   ```
   adb connect 192.168.1.XX:5555
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
5. L'app compare in **Le tue app e i tuoi canali** (in fondo alla lista: puoi spostarla).

*Alternativa senza PC:* app **Downloader** dall'Amazon Appstore, carica l'APK su un link e scaricalo da lì.

## 7. Build release (facoltativa)

1. Android Studio → **Build → Generate Signed App Bundle or APK → APK**.
2. Crea un nuovo keystore (conservalo con cura insieme alla password: serve per ogni aggiornamento).
3. Scegli `release`. Il file è in `app/release/`.

## 8. Eseguire i test

```
./gradlew testDebugUnitTest                 # test della logica, parsing ed errori di rete
./gradlew connectedDebugAndroidTest         # test telecomando: serve un emulatore "Android TV" o un dispositivo collegato
```

Per l'emulatore: Android Studio → Device Manager → Create Device → categoria **TV** → *Android TV (1080p)*.

---

## Uso con il telecomando

| Tasto | Funzione |
|---|---|
| Frecce | Spostano il focus (sempre visibile: bordo ciano e leggero ingrandimento) |
| OK | Apre / conferma |
| Indietro | Torna alla schermata precedente; dalla Home esce dall'app |
| Menu (se presente) | Apre la Ricerca |
| Freccia sinistra dal bordo | Apre il menu laterale |

Nei campi di testo la tastiera non si apre scorrendo: premi **OK** per scrivere, "Fatto" per chiudere.

## Schermate

Splash · Home (In evidenza, Popolari in Italia, Film del momento, Serie TV popolari, Novità, Disponibili sui tuoi servizi, Consigliati, Continua la scoperta, La tua lista) · Ricerca con filtri (tipo, genere, servizio, ordinamento), ricerca vocale e ricerche recenti · Scheda film/serie (trama, cast, stagioni, dove guardarlo con abbonamento/noleggio/acquisto distinti) · Servizi · La mia lista · Impostazioni · Configurazione API · Errori recuperabili · Servizio non installato · Contenuto non disponibile.

Le sezioni senza dati non vengono mostrate. "Continua a guardare" **non** esiste di proposito: nessun servizio offre un'integrazione autorizzata per leggere i tuoi progressi.

## Architettura

Un solo modulo Gradle (`app`) organizzato per pacchetti, più semplice da gestire per chi inizia:

```
com.lollomarta.streamhub
├── StreamHubApp.kt          AppContainer: dipendenze create a mano (niente librerie DI)
├── MainActivity.kt          splash di sistema + tema + navigazione
├── domain/                  modelli, errori, interfacce dei repository, logica pura testabile
├── data/network/            client TMDb (timeout, retry, cache HTTP, cache offline), DTO, mapper
├── data/catalog/            CatalogRepository e ProviderAvailabilityRepository (TMDb)
├── data/preferences/        PreferencesRepository (DataStore) + cifratura chiave (Keystore)
├── integrations/            StreamingService, adattatori, StreamingLaunchManager, ServiceMatcher, WatchOptions
├── navigation/              NavHost e rotte
└── ui/                      tema, componenti TV, schermate, transizione cinematografica
```

Interfacce principali: `CatalogRepository`, `ProviderAvailabilityRepository`, `PreferencesRepository`,
`StreamingLaunchManager`, `StreamingServiceAdapter`.

Scelte tecniche: Kotlin 2.0, Jetpack Compose (foundation + Material 3) con gestione del focus su misura per il D-pad,
OkHttp + kotlinx.serialization, Coil, DataStore, coroutine. `minSdk 25` (Android 7.1) copre Fire OS 6+ e i TV stick Android TV 9+;
`compileSdk`/`targetSdk 35`.

Permessi: solo `INTERNET`. La visibilità delle app installate è limitata ai package dei servizi e alle categorie di avvio dichiarate in `<queries>`.

## Integrazioni

Vedi **[docs/INTEGRAZIONI.md](docs/INTEGRAZIONI.md)** per l'elenco di cosa è verificato e cosa no.
In breve: la disponibilità dei titoli viene da dati reali (TMDb/JustWatch); l'apertura avviene sull'**app** del servizio,
perché nessun servizio (eccetto YouTube per i trailer) offre deep link documentati utilizzabili da app di terze parti con i dati TMDb.

## Limiti noti

- La transizione verso l'altra app dipende da Android: non può essere completamente invisibile.
- I dati di disponibilità possono essere incompleti o in ritardo rispetto ai cataloghi reali.
- "Disponibili sui tuoi servizi" usa i servizi che **selezioni tu**: STREAMHUB non verifica gli abbonamenti.
- Alcuni package (vedi documento integrazioni) vanno confermati sul tuo dispositivo.

---

Dati: TMDb. *This product uses the TMDB API but is not endorsed or certified by TMDB.* Disponibilità streaming: JustWatch.
Creato da LolloMarta.
