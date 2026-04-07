package com.ican.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ican.annotation.OptLogger;
import com.ican.annotation.VisitLogger;
import com.ican.model.dto.AlbumDTO;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.vo.AlbumBackVO;
import com.ican.model.vo.AlbumVO;
import com.ican.model.vo.PageResult;
import com.ican.model.vo.Result;
import com.ican.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.ican.constant.OptTypeConstant.*;

/**
 * 相册控制器
 *
 * @author xcs
 * @date 2022/12/29 22:10
 **/
@RestController
public class AlbumController {


    @Autowired
    private AlbumService albumService;

    /**
     * 查看后台相册列表
     *
     * @param condition 条件
     * @return {@link PageResult<AlbumBackVO>} 后台相册列表
     */
    @SaCheckPermission("web:album:list")
    @GetMapping("/admin/album/list")
    public Result<PageResult<AlbumBackVO>> listAlbumBackVO(ConditionDTO condition) {
        return Result.success(albumService.listAlbumBackVO(condition));

    }

    /**
     * 上传相册封面
     *
     * @param file 文件
     * @return {@link Result<String>} 相册封面地址
     */
    @OptLogger(value = UPLOAD)
    @SaCheckPermission("web:album:upload")
    @PostMapping("/admin/album/upload")
    public Result<String> uploadAlbumCover(@RequestParam("file") MultipartFile file) {
        return Result.success(albumService.uploadAlbumCover(file));
    }

    /**
     * 添加相册
     *
     * @param album 相册信息
     * @return {@link Result<>}
     */
    @OptLogger(value = ADD)
    @SaCheckPermission("web:album:add")
    @PostMapping("/admin/album/add")
    public Result<?> addAlbum(@Validated @RequestBody AlbumDTO album) {
        albumService.addAlbum(album);
        return Result.success();
    }

    /**
     * 删除相册
     *
     * @param albumId 相册id
     * @return {@link Result}
     */
    @OptLogger(value = DELETE)
    @SaCheckPermission("web:album:delete")
    @DeleteMapping("/admin/album/delete/{albumId}")
    public Result<?> deleteAlbum(@PathVariable("albumId") Integer albumId) {
        albumService.deleteAlbum(albumId);
        return Result.success();
    }

    /**
     * 修改相册
     *
     * @param album 相册信息
     * @return {@link Result<>}
     */
    @OptLogger(value = UPDATE)
    @SaCheckPermission("web:album:update")
    @PutMapping("/admin/album/update")
    public Result<?> updateAlbum(@Validated @RequestBody AlbumDTO album) {
        albumService.updateAlbum(album);
        return Result.success();
    }

    /**
     * 编辑相册
     *
     * @param albumId 相册id
     * @return {@link Result<AlbumDTO>} 相册
     */
    @SaCheckPermission("web:album:edit")
    @GetMapping("/admin/album/edit/{albumId}")
    public Result<AlbumDTO> editAlbum(@PathVariable("albumId") Integer albumId) {
        return Result.success(albumService.editAlbum(albumId));
    }

    /**
     * 查看相册列表
     *
     * @return {@link Result<AlbumVO> 相册列表
     */
    @VisitLogger(value = "相册")
    @GetMapping("/album/list")
    public Result<List<AlbumVO>> listAlbumVO() {
        return Result.success(albumService.listAlbumVO());
    }

}
