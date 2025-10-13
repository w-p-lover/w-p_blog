package com.ican.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ican.annotation.OptLogger;
import com.ican.annotation.VisitLogger;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.dto.PhotoDTO;
import com.ican.model.dto.PhotoInfoDTO;
import com.ican.model.vo.*;
import com.ican.service.PhotoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static com.ican.constant.OptTypeConstant.*;

/**
 * 照片控制器
 *
 * @author xcs
 * @date 2022/12/30 16:35
 **/
@Api(tags = "照片模块")
@RestController
@Slf4j
public class PhotoController {

    @Autowired
    private PhotoService photoService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final AtomicReference<String> spiderStatus = new AtomicReference<>("IDLE");
    /**
     * 查看后台照片列表
     *
     * @param condition 条件
     * @return {@link Result<PhotoBackVO>} 后台照片列表
     */
    @ApiOperation(value = "查看后台照片列表")
    @SaCheckPermission("web:photo:list")
    @GetMapping("/admin/photo/list")
    public Result<PageResult<PhotoBackVO>> listPhotoBackVO(ConditionDTO condition) {
        return Result.success(photoService.listPhotoBackVO(condition));
    }

    /**
     * 查看照片相册信息
     *
     * @param albumId 相册id
     * @return {@link Result<AlbumBackVO>} 相册信息
     */
    @ApiOperation(value = "查看照片相册信息")
    @SaCheckPermission("web:photo:list")
    @GetMapping("/admin/photo/album/{albumId}/info")
    public Result<AlbumBackVO> getAlbumInfo(@PathVariable("albumId") Integer albumId) {
        return Result.success(photoService.getAlbumInfo(albumId));
    }

    /**
     * 上传照片
     *
     * @param file 文件
     * @return {@link Result<String>} 照片地址
     */
    @OptLogger(value = UPLOAD)
    @ApiOperation(value = "上传照片")
    @ApiImplicitParam(name = "file", value = "照片", required = true, dataType = "MultipartFile")
    @SaCheckPermission("web:photo:upload")
    @PostMapping("/admin/photo/upload")
    public Result<String> uploadPhoto(@RequestParam("file") MultipartFile file) {
        return Result.success(photoService.uploadPhoto(file));
    }

    /**
     * 添加照片
     *
     * @param photo 照片
     * @return {@link Result<>}
     */
    @OptLogger(value = ADD)
    @ApiOperation(value = "添加照片")
    @SaCheckPermission("web:photo:add")
    @PostMapping("/admin/photo/add")
    public Result<?> addPhoto(@Validated @RequestBody PhotoDTO photo) {
        photoService.addPhoto(photo);
        return Result.success();
    }

    /**
     * 修改照片信息
     *
     * @param photoInfo 照片信息
     * @return {@link Result<>}
     */
    @OptLogger(value = UPDATE)
    @ApiOperation(value = "修改照片信息")
    @SaCheckPermission("web:photo:update")
    @PutMapping("/admin/photo/update")
    public Result<?> updatePhoto(@Validated @RequestBody PhotoInfoDTO photoInfo) {
        photoService.updatePhoto(photoInfo);
        return Result.success();
    }

    /**
     * 删除照片
     *
     * @param photoIdList 照片id列表
     * @return {@link Result<>}
     */
    @OptLogger(value = DELETE)
    @ApiOperation(value = "删除照片")
    @SaCheckPermission("web:photo:delete")
    @DeleteMapping("/admin/photo/delete")
    public Result<?> deletePhoto(@RequestBody List<Integer> photoIdList) {
        photoService.deletePhoto(photoIdList);
        return Result.success();
    }

    /**
     * 移动照片
     *
     * @param photo 照片信息
     * @return {@link Result<>}
     */
    @OptLogger(value = UPDATE)
    @ApiOperation(value = "移动照片")
    @SaCheckPermission("web:photo:move")
    @PutMapping("/admin/photo/move")
    public Result<?> movePhoto(@Validated @RequestBody PhotoDTO photo) {
        photoService.movePhoto(photo);
        return Result.success();
    }

    /**
     * 查看照片列表
     *
     * @return {@link Result<PhotoVO> 照片列表
     */
    @VisitLogger(value = "照片")
    @ApiOperation(value = "查看照片列表")
    @GetMapping("/photo/list")
    public Result<Map<String, Object>> listPhotoVO(ConditionDTO condition) {
        return Result.success(photoService.listPhotoVO(condition));
    }

    @PostMapping("/photo/run")
    public Result<?> runSpider() {
        if ("RUNNING".equals(spiderStatus.get())) {
            return Result.fail("爬虫正在运行，请稍后重试");
        }
        spiderStatus.set("RUNNING");
        executor.submit(() -> {
            try {
                photoService.runPythonSpider(spiderStatus);
                spiderStatus.set("COMPLETED");
            } catch (Exception e) {
                spiderStatus.set("FAILED");
                log.error("爬虫任务发生异常：{}", e.getMessage());
            }
        });
        return Result.success("爬虫任务已启动");
    }

    @GetMapping("/photo/status")
    public Result<?> getStatus() {
        Map<String, Object> statusInfo = new HashMap<>();
        double photoCount = photoService.getPhotoCount();
        double spiderPercentage = photoCount * 100 / 24;
        String message;
        switch (spiderStatus.get()) {
            case "RUNNING":
                message = "爬虫正在运行中...";
                break;
            case "INSERTING":
                message = "正在插入图片数据...";
                break;
            case "COMPLETED":
                message = "爬虫任务已完成 ✅";
                break;
            case "FAILED":
                message = "爬虫任务失败 ❌";
                break;
            default:
                message = "空闲中";
                break;
        }

        statusInfo.put("status", spiderStatus.get());
        statusInfo.put("timestamp", System.currentTimeMillis());
        statusInfo.put("message", message);
        statusInfo.put("spiderPercentage", spiderPercentage);
        return Result.success(statusInfo);
    }

}