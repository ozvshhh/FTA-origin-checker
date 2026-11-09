#!/usr/bin/env bash
# 내 컴퓨터에서 Lightsail 인스턴스로 배포한다: 인스턴스에서 브랜치를 받아 이미지를 다시 빌드하고 띄운다.
#
#   LIGHTSAIL_HOST=ubuntu@<고정IP> [LIGHTSAIL_KEY=~/.ssh/lightsail.pem] [DEPLOY_BRANCH=main] scripts/deploy-lightsail.sh
#
# 인스턴스에는 미리 저장소를 ~/trade-doc-origin-check 에 clone 하고 deploy/.env 를 채워 두어야 한다(docs/배포_Lightsail.md).
set -euo pipefail

: "${LIGHTSAIL_HOST:?LIGHTSAIL_HOST 를 넣어 주세요 (예: ubuntu@3.35.1.2)}"
BRANCH="${DEPLOY_BRANCH:-main}"
APP_DIR="${APP_DIR:-~/trade-doc-origin-check}"
SSH_OPTS=()
[ -n "${LIGHTSAIL_KEY:-}" ] && SSH_OPTS+=(-i "$LIGHTSAIL_KEY")

ssh "${SSH_OPTS[@]}" "$LIGHTSAIL_HOST" bash -s -- "$BRANCH" "$APP_DIR" <<'REMOTE'
set -euo pipefail
BRANCH="$1"; APP_DIR="${2/#\~/$HOME}"
cd "$APP_DIR"
test -f deploy/.env || { echo "deploy/.env 가 없습니다. deploy/.env.prod.example 을 복사해 채워 주세요."; exit 1; }
git fetch --quiet origin "$BRANCH"
git checkout --quiet "$BRANCH"
git reset --quiet --hard "origin/$BRANCH"
echo "배포: $(git log --oneline -1)"
docker compose -f deploy/compose.prod.yaml up -d --build --remove-orphans
docker image prune -f > /dev/null
docker compose -f deploy/compose.prod.yaml ps
REMOTE
