#!/usr/bin/env bash
# deploy-website.sh - Deploy public website to S3 with smart caching

set -euo pipefail

BUCKET="${WEBSITE_S3_BUCKET:-smartcbwtf.com}"
CLOUDFRONT_DISTRIBUTION_ID="${WEBSITE_CLOUDFRONT_DISTRIBUTION_ID:-${CLOUDFRONT_DISTRIBUTION_ID:-}}"
WEBSITE_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "Building website..."
cd "$WEBSITE_DIR"
npm run build

echo "Uploading static assets (1-year cache)..."
# CSS, JS, and _next assets with immutable cache
aws s3 sync ./out/_next "s3://$BUCKET/_next" \
  --delete \
  --cache-control "public, max-age=31536000, immutable"

echo "Uploading images (1-year cache)..."
# Screenshots and other static images
aws s3 sync ./out/screenshots "s3://$BUCKET/screenshots" \
  --delete \
  --cache-control "public, max-age=31536000, immutable"

aws s3 sync ./out/clients "s3://$BUCKET/clients" \
  --delete \
  --cache-control "public, max-age=31536000, immutable"

echo "Uploading HTML and root files (no cache)..."
# HTML files should always be fresh
aws s3 sync ./out "s3://$BUCKET" \
  --delete \
  --exclude "_next/*" \
  --exclude "screenshots/*" \
  --exclude "clients/*" \
  --cache-control "public, max-age=0, must-revalidate"

echo "Uploading extensionless route aliases..."
# CloudFront uses the S3 REST origin, so /contact does not automatically map to /contact.html.
# Keep the public navigation URLs extensionless by publishing each exported route at both keys.
for route in about compliance contact features login platform security; do
  aws s3 cp "./out/$route.html" "s3://$BUCKET/$route" \
    --content-type "text/html" \
    --cache-control "public, max-age=0, must-revalidate"
done

if [ -n "$CLOUDFRONT_DISTRIBUTION_ID" ]; then
  echo "Invalidating CloudFront cache..."
  aws cloudfront create-invalidation \
    --distribution-id "$CLOUDFRONT_DISTRIBUTION_ID" \
    --paths "/*"
else
  echo "Skipping CloudFront invalidation. Set WEBSITE_CLOUDFRONT_DISTRIBUTION_ID to enable it."
fi

echo "Deployment complete."
echo "Visit: https://smartcbwtf.com"
