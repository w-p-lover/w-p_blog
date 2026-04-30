package com.ican.model.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatMessagePageVO {
    private List<ChatRecordVO> records;

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    private Boolean hasMore;
}
