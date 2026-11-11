# Importare lo storico da Apple Salute

[Indice](README.md) · [Preparazione e verifiche](preparazione.md)

OpenVitals su Android dispone di un importatore per l’esportazione di **Salute / Apple Health**. Il trasferimento avviene tramite file.

## Esportare da iPhone

1. Apri **Salute** sull’iPhone.
2. Tocca il profilo in alto e scegli **Esporta tutti i dati sanitari**.
3. Salva lo ZIP e trasferiscilo sul telefono Android senza pubblicarlo: contiene dati personali.

## Importare in OpenVitals

1. Apri **Impostazioni → Importazione dati → Apple Health**.
2. Seleziona lo ZIP esportato; è accettato anche il file XML supportato.
3. Attendi l’analisi, che non scrive ancora dati.
4. Seleziona le categorie da importare e concedi i relativi permessi di scrittura in Health Connect.
5. Avvia l’importazione e leggi il riepilogo: importati, duplicati, non supportati ed eventuali errori.
6. Controlla una data passata in Health Connect e nel periodo corrispondente di OpenVitals.

Per un archivio grande lascia terminare il lavoro avviato dall’app. Il file ZIP può contenere i percorsi degli allenamenti: conserva l’archivio originale per includerli.

## Limiti

Vengono trasferiti soltanto i dati compatibili con l’importatore e Health Connect. L’importazione del file non crea una sincronizzazione continua con iPhone. I record clinici seguono un percorso separato: consulta la documentazione dell’importatore. Per evitare copie, non importare lo stesso intervallo anche attraverso un altro servizio.

Fonti: [Apple: esportazione dei dati Salute](https://www.apple.com/legal/privacy/data/en/health-app/), [OpenVitals: formati, categorie e gestione duplicati](../../features/apple-health-import.md).
