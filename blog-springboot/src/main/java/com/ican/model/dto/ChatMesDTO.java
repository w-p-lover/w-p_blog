package com.ican.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMesDTO {

    private String senderAvatar;

    private String senderName;

    private String time;

    private String content;

    private String messageType;

    private String senderId;

    private String receiveId;

    private LocalDateTime createTime;
}
