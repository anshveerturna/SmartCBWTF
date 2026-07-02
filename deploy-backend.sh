#!/usr/bin/env bash
set -euo pipefail

: "${SMARTCBWTF_DEPLOY_HOST:?Set SMARTCBWTF_DEPLOY_HOST, for example ec2-user@your-host}"

KEY="${SMARTCBWTF_DEPLOY_KEY:-$HOME/.ssh/smartcbwtf-key.pem}"
HOST="$SMARTCBWTF_DEPLOY_HOST"
REMOTE_HOME="${SMARTCBWTF_REMOTE_HOME:-/home/ec2-user}"
LOCAL_JAR="${LOCAL_JAR:-backend/target/backend-0.0.1-SNAPSHOT.jar}"
DEPLOY_ENV="${DEPLOY_ENV:-staging}"

case "$DEPLOY_ENV" in
  staging|production) ;;
  *)
    echo "DEPLOY_ENV must be staging or production. Found: $DEPLOY_ENV"
    exit 1
    ;;
esac

if [ ! -f "$LOCAL_JAR" ]; then
  echo "Missing backend jar: $LOCAL_JAR"
  echo "Run: cd backend && mvn -DskipTests package"
  exit 1
fi

SSH_OPTS=(-i "$KEY" -o IdentitiesOnly=yes)

echo "Preparing remote directory..."
ssh "${SSH_OPTS[@]}" "$HOST" "mkdir -p '$REMOTE_HOME'"

echo "Copying backend jar and start script..."
scp "${SSH_OPTS[@]}" "$LOCAL_JAR" "$HOST:$REMOTE_HOME/backend.jar"
scp "${SSH_OPTS[@]}" deploy.sh "$HOST:$REMOTE_HOME/deploy.sh"

echo "Starting backend..."
REMOTE_ENV="APP_HOME=$(printf '%q' "$REMOTE_HOME") DEPLOY_ENV=$(printf '%q' "$DEPLOY_ENV")"
if [ "${DB_BACKUP_DIR+x}" ]; then
  REMOTE_ENV="$REMOTE_ENV DB_BACKUP_DIR=$(printf '%q' "$DB_BACKUP_DIR")"
fi
if [ "${JAVA_BIN+x}" ]; then
  REMOTE_ENV="$REMOTE_ENV JAVA_BIN=$(printf '%q' "$JAVA_BIN")"
fi
if [ "${SPRING_FLYWAY_OUT_OF_ORDER+x}" ]; then
  REMOTE_ENV="$REMOTE_ENV SPRING_FLYWAY_OUT_OF_ORDER=$(printf '%q' "$SPRING_FLYWAY_OUT_OF_ORDER")"
fi
ssh "${SSH_OPTS[@]}" "$HOST" "chmod +x '$REMOTE_HOME/deploy.sh' && $REMOTE_ENV '$REMOTE_HOME/deploy.sh'"

sleep 10
ssh "${SSH_OPTS[@]}" "$HOST" "pgrep -af 'java.*backend.jar'"
