package com.ican.model.vo;

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
public class ChatRecordVO {
    private Long messageId; // 消息唯一标识

    private Long senderId;  // 发送者用户 ID

    private String senderName; // 发送者昵称或用户名（如果需要展示）

    private String senderAvatar; // 发送者头像链接（如果需要展示）

    private Long receiverId; // 接收者用户 ID

    private Integer isRead;

    private FileInfo fileInfo;

    private String content; // 消息内容

    private String messageType; // 消息类型 (如 TEXT, IMAGE, FILE 等)

    private LocalDateTime createTime; // 消息发送时间


    @Data
    @Builder
    public static class FileInfo {
        private String fileName;

        private String fileSize;

        private Integer fileType;
    }
}
