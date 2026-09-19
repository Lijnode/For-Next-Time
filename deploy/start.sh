#!/usr/bin/env bash
set -euo pipefail

# 1. 定位部署根目录。
ROOT="$(cd "$(dirname "$0")" && pwd)"

# 2. 首次启动时生成并持久化 JWT 密钥，后续重启不会让登录状态失效。
set -a
. "$ROOT/.env"
set +a

if [ -z "${FOR_NEXT_TIME_JWT_SECRET:-}" ]; then
  FOR_NEXT_TIME_JWT_SECRET="$(openssl rand -base64 48 | tr -d '\n')"
  printf '\nFOR_NEXT_TIME_JWT_SECRET=%s\n' "$FOR_NEXT_TIME_JWT_SECRET" >> "$ROOT/.env"
fi

# 3. 按数据库、后端的顺序启动本项目容器。
bash "$ROOT/tools/mysql/start.sh"
echo
bash "$ROOT/backend/app.sh"
echo

echo "下次一定后端已启动。Nginx 由现有知止容器统一提供。"
