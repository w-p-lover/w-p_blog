-- 2026-10-26经用户授权，已在当前配置的121.41.87.40:3306/blog执行并独立读回。
-- 下列原始未执行说明保留为交付时背景；其他库仍需单独核实，禁止重复执行ALTER。
-- 本次结果见docs/06-部署运维/06-数据库迁移执行记录.md，原执行SHA256见本地execution.json。
-- P6-01/C3：人工审核后在隔离环境验证；本脚本未在真实数据库执行。
-- 目标为现有博客库 t_article，不写死数据库名，不自动重跑。
-- 切换要求：暂停文章/AI等相关写入，备份并确认表结构；完成迁移后统一升级读写实例。
-- 旧版本实例不会维护 content_version，禁止新旧应用混跑后宣称正文版本保证成立。
-- ALTER TABLE 的锁/耗时取决于实际 MySQL 版本、表大小及DDL能力，须先验证。

ALTER TABLE t_article
    ADD COLUMN content_version VARCHAR(36) NULL COMMENT '文章详情正文版本标记';

-- 显式保持历史更新时间；实际库若有触发器须另核实其行为。
UPDATE t_article SET content_version = UUID(), update_time = update_time WHERE content_version IS NULL;

-- 上一步必须确认 NULL 行数为0再继续；此处不设置默认值，遗漏版本的新写入应明确失败。
ALTER TABLE t_article
    MODIFY COLUMN content_version VARCHAR(36) NOT NULL COMMENT '文章详情正文版本标记';

-- 验收查询：应为0。这里只是迁移结果检查，不证明所有写入口已维护版本。
SELECT COUNT(*) AS invalid_content_versions
FROM t_article WHERE content_version IS NULL OR content_version = '';

-- 回退：先停止新版本应用，并评估恢复旧行为的一致性风险，再人工决定是否删列。
-- 不在本文件自动删除字段或清理历史缓存。
