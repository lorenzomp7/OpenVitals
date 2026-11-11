# Importare lo storico da Garmin Connect

[Indice](README.md) · [Preparazione e verifiche](preparazione.md)

## Collegamento diretto

Garmin documenta l’invio a Health Connect su **Android 14 o successivi**, dopo una sincronizzazione riuscita del dispositivo. Il collegamento è da Garmin verso Health Connect.

1. Aggiorna Garmin Connect e sincronizza il dispositivo.
2. Nelle impostazioni delle app collegate cerca Health Connect e abilita le categorie da condividere.
3. Controlla le autorizzazioni di scrittura di Garmin Connect in Health Connect.
4. Verifica una data recente e una passata: il collegamento non va considerato prova del trasferimento completo del passato.

## Recupero tramite intermediario

Segui la [procedura Health Sync](preparazione.md#recupero-con-health-sync) con **Garmin Connect → Health Connect**. Durante l’autorizzazione Garmin abilita anche **Historical Data**, se proposta.

Health Sync indica un limite di cinque anni per le attività e due per gli altri dati Garmin. Controlla i limiti attuali prima di avviare il recupero.

## Importazione dei vecchi allenamenti con file FIT

1. Dal sito Garmin Connect apri un’attività e usa **Esporta originale / Export Original**.
2. Se scarichi uno ZIP, estrai il file FIT e trasferiscilo sul telefono Android.
3. In OpenVitals apri **Impostazioni → Importazione dati → FIT**, seleziona il file, controlla l’anteprima e salva.
4. Per molti file è disponibile l’importazione di una cartella FIT: questa scrive direttamente, quindi prepara prima un piccolo campione e i permessi.

Questa procedura recupera i dati contenuti nei file; non sostituisce un’esportazione completa di sonno, passi giornalieri e altri parametri.

Fonti: [Garmin: Health Connect](https://support.garmin.com/en-US/?faq=JToBEy0jfe6pIygark2Ui5), [Garmin: esportazione](https://support.garmin.com/en-US/?faq=W1TvTPW8JZ6LfJSfK512Q8), [Health Sync: limiti](https://healthsync.app/about/), [autorizzazione storica Garmin](https://healthsync.app/status/), [importatore FIT](../../features/fit-files-import.md).
