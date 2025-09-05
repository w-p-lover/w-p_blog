package com.ican.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 游戏展示对象（VO）
 * 用于前端展示游戏信息
 */
@Data
@ApiModel(value = "GameVO", description = "游戏展示对象")
public class GameVO {

    /** 游戏ID */
    @ApiModelProperty(value = "游戏ID")
    private Long id;

    /** 游戏名称 */
    @ApiModelProperty(value = "游戏名称")
    private String name;

    /** 游戏封面图片URL */
    @ApiModelProperty(value = "封面图片URL")
    private String coverUrl;

    /** 游戏简介 */
    @ApiModelProperty(value = "游戏简介")
    private String description;

    /** 是否收藏 */
    @ApiModelProperty(value = "是否收藏")
    private Boolean isInstalled;

    /** 游玩时长 */
    @ApiModelProperty(value = "游玩时长")
    private String playTime;

    /** 最近游玩时间，格式化为字符串返回 */
    @ApiModelProperty(value = "最近游玩时间")
    private LocalDateTime lastPlayed;

    /** 游戏标签列表 */
    @ApiModelProperty(value = "游戏标签列表")
    private List<String> tags;

    /** 游戏评分 */
    @ApiModelProperty(value = "游戏评分")
    private BigDecimal rating;

    /** 发行日期 */
    @ApiModelProperty(value = "发行日期")
    private LocalDate releaseDate;

    /** 开发商 */
    @ApiModelProperty(value = "开发商")
    private String developer;
}
