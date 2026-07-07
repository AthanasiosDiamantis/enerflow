#!/bin/bash
set -e

PROJECT_DIR="$HOME/Entwicklung/SpringBoot/reference-project/enerflow"
cd "$PROJECT_DIR"

echo "Starte EnerFlow-Container..."
docker compose up -d

echo "Warte, bis das Dashboard erreichbar ist..."
MAX_TRIES=60
TRIES=0

until curl -s -o /dev/null "http://localhost:8080/dashboard"; do
    TRIES=$((TRIES + 1))
    if [ "$TRIES" -ge "$MAX_TRIES" ]; then
        echo "Fehler: Dashboard nach ${MAX_TRIES}s nicht erreichbar."
        echo "Prüfe die Logs mit: docker compose logs -f app"
        read -p "Drücke Enter zum Schließen..."
        exit 1
    fi
    sleep 1
done

echo "Dashboard bereit — öffne Browser."
open "http://localhost:8080/dashboard"