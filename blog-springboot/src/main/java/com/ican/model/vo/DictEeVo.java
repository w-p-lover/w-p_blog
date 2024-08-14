package com.ican.model.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * @Author：yep
 * @Project：blog
 * @name：DictEeVo
 * @Date：2024/8/14 15:00
 * @Filename：DictEeVo
 */
@Data
@ApiModel(description = "文章导出")
public class DictEeVo {
    /**
     * 文章id
     */
    @ExcelProperty(value = "文章id",index = 0)
    @ApiModelProperty(value = "文章id")
    private Integer id;

    /**
     * 文章缩略图
     */
    @ExcelProperty(value = "文章缩略图",index = 1)
    @ApiModelProperty(value = "文章缩略图")
    private String articleCover;

    /**
     * 文章标题
     */
    @ExcelProperty(value = "文章标题",index = 2)
    @ApiModelProperty(value = "文章标题")
    private String articleTitle;

    /**
     * 文章内容
     */
    @ExcelProperty(value = "文章内容",index = 3)
    @ApiModelProperty(value = "文章内容")
    private String articleContent;

    /**
     * 文章类型 (1原创 2转载 3翻译)
     */
    @ExcelProperty(value = "文章类型",index = 4)
    @ApiModelProperty(value = "文章类型 (1原创 2转载 3翻译)")
    private String articleType;

/*    *//**
     * 分类名
     *//*
    @ExcelProperty(value = "分类名",index = 5)
    @ApiModelProperty(value = "分类名")
    private String categoryName;

    *//**
     * 标签名
     *//*
    @ExcelProperty(value = "标签名",index =6)
    @ApiModelProperty(value = "标签名")
    private List<String> tagNameList;*/

    /**
     * 是否置顶 (0否 1是)
     */
    @ExcelProperty(value = "是否置顶",index = 5)
    @ApiModelProperty(value = "是否置顶")
    private String isTop;

    /**
     * 是否推荐 (0否 1是)
     */
    @ExcelProperty(value = "文章推荐",index = 6)
    @ApiModelProperty(value = "是否推荐 (0否 1是)")
    private String isRecommend;

    /**
     * 状态 (1公开 2私密 3草稿)
     */
    @ExcelProperty(value = "文章状态",index = 7)
    @ApiModelProperty(value = "状态 (1公开 2私密 3草稿)")
    private String status;
}
