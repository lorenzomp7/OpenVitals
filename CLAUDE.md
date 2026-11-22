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

### Hook automatici (`.claude/settings.json`)

- Dopo ogni modifica un controllo rapido del file (sintassi, nomi non definiti, JSON/YAML validi). Se segnala un errore, correggilo subito.
- Nessun test automatico a fine turno: la build Gradle è troppo lenta per un hook: esegui tu le verifiche indicate in AGENTS.md e in `docs/engineering/development.md`.

### Skill

- In `.claude/skills/` ci sono le skill di progetto (`release`, `r8-analyzer`, `test-case-reviewer`) e 7 procedure di `addyosmani/agent-skills` collegate al submodule: se risultano vuote esegui `git submodule update --init tools/agent-skills`.

### Server MCP

- `.mcp.json` carica solo `playwriter` (browser, nessuna chiave).
- `vibetest` (richiede la chiave Google) e `container-use` (richiede Docker) sono in `.mcp.optional.json`: avvia Claude con `claude --mcp-config .mcp.optional.json` quando servono.
