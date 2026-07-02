#!/usr/bin/env bash
# deploy-portal.sh - Deploy portal frontend to S3

set -euo pipefail

BUCKET="${PORTAL_S3_BUCKET:-portal.smartcbwtf.com}"
CLOUDFRONT_DISTRIBUTION_ID="${PORTAL_CLOUDFRONT_DISTRIBUTION_ID:-${CLOUDFRONT_DISTRIBUTION_ID:-}}"
WEB_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Building portal..."
cd "$WEB_DIR"
npm run build

if grep -R "localhost:8080" -n ./dist >/dev/null; then
  echo "Portal build contains localhost:8080. Set VITE_API_BASE_URL for production builds."
  exit 1
fi

echo "Uploading assets (1-year cache)..."
# Upload assets with immutable cache
aws s3 sync ./dist/assets "s3://$BUCKET/assets" \
  --delete \
  --cache-control "public, max-age=31536000, immutable"

echo "Uploading HTML and root files (no cache)..."
# Upload root files (index.html, etc) with no cache
aws s3 sync ./dist "s3://$BUCKET" \
  --delete \
  --exclude "assets/*" \
  --cache-control "public, max-age=0, must-revalidate"

if [ -n "$CLOUDFRONT_DISTRIBUTION_ID" ]; then
  echo "Invalidating CloudFront cache..."
  aws cloudfront create-invalidation \
    --distribution-id "$CLOUDFRONT_DISTRIBUTION_ID" \
    --paths "/*"
else
  echo "Skipping CloudFront invalidation. Set PORTAL_CLOUDFRONT_DISTRIBUTION_ID to enable it."
fi

echo "Deployment complete."
echo "Visit: https://portal.smartcbwtf.com"
