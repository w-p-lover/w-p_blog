package com.ican.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Game {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 游戏名
     */
    private String name;

    /**
     * 封面图片URL
     */
    private String coverUrl;

    /**
     * 简介
     */
    private String description;

    /**
     * 是否收藏
     */
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
     * 标签（用逗号分隔存储）
     */
    private String tags;

    /**
     * 评分
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
     * 截图
     */
    private String screenshotUrl;
}
