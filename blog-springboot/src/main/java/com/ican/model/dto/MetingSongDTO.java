package com.ican.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Meting 歌曲响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetingSongDTO {

    private String title;

    private String author;

    private String url;

    private String pic;

    private String lrc;
}
