-- ============================================================
-- 鐏垫劅宀涳紙idea-island锛夋暟鎹簱 DDL
-- 鏁版嵁搴擄細idea_island
-- 瀛楃闆嗭細utf8mb4
-- 鏃跺尯锛氭墍鏈?datetime 瀛楁瀛?UTC 鏃堕棿
-- 鏃犲閿紝鍏宠仈鍏崇郴鐢卞簲鐢ㄥ眰淇濊瘉
-- ============================================================

CREATE DATABASE IF NOT EXISTS idea_island
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE idea_island;

-- ============================================================
-- 鐢ㄦ埛琛?
-- ============================================================
CREATE TABLE IF NOT EXISTS `user`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
    `username`     varchar(50)  NOT NULL COMMENT '鐢ㄦ埛鍚嶏紙鍞竴锛?,
    `email`        varchar(100) COMMENT '閭锛堝敮涓€锛屽彲閫夛級',
    `phone`        varchar(20)  COMMENT '鎵嬫満鍙凤紙鍞竴锛屽彲閫夛級',
    `password_hash` varchar(100) COMMENT 'BCrypt 鍝堝笇瀵嗙爜',
    `nickname`     varchar(50)  NOT NULL DEFAULT '' COMMENT '鏄电О',
    `avatar_key`   varchar(500) COMMENT '澶村儚 COS key',
    `status`       tinyint      NOT NULL DEFAULT 1 COMMENT '1=姝ｅ父, 0=绂佺敤',
    `created_at`   datetime     NOT NULL COMMENT '鍒涘缓鏃堕棿锛圲TC锛?,
    `updated_at`   datetime     NOT NULL COMMENT '鏇存柊鏃堕棿锛圲TC锛?,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '鐢ㄦ埛琛?;

-- ============================================================
-- 涓婚琛?
-- ============================================================
CREATE TABLE IF NOT EXISTS `topic`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
    `user_id`        bigint       NOT NULL COMMENT '鎵€灞炵敤鎴?ID',
    `name`           varchar(50)  NOT NULL COMMENT '涓婚鍚嶇О锛堝悓鐢ㄦ埛涓嬪敮涓€锛?,
    `description`    varchar(500) COMMENT '涓婚璇存槑',
    `status`         tinyint      NOT NULL DEFAULT 1 COMMENT '1=鍚敤, 0=鍋滅敤',
    `material_count` int          NOT NULL DEFAULT 0 COMMENT '璧勬枡鎬绘暟锛堝啑浣欙級',
    `created_at`     datetime     NOT NULL,
    `updated_at`     datetime     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_topic_name` (`user_id`, `name`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '涓婚琛?;

-- ============================================================
-- 涓婚鑷姩澶辨晥瑙勫垯琛?
-- ============================================================
CREATE TABLE IF NOT EXISTS `topic_auto_invalid_rule`
(
    `id`              bigint  NOT NULL AUTO_INCREMENT,
    `topic_id`        bigint  NOT NULL COMMENT '鎵€灞炰富棰?ID',
    `rule_type`       varchar(30) NOT NULL COMMENT 'INBOX_TIMEOUT / PENDING_REVIEW_TIMEOUT',
    `threshold_days`  int     NOT NULL COMMENT '瓒呰繃 N 澶╂湭澶勭悊鍒欒嚜鍔ㄥけ鏁?,
    `is_enabled`      tinyint NOT NULL DEFAULT 1 COMMENT '1=鍚敤, 0=鍋滅敤',
    `created_at`      datetime NOT NULL,
    `updated_at`      datetime NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_rule_type` (`topic_id`, `rule_type`),
    KEY `idx_topic_id` (`topic_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '涓婚鑷姩澶辨晥瑙勫垯琛?;

-- ============================================================
-- 鐢ㄦ埛鏍囩缁勮〃
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_tag_group`
(
    `id`           bigint      NOT NULL AUTO_INCREMENT,
    `topic_id`     bigint      NOT NULL COMMENT '鎵€灞炰富棰?ID',
    `name`         varchar(50) NOT NULL COMMENT '鏍囩缁勫悕绉帮紙鍚屼富棰樹笅鍞竴锛?,
    `color`        varchar(7)  COMMENT '鏍囩缁勯鑹?HEX锛岃缃悗瑕嗙洊缁勫唴鎵€鏈夋爣绛惧€奸鑹?,
    `is_exclusive` tinyint     NOT NULL DEFAULT 1 COMMENT '1=浜掓枼锛堝悓缁勫彧閫変竴涓€硷級',
    `is_required`  tinyint     NOT NULL DEFAULT 0 COMMENT '1=蹇呭～',
    `sort_order`   int         NOT NULL DEFAULT 0 COMMENT '灞曠ず椤哄簭',
    `created_at`   datetime    NOT NULL,
    `updated_at`   datetime    NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_topic_group_name` (`topic_id`, `name`),
    KEY `idx_topic_id` (`topic_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '鐢ㄦ埛鏍囩缁勮〃';

-- ============================================================
-- 鐢ㄦ埛鏍囩鍊艰〃
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_tag_value`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT,
    `group_id`   bigint      NOT NULL COMMENT '鎵€灞炴爣绛剧粍 ID',
    `value`      varchar(50) NOT NULL COMMENT '鏍囩鍊兼枃鏈紙鍚岀粍鍐呭敮涓€锛?,
    `color`      varchar(7)  COMMENT '鏍囩棰滆壊 HEX',
    `sort_order` int         NOT NULL DEFAULT 0,
    `created_at` datetime    NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_value` (`group_id`, `value`),
    KEY `idx_group_id` (`group_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '鐢ㄦ埛鏍囩鍊艰〃';

-- ============================================================
-- 璧勬枡琛?
-- ============================================================
CREATE TABLE IF NOT EXISTS `material`
(
    `id`                  bigint         NOT NULL AUTO_INCREMENT,
    `user_id`             bigint         NOT NULL COMMENT '鎵€灞炵敤鎴?,
    `topic_id`            bigint         NOT NULL COMMENT '鎵€灞炰富棰?,
    `material_type`       varchar(20)    NOT NULL COMMENT 'article/social/media/image/excerpt/input',
    `status`              varchar(20)    NOT NULL DEFAULT 'INBOX' COMMENT 'INBOX/PENDING_REVIEW/COLLECTED/ARCHIVED/INVALID',
    `title`               varchar(200)   COMMENT '鏍囬',
    `description`         varchar(500)   COMMENT '绠€瑕佹弿杩帮紝鍓嶇鍙綔涓烘爣棰樺睍绀?,
    `raw_content`         text           COMMENT '鍘熷鏂囨湰鍐呭锛坋xcerpt/input/social 绾枃鏈級',
    `source_url`          varchar(2000)  COMMENT '鍐呭鏉ユ簮閾炬帴',
    `file_key`            varchar(500)   COMMENT '涓婁紶鏂囦欢 COS key锛坕mage/media 鏂囦欢涓婁紶锛?,
    `comment`             text           COMMENT '璇勮',
    `score`               decimal(3, 1)  COMMENT '璇勫垎 0.0~10.0',
    `invalid_reason`      varchar(500)   COMMENT '澶辨晥鍘熷洜锛圛NVALID 鐘舵€佸繀濉級',
    `inbox_at`            datetime       COMMENT '杩涘叆鏀朵欢绠辨椂闂?,
    `inbox_read_at`       datetime       COMMENT '鏀朵欢绠卞凡璇绘椂闂达紝NULL 琛ㄧず褰撳墠鏄湭璇?,
    `collected_at`        datetime       COMMENT '杩涘叆宸叉敹褰曟椂闂?,
    `collected_read_at`   datetime       COMMENT '宸叉敹褰曞凡璇绘椂闂达紝NULL 琛ㄧず褰撳墠鏄湭璇?,
    `archived_at`         datetime       COMMENT '褰掓。鏃堕棿',
    `invalid_at`          datetime       COMMENT '澶辨晥鏃堕棿',
    `last_retrieved_at`   datetime       COMMENT '鏈€鍚庢绱㈠懡涓椂闂?,
    `is_deleted`          tinyint        NOT NULL DEFAULT 0 COMMENT '杞垹闄ゆ爣璁?,
    `deleted_at`          datetime       COMMENT '杞垹闄ゆ椂闂?,
    `created_at`          datetime       NOT NULL,
    `updated_at`          datetime       NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_user_topic_status` (`user_id`, `topic_id`, `status`, `is_deleted`),
    KEY `idx_user_created` (`user_id`, `is_deleted`, `created_at`),
    KEY `idx_topic_id` (`topic_id`),
    KEY `idx_status_inbox_at` (`status`, `inbox_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '璧勬枡琛?;

-- ============================================================
-- 璧勬枡鍏冧俊鎭〃锛堜笌 material 1:1锛?
-- ============================================================
CREATE TABLE IF NOT EXISTS `material_meta`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT,
    `material_id`      bigint       NOT NULL COMMENT '璧勬枡 ID锛堝敮涓€锛?,
    `author`           varchar(100) COMMENT '浣滆€?鍙戝竷鑰?,
    `source_platform`  varchar(50)  COMMENT '鏉ユ簮骞冲彴锛堝井淇?鐭ヤ箮/B绔欑瓑锛?,
    `publish_time`     datetime     COMMENT '鍐呭鍙戝竷鏃堕棿锛圲TC锛?,
    `word_count`       int          COMMENT '鏂囩珷瀛楁暟锛坅rticle 绫诲瀷锛?,
    `duration_seconds` int          COMMENT '鏃堕暱绉掓暟锛坢edia 绫诲瀷锛?,
    `thumbnail_key`    varchar(500) COMMENT '灏侀潰鍥?COS key',
    `extra_json`       json         COMMENT '鎵╁睍瀛楁锛圓I鎻愮偧绛夐鐣欙級',
    `created_at`       datetime     NOT NULL,
    `updated_at`       datetime     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_material_id` (`material_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '璧勬枡鍏冧俊鎭〃';

-- ============================================================
-- 璧勬枡鏍囩鍏宠仈琛?
-- ============================================================
CREATE TABLE IF NOT EXISTS `material_tag`
(
    `id`            bigint      NOT NULL AUTO_INCREMENT,
    `material_id`   bigint      NOT NULL COMMENT '璧勬枡 ID',
    `tag_type`      varchar(20) NOT NULL COMMENT 'system / user',
    `tag_group_key` varchar(50) NOT NULL COMMENT '鏍囩缁?key锛堢郴缁熸爣绛剧敤鍥哄畾 key锛岀敤鎴锋爣绛剧敤 group_id 瀛楃涓诧級',
    `tag_value`     varchar(50) NOT NULL COMMENT '鏍囩鍊?,
    `created_at`    datetime    NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_material_id` (`material_id`),
    KEY `idx_tag_group_value` (`tag_group_key`, `tag_value`),
    UNIQUE KEY `uk_material_group_value` (`material_id`, `tag_group_key`, `tag_value`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '璧勬枡鏍囩鍏宠仈琛?;
