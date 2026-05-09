package com.ican.mapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UserFavoriteMapperContractTest {

    @Test
    void mapperXml_shouldUseFavoriteIdColumnInsteadOfDocId() throws Exception {
        String xml = Files.readString(Path.of("src/main/resources/mapper/UserFavoriteMapper.xml"));

        assertThat(xml).contains("favorite_id");
        assertThat(xml).doesNotContain("doc_id");
    }

    @Test
    void docMapper_shouldUseFavoriteIdColumnWhenWritingFavorites() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/ican/mapper/DocMapper.java"));

        assertThat(source).contains("t_user_favorite (user_id, favorite_id");
        assertThat(source).contains("favorite_id = #{docId}");
        assertThat(source).doesNotContain("t_user_favorite (user_id, doc_id");
        assertThat(source).doesNotContain("doc_id = #{docId}");
    }
}
