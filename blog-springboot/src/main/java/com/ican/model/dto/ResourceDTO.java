package com.ican.model.dto;

import lombok.Data;

@Data
public class ResourceDTO {
    private String type;  // pdf, note, video
    private String name;
    private String url;
}
