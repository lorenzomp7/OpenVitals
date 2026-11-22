#!/usr/bin/env bash
# Attende che finisca un'installazione avviata in background da session_start.sh.
# Uso: bash .claude/hooks/wait_setup.sh NOME [MINUTI_MASSIMI]   (es. android, flutter)
# Exit 0 = pronto, 1 = fallito, 2 = ancora in corso allo scadere dell'attesa.
NAME=${1:?nome: android o flutter}
MAX=$(( ${2:-15} * 60 ))
STATE="${CLAUDE_PROJECT_DIR:-$(pwd)}/.claude/hooks/.state"
STATUS="$STATE/$NAME.status"
waited=0
while [ $waited -le $MAX ]; do
  s=$(cat "$STATUS" 2>/dev/null || echo "assente")
  case "$s" in
    ready*) echo "$NAME: $s"; exit 0 ;;
    failed*) echo "$NAME: $s"; echo "--- ultime righe di $STATE/$NAME.log"; tail -n 25 "$STATE/$NAME.log" 2>/dev/null; exit 1 ;;
    running)
      pid=$(cat "$STATE/$NAME.pid" 2>/dev/null)
      if [ -n "$pid" ] && ! kill -0 "$pid" 2>/dev/null; then
        echo "$NAME: il processo di installazione si è fermato senza finire."
        echo "Riavvialo con: CLAUDE_SETUP_FORCE=1 bash .claude/hooks/session_start.sh"
        tail -n 25 "$STATE/$NAME.log" 2>/dev/null; exit 1
      fi ;;
    *) echo "$NAME: nessuna installazione avviata. Avviala con: CLAUDE_SETUP_FORCE=1 bash .claude/hooks/session_start.sh"; exit 1 ;;
  esac
  [ $((waited + 10)) -gt $MAX ] && break
  sleep 10; waited=$((waited + 10))
done
echo "$NAME: ancora in corso dopo ${2:-15} minuti; log: $STATE/$NAME.log"
exit 2
