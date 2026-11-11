# Preparare l’importazione dello storico

[Indice delle guide](README.md)

## Prima di trasferire

1. Aggiorna OpenVitals, Health Connect e l’app di origine. Per la configurazione iniziale consulta il [README](../../../README.md#prima-configurazione-di-health-connect).
2. Nell’app originale scegli un giorno passato con un dato facilmente riconoscibile, per esempio un peso o un allenamento. Servirà per il confronto.
3. Se disponibile, conserva un’esportazione originale prima di procedere. Non cancellare lo storico della sorgente.
4. Scegli una sola procedura per ciascuna categoria e intervallo: evita di inviare gli stessi passi o allenamenti contemporaneamente attraverso più app.

## Scrittura e lettura sono due autorizzazioni diverse

L’app che trasferisce i dati deve avere il permesso di **scriverli** in Health Connect. OpenVitals deve poter **leggere** le categorie interessate. Se importi un file con OpenVitals, autorizza anche la sua scrittura.

Per leggere dati anteriori ai 30 giorni precedenti la prima autorizzazione serve l’accesso aggiuntivo allo storico, quando supportato. OpenVitals lo richiede sui dispositivi compatibili. Concederlo non trasferisce i dati ancora presenti soltanto nell’app originale.

## Recupero con Health Sync

Usa questo metodo soltanto se la guida della sorgente lo indica:

1. Installa [Health Sync](https://healthsync.app/) e controlla che la sorgente sia disponibile.
2. Seleziona quell’app come origine e **Health Connect** come destinazione; autorizza gli accessi.
3. Controlla le categorie proposte. Per il passato serve una licenza: lo storico non è incluso nella prova gratuita.
4. Apri **Historical Data / Dati storici**, scegli categorie e date, iniziando da pochi giorni.
5. Attendi i lotti di sincronizzazione e controlla l’avanzamento; anni di dati richiedono tempo.
6. Se usi già un collegamento diretto per i dati nuovi, delimita il periodo storico ed evita sovrapposizioni.

Prima di acquistare controlla [compatibilità](https://healthsync.app/about/) e [stato del servizio](https://healthsync.app/status/). Nessun intermediario garantisce dati che la sorgente non rende disponibili.

## Controllo finale

1. In Health Connect apri i dati della categoria e cerca il giorno scelto, controllando anche l’app di origine.
2. In OpenVitals apri lo stesso periodo e confronta una misurazione o un allenamento. Un riepilogo giornaliero non sostituisce necessariamente i singoli campioni.
3. Se manca in Health Connect, verifica la procedura di trasferimento. Se è presente lì e manca in OpenVitals, verifica lettura, accesso allo storico e periodo selezionato.
4. In caso di duplicati identifica l’importatore e l’intervallo prima di usare gli strumenti di eliminazione di Health Connect. Cancellare tutti i dati rimuove anche quelli corretti.
5. Controlla l’eliminazione automatica di Health Connect se vuoi conservare il passato.

I controlli dei duplicati di OpenVitals non garantiscono l’eliminazione delle copie scritte da app diverse.

Fonti: [Google: configurazione](https://support.google.com/android/answer/12201227?hl=it), [Google: lettura dello storico](https://developer.android.com/health-and-fitness/health-connect/read-data), [Health Sync: storico e licenza](https://healthsync.app/about/).
