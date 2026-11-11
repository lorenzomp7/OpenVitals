# Importare lo storico da Samsung Health

[Indice](README.md) · [Preparazione e verifiche](preparazione.md)

## Collegamento diretto

1. Apri Samsung Health e controlla che lo storico desiderato sia visibile.
2. Vai in **Impostazioni → Health Connect → Autorizzazioni app → Samsung Health** e consenti la scrittura delle categorie.
3. Riapri Samsung Health. Se usi l’account Samsung, nelle impostazioni di sincronizzazione scegli **Sincronizza ora**.
4. Controlla una data passata in Health Connect e poi in OpenVitals, seguendo la verifica della preparazione.

Il collegamento diretto sincronizza dati supportati quando vengono creati o modificati. La sincronizzazione dell’account Samsung non garantisce la riscrittura dell’intero passato in Health Connect.

## Se lo storico manca

Segui la [procedura Health Sync](preparazione.md#recupero-con-health-sync), scegliendo **Samsung Health** come origine e **Health Connect** come destinazione. Prova un breve periodo prima di estenderlo; limita il trasferimento agli intervalli mancanti.

Un backup o ZIP Samsung non va selezionato nell’importatore Apple Health. Se hai un CSV utilizzabile, l’[importatore CSV](../../features/csv-import.md) permette di associare colonne compatibili, date e unità. Sonno e allenamenti non sono ricostruibili da qualunque tabella generica: consulta i formati supportati.

Fonte: [FAQ ufficiale Samsung](https://developer.samsung.com/health/health-connect-faq.html).
