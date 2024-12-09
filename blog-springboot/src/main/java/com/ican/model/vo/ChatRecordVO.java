package com.ican.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.models.auth.In;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

import java.time.LocalDateTime;

/**
 * @Author：yep
 * @Project：blog-springboot
 * @name：ChatRecordVO
 * @Date：2024/11/25 16:18
 * @Filename：ChatRecordVO
 */
@Data
@ApiModel(description = "聊天记录VO")
public class ChatRecordVO {
    @ApiModelProperty(value = "消息唯一标识")
    private Long messageId; // 消息唯一标识

    private Long senderId;  // 发送者用户 ID

    @ApiModelProperty(value = "发送者昵称")
    private String senderName; // 发送者昵称或用户名（如果需要展示）

    @ApiModelProperty(value = "发送者头像")
    private String senderAvatar; // 发送者头像链接（如果需要展示）

    @ApiModelProperty(value = "接收者用户 ID")
    private Long receiverId; // 接收者用户 ID

    @ApiModelProperty(value = "是否通过 (0否 1是)")
    private Integer isRead;

    @ApiModelProperty(value = "文件信息")
    private FileInfo fileInfo;

    private String content; // 消息内容

    private String messageType; // 消息类型 (如 TEXT, IMAGE, FILE 等)

    private LocalDateTime createTime; // 消息发送时间


    @Data
    @Builder
    public static class FileInfo {
        @ApiModelProperty(value = "传输文件名称")
        private String fileName;

        @ApiModelProperty(value = "传输文件大小")
        private String fileSize;

        @ApiModelProperty(value = "传输文件类型(1:WORD,2:PPT...)")
        private Integer fileType;
    }
}
