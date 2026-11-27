package com.ican.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ican.entity.MusicPlaylist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 音乐歌单 Mapper
 */
@Mapper
public interface MusicPlaylistMapper extends BaseMapper<MusicPlaylist> {
}
