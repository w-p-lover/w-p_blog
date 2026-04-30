package com.ican.model.dto;

import lombok.Data;

@Data
public class ChatPageDTO {
    private String senderId;

    private String receiveId;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
