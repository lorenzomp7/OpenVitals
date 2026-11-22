@AGENTS.md

## Istruzioni specifiche per Claude Code

AGENTS.md (importato sopra) vale per tutti gli agenti; qui solo ciò che riguarda Claude Code.

### Metodo di lavoro

- Un compito per sessione. Tra un compito e il successivo usa `/clear`.
- Modifiche su più file o di architettura: prima plan mode, fai approvare il piano, poi implementa.
- Prima di modificare leggi i file coinvolti e i test esistenti. Niente refactor non richiesti.
- Ogni cambio di comportamento va con un test; esegui la verifica standard di AGENTS.md.
- Alla fine riporta: cosa è cambiato, comandi eseguiti con esito, cosa resta da fare.
- Subagenti solo se l'utente lo chiede. Il caso più utile: una revisione indipendente del diff prima di chiudere un compito grande.

### Ambiente (sessioni cloud)

- All'avvio di ogni sessione cloud `.claude/hooks/session_start.sh` scarica il submodule `tools/agent-skills` (skill) e avvia in background JDK 17 + SDK Android in `~/android-sdk` (5-10 minuti; servono `dl.google.com` e, per Gradle, `api.mapbox.com` tra i domini consentiti dell'ambiente).
- Prima di qualunque comando Gradle esegui `bash .claude/hooks/wait_setup.sh android`: aspetta la fine dell'installazione o spiega perché è fallita. Se fallisce per la rete, dillo all'utente invece di aggirare il problema.
- Se qualcosa manca, i dettagli sono in `.claude/hooks/.state/session-setup.log`; per ripetere la preparazione: `CLAUDE_SETUP_FORCE=1 bash .claude/hooks/session_start.sh`.

### Hook automatici (`.claude/settings.json`)

- Gli hook partono tramite `.claude/hooks/run.sh`, che usa il primo Python 3.9+ disponibile (`python3`, `python` o `py`). Se Python manca vengono saltati senza bloccare.
- Dopo ogni modifica un controllo rapido del file (sintassi, nomi non definiti, JSON/YAML validi). Se segnala un errore, correggilo subito.
- Nessun test automatico a fine turno: la build Gradle è troppo lenta per un hook: esegui tu le verifiche indicate in AGENTS.md e in `docs/engineering/development.md`.

### Skill

- In `.claude/skills/` ci sono le skill di progetto (`r8-analyzer`, `test-case-reviewer`) e 7 procedure di `addyosmani/agent-skills`, collegate al submodule che l'avvio della sessione scarica in automatico.

### Server MCP

- Il progetto non configura server MCP.
