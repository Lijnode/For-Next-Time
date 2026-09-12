# 下次一定 / For Next Time

“下次一定”是一款私人内容收藏与快速检索工具，围绕“先留下 → 找得到 → 用得上”构建。

当前处于 V1.0 项目初始化阶段。产品、原型、UI、系统、数据库和 API 设计请见 [docs](docs)。

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

复制 `.env.example` 为 `.env`，再填写本地或部署环境的真实配置。`.env` 不应提交到版本控制。
