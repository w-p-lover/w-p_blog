package com.ican.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户收藏内容实体类
 *
 * @author xcs
 * @date 2025/9/19 12:13
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFavorite {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID（关联用户表）
     */
    private Integer userId;

    /**
     * 收藏内容ID（可关联文档/文章等表）
     */
    private Integer favoriteId;

    /**
     * 收藏类型（1-文档，2-文章，3-其他）
     * 用于区分收藏的内容类型，支持多类型收藏
     */
    private Integer favoriteType;

    /**
     * 收藏时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标识（0-未删除，1-已删除）
     */
    @TableLogic
    @TableField(select = false) // 查询时默认不返回该字段
    private Integer isDeleted;
}
