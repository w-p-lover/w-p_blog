package com.ican.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

/**
 * 说说信息VO
 *
 * @author xcs
 **/
@Data
public class TalkBackInfoVO {

    /**
     * 说说id
     */
    private Integer id;

    /**
     * 说说内容
     */
    private String talkContent;

    /**
     * 图片
     */
    @JsonIgnore
    private String images;

    /**
     * 图片列表
     */
    private List<String> imgList;

    /**
     * 是否置顶 (0否 1是)
     */
    private Integer isTop;

    /**
     * 说说状态 (1公开 2私密)
     */
    private Integer status;

}