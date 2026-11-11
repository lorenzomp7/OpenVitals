<p align="center">
  <img src="docs/images/readme-logo.png" width="120" alt="Logo OpenVitals">
</p>

# OpenVitals

**La tua dashboard Android per salute, allenamenti e recupero, con i dati di Health Connect.**

Questa repository distribuisce la versione personalizzata di OpenVitals: dashboard configurabile, periodi giorno/settimana/mese/anno, dettagli degli allenamenti, mappe dei percorsi e importazione da screenshot sul telefono.

## Scarica l’app

[![Scarica APK](https://img.shields.io/badge/Scarica_APK-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/lorenzomp7/OpenVitals/releases/latest/download/OpenVitals-personal.apk)
[![Tutte le release](https://img.shields.io/badge/Versioni-e_note_di_rilascio-181717?style=for-the-badge&logo=github)](https://github.com/lorenzomp7/OpenVitals/releases)

### [⬇ Scarica OpenVitals-personal.apk](https://github.com/lorenzomp7/OpenVitals/releases/latest/download/OpenVitals-personal.apk)

**Download the Android APK directly using the link above.** Non servono un computer, Android Studio o l’accesso a GitHub Actions.

Ogni release contiene l’APK installabile, il checksum SHA-256 e i collegamenti al codice sorgente e alla compilazione verificata. Le release sono build di sviluppo personali: consulta le note della versione che stai scaricando. Le modifiche presenti nel branch possono essere più recenti dell’ultima release.

## Installazione e aggiornamenti

1. Apri il link **Scarica APK** dal telefono Android.
2. Scarica e apri **OpenVitals-personal.apk**.
3. Se Android lo richiede, autorizza l’installazione da questa sorgente per il browser o il gestore file utilizzato.
4. Avvia OpenVitals e concedi in Health Connect i permessi per le categorie che vuoi visualizzare.

Per aggiornare, scarica il nuovo APK dallo stesso link e installalo sopra la versione personale compatibile. La firma e l’identificativo dell’app sono mantenuti per consentire gli aggiornamenti senza disinstallazione.

| File o distribuzione | A cosa serve |
| --- | --- |
| **OpenVitals-personal.apk** nelle Releases | L’app da installare sul telefono; identificativo `tech.mmarca.openvitals.debug`. |
| `app-ci.apk` negli artefatti di verifica | Una diversa installazione per i test, con firma temporanea. |
| APK `androidTest` | Il programma che esegue i test; non è l’app per l’utente. |
| Versioni upstream Play Store / F-Droid | Distribuzioni del progetto originale, separate da questa versione personalizzata. |

Se Android rifiuta un aggiornamento, controlla di aver scaricato l’APK personale e di avere una versione con firma compatibile. [Dettagli su firma e verifica degli aggiornamenti](https://github.com/lorenzomp7/OpenVitals/blob/main_chatgpt/docs/engineering/personal-build.md).

## Cosa offre questa versione

| Area | Funzioni |
| --- | --- |
| **Dashboard personalizzabile** | Riquadri ridimensionabili e riordinabili, riepilogo modificabile e pagine laterali con sei riquadri medi. |
| **Periodi e storico** | Selezione giorno, settimana, mese e anno; totali, medie e grafici secondo il tipo di metrica. |
| **Allenamenti** | Cronologia, riepiloghi specifici per sport, passi, frequenza cardiaca, velocità, cadenza e split quando disponibili. |
| **Percorsi e mappe** | Percorsi GPS salvati, cartografia Mapbox configurabile sul telefono e anteprima locale del tracciato. |
| **Corpo e parametri vitali** | Peso, composizione corporea, pressione, SpO₂, HRV e altri dati supportati da Health Connect. |
| **Sonno e recupero** | Sessioni e fasi del sonno, viste Daily Readiness, Body Energy e Training Readiness in base ai dati disponibili. |
| **Importazione screenshot** | Selezione da impostazioni o condivisione Android; riconoscimento OCR sul telefono e anteprima da correggere prima del salvataggio. |
| **Altri strumenti** | Registrazione attività, inserimento manuale, importazione di file, report PDF, nutrizione, idratazione, ciclo e sincronizzazione Bluetooth tra telefoni. |

I dati dipendono dai permessi e da ciò che le app dei tuoi dispositivi scrivono effettivamente in Health Connect. OpenVitals non può mostrare una misurazione rimasta soltanto nell’app del produttore.

Le note di ciascuna [release](https://github.com/lorenzomp7/OpenVitals/releases) identificano il codice incluso nell’APK. Il [branch di sviluppo `main_chatgpt`](https://github.com/lorenzomp7/OpenVitals/tree/main_chatgpt) contiene anche le correzioni più recenti, fra cui mappe a schermo intero, aggiornamento del peso al ritorno nell’app e lettura dei passi e della cadenza degli allenamenti.

## Health Connect e mappe

- **Health Connect:** deve essere disponibile sul telefono. Su Android 14 e successivi è integrato nel sistema; sulle versioni precedenti compatibili si installa separatamente.
- **Permessi:** puoi scegliere le categorie da leggere e autorizzare la scrittura quando usi inserimento manuale o importazioni.
- **Mapbox:** la configurazione avviene sul telefono. Nessun token Mapbox è incluso nel sorgente o nell’APK. [Configurazione mappe e comportamento offline](https://github.com/lorenzomp7/OpenVitals/blob/main_chatgpt/docs/engineering/mapbox.md).
- **Senza cartografia:** resta disponibile l’anteprima locale del percorso GPS salvato.

## Dati e privacy

Non è richiesto un account OpenVitals. I dati sanitari sono letti da Health Connect ed elaborati sul dispositivo; le scritture avvengono tramite le azioni di salvataggio, importazione o sincronizzazione avviate o abilitate dall’utente.

Questa versione utilizza Internet per la cartografia e l’accesso Mapbox. Le richieste delle mappe comunicano con il fornitore della cartografia; non viene introdotto un server OpenVitals a cui caricare i dati sanitari. Le credenziali Mapbox sono conservate localmente in forma cifrata.

OCR e generazione dei report vengono eseguiti sul telefono. Le valutazioni di recupero sono informazioni derivate dai dati disponibili e non diagnosi mediche.

## Release e sviluppo

L’APK personale viene compilato dal branch `main_chatgpt`. La pubblicazione automatica nelle Releases riutilizza soltanto una compilazione **personal-apk** terminata con successo, inclusi i controlli di firma e di installazione/aggiornamento sull’emulatore.

- [APK, versioni precedenti e note](https://github.com/lorenzomp7/OpenVitals/releases)
- [Codice della versione personalizzata](https://github.com/lorenzomp7/OpenVitals/tree/main_chatgpt)
- [Compilazioni e verifiche](https://github.com/lorenzomp7/OpenVitals/actions)
- [Segnala un problema in questa repository](https://github.com/lorenzomp7/OpenVitals/issues)
- [Guida tecnica delle build personali](https://github.com/lorenzomp7/OpenVitals/blob/main_chatgpt/docs/engineering/personal-build.md)

Per compilare la versione personalizzata: clona questa repository con i sottomoduli, seleziona `main_chatgpt` e usa Android SDK 37 e JDK 17.

```bash
git clone --recurse-submodules --branch main_chatgpt https://github.com/lorenzomp7/OpenVitals.git
cd OpenVitals
./gradlew :app:assembleDebug
./gradlew verifyCi
```

## Progetto originale e licenza

Questa versione deriva da [OpenVitals di Hale-MTU](https://github.com/Hale-MTU/OpenVitals). La documentazione e le distribuzioni del progetto originale possono differire dalle funzioni e dagli APK di questa repository.

OpenVitals è distribuito con licenza [GNU AGPL v3.0 o successiva](LICENSE). Crediti e componenti di terze parti sono elencati in [THANKS.md](THANKS.md) e [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
