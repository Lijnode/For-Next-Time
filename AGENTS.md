# AGENTS.md

> 项目名称：下次一定 / For Next Time  
> 产品版本：V1.0 MVP  
> 本文件作用：约束所有 Coding Agent、AI 编程助手和自动化代码生成工具的开发行为。  
> 适用对象：Codex、Claude Code、Cursor、Copilot、Qoder、DeepSeek 等。  
> 优先级：项目级最高开发约束之一。若与临时代码生成习惯冲突，以本文件和 `docs/` 下已评审文档为准。

---

# 1. 项目目标

“下次一定”是一款私人内容收藏与快速检索工具。

核心产品闭环：

```text
先留下
→ 找得到
→ 用得上
```

V1.0 的核心能力：

- 用户注册
- 用户登录
- 修改密码
- 新增内容
- 内容列表
- 内容详情
- 编辑内容
- 删除内容
- 固定分类
- 自由标签
- 关键词搜索
- 一键复制
- 复制次数统计
- 最近使用
- 置顶

V1.0 不追求功能数量，不允许为了“完整”或“未来扩展”擅自增加需求外功能。

---

# 2. 开发前必须阅读的文档

任何 Coding Agent 在修改代码前，必须先阅读：

```text
docs/01-下次一定-V1.0-产品需求文档.md
docs/02-下次一定-V1.0-高保真原型设计文档.md
docs/03-下次一定-V1.0-UI设计规范.md
docs/04-下次一定-V1.0-系统设计文档.md
docs/05-下次一定-V1.0-数据库设计文档.md
docs/06-下次一定-V1.0-API设计文档.md
```

涉及具体页面时，还应参考：

```text
docs/prototypes/pc-web.html
docs/prototypes/mobile-h5.html
```

如果仓库实际原型文件名不同，应先定位对应文件后再开发。

---

# 3. 文档优先级

发生冲突时，按以下优先级处理：

```text
1. 用户当前明确指令
2. 已评审的最新版产品需求文档
3. 系统设计文档
4. 数据库设计文档
5. API 设计文档
6. UI 设计规范
7. 高保真原型设计文档
8. 当前代码实现
```

如果代码与文档冲突：

> 默认认为代码需要调整，而不是擅自修改文档来迁就代码。

如果多个文档之间存在明显冲突：

> 停止扩大修改范围，明确指出冲突，等待评审后再继续。

不要自行猜测“哪个应该算”。

---

# 4. 最重要的开发流程

所有开发严格遵循：

```text
先读文档
→ 再看现有代码
→ 理解当前实现
→ 给出最小变更方案
→ 再编码
→ 自检
→ 汇报改动
```

禁止：

```text
看到需求
→ 不看现有代码
→ 直接新建一套实现
```

---

# 5. 设计先于编码

本项目明确要求：

> 设计文档评审通过后再编码。

如果一个需求会导致以下任何变化：

- 新增数据库表
- 新增数据库字段
- 修改字段含义
- 修改 API 路径
- 修改 API 请求结构
- 修改 API 响应结构
- 修改核心业务规则
- 修改技术栈
- 修改 PC/H5 双前端策略
- 新增中间件
- 新增模块
- 新增 V1 范围外功能

必须先修改对应设计文档并完成评审。

未经评审：

> 不得直接编码。

---

# 6. 项目总体架构

V1.0 使用：

```text
PC Web：Vue 3 + Vite + TypeScript + Element Plus
                         │
                         │
                         ├──────┐
                         │      │
移动 H5：Vue 3 + Vite + TypeScript + Vant 4
                         │      │
                         └──┬───┘
                            ↓
                     Spring Boot 3
                       + Java 21
                            ↓
                         MyBatis
                            ↓
                         MySQL 8
```

总原则：

```text
双前端
+
单后端
+
单数据库
```

---

# 7. 项目目录约定

建议项目结构：

```text
for-next-time/
├── docs/
├── for-next-time-pc/
├── for-next-time-h5/
├── for-next-time-server/
├── deploy/
├── AGENTS.md
├── .gitignore
└── README.md
```

不要擅自：

- 拆成多个 Git 仓库
- 合并 PC 与 H5 为同一前端工程
- 创建第二套后端工程

---

# 8. V1.0 技术栈锁定

## PC Web

```text
Vue 3
Vite
TypeScript
Vue Router
Pinia
Axios
Element Plus
Lucide Icons
```

## 移动 H5

```text
Vue 3
Vite
TypeScript
Vue Router
Pinia
Axios
Vant 4
Lucide Icons
```

## 后端

```text
Java 21
Spring Boot 3.x
Spring Web
Spring Validation
MyBatis
MySQL 8
Flyway
JWT
BCrypt
Lombok
SLF4J + Logback
Maven
```

## 部署

```text
Docker
Docker Compose
Nginx
HTTPS
```

---

# 9. V1.0 禁止擅自引入的技术

未经设计评审，不得增加：

```text
Redis
Elasticsearch
RabbitMQ
Kafka
RocketMQ
Spring Cloud
Nacos
Sentinel
WebSocket
MongoDB
MinIO
OSS
COS
向量数据库
AI SDK
OCR SDK
复杂 RBAC
多租户框架
后台管理系统
```

不要因为“以后可能会用”提前引入。

---

# 10. 核心产品边界

V1.0 明确不做：

- AI 自动生成
- AI 改写
- AI 自动标签
- AI 语义搜索
- 相似文案
- OCR
- 图片上传
- 来源平台
- 原始链接
- 社区
- 点赞
- 评论
- 关注
- 私信
- 推荐流
- 搜索历史
- 数据导入
- 数据导出
- 回收站 UI
- 二次收藏
- 星标
- 多层文件夹
- 富文本
- Markdown 编辑器
- 原生 App
- 微信小程序

移动 H5 只需要：

> 为未来小程序迁移降低成本。

不代表 V1 要开发小程序。

---

# 11. 核心业务对象

系统内部统一使用：

```text
Content
```

表示用户保存的一条内容。

内容字段：

```text
正文
分类
标签
备注
```

系统字段：

```text
所属用户
是否置顶
复制次数
最近复制时间
创建时间
修改时间
逻辑删除
```

明确没有：

```text
title
source_platform
source_url
```

任何 Agent 不得重新添加这些字段。

---

# 12. 分类固定

V1.0 固定分类：

```text
FUNNY      搞笑
ABSTRACT   抽象
EMOTION    感情
LITERARY   文艺
HEALING    治愈
SOBER      清醒
SARCASM    阴阳
OTHER      其他
```

规则：

- 一条内容最多一个分类
- 可以不分类
- 不分类使用 `NULL`
- `OTHER` 表示明确选择“其他”

不得：

- 新建 category 表
- 增加自定义分类
- 增加子分类
- 增加分类管理页面

---

# 13. 标签规则

标签：

- 用户自由创建
- 一条内容多个标签
- 单条内容最多 10 个
- 单个标签 1～20 字符
- 同一用户下同名标签唯一
- 不同用户允许相同标签名

新增/编辑内容时：

> 前端提交 `tagNames`。

后端负责：

```text
trim
→ 去空
→ 去重
→ 查询已有标签
→ 不存在则创建
→ 建立关系
```

不要要求前端先创建标签再提交 tagId。

---

# 14. 数据库结构锁定

V1.0 核心表只有：

```text
user_account
content
tag
content_tag
```

未经设计评审不得：

- 新增表
- 删除表
- 改字段
- 改索引
- 改字段业务含义

所有数据库结构变化：

> 必须通过 Flyway Migration。

已执行的历史 Migration：

> 不允许修改。

---

# 15. 数据库主键

主要实体统一：

```text
BIGINT UNSIGNED AUTO_INCREMENT
```

不要擅自改成：

- UUID
- Snowflake ID

---

# 16. 数据权限是硬约束

所有用户业务数据必须严格按当前登录用户隔离。

前端：

> 不允许通过 userId 决定数据归属。

后端：

```text
JWT
→ UserContext
→ 当前 userId
```

所有内容详情、更新、删除、置顶、复制必须包含：

```sql
id = ?
AND user_id = ?
AND deleted = 0
```

禁止：

```sql
WHERE id = ?
```

直接操作私人内容。

---

# 17. 登录认证

V1 使用：

```text
JWT Bearer Token
```

Header：

```http
Authorization: Bearer <token>
```

Token 有效期：

```text
7 天
```

V1：

- 不做 Refresh Token
- 不做 Redis Session
- 不做 Token 黑名单
- 不做后端 logout API

退出登录：

> 前端本地清 Token。

---

# 18. 密码规则

密码：

```text
8～64 字符
```

后端使用：

```text
BCrypt
```

数据库只保存：

```text
password_hash
```

严禁：

- 明文密码入库
- 日志打印密码
- API 返回 passwordHash

---

# 19. API 前缀

业务接口：

```text
/api/v1
```

健康检查：

```text
/api/health
```

PC Web 和移动 H5：

> 必须共用同一套 API。

禁止设计：

```text
/api/v1/pc/*
/api/v1/h5/*
```

两套重复接口。

---

# 20. API 接口锁定

V1.0 业务接口：

```text
POST   /api/v1/auth/register
POST   /api/v1/auth/login

GET    /api/v1/user/profile
PUT    /api/v1/user/password

POST   /api/v1/contents
GET    /api/v1/contents
GET    /api/v1/contents/{id}
PUT    /api/v1/contents/{id}
DELETE /api/v1/contents/{id}

GET    /api/v1/contents/search
GET    /api/v1/contents/recent
POST   /api/v1/contents/{id}/copy
PUT    /api/v1/contents/{id}/pin

GET    /api/v1/tags
```

不得擅自修改路径或 Method。

---

# 21. 统一 API 响应

统一：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

分页统一：

```json
{
  "pageNum": 1,
  "pageSize": 20,
  "total": 0,
  "pages": 0,
  "list": []
}
```

无数组数据：

```json
[]
```

不要返回：

```json
null
```

可选单值无数据：

```json
null
```

---

# 22. 后端分层

领域目录：

```text
auth/
user/
content/
tag/
```

每个领域按需包含：

```text
controller/
service/
mapper/
entity/
req/
res/
convert/
```

职责：

## Controller

只负责：

- 接参
- 参数校验
- 调 Service
- 返回结果

## Service

负责：

- 业务规则
- 权限
- 事务
- 数据组装

## Mapper

只负责：

- SQL
- CRUD

禁止 Controller 直接调用 Mapper。

---

# 23. Entity 不直接暴露

禁止长期：

```java
return entity;
```

作为 API 响应。

接口必须使用：

```text
Req DTO
Res DTO
```

原因：

Entity 可能包含：

- userId
- deleted
- 数据库表现形式

这些不应该直接暴露给前端。

---

# 23.1 后端 Entity 与依赖注入规范

后端数据库实体统一使用 Lombok：

```java
@Data
public class Content {
}
```

不得在 Entity 中重复手写 Lombok 已生成的 `getter`、`setter`、`equals`、`hashCode`、`toString`。

Spring Bean 的业务依赖统一使用字段注入：

```java
@Resource
private ContentService contentService;
```

使用 Spring Boot 3 时导入：

```java
import jakarta.annotation.Resource;
```

---

# 23.2 Req / Res 注释与排版规范

所有后端 `req/`、`res/` 下的 DTO 必须具备：

- 类型前的 Javadoc，说明请求或响应的业务用途
- 每个字段或 record 组件前的 Javadoc，说明字段含义、空值规则或安全边界（适用时）
- 每个字段、校验注解和 record 组件独占一行；相邻字段之间保留空行，禁止压缩成单行或多字段同行

DTO 仅承载数据时，可以不写方法体步骤注释；参数校验注解仍需紧贴对应字段。

---

# 24. 全局异常处理

统一：

```java
@RestControllerAdvice
```

禁止每个 Controller 到处：

```java
try {
} catch (Exception e) {
}
```

统一处理：

- 参数异常
- 业务异常
- JWT 异常
- 资源不存在
- 系统异常

---

# 25. 参数校验

使用：

```text
jakarta.validation
```

例如：

```text
@NotBlank
@NotNull
@Size
@Valid
```

不要手工重复写大量：

```java
if (x == null) ...
```

除非属于业务规则而非格式校验。

---

# 26. MyBatis 规则

使用：

```text
Mapper Interface
+
XML
```

SQL 必须参数绑定。

禁止：

```java
"WHERE content LIKE '%" + keyword + "%'"
```

禁止拼接用户输入。

---

# 27. 搜索实现

V1：

```text
MySQL LIKE
```

搜索范围：

- 正文
- 备注
- 标签
- 分类

不引入：

- Elasticsearch
- AI
- 向量搜索

搜索排序：

```text
置顶优先
→ 最近使用优先
→ 最近更新优先
```

---

# 28. 列表排序

普通列表：

```text
置顶优先
→ 更新时间倒序
→ ID 倒序
```

不得在无设计变更情况下自行调整排序。

---

# 29. 最近使用定义

最近使用：

> 最近成功复制过的内容。

不是：

- 最近查看
- 最近打开
- 最近编辑

依据：

```text
last_copy_time
```

---

# 30. 复制流程

正确流程：

```text
前端执行剪贴板复制
→ 确认复制成功
→ 调用 POST /contents/{id}/copy
```

复制失败：

> 不更新 copy_count / last_copy_time。

后端使用原子更新：

```sql
copy_count = copy_count + 1
last_copy_time = NOW()
```

---

# 31. update_time 规则

以下行为更新：

- 正文修改
- 分类修改
- 标签修改
- 备注修改

以下行为不更新：

- 复制
- 置顶
- 取消置顶

不要给 `content.update_time` 使用：

```sql
ON UPDATE CURRENT_TIMESTAMP
```

否则会破坏业务语义。

---

# 32. 删除规则

内容删除：

> 逻辑删除。

```text
deleted = 1
```

V1：

- 不做回收站页面
- 不物理删除 content
- 不物理删除 content_tag

所有正常查询：

```text
deleted = 0
```

---

# 33. 事务规则

以下操作必须事务：

## 新增内容

```text
INSERT content
→ 查询 / 创建 Tag
→ INSERT content_tag
```

## 编辑内容

```text
UPDATE content
→ DELETE 旧 content_tag
→ 查询 / 创建 Tag
→ INSERT 新 content_tag
```

任一步失败：

> 全部回滚。

---

# 34. 禁止 N+1

内容列表不能：

```text
查 20 条 content
→ 再循环查 20 次 tag
```

推荐：

```text
分页查 content
→ 一次批量查当前页 tags
→ Java 分组
```

---

# 35. PC Web 开发规则

PC Web 工程：

```text
for-next-time-pc/
```

目标：

> 像完整桌面网站。

禁止：

- 直接放大 H5 页面
- 移动底部导航
- 后台左侧菜单
- 大量表格
- 企业后台 CRUD 风格

PC 特征：

- 顶部导航
- 宽屏搜索
- 2～3 列内容卡片
- 鼠标 Hover
- 键盘操作自然
- 内容阅读宽度合理

---

# 36. PC Web UI 约束

必须遵循：

```text
docs/03-下次一定-V1.0-UI设计规范.md
```

不得擅自：

- 修改品牌色
- 修改字号体系
- 修改圆角体系
- 修改品牌文案
- 引入另一套图标风格

UI 基础组件：

> Element Plus。

但必须使用自定义样式实现产品视觉。

不得直接呈现：

> Element Plus 默认后台系统风格。

---

# 37. 移动 H5 开发规则

移动工程：

```text
for-next-time-h5/
```

目标：

> 像轻量 App / 小程序。

必须：

- 单列内容
- 底部导航
- 中间新增入口
- 移动 Bottom Sheet
- 触控区域 >= 44px
- Safe Area
- 不依赖 Hover

UI 基础组件：

> Vant 4。

---

# 38. 移动端 platform 层

移动 H5 必须保留：

```text
src/platform/
```

建议：

```text
clipboard.ts
storage.ts
feedback.ts
navigation.ts
```

页面禁止直接调用：

```text
navigator.clipboard
localStorage
window.alert
window.confirm
```

应通过平台适配层。

目的：

> 降低未来迁移微信小程序时的改造成本。

---

# 39. 小程序迁移原则

V1 不开发小程序。

不要：

- 引入 uni-app
- 引入 Taro
- 同步开发微信小程序
- 为未来小程序强行抽复杂共享层

只需要保证：

- API 平台无关
- DTO 平台无关
- 平台 API 有适配层
- 业务规则不写进浏览器特有代码

---

# 40. Pinia 使用原则

只存真正跨页面状态。

推荐：

```text
auth
user
```

不要把：

- 首页列表
- 搜索结果
- 详情页临时数据
- 所有服务端数据

全部塞入 Pinia。

页面级数据：

> 页面自己管理。

---

# 41. Axios 使用原则

两端都统一：

```text
src/api/
+
request.ts
```

页面禁止直接散落：

```ts
axios.get(...)
```

应调用：

```text
contentApi.getList()
contentApi.search()
authApi.login()
```

---

# 42. TypeScript 规则

禁止滥用：

```ts
any
```

优先定义明确：

```text
Req
Res
DTO
PageResult
```

如果确实无法提前确定类型，必须说明原因。

---

# 43. Vue 开发规则

统一：

```vue
<script setup lang="ts">
```

优先：

- Composition API
- 小组件
- 清晰 Props / Emits
- 页面组件只做页面编排

不要：

- 单文件 1000+ 行堆所有逻辑
- 页面复制相同卡片代码
- 新增页和编辑页各写一套表单

---

# 44. 组件复用

必须优先复用已有组件。

PC：

```text
SearchBar
CategoryFilter
ContentCard
ContentForm
TagSelector
EmptyState
ConfirmDialog
```

H5：

```text
SearchBar
CategoryScroller
ContentCard
ContentForm
TagSheet
CategorySheet
EmptyState
ConfirmSheet
```

新增功能前：

> 先搜索项目已有组件。

不要第一反应创建新组件。

---

# 45. 代码简化原则

本项目明确要求：

> 代码尽量简单。

开发时优先：

- 复用已有方法
- 合并重复逻辑
- 减少不必要中间层
- 减少无意义封装
- 减少只调用一行代码的方法
- 减少为了“设计模式”而设计模式

不要：

> 把简单 CRUD 写成十几层抽象。

---

# 46. 方法设计

如果多个方法只有极少差异：

> 优先合并。

但不能为了追求“方法少”导致：

- 语义不清
- 参数一大堆 Boolean
- 一个方法承担过多职责

原则：

> 简单、清晰、可维护。

---

# 47. 保留原注释

修改现有代码时：

> 保留有价值的原注释。

禁止：

- 无理由删除历史业务说明
- 删除关键边界说明
- 删除排障注释

如果注释已与代码不符：

> 应同步更新，而不是保留错误注释。

---

# 48. 注释风格

从本规则生效起，所有新增或修改的业务方法、函数都必须包含：

1. 方法前的块注释，说明该方法的职责、关键输入/输出或业务边界。
2. 方法体内按执行顺序写步骤行注释，统一使用 `// 1. ...`、`// 2. ...` 的格式。

示例：

```java
/**
 * 根据当前登录用户创建内容，并同步保存标签关系。
 */
public void createContent(CreateContentReq request) {
    // 1. 校验并清洗用户输入。
    // 2. 保存内容主体。
    // 3. 保存标签关系。
}
```

简单 DTO、纯字段访问器和无业务分支的一行配置可省略方法体步骤注释；其余注释必须解释真实业务行为，不得写与代码无关的凑数说明。

---

# 49. 最小改动原则

每次任务：

> 只修改完成当前需求所必需的内容。

不要顺手：

- 大规模重构
- 全项目格式化
- 改文件名
- 改无关包结构
- 升级依赖
- 替换框架
- 改其他页面

如果发现其他问题：

> 可以指出，但不要擅自一起修改。

---

# 50. 先理解现有代码

编码前必须：

1. 定位相关文件
2. 阅读当前实现
3. 搜索已有类似逻辑
4. 理解调用链
5. 确认修改范围
6. 再编码

禁止凭文件名猜实现。

---

# 51. 修改前先给方案

对于非微小修改，Coding Agent 应先输出：

```text
涉及哪些文件
当前实现是什么
准备怎么改
为什么这样改
是否涉及 DB/API/配置
```

用户确认或任务明确无需额外确认后，再实施。

---

# 52. 不随意升级依赖

未经明确要求：

> 不升级 package / Maven 依赖版本。

禁止为了使用一个小工具：

> 引入重量级依赖。

能用 JDK / Vue / 已有工具完成：

> 优先不用新依赖。

---

# 53. 配置文件规则

敏感信息不得进入 Git：

```text
数据库密码
JWT Secret
生产环境密码
云服务 Secret
```

使用：

```text
.env
环境变量
application-prod.yml 占位符
```

仓库只提交：

```text
.env.example
```

---

# 54. Git 忽略

必须确保 `.gitignore` 覆盖：

```text
.env
.env.*
!.env.example

node_modules/
dist/

target/

.idea/
.vscode/

*.log
logs/

.DS_Store
Thumbs.db
```

如项目实际产生其他敏感 / 构建文件，再补充。

---

# 54.1 Git 双仓库发布策略

本项目使用一个本地 Git 仓库和两个远程仓库：

| 本地分支 | 远程仓库 | 用途 |
|---|---|---|
| `dev` | Gitee 私有仓库 | 完整开发历史，可包含 `docs/`、内部脚本和开发所需文件 |
| `main` | GitHub 公开仓库 | 仅公开发布代码，不包含 `docs/`、内部文件或完整开发历史 |

本地分支推送映射应保存在当前仓库的 `.git/config`：

```text
dev  → gitee/dev
main → github/main
```

日常开发只在 `dev` 进行。`dev` 可以正常提交项目完整内容并推送至 Gitee。

`main` 是公开发行分支，不是 `dev` 的常规合并目标。首次发布时必须创建独立的 orphan 历史；后续发布时只从 `dev` 选择需要公开的文件更新到 `main`，例如：

```text
for-next-time-pc/
for-next-time-h5/
for-next-time-server/
deploy/
README.md
```

严禁：

- 将 `dev` 直接 merge 或 rebase 到 `main`
- 将 `docs/`、内部脚本、私有配置或开发历史推送至 GitHub
- 仅在 `main` 删除文件后就认为历史中的私有内容已被移除

`main` 分支的 `.gitignore` 必须忽略 `docs/`。需要发布时，应先核对暂存区只包含公开文件，再提交和推送。

---

# 55. 日志规则

后端日志使用：

```text
SLF4J + Logback
```

禁止打印：

- 明文密码
- 完整 JWT
- JWT Secret
- DB Password
- 用户完整私人正文

推荐记录：

```text
userId
contentId
operation
result
```

---

# 56. XSS 安全

用户正文：

> 按纯文本渲染。

Vue：

```vue
{{ content }}
```

禁止直接：

```vue
v-html="content"
```

除非未来需求正式引入富文本并重新设计安全策略。

---

# 57. SQL 注入安全

MyBatis 使用：

```text
#{}
```

参数绑定。

不得使用用户输入进行：

```text
${}
```

字符串拼接。

---

# 58. 时间规则

后端和数据库：

```text
Asia/Shanghai
```

API：

```text
yyyy-MM-dd HH:mm:ss
```

前端可以显示：

```text
刚刚
3分钟前
昨天
```

但不要改变接口时间协议。

---

# 59. UI 品牌文案锁定

用户界面使用：

新增：

```text
先留一句
```

搜索：

```text
找找现在用得上的。
```

保存成功：

```text
先留着，下次一定。
```

复制成功：

```text
就是这句。
```

搜索为空：

```text
这次没翻到合适的。
```

无内容：

```text
这里还空着，先留下一句。
```

不要擅自改成后台系统语言：

```text
新增数据
暂无数据
操作成功
复制成功
```

---

# 60. UI 禁止项

禁止实现成：

```text
左侧菜单
+
面包屑
+
数据表格
+
新增弹窗
+
编辑弹窗
```

的企业后台。

也禁止：

- 大面积渐变
- 霓虹
- 重玻璃拟态
- 多套图标
- 每张卡片不同颜色
- AI 推荐区
- 热门榜单
- 数据大屏

---

# 61. 测试最低要求

后端重要逻辑至少覆盖：

- 注册
- 登录
- 用户数据隔离
- 新增内容
- 编辑内容
- 删除内容
- 标签创建 / 复用
- 搜索
- 复制计数
- 最近使用
- 置顶

对于修复 Bug：

> 优先补充能复现该 Bug 的测试。

---

# 62. 自检要求

完成修改后至少检查：

## 后端

```text
mvn test
```

或至少：

```text
mvn -DskipTests compile
```

具体取决于项目当前阶段。

## 前端

至少：

```text
npm run build
```

如配置：

```text
npm run lint
npm run type-check
```

也必须执行。

不要提交明显无法编译的代码。

---

# 63. 联调检查

涉及完整业务链时检查：

```text
PC Web
→ API
→ DB
```

以及：

```text
移动 H5
→ 同一 API
→ 同一 DB
```

不能出现：

> PC 正常、H5 使用另一套业务规则。

---

# 64. 修改完成后的汇报格式

任务完成后，简要说明：

```text
1. 改了什么
2. 改了哪些文件
3. 核心逻辑
4. 是否涉及数据库
5. 是否涉及 API
6. 做了哪些验证
7. 是否还有已知问题
```

不要只回复：

> 已完成。

---

# 65. 遇到不确定需求时

如果需求会影响：

- 产品范围
- DB
- API
- 技术架构
- 双端业务规则

不要自行猜测。

应优先：

> 指出不确定点，并基于现有设计给出最小建议。

对于明确且局部的小问题：

> 可以按文档和现有代码做合理最小实现，不需要反复询问。

---

# 66. 禁止过度开发

Coding Agent 特别容易犯的错误：

```text
“顺便把未来能力也做好”
```

本项目明确禁止。

例如用户只要求关键词搜索，不得顺便增加：

- 搜索历史
- 热门关键词
- AI 搜索
- 拼音搜索
- ES

用户只要求复制次数，不得顺便增加：

- copy_log
- 用户行为埋点
- 统计大盘

---

# 67. 数据库改动流程

如果未来明确需要数据库变更：

```text
先修改数据库设计文档
→ 评审
→ 新增 Flyway Migration
→ 更新 Entity / Mapper
→ 更新 API（如需要）
→ 前端适配
```

不得直接手工改生产库后忘记 Migration。

---

# 68. API 改动流程

如果未来需要修改 API：

```text
先修改 API 设计文档
→ 评审
→ 修改后端
→ 修改 PC API 层
→ 修改 H5 API 层
→ 双端联调
```

不要只改某一个前端。

---

# 69. 依赖倒置原则：适度使用

本项目不追求架构炫技。

只有在：

- 平台能力需要替换
- 明显存在多个实现
- 真实降低耦合

时使用抽象。

例如 H5 的：

```text
clipboard
storage
feedback
```

平台适配层是合理的。

但普通 CRUD：

> 不需要额外 Interface + Factory + Strategy + Builder 全套。

---

# 70. 当前开发阶段

当前阶段：

> 设计文档已完成，准备进入项目初始化与正式开发。

后续开发顺序建议：

```text
1. 创建项目目录
2. 初始化后端
3. 初始化 PC Web
4. 初始化移动 H5
5. 配置 MySQL / Flyway
6. 健康检查
7. 注册 / 登录
8. 内容新增
9. 内容列表
10. 内容详情
11. 编辑 / 删除
12. 标签
13. 搜索
14. 复制 / 最近使用
15. 置顶
16. 双端联调
17. 部署
```

---

# 71. Agent 开始任务前检查清单

开始编码前逐项确认：

```text
[ ] 我已经阅读相关设计文档
[ ] 我已经阅读相关现有代码
[ ] 我确认没有重复实现
[ ] 我确认不会擅自改 DB
[ ] 我确认不会擅自改 API
[ ] 我确认不会引入 V1 范围外功能
[ ] 我确认修改范围已经尽量小
[ ] 我确认 PC/H5 是否都需要同步
```

---

# 72. Agent 完成任务后检查清单

```text
[ ] 代码能够编译 / 构建
[ ] 没有无关修改
[ ] 没有泄露敏感配置
[ ] 没有破坏用户数据隔离
[ ] 没有新增未经评审的字段 / 表 / 接口
[ ] 没有引入重复代码
[ ] 有价值的原注释得到保留
[ ] UI 文案符合品牌规范
[ ] PC/H5 共用同一业务规则
[ ] 已说明验证结果
```

---

# 73. 最终开发原则

所有 Coding Agent 必须始终遵守以下原则：

> 文档先于代码。

> 先看现有实现，再修改。

> 只做当前需求。

> 最小改动优先。

> 简单方案优先。

> 能复用就不重复写。

> 能合并就不重复造方法。

> 保留有价值的原注释。

> 不擅自增加数据库结构。

> 不擅自修改 API。

> 不擅自引入中间件。

> 不擅自扩大 V1 范围。

> PC Web 与移动 H5 可以长得不同，但业务规则只能有一套。

最终所有代码都必须服务于：

```text
先留下
→ 找得到
→ 用得上
```
