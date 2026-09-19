#!/usr/bin/env bash
set -e

# 1. 加载部署根目录的私有配置。
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
set -a
. "$ROOT/.env"
set +a

# 2. 准备本项目网络，并重建后端容器。
sudo docker network inspect for-next-time_data >/dev/null 2>&1 || sudo docker network create for-next-time_data >/dev/null
sudo docker rm -f for-next-time-app >/dev/null 2>&1 || true
mkdir -p "$ROOT/files"

# 3. 启动 Java 容器；Java 端口不暴露公网，仅交给现有 Nginx 反向代理。
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
  -e FILE_PUBLIC_URL_PREFIX=/time/files \
  -v "$ROOT/backend/app.jar:/app/app.jar:ro" \
  -v "$ROOT/files:/app/files" \
  -w /app \
  eclipse-temurin:21.0.11_10-jre-noble \
  java -jar /app/app.jar --server.address=0.0.0.0 >/dev/null

sudo docker network connect zhizhi_edge for-next-time-app 2>/dev/null || true

# 4. 输出容器运行状态。
sudo docker ps --filter "name=for-next-time-app" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
