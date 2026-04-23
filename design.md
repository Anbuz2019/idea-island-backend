# 项目 -- 灵感岛（后端部分）

---

## 1. 技术栈

| 类别 | 选型 | 说明 |
|------|------|------|
| 语言 / 框架 | Java 21 + Spring Boot 3.4 | 已有脚手架 |
| 架构模式 | DDD 四层架构 | 见模块划分 |
| 持久层 | MyBatis + MySQL 8 | 无分库分表，移除 ShardingSphere |
| 缓存 | Redis（Redisson） | 登录 Token、热点数据 |
| 定时任务 | XXL-Job | 自动失效扫描 |
| 消息队列 | RocketMQ | 封面异步生成、内容处理管道、系统标签刷新 |
| 注册中心 | 暂不引入 Nacos / Dubbo | 单体服务，无需微服务拆分 |
| 认证 | JWT（java-jwt） | Bearer Token，有效期 7 天 |
| 文件存储 | 对象存储（OSS / MinIO） | 图片、音视频文件 |
| 构建 | Maven 3 | 多模块 |

> ShardingSphere 和 Dubbo 是脚手架遗留配置，本项目为个人单体应用，移除即可。

---

## 2. 模块职责划分（DDD 四层）

```
idea-island
├── idea-island-types          # 公共类型：枚举、常量、通用异常、Result 封装
├── idea-island-api            # 对外接口定义：DTO、接口声明（无实现）
├── idea-island-domain         # 核心领域层：实体、值对象、聚合根、领域服务、仓储接口
├── idea-island-infrastructure # 基础设施层：MyBatis DAO、Redis、OSS、MQ 生产者
├── idea-island-trigger        # 触发器层：HTTP Controller、MQ 消费者、XXL-Job Handler
└── idea-island-app            # 启动层：Spring Boot 入口、配置装配
```

### 模块依赖关系

```
app → trigger → domain ← infrastructure
              ↑
             api
              ↑
            types（所有模块均可依赖）
```

- `domain` 不依赖任何外层，通过接口（Repository / Adapter）与基础设施解耦
- `infrastructure` 实现 `domain` 中定义的仓储接口
- `trigger` 调用 `domain` 的领域服务，不直接操作数据库

---

## 3. 领域划分

### 3.1 领域总览

| 领域 | 核心职责 | 扩展方向 |
|------|---------|---------|
| user | 注册、登录、JWT 签发、用户信息管理 | OAuth 第三方登录 |
| topic | 主题 CRUD、标签集配置（标签组 + 标签值） | 主题模板、主题共享 |
| material | 资料生命周期：提交、状态流转、打标签、评语、评分 | 资料关联、版本历史 |
| content | 内容处理管道：URL 解析、元信息提取、封面生成 | AI 提炼、摘要生成、自动分类 |
| search | 结构化检索 + 全文关键词检索 | 向量语义检索、自然语言问答 |

**核心设计原则：**
- `material` 只关心资料的**状态和用户行为**，不关心内容如何被处理
- `content` 只负责**内容处理管道**，以事件驱动方式异步执行，不阻塞主流程
- `search` 只负责**查询**，不写业务数据，可独立演进（引入 ES、向量库等）

### 3.2 领域包结构

```
domain
├── user
│   ├── model
│   │   ├── entity       # User
│   │   └── valobj       # LoginType、UserStatus
│   ├── service          # UserService、AuthService
│   └── repository       # IUserRepository
│
├── topic
│   ├── model
│   │   ├── entity       # Topic、UserTagGroup、UserTagValue
│   │   └── valobj       # TopicStatus、TagColor
│   ├── service          # TopicService、TagSetService
│   └── repository       # ITopicRepository
│
├── material
│   ├── model
│   │   ├── entity       # Material、MaterialMeta、MaterialTag
│   │   ├── aggregate    # MaterialAggregate
│   │   └── valobj       # MaterialType、MaterialStatus、ScoreRange、Completeness
│   ├── service          # MaterialService、StatusTransitionService、SystemTagService
│   └── repository       # IMaterialRepository
│
├── content
│   ├── model
│   │   └── valobj       # ContentProcessTask、ParseResult、CoverSource
│   ├── service          # ContentProcessService、UrlParseService、CoverGenerateService
│   └── adapter          # IUrlParserAdapter、ICoverStorageAdapter（对外部服务的抽象）
│
└── search
    ├── model
    │   └── valobj       # SearchQuery、SearchResult
    ├── service          # SearchService
    └── repository       # ISearchRepository
```

---

## 4. 领域间交互方式

领域之间通过 **RocketMQ** 解耦，禁止跨领域直接调用 Repository。消息持久化到 Broker，进程重启不丢失，保证高可用。

| 事件 | 发布方 | 消费方 | 触发时机 |
|------|--------|--------|---------|
| MaterialSubmittedEvent | material | content | 资料提交成功后，触发 URL 解析、元信息提取、封面生成 |
| ContentProcessedEvent | content | material | 内容处理完成后，回写 meta / cover_key |
| MaterialChangedEvent | material | search | 资料状态、标签、评分变更后，刷新搜索索引 |
| SystemTagRefreshEvent | material | material（内部） | 打标签 / 评分变更后，异步刷新系统标签 |

**可靠性保障：**
- 发送方使用事务消息（或本地消息表），确保业务写库与消息发送的原子性
- 消费方幂等处理，以 `materialId + eventType` 作为幂等键，防止重复消费
- 消费失败进入重试队列，超过重试次数进入死信队列，人工介入

---

## 5. 关键流程技术实现

### 5.1 提交资料

```
Controller（trigger）
  → MaterialService.submit()
    → 校验主题归属与状态
    → 创建 Material 聚合，status = INBOX
    → IMaterialRepository.save()
  → 发布 MaterialSubmittedEvent（MQ）
    → ContentProcessService 异步处理：URL 解析、元信息提取、封面生成
    → 处理完成后通过 ContentParsedEvent / CoverGeneratedEvent 回写
```

### 5.2 状态流转

```
StatusTransitionService.transit(materialId, action, userId)
  → 加载 Material
  → 状态机校验：action 在当前 status 下是否合法
  → 前置条件校验（collect 需 comment + score 均不为空）
  → 更新 status 及对应时间戳
  → 发布 SystemTagRefreshEvent（异步刷新 sys_score_range / sys_completeness）
  → 发布 MaterialUpdatedEvent（异步更新搜索索引）
```

### 5.3 内容处理管道（content 领域）

```
ContentProcessService（MQ Consumer）
  ┌─ UrlParseAdapter     → 抓取网页 OG 信息、视频平台封面
  ├─ MetaExtractService  → 提取作者、平台、字数、时长等元信息
  └─ CoverGenerateService→ 生成或下载封面图，上传 OSS

# 未来扩展节点（不影响现有流程）：
  ├─ AiSummaryService   → AI 生成摘要
  └─ AiClassifyService  → AI 自动打标签
```

### 5.4 自动失效（XXL-Job）

```
AutoInvalidJobHandler（每天凌晨 2:00）
  → 查询所有启用了自动失效规则的主题
  → 按规则扫描超时资料（INBOX / PENDING_REVIEW 状态）
  → 批量更新 status = INVALID，invalid_reason = "系统自动失效：超过 N 天未处理"
```

---

## 6. 数据库设计要点

- 不使用外键，关联关系由应用层保证
- 所有表统一使用 `bigint` 自增主键
- 软删除：material 表使用 `is_deleted tinyint(1)` + `deleted_at datetime`
- 时间字段统一使用 `datetime`，存 UTC 时间

### 核心表清单

| 表名 | 所属领域 |
|------|---------|
| user | user |
| topic | topic |
| user_tag_group | topic |
| user_tag_value | topic |
| material | material |
| material_meta | material / content |
| material_tag | material |
| topic_auto_invalid_rule | topic |

---

## 7. 认证方案

- 登录成功后签发 JWT，payload 包含 `userId`、`exp`
- Token 同时写入 Redis（key: `token:userId`），支持主动登出
- 每次请求在 Filter 层校验 JWT 签名有效性 + Redis key 是否存在
- Token 剩余有效期 < 1 天时自动续签，响应头返回新 Token

---

## 8. 文件上传方案

- 客户端调用 `/api/v1/files/upload`，由后端接收 multipart 文件并上传到对象存储
- 后端生成唯一 `fileKey`，直接作为对象名上传到云存储
- 业务表仅保存 `fileKey`，例如 `material.file_key`、`material_meta.thumbnail_key`、`user.avatar_key`
- 前端需要展示文件时，调用 `/api/v1/files/resolve?fileKey=...`，后端直接根据 `fileKey` 拼接访问地址返回
- 文件访问地址优先使用配置项 `cos.base-url`；未配置时调用 COS SDK `getObjectUrl(bucket, fileKey)` 生成默认对象地址，避免手工拼接域名与云存储真实地址不一致
- 后端会校验 MIME 和大小限制，不再额外维护 `fileKey -> url` 映射关系表
- 后端异步生成的封面也使用同一套 `fileKey` 规则，保证 `thumbnailKey` 可直接解析

---

## 9. 统一响应与异常处理

```
Result<T> { int code; String message; T data; long timestamp; }

GlobalExceptionHandler（@RestControllerAdvice）
  → AppException（业务异常，含错误码）→ 返回对应 code + message
  → MethodArgumentNotValidException   → code 1001
  → 其余未知异常                       → code 5000，不暴露堆栈信息
```

---

## 10. 需要移除的脚手架配置

| 配置 | 处理方式 |
|------|---------|
| ShardingSphere | 移除依赖，改为普通 HikariCP 数据源 |
| Dubbo | 移除依赖和全部配置 |
| Nacos | 移除依赖和全部配置 |
| RocketMQ | 保留，用于领域事件驱动，保证消息持久化不丢失 |
| XXL-Job | 保留，用于自动失效定时任务 |
| Redisson | 保留，用于 Token 存储 |
