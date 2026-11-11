# Importare lo storico da Huawei Health

[Indice](README.md) · [Preparazione e verifiche](preparazione.md)

La procedura qui descritta usa **Health Sync** come intermediario. Serve un telefono Android su cui Health Connect sia disponibile: la sola presenza di Huawei Health non basta.

## Preparare Huawei Health

1. Apri Huawei Health e sincronizza il dispositivo; verifica che lo storico sia visibile nell’account corretto.
2. In **Io → Gestione privacy** cerca **HUAWEI Health Kit / Health Service Kit** e abilita la condivisione richiesta.
3. Se necessario usa **Io → Impostazioni → Sincronizza dati manualmente**.
4. Verifica che account e regione supportino Health Kit. Se manca l’opzione, consulta l’assistenza indicata sotto.

## Trasferire il passato

Segui la [procedura Health Sync](preparazione.md#recupero-con-health-sync) scegliendo **Huawei Health** come sorgente e **Health Connect** come destinazione. Autorizza l’account Huawei e seleziona un intervallo di prova.

Controlla il risultato per categoria: non presumere che tutti i campioni o dettagli presenti in Huawei siano disponibili all’intermediario. Al termine verifica le stesse date in Health Connect e in OpenVitals.

Se possiedi già un’esportazione, controlla i formati supportati da OpenVitals prima di usarla. Un backup Huawei generico non si importa nel lettore Apple Health; per misurazioni tabellari consulta l’[importatore CSV](../../features/csv-import.md).

Fonti: [Huawei: Health Kit e sincronizzazione](https://developer.huawei.com/consumer/en/doc/hmscore-guides/faq-0000001467892241/), [Health Sync: disponibilità per regione e problemi Huawei](https://healthsync.app/f-a-q/).
