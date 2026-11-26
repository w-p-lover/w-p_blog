package com.ican.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 详情输出前的一次数据库可见性、版本和关联快照，不写入正文缓存。
 */
@Data
public class ArticleDetailStateVO {

    private String contentVersion;

    private Integer views;

    private ArticlePaginationVO lastArticle;

    private ArticlePaginationVO nextArticle;

    private CategoryOptionVO category;

    private List<TagOptionVO> tagVOList;
}
