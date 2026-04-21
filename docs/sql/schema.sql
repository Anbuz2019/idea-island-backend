-- ============================================================
-- 灵感岛（idea-island）数据库 DDL
-- 数据库：idea_island
-- 字符集：utf8mb4
-- 时区：所有 datetime 字段存 UTC 时间
-- 无外键，关联关系由应用层保证
-- ============================================================

CREATE DATABASE IF NOT EXISTS idea_island
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE idea_island;

-- ============================================================
-- 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`     varchar(50)  NOT NULL COMMENT '用户名（唯一）',
    `email`        varchar(100) COMMENT '邮箱（唯一，可选）',
    `phone`        varchar(20)  COMMENT '手机号（唯一，可选）',
    `password_hash` varchar(100) COMMENT 'BCrypt 哈希密码',
    `nickname`     varchar(50)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar_key`   varchar(500) COMMENT '头像 COS key',
    `status`       tinyint      NOT NULL DEFAULT 1 COMMENT '1=正常, 0=禁用',
    `created_at`   datetime     NOT NULL COMMENT '创建时间（UTC）',
    `updated_at`   datetime     NOT NULL COMMENT '更新时间（UTC）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '用户表';

-- ============================================================
-- 主题表
-- ============================================================
CREATE TABLE IF NOT EXISTS `topic`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        bigint       NOT NULL COMMENT '所属用户 ID',
    `name`           varchar(50)  NOT NULL COMMENT '主题名称（同用户下唯一）',
    `description`    varchar(500) COMMENT '主题说明',
    `status`         tinyint      NOT NULL DEFAULT 1 COMMENT '1=启用, 0=停用',
    `material_count` int          NOT NULL DEFAULT 0 COMMENT '资料总数（冗余）',
    `created_at`     datetime     NOT NULL,
    `updated_at`     datetime     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_topic_name` (`user_id`, `name`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '主题表';

-- ============================================================
-- 主题自动失效规则表
-- ============================================================
CREATE TABLE IF NOT EXISTS `topic_auto_invalid_rule`
(
    `id`              bigint  NOT NULL AUTO_INCREMENT,
    `topic_id`        bigint  NOT NULL COMMENT '所属主题 ID',
    `rule_type`       varchar(30) NOT NULL COMMENT 'INBOX_TIMEOUT / PENDING_REVIEW_TIMEOUT',
    `threshold_days`  int     NOT NULL COMMENT '超过 N 天未处理则自动失效',
    `is_enabled`      tinyint NOT NULL DEFAULT 1 COMMENT '1=启用, 0=停用',
    `created_at`      datetime NOT NULL,
    `updated_at`      datetime NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_rule_type` (`topic_id`, `rule_type`),
    KEY `idx_topic_id` (`topic_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '主题自动失效规则表';

-- ============================================================
-- 用户标签组表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_tag_group`
(
    `id`           bigint      NOT NULL AUTO_INCREMENT,
    `topic_id`     bigint      NOT NULL COMMENT '所属主题 ID',
    `name`         varchar(50) NOT NULL COMMENT '标签组名称（同主题下唯一）',
    `color`        varchar(7)  COMMENT '标签组颜色 HEX，设置后覆盖组内所有标签值颜色',
    `is_exclusive` tinyint     NOT NULL DEFAULT 1 COMMENT '1=互斥（同组只选一个值）',
    `is_required`  tinyint     NOT NULL DEFAULT 0 COMMENT '1=必填',
    `sort_order`   int         NOT NULL DEFAULT 0 COMMENT '展示顺序',
    `created_at`   datetime    NOT NULL,
    `updated_at`   datetime    NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_group_name` (`topic_id`, `name`),
    KEY `idx_topic_id` (`topic_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '用户标签组表';

-- ============================================================
-- 用户标签值表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_tag_value`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT,
    `group_id`   bigint      NOT NULL COMMENT '所属标签组 ID',
    `value`      varchar(50) NOT NULL COMMENT '标签值文本（同组内唯一）',
    `color`      varchar(7)  COMMENT '标签颜色 HEX',
    `sort_order` int         NOT NULL DEFAULT 0,
    `created_at` datetime    NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_value` (`group_id`, `value`),
    KEY `idx_group_id` (`group_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '用户标签值表';

-- ============================================================
-- 资料表
-- ============================================================
CREATE TABLE IF NOT EXISTS `material`
(
    `id`                  bigint         NOT NULL AUTO_INCREMENT,
    `user_id`             bigint         NOT NULL COMMENT '所属用户',
    `topic_id`            bigint         NOT NULL COMMENT '所属主题',
    `material_type`       varchar(20)    NOT NULL COMMENT 'article/social/media/image/excerpt/input',
    `status`              varchar(20)    NOT NULL DEFAULT 'INBOX' COMMENT 'INBOX/PENDING_REVIEW/COLLECTED/ARCHIVED/INVALID',
    `title`               varchar(200)   COMMENT '标题',
    `description`         varchar(500)   COMMENT '简要描述，前端可作为标题展示',
    `raw_content`         text           COMMENT '原始文本内容（excerpt/input/social 纯文本）',
    `source_url`          varchar(2000)  COMMENT '内容来源链接',
    `file_key`            varchar(500)   COMMENT '上传文件 COS key（image/media 文件上传）',
    `comment`             text           COMMENT '评语',
    `score`               decimal(3, 1)  COMMENT '评分 0.0~10.0',
    `invalid_reason`      varchar(500)   COMMENT '失效原因（INVALID 状态必填）',
    `inbox_at`            datetime       COMMENT '进入收件箱时间',
    `collected_at`        datetime       COMMENT '进入已收录时间',
    `archived_at`         datetime       COMMENT '归档时间',
    `invalid_at`          datetime       COMMENT '失效时间',
    `last_retrieved_at`   datetime       COMMENT '最后检索命中时间',
    `is_deleted`          tinyint        NOT NULL DEFAULT 0 COMMENT '软删除标记',
    `deleted_at`          datetime       COMMENT '软删除时间',
    `created_at`          datetime       NOT NULL,
    `updated_at`          datetime       NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_topic_status` (`user_id`, `topic_id`, `status`, `is_deleted`),
    KEY `idx_user_created` (`user_id`, `is_deleted`, `created_at`),
    KEY `idx_topic_id` (`topic_id`),
    KEY `idx_status_inbox_at` (`status`, `inbox_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '资料表';

-- ============================================================
-- 资料元信息表（与 material 1:1）
-- ============================================================
CREATE TABLE IF NOT EXISTS `material_meta`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT,
    `material_id`      bigint       NOT NULL COMMENT '资料 ID（唯一）',
    `author`           varchar(100) COMMENT '作者/发布者',
    `source_platform`  varchar(50)  COMMENT '来源平台（微信/知乎/B站等）',
    `publish_time`     datetime     COMMENT '内容发布时间（UTC）',
    `word_count`       int          COMMENT '文章字数（article 类型）',
    `duration_seconds` int          COMMENT '时长秒数（media 类型）',
    `thumbnail_key`    varchar(500) COMMENT '封面图 COS key',
    `extra_json`       json         COMMENT '扩展字段（AI提炼等预留）',
    `created_at`       datetime     NOT NULL,
    `updated_at`       datetime     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_material_id` (`material_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '资料元信息表';

-- ============================================================
-- 资料标签关联表
-- ============================================================
CREATE TABLE IF NOT EXISTS `material_tag`
(
    `id`            bigint      NOT NULL AUTO_INCREMENT,
    `material_id`   bigint      NOT NULL COMMENT '资料 ID',
    `tag_type`      varchar(20) NOT NULL COMMENT 'system / user',
    `tag_group_key` varchar(50) NOT NULL COMMENT '标签组 key（系统标签用固定 key，用户标签用 group_id 字符串）',
    `tag_value`     varchar(50) NOT NULL COMMENT '标签值',
    `created_at`    datetime    NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_material_id` (`material_id`),
    KEY `idx_tag_group_value` (`tag_group_key`, `tag_value`),
    UNIQUE KEY `uk_material_group_value` (`material_id`, `tag_group_key`, `tag_value`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '资料标签关联表';
