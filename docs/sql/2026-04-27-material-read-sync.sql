use idea_island;

ALTER TABLE `material`
    ADD COLUMN `inbox_read_at` datetime NULL COMMENT '收件箱已读时间，NULL 表示当前未读' AFTER `inbox_at`,
    ADD COLUMN `collected_read_at` datetime NULL COMMENT '已收录已读时间，NULL 表示当前未读' AFTER `collected_at`;

SET SQL_SAFE_UPDATES = 0;

UPDATE `material`
SET `inbox_read_at` = CASE
                          WHEN `status` = 'PENDING_REVIEW' THEN COALESCE(`updated_at`, `inbox_at`, `created_at`)
                          WHEN `status` = 'INBOX' THEN NULL
                          ELSE `inbox_read_at`
    END,
    `collected_read_at` = CASE
                              WHEN `status` IN ('COLLECTED', 'ARCHIVED') THEN COALESCE(`collected_at`, `archived_at`, `updated_at`, `created_at`)
                              ELSE `collected_read_at`
        END;

SET SQL_SAFE_UPDATES = 1;
