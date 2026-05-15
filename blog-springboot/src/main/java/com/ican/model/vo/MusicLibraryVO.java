package com.ican.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 音乐库数据视图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MusicLibraryVO {

    private List<MusicItemVO> items = Collections.emptyList();

    private List<MusicPlaylistVO> playlists = Collections.emptyList();
}
