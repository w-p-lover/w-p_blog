-- 2026-10-26：真实库预检发现ai_indexed_at为int(11)，Java字段为LocalDateTime。
-- ai_summary varchar(100)也不足以承载现有100—200字摘要提示词。
-- 仅用于已核对的blog库；先备份、暂停写入/持有迁移写锁、确认相关实例不会混跑。
-- 如ai_indexed_at存在非空历史值，先明确单位/语义并单独转换，不可直接执行本ALTER。
-- 本次预检34篇文章的ai_summary/ai_suggest_tags/ai_indexed_at均为NULL。
-- 修改前读SHOW CREATE TABLE并核实字段，修改后读information_schema验收。
ALTER TABLE t_article
    MODIFY COLUMN ai_summary TEXT NULL COMMENT 'AI摘要',
    MODIFY COLUMN ai_indexed_at DATETIME NULL COMMENT '当前源版本向量发布时间';
