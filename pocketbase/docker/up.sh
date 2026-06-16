#!/usr/bin/env bash
# FunLife Docker 一键启动（Linux / macOS）
set -euo pipefail

PUSH=0
TUNNEL=0
BUILD=0
LOGS=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --push) PUSH=1; shift ;;
    --tunnel) TUNNEL=1; shift ;;
    --build) BUILD=1; shift ;;
    --logs) LOGS=1; shift ;;
    -h|--help)
      echo "Usage: $0 [--push] [--tunnel] [--build] [--logs]"
      exit 0
      ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

cd "$(dirname "$0")/.."

if ! command -v docker >/dev/null 2>&1; then
  echo "请先安装 Docker: https://docs.docker.com/engine/install/"
  exit 1
fi

if [[ ! -f .env && -f docker/.env.example ]]; then
  cp docker/.env.example .env
  echo "已创建 .env（请按需修改）"
fi

profiles=()
[[ "$PUSH" -eq 1 ]] && profiles+=(--profile push)
[[ "$TUNNEL" -eq 1 ]] && profiles+=(--profile tunnel)

if [[ "$PUSH" -eq 1 && ! -f secrets/firebase-adminsdk.json ]]; then
  echo "缺少 secrets/firebase-adminsdk.json"
  exit 1
fi

if [[ "$TUNNEL" -eq 1 && ! -f docker/cloudflared/config.yml ]]; then
  echo "缺少 docker/cloudflared/config.yml"
  exit 1
fi

cmd=(docker compose "${profiles[@]}")
[[ "$BUILD" -eq 1 ]] && cmd+=(build)
cmd+=(up -d)

echo "Running: ${cmd[*]}"
"${cmd[@]}"

sleep 3
echo ""
echo "健康检查:"
curl -fsS "http://127.0.0.1:8090/api/health" >/dev/null && echo "  PocketBase :8090 OK" || echo "  PocketBase :8090 未就绪"
curl -fsS "http://127.0.0.1:8790/health" >/dev/null && echo "  draw_ws       :8790 OK" || echo "  draw_ws       :8790 未就绪"
curl -fsS "http://127.0.0.1:8791/health" >/dev/null && echo "  pac-maze-ws   :8791 OK" || echo "  pac-maze-ws   :8791 未就绪"

if [[ "$LOGS" -eq 1 ]]; then
  docker compose "${profiles[@]}" logs -f
fi
