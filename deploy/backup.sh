#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BACKUP_DIR="${EVENTFLOW_BACKUP_DIRECTORY:-$SCRIPT_DIR/backups}"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

mkdir -p "$BACKUP_DIR"
cd "$SCRIPT_DIR"

docker compose exec -T mysql sh -c \
  'exec mysqldump --single-transaction --quick --lock-tables=false -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
  > "$BACKUP_DIR/eventflow-$TIMESTAMP.sql"

echo "Database backup created: $BACKUP_DIR/eventflow-$TIMESTAMP.sql"
