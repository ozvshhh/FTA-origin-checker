#!/usr/bin/env bash
# Lightsail 인스턴스(Ubuntu 24.04) 첫 준비. 인스턴스에 SSH 로 들어가서 한 번 실행한다.
#
#   curl -fsSL https://raw.githubusercontent.com/<저장소>/<브랜치>/deploy/setup-instance.sh | bash   또는
#   git clone 후 bash deploy/setup-instance.sh
#
# - Docker · Compose 플러그인 설치(Ubuntu 패키지), 현재 사용자를 docker 그룹에 추가
# - 스왑 2GB (이미지를 서버에서 빌드할 때 메모리가 모자라지 않게)
set -euo pipefail

sudo apt-get update -y
sudo apt-get install -y docker.io docker-compose-v2 git
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"

if ! swapon --show | grep -q /swapfile; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab > /dev/null
fi

echo "완료. docker 그룹을 적용하려면 SSH 를 끊고 다시 접속하세요."
