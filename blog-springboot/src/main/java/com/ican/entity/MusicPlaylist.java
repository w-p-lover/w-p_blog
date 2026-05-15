package com.ican.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 音乐歌单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_music_playlist")
public class MusicPlaylist {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;

    private String sourceUrl;

    private LocalDateTime importedAt;

    private LocalDateTime updateTime;

    private String cover;
}
