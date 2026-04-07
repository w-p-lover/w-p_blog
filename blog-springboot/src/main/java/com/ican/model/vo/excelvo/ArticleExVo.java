package com.ican.model.vo.excelvo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author：yep
 * @Project：blog
 * @name：DictEeVo
 * @Date：2024/8/14 15:00
 * @Filename：DictEeVo
 */
@Data
@HeadRowHeight(25)
@ContentRowHeight(26)
@EqualsAndHashCode
@ColumnWidth(15)
public class ArticleExVo {
    /**
     * 文章id
     */
    @ExcelProperty(value = "文章id", index = 0)
    private Integer id;

    /**
     * 文章标题
     */
    @ExcelProperty(value = "文章标题", index = 1)
    private String articleTitle;

    /**
     * 文章内容
     */
    @ExcelProperty(value = "文章内容", index = 2)
    @ColumnWidth(60) // 设置列宽（可以根据需要调整宽度）
    private String articleContent;

    /**
     * 文章类型 (1原创 2转载 3翻译)
     */
    @ExcelProperty(value = "文章类型", index = 3)
    private String articleType;

    /**
     * 是否置顶 (0否 1是)
     */
    @ExcelProperty(value = "是否置顶", index = 4)
    private String isTop;

    /**
     * 是否推荐 (0否 1是)
     */
    @ExcelProperty(value = "文章推荐", index = 5)
    private String isRecommend;

    /**
     * 状态 (1公开 2私密 3草稿)
     */
    @ExcelProperty(value = "文章状态", index = 6)
    private String status;

    /**
     * 文章缩略图
     */
    @ExcelProperty(value = "文章缩略图", index = 7)
    @ColumnWidth(90) // 设置列宽（可以根据需要调整宽度）
    private String articleCover;

    /*    *//**
     * 分类名
     *//*
    @ExcelProperty(value = "分类名",index = 5)
    private String categoryName;

    *//**
     * 标签名
     *//*
    @ExcelProperty(value = "标签名",index =6)
    private List<String> tagNameList;*/

}
