# Importare lo storico da Zepp e Amazfit

[Indice](README.md) · [Preparazione e verifiche](preparazione.md)

Questa guida riguarda **Zepp**, usata con molti dispositivi Amazfit. **Zepp Life è un’app distinta**: i collegamenti disponibili possono differire.

## Collegamento a Health Connect

1. Apri Zepp e sincronizza l’orologio.
2. Nel **Profilo** cerca **Collega account / Account di terze parti → Health Connect**. Il nome e la posizione possono variare.
3. Attiva la sincronizzazione e consenti la scrittura delle categorie.
4. Controlla un dato recente e uno passato in Health Connect, quindi apri il periodo corrispondente in OpenVitals.

La procedura di collegamento è descritta anche nella [guida di HeiaHeia](https://support.heiaheia.com/connecting-amazfit-zepp-with-heiaheia). Non costituisce una garanzia di esportazione di tutti gli anni precedenti.

## Dati vecchi rimasti soltanto in Zepp

Non abbiamo verificato un comando universale che trasferisca l’intero storico Zepp in Health Connect. Non usare la guida Samsung selezionando Zepp se l’intermediario non la supporta esplicitamente.

- Se puoi esportare un allenamento, verifica il formato: OpenVitals può importare [FIT](../../features/fit-files-import.md) e [GPX/TCX e altri percorsi supportati](../../features/route-file-import.md). Il file deve contenere i dati necessari.
- Per misurazioni in CSV usa la [mappatura delle colonne](../../features/csv-import.md); l’archivio completo dell’account non è automaticamente compatibile.
- Per pochi dati visibili, condividi lo screenshot con OpenVitals, controlla l’anteprima e conferma il salvataggio. Non recupera campioni assenti dall’immagine.

Un permesso HRV concesso a Zepp non prova che esistano record HRV: controllali in Health Connect. La disponibilità dipende da app, dispositivo e dati effettivamente esportati.
