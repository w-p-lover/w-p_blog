package com.ican.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ican.annotation.OptLogger;
import com.ican.annotation.VisitLogger;
import com.ican.model.dto.MusicImportDTO;
import com.ican.model.dto.MusicItemDTO;
import com.ican.model.vo.MusicItemVO;
import com.ican.model.vo.MusicLibraryVO;
import com.ican.model.vo.Result;
import com.ican.service.MusicService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ican.constant.OptTypeConstant.ADD;
import static com.ican.constant.OptTypeConstant.DELETE;
import static com.ican.constant.OptTypeConstant.UPDATE;

/**
 * 音乐库控制器
 */
@RestController
@RequiredArgsConstructor
public class MusicController {

    private final MusicService musicService;

    @VisitLogger(value = "音乐库")
    @GetMapping("/music/library")
    public Result<MusicLibraryVO> listMusicLibrary(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String playlistId,
                                                   @RequestParam(required = false) String tag,
                                                   @RequestParam(required = false) String mood,
                                                   @RequestParam(required = false, defaultValue = "curated") String sortType) {
        return Result.success(musicService.listMusicLibrary(keyword, playlistId, tag, mood, sortType));
    }

    @OptLogger(value = ADD)
    @SaCheckPermission("music:item:add")
    @PostMapping("/music/item")
    public Result<MusicItemVO> saveMusicItem(@Validated @RequestBody MusicItemDTO musicItemDTO) {
        return Result.success(musicService.saveMusicItem(musicItemDTO));
    }

    @OptLogger(value = UPDATE)
    @SaCheckPermission("music:item:update")
    @PutMapping("/music/item/{id}")
    public Result<MusicItemVO> updateMusicItem(@PathVariable("id") String id,
                                               @Validated @RequestBody MusicItemDTO musicItemDTO) {
        return Result.success(musicService.updateMusicItem(id, musicItemDTO));
    }

    @OptLogger(value = DELETE)
    @SaCheckPermission("music:item:delete")
    @DeleteMapping("/music/item")
    public Result<?> deleteMusicItems(@RequestBody List<String> idList) {
        musicService.deleteMusicItems(idList);
        return Result.success();
    }

    @OptLogger(value = ADD)
    @SaCheckPermission("music:library:import")
    @PostMapping("/music/import/netease")
    public Result<MusicLibraryVO> importNeteasePlaylist(@Validated @RequestBody MusicImportDTO musicImportDTO) {
        return Result.success(musicService.importNeteasePlaylist(musicImportDTO));
    }

    @OptLogger(value = DELETE)
    @SaCheckPermission("music:library:reset")
    @DeleteMapping("/music/library/reset")
    public Result<?> resetMusicLibrary() {
        musicService.resetMusicLibrary();
        return Result.success();
    }
}
