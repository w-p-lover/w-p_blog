package com.ican.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 音乐收藏条目保存请求
 */
@Data
public class MusicItemDTO {

    private String id;

    @NotBlank(message = "歌曲名不能为空")
    private String title;

    @NotBlank(message = "歌手不能为空")
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
}
