package com.ican.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GameDTO {

    /**
     * 游戏名称
     */
    @NotBlank(message = "游戏ID不能为空")
    private Long id;

    /**
     * 游戏名称
     */
    @NotBlank(message = "游戏名称不能为空")
    private String name;

    /**
     * 游戏封面URL
     */
    @NotBlank(message = "封面URL不能为空")
    private String coverUrl;

    /**
     * 游戏简介
     */
    private String description;

    /**
     * 是否收藏
     */
    @NotNull(message = "是否收藏不能为空")
    private Boolean isInstalled;

    /**
     * 游玩时长
     */
    private String playTime;

    /**
     * 最近游玩时间
     */
    private LocalDateTime lastPlayed;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 游戏评分
     */
    private BigDecimal rating;

    /**
     * 发行日期
     */
    private LocalDate releaseDate;

    /**
     * 开发商
     */
    private String developer;

    /**
     * 截图URL列表
     */
    private List<String> screenshotUrl;
}
