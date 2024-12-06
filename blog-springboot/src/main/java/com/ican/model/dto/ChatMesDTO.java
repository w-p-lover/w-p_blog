package com.ican.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMesDTO {

    @ApiModelProperty(value = "发送者头像")
    private String senderAvatar;

    @ApiModelProperty(value = "发送者名称")
    private String senderName;

    @ApiModelProperty(value = "发送时间")
    private String time;

    @ApiModelProperty(value = "消息内容")
    private String content;

    @ApiModelProperty(value = "消息类型")
    private String messageType;

    @ApiModelProperty(value = "发送者ID")
    private String senderId;

    @ApiModelProperty(value = "接收者ID")
    private String receiveId;

    private LocalDateTime createTime;
}
