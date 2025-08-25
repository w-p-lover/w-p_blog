package com.ican.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 书籍
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 书名 */
    private String title;

    /** 作者 */
    private String author;

    /** 封面地址 */
    private String cover;

    /** 状态: wish, reading, read */
    private String status;

    /** 标签，逗号分隔 */
    private String tags;

    /** 添加时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime addTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
