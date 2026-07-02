#!/usr/bin/env bash
set -euo pipefail

: "${SMARTCBWTF_DEPLOY_HOST:?Set SMARTCBWTF_DEPLOY_HOST, for example ec2-user@your-host}"

KEY="${SMARTCBWTF_DEPLOY_KEY:-$HOME/.ssh/smartcbwtf-key.pem}"
HOST="$SMARTCBWTF_DEPLOY_HOST"
REMOTE_HOME="${SMARTCBWTF_REMOTE_HOME:-/home/ec2-user}"
LOCAL_JAR="${LOCAL_JAR:-backend/target/backend-0.0.1-SNAPSHOT.jar}"

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
ssh "${SSH_OPTS[@]}" "$HOST" "chmod +x '$REMOTE_HOME/deploy.sh' && APP_HOME='$REMOTE_HOME' '$REMOTE_HOME/deploy.sh'"

sleep 10
ssh "${SSH_OPTS[@]}" "$HOST" "pgrep -af 'java.*backend.jar'"
