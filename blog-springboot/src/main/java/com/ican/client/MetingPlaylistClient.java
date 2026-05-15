package com.ican.client;

import com.ican.model.dto.MetingSongDTO;

import java.util.List;

/**
 * Meting 歌单客户端
 */
public interface MetingPlaylistClient {

    /**
     * 查询公开网易云歌单歌曲
     *
     * @param playlistId 歌单 ID
     * @return 歌曲列表
     */
    List<MetingSongDTO> listSongs(String playlistId);
}
