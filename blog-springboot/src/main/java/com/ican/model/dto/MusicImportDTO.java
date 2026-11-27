package com.ican.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 音乐歌单导入请求
 */
@Data
public class MusicImportDTO {

    @NotBlank(message = "歌单链接或 ID 不能为空")
    private String source;

    private String playlistName;
}
