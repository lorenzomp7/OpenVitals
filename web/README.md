# OpenVitals Web — demo

Demo browser indipendente dall'app Android, con dati inseriti manualmente. Non accede a Health Connect o HealthKit e non carica misurazioni su un server. Il sito viene servito dall'hosting GitHub Pages; i dati della demo sono conservati in `localStorage` per browser e origine. Il browser può eliminarli: usa il backup JSON per conservarli o spostarli su un altro dispositivo.

## Funzioni

- Giorno, settimana (lunedì–domenica), mese e anno, con navigazione e data.
- Passi, distanza, calorie attive, sonno, peso, battito a riposo e acqua.
- Una registrazione per giornata, modificabile o eliminabile; campi vuoti distinti da zero.
- Passi, distanza, calorie e acqua: totale; sonno e battito: media dei giorni disponibili; peso: ultimo valore del periodo. Nessuna stima per i giorni mancanti.
- Grafico delle giornate registrate e storico modificabile.
- Dati fittizi caricati solo su richiesta, esportazione e importazione JSON validata.
- Manifest per apertura dalla schermata Home sui browser compatibili. Questa versione non include cache offline/service worker.

## Sviluppo

Da `web/`, con Node.js 22 o successivo, senza dipendenze da installare:

```bash
npm test
npm run build
python3 -m http.server 8000 --directory dist
```

Apri `http://localhost:8000`. Non aprire direttamente `index.html` via `file://`.

## Pubblicazione automatica

Il workflow `.github/workflows/web.yml` esegue test e build a ogni push su `main` e `main_chatgpt_04`, e nelle pull request verso `main`. Pubblica **solo da `main`**, dopo il successo dei controlli web. Il workflow Android `verify.yml` già compila gli APK su ogni push a `main`: le due pipeline restano indipendenti. Una build web fallita non blocca Android e viceversa. La pubblicazione delle release Android mantiene le regole già presenti nei workflow Android.

Una sola configurazione iniziale, da proprietario della repository:

1. Apri **Settings → Pages → Build and deployment**.
2. Seleziona **GitHub Actions** come Source.
3. Unisci le modifiche di `main_chatgpt_04` in `main`.
4. Controlla **Actions → OpenVitals Web**: il job `deploy` restituisce l'indirizzo pubblicato, normalmente `https://lorenzomp7.github.io/OpenVitals/` (salvo dominio personalizzato).

I push sul branch di lavoro producono l'artefatto verificato ma non modificano il sito pubblico. In seguito, ogni push a `main` aggiorna il sito. I dati manuali dei visitatori non fanno parte del deploy e vengono conservati nello stesso browser finché l'origine rimane la stessa. Non viene configurato alcun progetto Firebase o servizio a pagamento.

La cartella `web/` non è un modulo Gradle, non viene inclusa nell'APK e non modifica firma, package o download Android. Il codice web deve essere aggiornato esplicitamente quando cambiano funzioni comuni: la pipeline pubblica il codice, non traduce Kotlin in JavaScript.

## Backup

Formato: `{ "version": 1, "records": [{ "date": "2024-01-01", "steps": 8000, "weight": 72.5 }] }`. Gli altri campi sono facoltativi. Importare un backup sostituisce i dati dopo conferma. Date impossibili/future/duplicate, valori fuori intervallo, giornate vuote e file maggiori di 5 MB vengono rifiutati prima del salvataggio.

Il codice è distribuito secondo la [licenza del progetto](../LICENSE).
