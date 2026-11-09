#!/usr/bin/env bash
# 프론트 목업 데모를 S3 + CloudFront 에 올린다 (백엔드 없이 가짜 데이터로 동작하는 1차본).
#
#   FRONT_BUCKET=<S3 버킷> FRONT_DISTRIBUTION_ID=<CloudFront 배포 ID> scripts/deploy-frontend-demo.sh
#
# - index.html 은 매번 새로 받게(no-cache), 해시가 붙은 assets/* 는 1년 캐시
# - 올린 뒤 CloudFront 캐시를 비운다
set -euo pipefail

: "${FRONT_BUCKET:?FRONT_BUCKET 을 넣어 주세요}"
: "${FRONT_DISTRIBUTION_ID:?FRONT_DISTRIBUTION_ID 를 넣어 주세요}"

cd "$(dirname "$0")/../frontend"
npm run build:demo

aws s3 sync dist-demo/assets "s3://$FRONT_BUCKET/assets" --delete \
  --cache-control "public, max-age=31536000, immutable"
aws s3 sync dist-demo "s3://$FRONT_BUCKET" --delete --exclude "assets/*" \
  --cache-control "no-cache"

aws cloudfront create-invalidation --distribution-id "$FRONT_DISTRIBUTION_ID" --paths "/*" \
  --query 'Invalidation.Id' --output text
