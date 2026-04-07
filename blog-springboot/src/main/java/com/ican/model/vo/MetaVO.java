package com.ican.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 其他信息
 *
 * @author xcs
 * @date 2022/12/11 19:35
 **/
@Data
@Builder
public class MetaVO {

    /**
     * 菜单名称
     */
    private String title;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 是否隐藏
     */
    private Boolean hidden;

}