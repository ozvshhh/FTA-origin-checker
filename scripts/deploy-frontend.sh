#!/usr/bin/env bash
# 프론트(실제 API 모드)를 S3 + CloudFront 에 올린다. API 는 frontend/.env.cloudfront 의 백엔드를 부른다.
#
#   FRONT_BUCKET=<S3 버킷> FRONT_DISTRIBUTION_ID=<CloudFront 배포 ID> scripts/deploy-frontend.sh
#
# 백엔드 deploy/.env 의 CORS_ALLOWED_ORIGINS 에 CloudFront 주소(https://xxxx.cloudfront.net)가 있어야 한다.
# 가짜 데이터 데모를 올릴 때는 scripts/deploy-frontend-demo.sh
set -euo pipefail

: "${FRONT_BUCKET:?FRONT_BUCKET 을 넣어 주세요}"
: "${FRONT_DISTRIBUTION_ID:?FRONT_DISTRIBUTION_ID 를 넣어 주세요}"

cd "$(dirname "$0")/../frontend"
npm run build:cloudfront

aws s3 sync dist-cloudfront/assets "s3://$FRONT_BUCKET/assets" --delete \
  --cache-control "public, max-age=31536000, immutable"
aws s3 sync dist-cloudfront "s3://$FRONT_BUCKET" --delete --exclude "assets/*" \
  --cache-control "no-cache"

aws cloudfront create-invalidation --distribution-id "$FRONT_DISTRIBUTION_ID" --paths "/*" \
  --query 'Invalidation.Id' --output text
