#!/usr/bin/env bash
set -euo pipefail

# 1. 加载部署根目录的私有配置。
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [ ! -f "$ROOT/.env" ]; then
  echo "缺少 $ROOT/.env，请先从 .env.example 复制并填写数据库账号、密码。" >&2
  exit 1
fi

set -a
. "$ROOT/.env"
set +a

# 2. 首次启动时生成并持久化 JWT 密钥，后续重启不会让登录状态失效。
if [ -z "${FOR_NEXT_TIME_JWT_SECRET:-}" ]; then
  FOR_NEXT_TIME_JWT_SECRET="$(openssl rand -base64 48 | tr -d '\n')"
  printf '\nFOR_NEXT_TIME_JWT_SECRET=%s\n' "$FOR_NEXT_TIME_JWT_SECRET" >> "$ROOT/.env"
  export FOR_NEXT_TIME_JWT_SECRET
  echo "已生成并保存 JWT 密钥。"
fi

if [ ! -f "$ROOT/backend/app.jar" ]; then
  echo "缺少 backend/app.jar，请先在本地执行 deploy/build-release.ps1 并上传 release 内容。" >&2
  exit 1
fi

# 3. 创建私有数据网络，并确认可连接现有知止 Nginx 的共享边缘网络。
sudo docker network inspect for-next-time_data >/dev/null 2>&1 || sudo docker network create for-next-time_data >/dev/null
if ! sudo docker network inspect zhizhi_edge >/dev/null 2>&1; then
  echo "未发现 zhizhi_edge 网络，请先确认知止 Nginx 正在运行。" >&2
  exit 1
fi

sudo docker rm -f for-next-time-app >/dev/null 2>&1 || true
mkdir -p "$ROOT/files"

# 4. 启动 Java 容器；Java 端口不暴露公网，仅交给现有 Nginx 反向代理。
echo "正在启动 for-next-time-app..."
sudo docker run -d \
  --name for-next-time-app \
  --restart unless-stopped \
  --network for-next-time_data \
  --log-opt max-size=20m \
  --log-opt max-file=3 \
  -e TZ=Asia/Shanghai \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e MYSQL_HOST=for-next-time-mysql \
  -e MYSQL_PORT=3306 \
  -e MYSQL_DATABASE=for_next_time \
  -e "MYSQL_USER=$FOR_NEXT_TIME_DB_USERNAME" \
  -e "MYSQL_PASSWORD=$FOR_NEXT_TIME_DB_PASSWORD" \
  -e "JWT_SECRET=$FOR_NEXT_TIME_JWT_SECRET" \
  -e FILE_STORAGE_DIR=/app/files \
  -v "$ROOT/backend/app.jar:/app/app.jar:ro" \
  -v "$ROOT/files:/app/files" \
  -w /app \
  eclipse-temurin:21.0.11_10-jre-noble \
  java -jar /app/app.jar --server.address=0.0.0.0 >/dev/null

sudo docker network connect zhizhi_edge for-next-time-app 2>/dev/null || true

# 5. 等待 Spring Boot 启动日志，失败时输出日志帮助定位。
for _ in $(seq 1 30); do
  if sudo docker logs for-next-time-app 2>&1 | grep -q 'Started ForNextTimeApplication'; then
    sudo docker ps --filter "name=for-next-time-app" --format "table {{.Names}}\t{{.Status}}"
    exit 0
  fi
  sleep 2
done

echo "Java 服务未在预期时间内启动。最近日志：" >&2
sudo docker logs --tail 80 for-next-time-app >&2
exit 1
