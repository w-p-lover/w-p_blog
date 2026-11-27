package com.ican.client.impl;

import com.ican.client.MetingPlaylistClient;
import com.ican.model.dto.MetingSongDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Meting 歌单客户端实现
 */
@Component
@RequiredArgsConstructor
public class MetingPlaylistClientImpl implements MetingPlaylistClient {

    private static final String METING_PLAYLIST_API = "https://api.i-meto.com/meting/api";

    private final RestTemplateBuilder restTemplateBuilder;

    @Override
    public List<MetingSongDTO> listSongs(String playlistId) {
        String url = UriComponentsBuilder.fromHttpUrl(METING_PLAYLIST_API)
                .queryParam("server", "netease")
                .queryParam("type", "playlist")
                .queryParam("id", playlistId)
                .toUriString();
        RestTemplate restTemplate = restTemplateBuilder.build();
        try {
            MetingSongDTO[] songs = restTemplate.getForObject(url, MetingSongDTO[].class);
            return songs == null ? Collections.emptyList() : Arrays.asList(songs);
        } catch (RestClientException e) {
            throw new IllegalStateException("歌曲读取失败，可以稍后重试，或先手动添加收藏。", e);
        }
    }
}
