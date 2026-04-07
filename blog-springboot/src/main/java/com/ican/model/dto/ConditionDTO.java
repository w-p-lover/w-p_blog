package com.ican.model.dto;

import lombok.Data;

/**
 * 查询条件
 *
 * @author xcs
 * @date 2022/12/04 21:59
 **/
@Data
public class ConditionDTO {

    /**
     * 页码
     */
    private Long current;

    /**
     * 条数
     */
    private Long size;

    /**
     * 搜索内容
     */
    private String keyword;

    /**
     * 搜索内容
     */
    private String language;

    /**
     * 是否禁用 (0否 1是)
     */
    private Integer isDisable;

    /**
     * 分类id
     */
    private Integer categoryId;

    /**
     * 标签id
     */
    private Integer tagId;

    /**
     * 相册id
     */
    private Integer albumId;

    /**
     * 类型id
     */
    private Integer typeId;

    /**
     * 评论主题类型
     */
    private Integer commentType;

    /**
     * 文章类型 (1原创 2转载 3翻译)
     */
    private Integer articleType;

    /**
     * 登录类型
     */
    private Integer loginType;

    /**
     * 用户类型
     */
    private Integer userType;

    /**
     * 项目类型
     */
    private String trendingType;

    /**
     * Stars
     */
    private String sortType;
    /**
     * 操作模块
     */
    private String optModule;

    /**
     * 是否删除 (0否 1是)
     */
    private Integer isDelete;

    /**
     * 是否通过 (0否 1是)
     */
    private Integer isCheck;

    /**
     * 文章状态 (1公开 2私密 3草稿)
     * 任务状态 (0运行 1暂停)
     */
    private Integer status;

    /**
     * 任务组名
     */
    private String taskGroup;

    /**
     * 调用目标
     */
    private String invokeTarget;

    /**
     * 文件路径
     */
    private String filePath;

}
