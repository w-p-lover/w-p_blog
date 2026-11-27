package com.ican.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequestDTO {

    @NotBlank(message = "问题不能为空")
    private String question;
}
