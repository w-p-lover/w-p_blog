package com.ican.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatListDTO {
    private String id;
    private String name;
    private String detail;
    private String lastMsg;
    private String headImg;
}
