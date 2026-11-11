# Importare lo storico da Fitbit e Google Health

[Indice](README.md) · [Preparazione e verifiche](preparazione.md)

I dati Fitbit sono gestiti nell’ecosistema Google Health. **Google Health e Health Connect sono servizi distinti**; il nome visualizzato può dipendere dalla versione installata.

## Collegamento diretto

1. Apri l’app che gestisce i tuoi dati Fitbit e sincronizza il dispositivo.
2. In Google Health vai in **Connessioni → App partner → Sincronizza le tue app preferite → Configura**. Alcune versioni mostrano **Gestisci Health Connect** nelle impostazioni.
3. Scegli i dati da condividere e autorizza gli accessi. Controlla in Health Connect che la scrittura delle categorie desiderate sia consentita.
4. Aspetta il completamento della sincronizzazione. Verifica un dato passato in Health Connect, poi in OpenVitals.

Concedere a Google Health la lettura dello storico di Health Connect non forza l’esportazione del passato Fitbit nella direzione opposta.

## Se mancano i dati precedenti

Prima di affidarti a un intermediario, verifica che supporti attualmente il tuo account e la connessione Google Health: le integrazioni precedenti Fitbit possono richiedere aggiornamenti. Se Health Sync rende disponibile la sorgente, segui la [procedura comune](preparazione.md#recupero-con-health-sync).

Per conservare una copia puoi usare [Google Takeout](https://takeout.google.com/), selezionando il servizio relativo ai tuoi dati. Lo ZIP Takeout non è un’esportazione Apple Health e non è importabile come tale. I CSV eventualmente presenti richiedono la verifica di struttura, date, unità e colonne compatibili con l’[importatore CSV](../../features/csv-import.md); non garantiscono il recupero di tutte le categorie.

Fonti: [Google: collegamento Health Connect](https://support.google.com/googlehealth/answer/14506680?hl=it), [Google: esportare i dati](https://support.google.com/accounts/answer/3024190?hl=it), [Google: stato delle integrazioni](https://support.google.com/googlehealth/answer/14236613?hl=it).
