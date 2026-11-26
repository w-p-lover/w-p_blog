package com.ican.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Activity {
    private Integer id;
    private Integer ownerId;
    private String title;
    private String description;
    private String status;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Integer capacity;
    private Integer remaining;
    private Integer pendingCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
