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

## Prima configurazione di Health Connect

Health Connect (in italiano **Connessione Salute**) è l’archivio sul telefono che permette alle app di scambiarsi dati con il tuo consenso. Il percorso è: **orologio → app del produttore → Health Connect → OpenVitals**. Installare OpenVitals da solo non trasferisce i dati dell’orologio.

1. **Apri Health Connect.** Su Android 14 o successivi cerca “Connessione Salute” nelle Impostazioni; su Android 13 o precedenti compatibili installa [l’app ufficiale dal Play Store](https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata). Completa l’avvio iniziale.
2. **Sincronizza l’orologio** nell’app del produttore e abilita lì il collegamento a Health Connect (vedi sotto).
3. **Controlla le autorizzazioni app** in Health Connect: consenti all’app del produttore di **scrivere** le categorie desiderate.
4. **Apri OpenVitals** e concedi la **lettura** delle stesse categorie. Abilita la scrittura se vuoi salvare inserimenti o importazioni; autorizza anche l’accesso allo storico, quando disponibile e richiesto.
5. **Verifica un dato recente** in Health Connect, nella sezione dati, prima di cercarlo in OpenVitals. Poi riapri o aggiorna la dashboard.

I nomi dei menu possono cambiare con telefono, lingua e versione dell’app.

### Collegare Samsung Health e Zepp

| App | Configurazione |
| --- | --- |
| **Samsung Health** | Apri **Impostazioni → Health Connect → Autorizzazioni app → Samsung Health** e abilita la scrittura. Riapri Samsung Health; nelle impostazioni dell’account Samsung usa **Sincronizza ora** per aggiornare i dati disponibili sul telefono. |
| **Zepp (Amazfit)** | Nel **Profilo** cerca **Account di terze parti / Collega account → Health Connect**, attiva la sincronizzazione e autorizza le categorie da scrivere. Sincronizza l’orologio in Zepp e verifica i record in Health Connect. Il percorso può variare tra versioni. Zepp e Zepp Life sono app diverse. |
| **Altre app** | Cerca **Health Connect / Connessione Salute** nelle impostazioni di condivisione o integrazione. Se manca, serve un’esportazione o un intermediario compatibile. |

**Un’autorizzazione non prova che ci siano dati:** vedere “HRV: scrittura consentita” non significa che l’app abbia registrato HRV. Controlla le singole voci e l’app di origine in Health Connect.

### Posso recuperare lo storico precedente?

**Sì, quando la sorgente e il metodo di trasferimento lo consentono; non è garantita l’importazione automatica di tutto lo storico.** Ci sono due passaggi distinti:

- **Trasferire i vecchi record in Health Connect.** Samsung Health e Zepp decidono quali dati e periodi esportare. La sincronizzazione dell’account recupera i dati nell’app del produttore, ma non garantisce che anni di storico vengano riscritti in Health Connect. Per Zepp non diamo per garantito un recupero completo tramite il solo collegamento.
- **Leggere i record già trasferiti.** Senza il permesso aggiuntivo per lo storico, la lettura è normalmente limitata ai 30 giorni precedenti la prima autorizzazione e ai dati successivi. OpenVitals richiede l’accesso allo storico sui dispositivi che lo supportano: concedilo quando proposto. Questo permesso non recupera dati rimasti nella sorgente.

Per provare il recupero:

1. Nell’app di origine verifica che esista una misurazione di una data passata.
2. Attiva il collegamento, sincronizza e cerca **la stessa data e categoria in Health Connect**.
3. Se manca, per **Samsung Health** puoi valutare [Health Sync](https://healthsync.app/about/): supporta Samsung Health come sorgente, Health Connect come destinazione e la sincronizzazione storica. Controlla categorie, intervallo disponibile e licenza; prova prima pochi giorni ed evita due trasferimenti paralleli degli stessi dati.
4. Per **Zepp o altre sorgenti**, verifica le opzioni di esportazione e un importatore che supporti esplicitamente quel formato. Un file CSV/ZIP del produttore non è automaticamente importabile in OpenVitals o Health Connect. Per pochi dati visibili puoi usare l’importazione screenshot di OpenVitals e confermare l’anteprima; non ricostruisce campioni assenti dall’immagine.
5. Quando i record sono presenti in Health Connect, riapri OpenVitals e seleziona il periodo corrispondente.

Se la dashboard resta vuota, controlla prima scrittura della sorgente, record presenti e lettura di OpenVitals per la stessa categoria. Verifica anche le impostazioni di eliminazione automatica di Health Connect se vuoi conservare lo storico.

Riferimenti: [guida Google alla configurazione](https://support.google.com/android/answer/12201227?hl=it), [Samsung Health e Health Connect](https://developer.samsung.com/health/health-connect-faq.html), [permesso di lettura dello storico](https://developer.android.com/health-and-fitness/health-connect/read-data#read-older-data), [collegamento Zepp](https://support.heiaheia.com/connecting-amazfit-zepp-with-heiaheia).

## Mappe

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
