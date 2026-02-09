#!/bin/bash
# deploy-portal.sh - Deploy portal frontend to S3

set -e

BUCKET="portal.smartcbwtf.com"
WEB_DIR="$(dirname "$0")"

echo "🔨 Building portal..."
cd "$WEB_DIR"
npm run build

echo "📤 Uploading assets (1-year cache)..."
# Upload assets with immutable cache
aws s3 sync ./dist/assets "s3://$BUCKET/assets" \
  --delete \
  --cache-control "public, max-age=31536000, immutable"

echo "📤 Uploading HTML and root files (no cache)..."
# Upload root files (index.html, etc) with no cache
aws s3 sync ./dist "s3://$BUCKET" \
  --delete \
  --exclude "assets/*" \
  --cache-control "public, max-age=0, must-revalidate"

echo "✅ Deployment complete!"
echo "🌐 Visit: https://portal.smartcbwtf.com"
