package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 音乐歌单视图
 */
@Data
public class MusicPlaylistVO {

    private String id;

    private String name;

    private String sourceUrl;

    private LocalDateTime importedAt;

    private LocalDateTime updateTime;

    private String cover;
}
