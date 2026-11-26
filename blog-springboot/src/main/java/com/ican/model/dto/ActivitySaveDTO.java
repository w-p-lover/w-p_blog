package com.ican.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivitySaveDTO {
    @NotBlank(message = "活动标题不能为空")
    @Size(max = 128)
    private String title;
    @NotBlank(message = "活动说明不能为空")
    @Size(max = 4096)
    private String description;
    @NotNull @Min(1) @Max(100000)
    private Integer capacity;
    @NotNull
    private LocalDateTime startsAt;
    @NotNull
    private LocalDateTime endsAt;
}
