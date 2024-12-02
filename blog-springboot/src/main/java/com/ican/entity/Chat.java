package com.ican.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author：yep
 * @Project：blog-springboot
 * @name：ChatRecord
 * @Date：2024/11/25 16:25
 * @Filename：ChatRecord
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chat {

    @TableId(type = IdType.AUTO)
    private Integer id; // 聊天记录 ID

    private Integer senderId; // 发送者用户 ID

    private Integer receiverId; // 接收者用户 ID

    private Integer isRead; // 接收者用户 ID

    private String content; // 消息内容

    private String senderName; // 发送者姓名

    private String messageType; // 消息内容

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 消息发送时间
}
