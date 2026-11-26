-- 2026-10-26经用户授权，已在当前配置的121.41.87.40:3306/blog执行并独立读回。
-- 下列原始未执行说明保留为交付时背景；其他库仍需单独核实，禁止重复执行ALTER。
-- 本次结果见docs/06-部署运维/06-数据库迁移执行记录.md，原执行SHA256见本地execution.json。
-- Phase6/P6-02/Q2；2026-10-26。未执行；依赖迁移03/04先完成。
-- 必须先核实目标库、SHOW CREATE TABLE t_article / t_ai_task、备份与受影响行数。
-- 停止所有旧写入者/消费者/历史回填任务后统一迁移与切换，禁止混跑。
-- 非幂等DDL，部分执行后核对元数据只补缺项，不可直接重复运行。
ALTER TABLE t_article
    ADD COLUMN source_version varchar(36) DEFAULT NULL COMMENT '作者保存的正文身份，AI写入不改变',
    ADD COLUMN indexed_source_version varchar(36) DEFAULT NULL COMMENT '向量全部批次完成后发布的源版本';
ALTER TABLE t_ai_task
    ADD COLUMN source_version varchar(36) DEFAULT NULL COMMENT '任务绑定的作者正文身份，旧任务不猜版本',
    ADD COLUMN generated_payload longtext DEFAULT NULL COMMENT '摘要标签生成检查点，失败重试保留';

-- 仅为尚无身份的历史文章建立新基线；清除无法证明所属版本的旧AI摘要。
-- content_version同改，阻止详情缓存继续选择原摘要。数据量/锁时长需先核实，必要时按主键分批。
UPDATE t_article
SET source_version = UUID(), content_version = UUID(), indexed_source_version = NULL,
    ai_summary = NULL, ai_suggest_tags = NULL, ai_indexed_at = NULL
WHERE source_version IS NULL;

-- 不将历史任务绑定到迁移时正文：无法证明那就是受理时内容。
-- 无source_version的旧PENDING被新消费者认领后CANCELED且不调用模型。
-- 旧RUNNING/失败任务不自动重置；旧版本进程必须确已退出，不能仅停止消息入口。
-- 历史无版本向量在RAG读取时拒绝；问答召回会暂时降低，需授权后有限回填/核查。
-- 回填只做当前公开源版本向量，不生成摘要；生产Embedding仍可能收费，本轮没有执行。
-- 核对各列、source_version空值与历史任务分布，确认全部实例统一后才启用调度。
-- 回滚应用不能直接退回无版本旧写入协议；保留新列/备份，单独评估恢复方案。
