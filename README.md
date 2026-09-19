# 下次一定 / For Next Time

“下次一定”是一款私人内容收藏与快速检索工具，围绕“先留下 → 找得到 → 用得上”构建。

V1.0 已完成 PC Web、移动 H5 与后端服务。产品、原型、UI、系统、数据库和 API 设计请见 [docs](docs)。

## 项目结构

```text
for-next-time-pc/       PC Web：Vue 3 + Vite + TypeScript + Element Plus
for-next-time-h5/       移动 H5：Vue 3 + Vite + TypeScript + Vant 4
for-next-time-server/   后端：Spring Boot 3 + Java 21 + MyBatis
deploy/                 Docker、Nginx 与部署配置
docs/                   已评审的产品与设计文档
```

## Git 策略

- `dev` 推送至 Gitee 私有仓库，保存完整开发历史。
- `main` 推送至 GitHub 公开仓库，仅保存筛选后的公开发布代码。
- `main` 不合并 `dev`；详细规则见 [AGENTS.md](AGENTS.md)。

## 配置

开发环境的后端配置位于 `for-next-time-server/src/main/resources/application-dev.yml`。生产环境不要修改或提交密码，统一通过 `deploy/.env` 注入。

## 服务器部署

本项目按服务器上“知止”的成熟方式部署：本地构建、上传发布包、执行启动脚本。服务器只需要 Docker；不需要安装 Java、Maven、Node.js 或第二个 Nginx。

现有的知止 Nginx 是服务器唯一对外入口，继续占用 80/443。“下次一定”只新增两个容器：

| 容器 | 作用 | 是否对公网开放 |
| --- | --- | --- |
| `for-next-time-mysql` | 保存账号、内容、标签等数据 | 否 |
| `for-next-time-app` | 运行 Spring Boot、Flyway 与头像文件服务 | 否，仅由现有 Nginx 访问 |

### 本地生成发布包

在项目根目录的 PowerShell 执行：

```powershell
.\deploy\build-release.ps1
```

它会构建 PC、H5 和 Java JAR，并生成 `deploy/release/`。将 **release 目录内的内容** 上传到服务器 `/data/for-next-time/`。

### 服务器首次启动

```bash
cd /data/for-next-time
cp .env.example .env
nano .env
chmod +x start.sh backend/app.sh tools/mysql/start.sh
./start.sh
```

`.env` 只需要填写：

```env
FOR_NEXT_TIME_DB_USERNAME=for_next_time_app
FOR_NEXT_TIME_DB_PASSWORD=设置一个强密码
```

JWT 密钥由首次 `./start.sh` 自动生成并保存；MySQL root 初始化密码复用同一份数据库密码，无需重复填写。

MySQL 数据位于 Docker 卷 `for-next-time_mysql-data`，头像文件位于 `/data/for-next-time/files/`；两者都应纳入服务器备份。

### Nginx 接入

下次一定暂未配置独立域名，先复用知止现有的 HTTPS 域名，以路径区分应用：知止使用 `/zhizhi/`，下次一定 PC 使用 `/time/`，移动 H5 使用 `/time/m/`。将 `tools/nginx/for-next-time.server.conf.template` 中的 location 块合并到知止现有的 HTTPS server，并在知止的 Nginx 启动脚本中增加：

```bash
-v /data/for-next-time/pc:/usr/share/nginx/html/time:ro
-v /data/for-next-time/h5:/usr/share/nginx/html/time/m:ro
```

下次一定的 API 与头像文件入口分别为 `/time/api/`、`/time/files/`，由同一 Nginx 去除 `/time` 前缀后反向代理到后端，因此不会与知止根路径的 `/api/` 规则冲突。届时只会重启 Nginx 容器，Java 与 MySQL 容器无需变动。

不要开放 MySQL 的 3306 或 Java 服务的 6688；只由现有 Nginx 暴露 HTTPS。
