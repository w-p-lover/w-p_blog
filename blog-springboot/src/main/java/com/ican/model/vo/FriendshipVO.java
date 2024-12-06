package com.ican.model.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipVO {
    @ApiModelProperty(value = "好友关系唯一标识符")
    private String id;

    @ApiModelProperty(value = "好友名称")
    private String name;

    @ApiModelProperty(value = "好友简介或详细信息")
    private String detail;

    @ApiModelProperty(value = "好友头像的URL地址")
    private String headImg;

}
