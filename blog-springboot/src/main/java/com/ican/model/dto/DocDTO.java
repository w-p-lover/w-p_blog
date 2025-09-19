package com.ican.model.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class DocDTO {
    private Long id;
    private String title;
    private String categoryName;
    private String description;  // 改了名称
    private String content;
    private List<String> tags = new ArrayList<>();
    private List<CollabDTO> collaborators = new ArrayList<>();
    private Integer version;

    @Data
    public static class CollabDTO {
        private String name;
        private String avatar; // editor/viewer
        private String role;
    }
}
