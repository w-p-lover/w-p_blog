package com.ican.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.MusicItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 音乐收藏条目 Mapper
 */
@Mapper
public interface MusicItemMapper extends BaseMapper<MusicItem> {
}
