#!/usr/bin/env bash
set -euo pipefail

# 1. 加载数据库账号密码，并准备项目私有网络与数据卷。
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
set -a
. "$ROOT/.env"
set +a

sudo docker network inspect for-next-time_data >/dev/null 2>&1 || sudo docker network create for-next-time_data >/dev/null
sudo docker volume create for-next-time_mysql-data >/dev/null
sudo docker rm -f for-next-time-mysql >/dev/null 2>&1 || true

# 2. 启动 MySQL；root 与应用账号使用独立初始化密码。
echo "正在启动 for-next-time-mysql..."
sudo docker run -d \
  --name for-next-time-mysql \
  --restart unless-stopped \
  --network for-next-time_data \
  --log-opt max-size=20m \
  --log-opt max-file=3 \
  --health-cmd='mysqladmin ping -h 127.0.0.1 --silent' \
  --health-interval=5s \
  --health-timeout=3s \
  --health-retries=30 \
  --health-start-period=20s \
  -e TZ=Asia/Shanghai \
  -e "MYSQL_ROOT_PASSWORD=$FOR_NEXT_TIME_DB_ROOT_PASSWORD" \
  -e MYSQL_DATABASE=for_next_time \
  -e "MYSQL_USER=$FOR_NEXT_TIME_DB_USERNAME" \
  -e "MYSQL_PASSWORD=$FOR_NEXT_TIME_DB_PASSWORD" \
  -v for-next-time_mysql-data:/var/lib/mysql \
  mysql:8.4.10 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_0900_ai_ci >/dev/null

# 3. 等待数据库健康后再允许 Java 服务连接。
for _ in $(seq 1 30); do
  STATUS=$(sudo docker inspect -f '{{.State.Health.Status}}' for-next-time-mysql)
  if [ "$STATUS" = "healthy" ]; then
    sudo docker ps --filter "name=for-next-time-mysql" --format "table {{.Names}}\t{{.Status}}"
    exit 0
  fi
  sleep 2
done

echo "MySQL 未在预期时间内就绪。最近日志：" >&2
sudo docker logs --tail 80 for-next-time-mysql >&2
exit 1
