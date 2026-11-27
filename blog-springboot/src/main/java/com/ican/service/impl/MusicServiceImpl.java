package com.ican.service.impl;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ican.client.MetingPlaylistClient;
import com.ican.entity.MusicItem;
import com.ican.entity.MusicPlaylist;
import com.ican.mapper.MusicItemMapper;
import com.ican.mapper.MusicPlaylistMapper;
import com.ican.model.dto.MetingSongDTO;
import com.ican.model.dto.MusicImportDTO;
import com.ican.model.dto.MusicItemDTO;
import com.ican.model.vo.MusicItemVO;
import com.ican.model.vo.MusicLibraryVO;
import com.ican.model.vo.MusicPlaylistVO;
import com.ican.service.MusicService;
import com.ican.utils.BeanCopyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 音乐库服务实现
 */
@Service
@RequiredArgsConstructor
public class MusicServiceImpl extends ServiceImpl<MusicItemMapper, MusicItem> implements MusicService {

    private static final String DEFAULT_COVER =
            "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=900&q=80";
    private static final String DEFAULT_NETEASE_PLAYLIST_ID = "7573319758";
    private static final String DEFAULT_NETEASE_PLAYLIST_NAME = "我的网易云歌曲";
    private static final Pattern NETEASE_ID_QUERY_PATTERN = Pattern.compile("[?&#]id=(\\d{4,})");
    private static final Pattern NETEASE_ID_PATH_PATTERN = Pattern.compile("playlist/(\\d{4,})");

    private final MusicItemMapper musicItemMapper;
    private final MusicPlaylistMapper musicPlaylistMapper;
    private final MetingPlaylistClient metingPlaylistClient;

    @Override
    public MusicLibraryVO listMusicLibrary(String keyword, String playlistId, String tag, String mood, String sortType) {
        LambdaQueryWrapper<MusicItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.and(hasText(keyword), wrapper -> wrapper
                .like(MusicItem::getTitle, keyword)
                .or()
                .like(MusicItem::getArtist, keyword)
                .or()
                .like(MusicItem::getPlaylistName, keyword)
                .or()
                .like(MusicItem::getTags, keyword)
                .or()
                .like(MusicItem::getMood, keyword)
                .or()
                .like(MusicItem::getNote, keyword)
                .or()
                .like(MusicItem::getSummary, keyword));
        itemWrapper.eq(hasRealFilter(playlistId), MusicItem::getPlaylistId, playlistId);
        itemWrapper.like(hasRealFilter(tag), MusicItem::getTags, tag);
        itemWrapper.eq(hasRealFilter(mood), MusicItem::getMood, mood);
        applySort(itemWrapper, sortType);

        List<MusicItemVO> items = BeanCopyUtils.copyBeanList(nullSafe(musicItemMapper.selectList(itemWrapper)), MusicItemVO.class);
        List<MusicPlaylistVO> playlists = BeanCopyUtils.copyBeanList(nullSafe(musicPlaylistMapper.selectList(
                new LambdaQueryWrapper<MusicPlaylist>()
                        .orderByDesc(MusicPlaylist::getUpdateTime)
                        .orderByDesc(MusicPlaylist::getImportedAt)
        )), MusicPlaylistVO.class);
        return new MusicLibraryVO(items, playlists);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MusicItemVO saveMusicItem(MusicItemDTO musicItemDTO) {
        MusicItem musicItem = buildMusicItem(musicItemDTO, musicItemDTO.getId(), LocalDateTime.now());
        Assert.isNull(musicItemMapper.selectById(musicItem.getId()), "Music item already exists");
        upsertPlaylist(musicItem.getPlaylistId(), musicItem.getPlaylistName(), musicItem.getSourceUrl(),
                musicItem.getCover(), musicItem.getImportedAt());
        musicItemMapper.insert(musicItem);
        return BeanCopyUtils.copyBean(musicItem, MusicItemVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MusicItemVO updateMusicItem(String id, MusicItemDTO musicItemDTO) {
        MusicItem oldMusicItem = musicItemMapper.selectById(id);
        Assert.notNull(oldMusicItem, "Music item not found");
        MusicItem musicItem = buildMusicItem(musicItemDTO, id, oldMusicItem.getImportedAt());
        musicItemMapper.updateById(musicItem);
        upsertPlaylist(musicItem.getPlaylistId(), musicItem.getPlaylistName(), musicItem.getSourceUrl(),
                musicItem.getCover(), musicItem.getImportedAt());
        return BeanCopyUtils.copyBean(musicItem, MusicItemVO.class);
    }

    @Override
    public void deleteMusicItems(List<String> idList) {
        Assert.isFalse(CollectionUtils.isEmpty(idList), "Please select music items to delete");
        musicItemMapper.deleteBatchIds(idList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MusicLibraryVO importNeteasePlaylist(MusicImportDTO musicImportDTO) {
        String playlistId = extractNeteasePlaylistId(musicImportDTO.getSource());
        Assert.isFalse(!hasText(playlistId), "请输入公开网易云歌单链接或歌单 ID");

        String playlistName = hasText(musicImportDTO.getPlaylistName())
                ? musicImportDTO.getPlaylistName().trim()
                : defaultPlaylistName(playlistId);
        String sourceUrl = "https://music.163.com/#/playlist?id=" + playlistId;
        List<MetingSongDTO> songs = metingPlaylistClient.listSongs(playlistId);
        Assert.isFalse(CollectionUtils.isEmpty(songs), "歌单为空或暂时无法读取");
        String playlistCover = songs.stream()
                .map(MetingSongDTO::getPic)
                .filter(this::hasText)
                .findFirst()
                .orElse(null);
        upsertPlaylist(playlistId, playlistName, sourceUrl, playlistCover, LocalDateTime.now());

        Set<String> existingKeys = new HashSet<>();
        for (MusicItem item : nullSafe(musicItemMapper.selectList(new LambdaQueryWrapper<MusicItem>()
                .eq(MusicItem::getPlaylistId, playlistId)))) {
            existingKeys.add(duplicateKey(item.getPlaylistId(), item.getTitle(), item.getArtist()));
        }

        LocalDateTime importedAt = LocalDateTime.now();
        List<MusicItemVO> importedItems = new ArrayList<>();
        for (int index = 0; index < songs.size(); index++) {
            MetingSongDTO song = songs.get(index);
            String title = defaultText(song.getTitle(), "未命名歌曲");
            String artist = defaultText(song.getAuthor(), "未知歌手");
            String duplicateKey = duplicateKey(playlistId, title, artist);
            if (existingKeys.contains(duplicateKey)) {
                continue;
            }
            MusicItem musicItem = normalizeMetingSong(song, playlistId, playlistName, sourceUrl, index, importedAt);
            musicItemMapper.insert(musicItem);
            importedItems.add(BeanCopyUtils.copyBean(musicItem, MusicItemVO.class));
            existingKeys.add(duplicateKey);
        }

        return new MusicLibraryVO(importedItems, BeanCopyUtils.copyBeanList(nullSafe(musicPlaylistMapper.selectList(
                new LambdaQueryWrapper<MusicPlaylist>().orderByDesc(MusicPlaylist::getUpdateTime)
        )), MusicPlaylistVO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetMusicLibrary() {
        musicItemMapper.delete(new LambdaQueryWrapper<>());
        musicPlaylistMapper.delete(new LambdaQueryWrapper<>());
    }

    private MusicItem buildMusicItem(MusicItemDTO dto, String id, LocalDateTime importedAt) {
        LocalDateTime now = LocalDateTime.now();
        String title = defaultText(dto.getTitle(), "未命名歌曲");
        String artist = defaultText(dto.getArtist(), "未知歌手");
        String sourceType = defaultText(dto.getSourceType(), "manual");
        String playlistId = defaultText(dto.getPlaylistId(), "manual");
        String playlistName = defaultText(dto.getPlaylistName(), "手动收藏");
        return MusicItem.builder()
                .id(hasText(id) ? id : sourceType + "-" + shortHash(title + "-" + artist + "-" + now))
                .title(title)
                .artist(artist)
                .album(trimToNull(dto.getAlbum()))
                .cover(defaultText(dto.getCover(), DEFAULT_COVER))
                .url(trimToNull(dto.getUrl()))
                .lyricUrl(trimToNull(dto.getLyricUrl()))
                .playlistId(playlistId)
                .playlistName(playlistName)
                .sourceType(sourceType)
                .sourceUrl(trimToNull(dto.getSourceUrl()))
                .tags(defaultText(dto.getTags(), ""))
                .mood(defaultText(dto.getMood(), "待整理"))
                .rating(clampRating(dto.getRating()))
                .note(defaultText(dto.getNote(), ""))
                .summary(defaultText(dto.getSummary(), "手动添加的收藏条目。"))
                .favoriteLevel(defaultText(dto.getFavoriteLevel(), "spark"))
                .isPinned(Boolean.TRUE.equals(dto.getIsPinned()))
                .importedAt(importedAt == null ? now : importedAt)
                .updateTime(now)
                .build();
    }

    private MusicItem normalizeMetingSong(MetingSongDTO song, String playlistId, String playlistName,
                                          String sourceUrl, int index, LocalDateTime importedAt) {
        String title = defaultText(song.getTitle(), "未命名歌曲");
        String artist = defaultText(song.getAuthor(), "未知歌手");
        return MusicItem.builder()
                .id("netease-" + playlistId + "-" + shortHash(title + "-" + artist + "-" + index))
                .title(title)
                .artist(artist)
                .cover(defaultText(song.getPic(), DEFAULT_COVER))
                .url(trimToNull(song.getUrl()))
                .lyricUrl(trimToNull(song.getLrc()))
                .playlistId(playlistId)
                .playlistName(playlistName)
                .sourceType("netease")
                .sourceUrl(sourceUrl)
                .tags("")
                .mood("待整理")
                .rating(3)
                .note("")
                .summary("从网易云导入的歌曲，等待补充收藏理由。")
                .favoriteLevel("spark")
                .isPinned(false)
                .importedAt(importedAt)
                .updateTime(importedAt)
                .build();
    }

    private void upsertPlaylist(String id, String name, String sourceUrl, String cover, LocalDateTime importedAt) {
        MusicPlaylist oldPlaylist = musicPlaylistMapper.selectById(id);
        LocalDateTime now = LocalDateTime.now();
        MusicPlaylist playlist = MusicPlaylist.builder()
                .id(id)
                .name(name)
                .sourceUrl(sourceUrl)
                .cover(hasText(cover) ? cover : (oldPlaylist == null ? null : oldPlaylist.getCover()))
                .importedAt(oldPlaylist == null ? importedAt : oldPlaylist.getImportedAt())
                .updateTime(now)
                .build();
        if (oldPlaylist == null) {
            musicPlaylistMapper.insert(playlist);
            return;
        }
        musicPlaylistMapper.updateById(playlist);
    }

    private void applySort(LambdaQueryWrapper<MusicItem> wrapper, String sortType) {
        if ("rating".equals(sortType)) {
            wrapper.orderByDesc(MusicItem::getRating).orderByDesc(MusicItem::getUpdateTime);
            return;
        }
        if ("title".equals(sortType)) {
            wrapper.orderByAsc(MusicItem::getTitle);
            return;
        }
        if ("newest".equals(sortType)) {
            wrapper.orderByDesc(MusicItem::getImportedAt);
            return;
        }
        wrapper.orderByDesc(MusicItem::getIsPinned)
                .orderByDesc(MusicItem::getRating)
                .orderByDesc(MusicItem::getUpdateTime);
    }

    private String extractNeteasePlaylistId(String source) {
        if (!hasText(source)) {
            return "";
        }
        String value = source.trim();
        if (value.matches("\\d{4,}")) {
            return value;
        }
        Matcher queryMatcher = NETEASE_ID_QUERY_PATTERN.matcher(value);
        if (queryMatcher.find()) {
            return queryMatcher.group(1);
        }
        Matcher pathMatcher = NETEASE_ID_PATH_PATTERN.matcher(value);
        return pathMatcher.find() ? pathMatcher.group(1) : "";
    }

    private String defaultPlaylistName(String playlistId) {
        return DEFAULT_NETEASE_PLAYLIST_ID.equals(playlistId)
                ? DEFAULT_NETEASE_PLAYLIST_NAME
                : "网易云歌曲 " + playlistId;
    }

    private String duplicateKey(String playlistId, String title, String artist) {
        return String.join("-",
                defaultText(playlistId, "").toLowerCase(Locale.ROOT),
                defaultText(title, "").toLowerCase(Locale.ROOT),
                defaultText(artist, "").toLowerCase(Locale.ROOT));
    }

    private int clampRating(Integer rating) {
        if (rating == null) {
            return 3;
        }
        return Math.min(5, Math.max(1, rating));
    }

    private boolean hasRealFilter(String value) {
        return hasText(value) && !"all".equals(value);
    }

    private String defaultText(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }

    private String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toUnsignedString(Objects.hash(value), 36);
        }
    }
}
