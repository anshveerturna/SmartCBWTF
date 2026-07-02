#!/usr/bin/env bash
set -euo pipefail

APP_HOME="${APP_HOME:-/home/ec2-user}"
JAR_PATH="${JAR_PATH:-$APP_HOME/backend.jar}"
LOG_PATH="${LOG_PATH:-$APP_HOME/backend.log}"
JAVA_BIN="${JAVA_BIN:-java}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"

if [ -f "$APP_HOME/.bashrc" ]; then
  # Load deployment secrets exported on the host.
  # shellcheck disable=SC1091
  source "$APP_HOME/.bashrc"
fi

DB_URL="${DB_URL:-${DATABASE_URL:-}}"
DB_USERNAME="${DB_USERNAME:-${DATABASE_USERNAME:-}}"
DB_PASSWORD="${DB_PASSWORD:-${DATABASE_PASSWORD:-}}"

: "${DB_URL:?DB_URL or DATABASE_URL is required for prod deployment}"
: "${DB_USERNAME:?DB_USERNAME or DATABASE_USERNAME is required for prod deployment}"
: "${DB_PASSWORD:?DB_PASSWORD or DATABASE_PASSWORD is required for prod deployment}"
: "${JWT_SECRET:?JWT_SECRET is required for prod deployment}"

export DB_URL DB_USERNAME DB_PASSWORD JWT_SECRET

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
if ! [[ "$JAVA_MAJOR" =~ ^[0-9]+$ ]] || [ "$JAVA_MAJOR" -lt 21 ]; then
  echo "Java 21 or newer is required to run the backend jar. Found: $JAVA_VERSION_OUTPUT"
  exit 1
fi

cd "$APP_HOME"

pkill -TERM -f "java.*$(basename "$JAR_PATH")" 2>/dev/null || true
sleep 5
pkill -KILL -f "java.*$(basename "$JAR_PATH")" 2>/dev/null || true

nohup "$JAVA_BIN" $JAVA_OPTS -jar "$JAR_PATH" --spring.profiles.active=prod > "$LOG_PATH" 2>&1 &
echo "PID=$!"
