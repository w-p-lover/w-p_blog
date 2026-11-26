package com.ican.model.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityVO {
    private Integer id;
    private String title;
    private String description;
    private String status;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Integer capacity;
    private Integer remaining;
}
