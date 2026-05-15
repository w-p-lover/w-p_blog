package com.ican.service;

import com.ican.model.dto.MusicImportDTO;
import com.ican.model.dto.MusicItemDTO;
import com.ican.model.vo.MusicItemVO;
import com.ican.model.vo.MusicLibraryVO;

import java.util.List;

/**
 * 音乐库服务
 */
public interface MusicService {

    /**
     * 查看音乐库
     */
    MusicLibraryVO listMusicLibrary(String keyword, String playlistId, String tag, String mood, String sortType);

    /**
     * 保存音乐条目
     */
    MusicItemVO saveMusicItem(MusicItemDTO musicItemDTO);

    /**
     * 更新音乐条目
     */
    MusicItemVO updateMusicItem(String id, MusicItemDTO musicItemDTO);

    /**
     * 删除音乐条目
     */
    void deleteMusicItems(List<String> idList);

    /**
     * 导入网易云歌单
     */
    MusicLibraryVO importNeteasePlaylist(MusicImportDTO musicImportDTO);

    /**
     * 重置音乐库
     */
    void resetMusicLibrary();
}
