package com.ican.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ican.client.MetingPlaylistClient;
import com.ican.entity.MusicItem;
import com.ican.entity.MusicPlaylist;
import com.ican.mapper.MusicItemMapper;
import com.ican.mapper.MusicPlaylistMapper;
import com.ican.model.dto.MetingSongDTO;
import com.ican.model.dto.MusicImportDTO;
import com.ican.model.dto.MusicItemDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MusicServiceImplTest {

    @Mock
    private MusicItemMapper musicItemMapper;

    @Mock
    private MusicPlaylistMapper musicPlaylistMapper;

    @Mock
    private MetingPlaylistClient metingPlaylistClient;

    private MusicServiceImpl musicService;

    @BeforeEach
    void setUp() {
        musicService = new MusicServiceImpl(musicItemMapper, musicPlaylistMapper, metingPlaylistClient);
    }

    @Test
    void saveMusicItem_shouldFillDefaultsAndCreatePlaylist() {
        MusicItemDTO dto = new MusicItemDTO();
        dto.setTitle("九三");
        dto.setArtist("李莎旻子");
        dto.setPlaylistId("manual");
        dto.setPlaylistName("手动收藏");
        dto.setRating(8);

        musicService.saveMusicItem(dto);

        ArgumentCaptor<MusicItem> itemCaptor = ArgumentCaptor.forClass(MusicItem.class);
        verify(musicItemMapper).insert(itemCaptor.capture());
        MusicItem item = itemCaptor.getValue();
        assertTrue(item.getId().startsWith("manual-"));
        assertEquals("九三", item.getTitle());
        assertEquals("李莎旻子", item.getArtist());
        assertEquals(5, item.getRating());
        assertEquals("manual", item.getSourceType());
        assertEquals("待整理", item.getMood());

        ArgumentCaptor<MusicPlaylist> playlistCaptor = ArgumentCaptor.forClass(MusicPlaylist.class);
        verify(musicPlaylistMapper).insert(playlistCaptor.capture());
        assertEquals("manual", playlistCaptor.getValue().getId());
        assertEquals("手动收藏", playlistCaptor.getValue().getName());
    }

    @Test
    void importNeteasePlaylist_shouldPersistOnlyNewSongs() {
        MusicItem existing = new MusicItem();
        existing.setPlaylistId("7573319758");
        existing.setTitle("九三");
        existing.setArtist("李莎旻子");

        when(musicItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));
        when(metingPlaylistClient.listSongs("7573319758")).thenReturn(List.of(
                new MetingSongDTO("九三", "李莎旻子", "song-url", "cover-url", "lyric-url"),
                new MetingSongDTO("起风了", "买辣椒也用券", "song-url-2", "cover-url-2", "lyric-url-2")
        ));

        MusicImportDTO dto = new MusicImportDTO();
        dto.setSource("https://music.163.com/#/playlist?id=7573319758");
        dto.setPlaylistName("我的网易云歌曲");

        List<?> result = musicService.importNeteasePlaylist(dto).getItems();

        assertEquals(1, result.size());
        ArgumentCaptor<MusicItem> itemCaptor = ArgumentCaptor.forClass(MusicItem.class);
        verify(musicItemMapper).insert(itemCaptor.capture());
        MusicItem inserted = itemCaptor.getValue();
        assertEquals("起风了", inserted.getTitle());
        assertEquals("买辣椒也用券", inserted.getArtist());
        assertEquals("netease", inserted.getSourceType());
        assertEquals("7573319758", inserted.getPlaylistId());

        verify(musicPlaylistMapper).insert(any(MusicPlaylist.class));
    }

    @Test
    void importNeteasePlaylist_shouldRejectInvalidSourceBeforeCallingClient() {
        MusicImportDTO dto = new MusicImportDTO();
        dto.setSource("bad");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> musicService.importNeteasePlaylist(dto)
        );
        assertEquals("请输入公开网易云歌单链接或歌单 ID", exception.getMessage());

        verify(metingPlaylistClient, never()).listSongs(any());
    }
}
