#!/usr/bin/env bash
set -euo pipefail

# 1. 定位部署根目录。
ROOT="$(cd "$(dirname "$0")" && pwd)"

# 2. 按数据库、后端的顺序启动本项目容器。
bash "$ROOT/tools/mysql/start.sh"
echo
bash "$ROOT/backend/app.sh"
echo

echo "下次一定后端已启动。Nginx 由现有知止容器统一提供。"
