package com.ican.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文章条件列表VO
 *
 * @author xcs
 **/
@Data
@Builder
public class ArticleConditionList {

    /**
     * 文章列表
     */
    private List<ArticleConditionVO> articleConditionVOList;

    /**
     * 条件名
     */
    private String name;
}