CREATE TABLE IF NOT EXISTS `t_ai_task` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'AI任务ID',
  `biz_type` varchar(64) NOT NULL COMMENT '业务类型，例如 ARTICLE',
  `biz_id` int NOT NULL COMMENT '业务ID，例如文章ID',
  `task_type` varchar(96) NOT NULL COMMENT '任务类型，例如 ARTICLE_SUMMARY_TAG_VECTOR',
  `status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态: PENDING/RUNNING/SUCCESS/FAILED/RETRYING/CANCELED',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '失败次数',
  `max_retry_count` int NOT NULL DEFAULT 3 COMMENT '最大失败次数',
  `request_payload` text COMMENT '请求摘要，不保存完整文章正文',
  `result_payload` text COMMENT '处理结果摘要',
  `error_message` varchar(1000) DEFAULT NULL COMMENT '错误原因',
  `model_name` varchar(128) DEFAULT NULL COMMENT '模型名称',
  `prompt_version` varchar(64) DEFAULT NULL COMMENT 'Prompt版本',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '结束时间',
  `cost_time` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_task_biz` (`biz_type`, `biz_id`),
  KEY `idx_ai_task_status` (`status`),
  KEY `idx_ai_task_task_type` (`task_type`),
  KEY `idx_ai_task_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI任务中心';

INSERT INTO `t_menu`
(`parent_id`, `menu_type`, `menu_name`, `path`, `icon`, `component`, `perms`, `is_hidden`, `is_disable`, `order_num`, `create_time`)
SELECT 0, 'M', 'AI任务中心', NULL, 'robot', NULL, NULL, 1, 0, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_name` = 'AI任务中心' AND `menu_type` = 'M');

SET @ai_task_menu_id := (SELECT `id` FROM `t_menu` WHERE `menu_name` = 'AI任务中心' AND `menu_type` = 'M' LIMIT 1);

INSERT INTO `t_menu`
(`parent_id`, `menu_type`, `menu_name`, `path`, `icon`, `component`, `perms`, `is_hidden`, `is_disable`, `order_num`, `create_time`)
SELECT @ai_task_menu_id, 'B', 'AI任务列表', NULL, NULL, NULL, 'ai:task:list', 1, 0, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `perms` = 'ai:task:list');

INSERT INTO `t_menu`
(`parent_id`, `menu_type`, `menu_name`, `path`, `icon`, `component`, `perms`, `is_hidden`, `is_disable`, `order_num`, `create_time`)
SELECT @ai_task_menu_id, 'B', 'AI任务重试', NULL, NULL, NULL, 'ai:task:retry', 1, 0, 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `perms` = 'ai:task:retry');
