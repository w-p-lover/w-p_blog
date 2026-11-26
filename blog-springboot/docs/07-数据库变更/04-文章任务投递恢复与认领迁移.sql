-- 2026-10-26经用户授权，已在当前配置的121.41.87.40:3306/blog执行并独立读回。
-- 下列原始未执行说明保留为交付时背景；其他库仍需单独核实，禁止重复执行ALTER。
-- 本次结果见docs/06-部署运维/06-数据库迁移执行记录.md，原执行SHA256见本地execution.json。
-- Phase6/P6-02/Q1；2026-10-25。未执行，先核实目标库/表元数据与备份。
-- 停止旧版本生产者/消费者与调度后统一切换，禁止新旧协议混跑。
-- 先核实：SHOW CREATE TABLE t_ai_task;
-- 本脚本不重复执行；若部分列已存在，应核对定义后仅执行缺失项，不能盲目重跑。
ALTER TABLE t_ai_task
    ADD COLUMN dispatch_attempts int NOT NULL DEFAULT 0 COMMENT '唤醒投递预约次数，非模型失败次数',
    ADD COLUMN next_dispatch_at datetime(6) DEFAULT NULL COMMENT '待办下次可预约投递时间，NULL立即可投递',
    ADD COLUMN execution_token varchar(36) DEFAULT NULL COMMENT '本次认领执行代，限制任务状态回写',
    ADD INDEX idx_ai_task_dispatch (biz_type, task_type, status, next_dispatch_at, id);

-- 不批量重置RUNNING/RETRYING/FAILED，不自动回放旧DLQ，也不重写旧文章/向量。
-- 历史PENDING在迁移后会被发现；需逐项核实历史业务状态与模型费用预算后再启用新应用。
-- 验证列/索引后，新应用统一部署；数据库迁移/应用启动均不在本次自动化执行范围。
