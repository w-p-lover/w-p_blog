-- P6-03/A1：仅新增限量活动与报名表、管理权限，不修改已有文章/AI任务。
-- MySQL 5.7/InnoDB；活动LocalDateTime固定北京时间，业务SQL用UTC_TIMESTAMP(6)+8小时。
-- 本文件是迁移交付，不代表已在目标库执行。开启blog.registration.enabled前先核对结构/权限。
CREATE TABLE IF NOT EXISTS t_activity (
  id int NOT NULL AUTO_INCREMENT,
  owner_id int NOT NULL COMMENT '创建管理员，非前端提交身份',
  title varchar(128) NOT NULL,
  description text NOT NULL,
  status varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'DRAFT',
  starts_at datetime(6) NOT NULL COMMENT '报名开始时间，含边界',
  ends_at datetime(6) NOT NULL COMMENT '报名结束时间，不含边界',
  capacity int NOT NULL,
  remaining int NOT NULL COMMENT '最终成功占用后的余量',
  pending_count int NOT NULL DEFAULT 0 COMMENT '持久PENDING数；单活动上限1000',
  create_time datetime(6) NOT NULL,
  update_time datetime(6) NOT NULL,
  PRIMARY KEY(id),
  KEY idx_activity_public(status,starts_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限量技术活动';

CREATE TABLE IF NOT EXISTS t_activity_registration (
  request_id char(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  activity_id int NOT NULL,
  user_id int NOT NULL,
  status varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'PENDING',
  dispatch_attempts int NOT NULL DEFAULT 0,
  dispatch_token char(36) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  failure_count int NOT NULL DEFAULT 0,
  next_dispatch_at datetime(6) DEFAULT NULL,
  last_error varchar(256) DEFAULT NULL,
  create_time datetime(6) NOT NULL,
  update_time datetime(6) NOT NULL,
  PRIMARY KEY(request_id),
  UNIQUE KEY uk_activity_user(activity_id,user_id),
  KEY idx_registration_dispatch(status,next_dispatch_at,request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动报名身份、结果与持久待办';

-- 与现有菜单权限模式一致，默认不向普通角色自动授予管理权。
INSERT INTO t_menu(parent_id,menu_type,menu_name,path,icon,component,perms,is_hidden,is_disable,order_num,create_time)
SELECT 0,'C','技术活动管理','activities','date','/activity/index','activity:event:list',0,0,0,NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_menu WHERE menu_name='技术活动管理' AND menu_type='C');
SET @activity_menu_id := (SELECT id FROM t_menu WHERE menu_name='技术活动管理' AND menu_type='C' LIMIT 1);

INSERT INTO t_menu(parent_id,menu_type,menu_name,path,icon,component,perms,is_hidden,is_disable,order_num,create_time)
SELECT @activity_menu_id,'B','活动创建',NULL,NULL,NULL,'activity:event:add',1,0,2,NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_menu WHERE perms='activity:event:add');

INSERT INTO t_menu(parent_id,menu_type,menu_name,path,icon,component,perms,is_hidden,is_disable,order_num,create_time)
SELECT @activity_menu_id,'B','活动编辑',NULL,NULL,NULL,'activity:event:update',1,0,3,NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_menu WHERE perms='activity:event:update');

INSERT INTO t_menu(parent_id,menu_type,menu_name,path,icon,component,perms,is_hidden,is_disable,order_num,create_time)
SELECT @activity_menu_id,'B','活动发布',NULL,NULL,NULL,'activity:event:publish',1,0,4,NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_menu WHERE perms='activity:event:publish');

INSERT INTO t_menu(parent_id,menu_type,menu_name,path,icon,component,perms,is_hidden,is_disable,order_num,create_time)
SELECT @activity_menu_id,'B','报名失败恢复',NULL,NULL,NULL,'activity:registration:retry',1,0,5,NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_menu WHERE perms='activity:registration:retry');

INSERT INTO t_menu(parent_id,menu_type,menu_name,path,icon,component,perms,is_hidden,is_disable,order_num,create_time)
SELECT @activity_menu_id,'B','报名查询',NULL,NULL,NULL,'activity:registration:list',1,0,6,NOW()
WHERE NOT EXISTS (SELECT 1 FROM t_menu WHERE perms='activity:registration:list');
