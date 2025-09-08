package com.ican.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.models.auth.In;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(description = "游戏数据传输对象，用于新增或更新游戏信息")
public class GameDTO {

    /**
     * 游戏名称
     */
    @NotBlank(message = "游戏ID不能为空")
    @ApiModelProperty(value = "游戏ID", required = true, example = "1")
    private Long id;

    /**
     * 游戏名称
     */
    @NotBlank(message = "游戏名称不能为空")
    @ApiModelProperty(value = "游戏名称", required = true, example = "Cyberpunk 2077")
    private String name;

    /**
     * 游戏封面URL
     */
    @NotBlank(message = "封面URL不能为空")
    @ApiModelProperty(value = "游戏封面URL", required = true, example = "https://example.com/cover.jpg")
    private String coverUrl;

    /**
     * 游戏简介
     */
    @ApiModelProperty(value = "游戏简介", example = "开放世界角色扮演游戏")
    private String description;

    /**
     * 是否收藏
     */
    @NotNull(message = "是否收藏不能为空")
    @ApiModelProperty(value = "是否收藏（true表示收藏）", required = true, example = "true")
    private Boolean isInstalled;

    /**
     * 游玩时长
     */
    @ApiModelProperty(value = "游玩时长", example = "120小时")
    private String playTime;

    /**
     * 最近游玩时间
     */
    @ApiModelProperty(value = "最近游玩时间", example = "2025-09-01T15:30:00")
    private LocalDateTime lastPlayed;

    /**
     * 标签列表
     */
    @ApiModelProperty(value = "标签列表", example = "[\"RPG\", \"动作\"]")
    private List<String> tags;

    /**
     * 游戏评分
     */
    @ApiModelProperty(value = "评分", example = "9.5")
    private BigDecimal rating;

    /**
     * 发行日期
     */
    @ApiModelProperty(value = "发行日期", example = "2020-12-10")
    private LocalDate releaseDate;

    /**
     * 开发商
     */
    @ApiModelProperty(value = "开发商", example = "CD Projekt Red")
    private String developer;

    /**
     * 截图URL列表
     */
    @ApiModelProperty(value = "截图URL列表")
    private List<String> screenshotUrl;
}
