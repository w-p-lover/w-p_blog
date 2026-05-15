CREATE TABLE IF NOT EXISTS `t_music_playlist` (
  `id` varchar(64) NOT NULL COMMENT '歌单 ID',
  `name` varchar(128) NOT NULL COMMENT '歌单名称',
  `source_url` varchar(512) DEFAULT NULL COMMENT '来源链接',
  `cover` varchar(512) DEFAULT NULL COMMENT '歌单封面',
  `imported_at` datetime NOT NULL COMMENT '导入时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='音乐歌单';

CREATE TABLE IF NOT EXISTS `t_music_item` (
  `id` varchar(96) NOT NULL COMMENT '音乐条目 ID',
  `title` varchar(256) NOT NULL COMMENT '歌曲名',
  `artist` varchar(256) NOT NULL COMMENT '歌手',
  `album` varchar(256) DEFAULT NULL COMMENT '专辑',
  `cover` varchar(512) NOT NULL COMMENT '封面',
  `url` varchar(1024) DEFAULT NULL COMMENT '播放链接',
  `lyric_url` varchar(1024) DEFAULT NULL COMMENT '歌词链接',
  `playlist_id` varchar(64) NOT NULL COMMENT '歌单 ID',
  `playlist_name` varchar(128) NOT NULL COMMENT '歌单名称',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型: netease/manual',
  `source_url` varchar(512) DEFAULT NULL COMMENT '来源链接',
  `tags` varchar(512) NOT NULL DEFAULT '' COMMENT '标签，逗号分隔',
  `mood` varchar(64) NOT NULL DEFAULT '待整理' COMMENT '心情或场景',
  `rating` tinyint NOT NULL DEFAULT 3 COMMENT '评分 1-5',
  `note` varchar(512) NOT NULL DEFAULT '' COMMENT '一句话备注',
  `summary` text COMMENT '收藏理由',
  `favorite_level` varchar(32) NOT NULL DEFAULT 'spark' COMMENT '喜好等级',
  `is_pinned` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `imported_at` datetime NOT NULL COMMENT '导入时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_music_item_playlist` (`playlist_id`),
  KEY `idx_music_item_rating` (`rating`),
  KEY `idx_music_item_update_time` (`update_time`),
  CONSTRAINT `fk_music_item_playlist`
    FOREIGN KEY (`playlist_id`) REFERENCES `t_music_playlist` (`id`)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='音乐收藏条目';

INSERT INTO `t_menu`
(`parent_id`, `menu_type`, `menu_name`, `path`, `icon`, `component`, `perms`, `is_hidden`, `is_disable`, `order_num`, `create_time`)
SELECT 0, 'M', '音乐库管理', NULL, 'music', NULL, NULL, 1, 0, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `menu_name` = '音乐库管理' AND `menu_type` = 'M');

SET @music_menu_id := (SELECT `id` FROM `t_menu` WHERE `menu_name` = '音乐库管理' AND `menu_type` = 'M' LIMIT 1);

INSERT INTO `t_menu`
(`parent_id`, `menu_type`, `menu_name`, `path`, `icon`, `component`, `perms`, `is_hidden`, `is_disable`, `order_num`, `create_time`)
SELECT @music_menu_id, 'B', '音乐导入', NULL, NULL, NULL, 'music:library:import', 1, 0, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `perms` = 'music:library:import');

INSERT INTO `t_menu`
(`parent_id`, `menu_type`, `menu_name`, `path`, `icon`, `component`, `perms`, `is_hidden`, `is_disable`, `order_num`, `create_time`)
SELECT @music_menu_id, 'B', '音乐新增', NULL, NULL, NULL, 'music:item:add', 1, 0, 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `perms` = 'music:item:add');

INSERT INTO `t_menu`
(`parent_id`, `menu_type`, `menu_name`, `path`, `icon`, `component`, `perms`, `is_hidden`, `is_disable`, `order_num`, `create_time`)
SELECT @music_menu_id, 'B', '音乐编辑', NULL, NULL, NULL, 'music:item:update', 1, 0, 3, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `perms` = 'music:item:update');

INSERT INTO `t_menu`
(`parent_id`, `menu_type`, `menu_name`, `path`, `icon`, `component`, `perms`, `is_hidden`, `is_disable`, `order_num`, `create_time`)
SELECT @music_menu_id, 'B', '音乐删除', NULL, NULL, NULL, 'music:item:delete', 1, 0, 4, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `perms` = 'music:item:delete');

INSERT INTO `t_menu`
(`parent_id`, `menu_type`, `menu_name`, `path`, `icon`, `component`, `perms`, `is_hidden`, `is_disable`, `order_num`, `create_time`)
SELECT @music_menu_id, 'B', '音乐重置', NULL, NULL, NULL, 'music:library:reset', 1, 0, 5, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `t_menu` WHERE `perms` = 'music:library:reset');
