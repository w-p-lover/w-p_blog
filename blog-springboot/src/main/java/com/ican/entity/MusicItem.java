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
 * 音乐收藏条目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_music_item")
public class MusicItem {

    @TableId(type = IdType.INPUT)
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
