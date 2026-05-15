package com.ican.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 音乐收藏条目视图
 */
@Data
public class MusicItemVO {

    private String id;

    private String title;

    private String artist;

    private String album;

    private String cover;

    private String url;

    private String lyricUrl;

    private String playlistId;

    private String playlistName;

    private String sourceType;

    private String sourceUrl;

    private String tags;

    private String mood;

    private Integer rating;

    private String note;

    private String summary;

    private String favoriteLevel;

    private Boolean isPinned;

    private LocalDateTime importedAt;

    private LocalDateTime updateTime;
}
