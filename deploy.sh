#!/usr/bin/env bash
set -euo pipefail

APP_HOME="${APP_HOME:-/home/ec2-user}"
JAR_PATH="${JAR_PATH:-$APP_HOME/backend.jar}"
LOG_PATH="${LOG_PATH:-$APP_HOME/backend.log}"
JAVA_BIN="${JAVA_BIN:-java}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"
DEPLOY_ENV="${DEPLOY_ENV:-staging}"
DB_BACKUP_DIR="${DB_BACKUP_DIR:-$APP_HOME/db-backups}"

if [ -f "$APP_HOME/.bashrc" ]; then
  # Load deployment secrets exported on the host.
  # shellcheck disable=SC1091
  set +u
  source "$APP_HOME/.bashrc" || true
  set -u
fi

DB_URL="${DB_URL:-${DATABASE_URL:-}}"
DB_USERNAME="${DB_USERNAME:-${DATABASE_USERNAME:-}}"
DB_PASSWORD="${DB_PASSWORD:-${DATABASE_PASSWORD:-}}"

read_external_yaml() {
  local file="$1"
  local path="$2"
  [ -f "$file" ] || return 1
  python3 - "$file" "$path" <<'PY'
import os
import re
import sys

file_path, wanted_path = sys.argv[1], sys.argv[2].split(".")
stack = []

try:
    lines = open(file_path, encoding="utf-8").read().splitlines()
except OSError:
    sys.exit(1)

for raw in lines:
    if not raw.strip() or raw.lstrip().startswith("#"):
        continue
    match = re.match(r"^(\s*)([^:#]+):(?:\s*(.*))?$", raw)
    if not match:
        continue
    indent = len(match.group(1))
    key = match.group(2).strip()
    value = (match.group(3) or "").strip()
    while stack and stack[-1][0] >= indent:
        stack.pop()
    current = [item[1] for item in stack] + [key]
    if current == wanted_path and value:
        if (value.startswith('"') and value.endswith('"')) or (value.startswith("'") and value.endswith("'")):
            value = value[1:-1]
        placeholder = re.fullmatch(r"\$\{([A-Za-z_][A-Za-z0-9_]*)(?::(.*))?\}", value)
        if placeholder:
            value = os.environ.get(placeholder.group(1), placeholder.group(2) or "")
        print(value)
        sys.exit(0)
    stack.append((indent, key))

sys.exit(1)
PY
}

set_default_from_external_config() {
  local var_name="$1"
  local yaml_path="$2"
  local current_value="${!var_name:-}"
  [ -z "$current_value" ] || return 0

  local config_file value
  for config_file in "$APP_HOME/application-prod.yml" "$APP_HOME/application.yml"; do
    value="$(read_external_yaml "$config_file" "$yaml_path" 2>/dev/null || true)"
    if [ -n "$value" ]; then
      export "$var_name=$value"
      return 0
    fi
  done
}

set_default_from_external_config DB_URL spring.datasource.url
set_default_from_external_config DB_USERNAME spring.datasource.username
set_default_from_external_config DB_PASSWORD spring.datasource.password
set_default_from_external_config JWT_SECRET security.jwt.secret
set_default_from_external_config APP_EMAIL_ENABLED app.email.enabled
set_default_from_external_config BREVO_API_KEY app.email.brevo.api-key

DB_URL="${DB_URL:-${DATABASE_URL:-}}"
DB_USERNAME="${DB_USERNAME:-${DATABASE_USERNAME:-}}"
DB_PASSWORD="${DB_PASSWORD:-${DATABASE_PASSWORD:-}}"

: "${DB_URL:?DB_URL or DATABASE_URL is required for deployment}"
: "${DB_USERNAME:?DB_USERNAME or DATABASE_USERNAME is required for deployment}"
: "${DB_PASSWORD:?DB_PASSWORD or DATABASE_PASSWORD is required for deployment}"
: "${JWT_SECRET:?JWT_SECRET is required for deployment}"

case "$DEPLOY_ENV" in
  staging|production) ;;
  *)
    echo "DEPLOY_ENV must be staging or production. Found: $DEPLOY_ENV"
    exit 1
    ;;
esac

export DB_URL DB_USERNAME DB_PASSWORD JWT_SECRET DEPLOY_ENV

# Force hardened production behavior even when the server has an older external
# application-prod.yml next to the jar.
export SPRING_JPA_HIBERNATE_DDL_AUTO="${SPRING_JPA_HIBERNATE_DDL_AUTO:-validate}"
export SPRING_FLYWAY_ENABLED="${SPRING_FLYWAY_ENABLED:-true}"
export SPRING_FLYWAY_IGNORE_MISSING_MIGRATIONS="${SPRING_FLYWAY_IGNORE_MISSING_MIGRATIONS:-false}"
export SPRING_FLYWAY_VALIDATE_ON_MIGRATE="${SPRING_FLYWAY_VALIDATE_ON_MIGRATE:-true}"
export SPRINGDOC_API_DOCS_ENABLED="${SPRINGDOC_API_DOCS_ENABLED:-false}"
export SPRINGDOC_SWAGGER_UI_ENABLED="${SPRINGDOC_SWAGGER_UI_ENABLED:-false}"
export EXPOSE_API_DOCS="${EXPOSE_API_DOCS:-false}"
export MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE="${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE:-health}"
export MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS="${MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS:-never}"

EMAIL_ENABLED_NORMALIZED="$(printf '%s' "${APP_EMAIL_ENABLED:-true}" | tr '[:upper:]' '[:lower:]')"
if [ "$EMAIL_ENABLED_NORMALIZED" != "false" ] && [ -z "${BREVO_API_KEY:-}" ]; then
  echo "BREVO_API_KEY is required when production email is enabled. Set BREVO_API_KEY or APP_EMAIL_ENABLED=false."
  exit 1
fi

for optional_env in \
  APP_EMAIL_ENABLED \
  BREVO_API_KEY \
  CORS_ALLOWED_ORIGINS \
  EXPOSE_API_DOCS \
  SPRINGDOC_API_DOCS_ENABLED \
  SPRINGDOC_SWAGGER_UI_ENABLED \
  INITIAL_SUPER_ADMIN_USERNAME \
  INITIAL_SUPER_ADMIN_PASSWORD \
  INITIAL_SUPER_ADMIN_EMAIL \
  INITIAL_SUPER_ADMIN_FULL_NAME
do
  if [ "${!optional_env+x}" ]; then
    export "$optional_env"
  fi
done

JAVA_VERSION_OUTPUT="$("$JAVA_BIN" -version 2>&1 | head -n 1)"
JAVA_MAJOR="$(printf '%s\n' "$JAVA_VERSION_OUTPUT" | sed -E 's/.*version "([0-9]+).*/\1/')"
if [ "$JAVA_MAJOR" != "21" ]; then
  echo "Java 21 LTS is required for staging/production runtime. Found: $JAVA_VERSION_OUTPUT"
  exit 1
fi

cd "$APP_HOME"

backup_database() {
  if ! command -v pg_dump >/dev/null 2>&1; then
    echo "pg_dump is required to create a pre-migration DB backup."
    exit 1
  fi

  mkdir -p "$DB_BACKUP_DIR"
  chmod 700 "$DB_BACKUP_DIR"

  local pg_url="$DB_URL"
  case "$pg_url" in
    jdbc:postgresql://*)
      pg_url="postgresql://${pg_url#jdbc:postgresql://}"
      ;;
  esac

  local timestamp backup_path
  timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
  backup_path="$DB_BACKUP_DIR/${DEPLOY_ENV}-smartcbwtf-${timestamp}.dump"

  echo "Creating pre-migration DB backup: $backup_path"
  if PGPASSWORD="$DB_PASSWORD" pg_dump \
    --format=custom \
    --no-owner \
    --no-privileges \
    --dbname="$pg_url" \
    --username="$DB_USERNAME" \
    --file="$backup_path"; then
    chmod 600 "$backup_path"
    echo "DB backup complete: $backup_path"
    return
  fi

  rm -f "$backup_path"

  local db_name
  db_name="$(printf '%s' "$pg_url" | sed -E 's#^postgresql://([^/]+/)?([^?]+).*#\2#')"
  if [ -z "$db_name" ] || [ "$db_name" = "$pg_url" ]; then
    echo "DB backup failed and database name could not be parsed for local postgres fallback."
    exit 1
  fi
  if ! command -v sudo >/dev/null 2>&1; then
    echo "DB backup failed and sudo is unavailable for local postgres fallback."
    exit 1
  fi

  local temp_backup
  temp_backup="/tmp/$(basename "$backup_path")"
  rm -f "$temp_backup"
  echo "Password backup failed; trying local postgres backup fallback."
  sudo -n -u postgres pg_dump \
    --format=custom \
    --no-owner \
    --no-privileges \
    --dbname="$db_name" \
    --file="$temp_backup"
  sudo chown "$(id -u):$(id -g)" "$temp_backup"
  mv "$temp_backup" "$backup_path"
  chmod 600 "$backup_path"
  echo "DB backup complete: $backup_path"
}

backup_database

pkill -TERM -f "java.*$(basename "$JAR_PATH")" 2>/dev/null || true
sleep 5
pkill -KILL -f "java.*$(basename "$JAR_PATH")" 2>/dev/null || true

nohup "$JAVA_BIN" $JAVA_OPTS -jar "$JAR_PATH" --spring.profiles.active=prod > "$LOG_PATH" 2>&1 &
echo "PID=$!"
