#!/bin/bash
# deploy-website.sh - Deploy public website to S3 with smart caching

set -e

BUCKET="smartcbwtf.com"
WEBSITE_DIR="/Users/anshveerturna/Documents/SmartCBWTF/website"

echo "🔨 Building website..."
cd "$WEBSITE_DIR"
npm run build

echo "📤 Uploading static assets (1-year cache)..."
# CSS, JS, and _next assets with immutable cache
aws s3 sync ./out/_next "s3://$BUCKET/_next" \
  --delete \
  --cache-control "public, max-age=31536000, immutable"

echo "📤 Uploading images (1-year cache)..."
# Screenshots and other static images
aws s3 sync ./out/screenshots "s3://$BUCKET/screenshots" \
  --delete \
  --cache-control "public, max-age=31536000, immutable"

aws s3 sync ./out/clients "s3://$BUCKET/clients" \
  --delete \
  --cache-control "public, max-age=31536000, immutable"

echo "📤 Uploading HTML and root files (no cache)..."
# HTML files should always be fresh
aws s3 sync ./out "s3://$BUCKET" \
  --delete \
  --exclude "_next/*" \
  --exclude "screenshots/*" \
  --exclude "clients/*" \
  --cache-control "public, max-age=0, must-revalidate"

echo "🧹 Invalidating CloudFront cache..."
# Get CloudFront distribution ID (you may need to set this)
# aws cloudfront create-invalidation --distribution-id YOUR_DISTRIBUTION_ID --paths "/*"
echo "⚠️  Remember to run CloudFront invalidation manually or add distribution ID above"

echo "✅ Deployment complete!"
echo "🌐 Visit: https://smartcbwtf.com"
