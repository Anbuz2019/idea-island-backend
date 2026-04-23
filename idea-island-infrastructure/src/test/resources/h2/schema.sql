-- H2 测试数据库 DDL（MySQL 兼容模式）

CREATE TABLE IF NOT EXISTS `user`
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(100),
    phone         VARCHAR(20),
    password_hash VARCHAR(100),
    nickname      VARCHAR(50)  NOT NULL DEFAULT '',
    avatar_key    VARCHAR(500),
    status        TINYINT      NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL
);

CREATE TABLE IF NOT EXISTS topic
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT      NOT NULL,
    name           VARCHAR(50) NOT NULL,
    description    VARCHAR(500),
    status         TINYINT     NOT NULL DEFAULT 1,
    material_count INT         NOT NULL DEFAULT 0,
    created_at     DATETIME    NOT NULL,
    updated_at     DATETIME    NOT NULL
);

CREATE TABLE IF NOT EXISTS user_tag_group
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id     BIGINT      NOT NULL,
    name         VARCHAR(50) NOT NULL,
    color        VARCHAR(7),
    is_exclusive TINYINT     NOT NULL DEFAULT 1,
    is_required  TINYINT     NOT NULL DEFAULT 0,
    sort_order   INT         NOT NULL DEFAULT 0,
    created_at   DATETIME    NOT NULL,
    updated_at   DATETIME    NOT NULL
);

CREATE TABLE IF NOT EXISTS user_tag_value
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id   BIGINT      NOT NULL,
    `value`    VARCHAR(50) NOT NULL,
    color      VARCHAR(7),
    sort_order INT         NOT NULL DEFAULT 0,
    created_at DATETIME    NOT NULL
);

CREATE TABLE IF NOT EXISTS topic_auto_invalid_rule
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id       BIGINT      NOT NULL,
    rule_type      VARCHAR(30) NOT NULL,
    threshold_days INT         NOT NULL,
    is_enabled     TINYINT     NOT NULL DEFAULT 1,
    created_at     DATETIME    NOT NULL,
    updated_at     DATETIME    NOT NULL
);

CREATE TABLE IF NOT EXISTS material
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT        NOT NULL,
    topic_id          BIGINT        NOT NULL,
    material_type     VARCHAR(20)   NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'INBOX',
    title             VARCHAR(200),
    description       VARCHAR(500),
    raw_content       TEXT,
    source_url        VARCHAR(2000),
    file_key          VARCHAR(500),
    comment           TEXT,
    score             DECIMAL(3, 1),
    invalid_reason    VARCHAR(500),
    inbox_at          DATETIME,
    collected_at      DATETIME,
    archived_at       DATETIME,
    invalid_at        DATETIME,
    last_retrieved_at DATETIME,
    is_deleted        TINYINT       NOT NULL DEFAULT 0,
    deleted_at        DATETIME,
    created_at        DATETIME      NOT NULL,
    updated_at        DATETIME      NOT NULL
);

CREATE TABLE IF NOT EXISTS material_meta
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id       BIGINT UNIQUE NOT NULL,
    author            VARCHAR(100),
    source_platform   VARCHAR(50),
    publish_time      DATETIME,
    word_count        INT,
    duration_seconds  INT,
    thumbnail_key     VARCHAR(500),
    extra_json        CLOB,
    created_at        DATETIME      NOT NULL,
    updated_at        DATETIME      NOT NULL
);

CREATE TABLE IF NOT EXISTS material_tag
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id   BIGINT      NOT NULL,
    tag_type      VARCHAR(20) NOT NULL,
    tag_group_key VARCHAR(50) NOT NULL,
    tag_value     VARCHAR(50) NOT NULL,
    created_at    DATETIME    NOT NULL
);
