package com.ican.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ican.annotation.OptLogger;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.dto.FolderDTO;
import com.ican.model.vo.FileVO;
import com.ican.model.vo.PageResult;
import com.ican.model.vo.Result;
import com.ican.service.BlogFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.ican.constant.OptTypeConstant.*;

/**
 * 文件控制器
 *
 * @author xcs
 * @date 2023/03/08 12:36
 **/
@RestController
public class BlogFileController {

    @Autowired
    private BlogFileService fileService;

    /**
     * 查看文件列表
     *
     * @param condition 查询条件
     * @return {@link Result<FileVO>} 文件列表
     */
    @SaCheckPermission("system:file:list")
    @GetMapping("/admin/file/list")
    public Result<PageResult<FileVO>> listFileVOList(ConditionDTO condition) {
        return Result.success(fileService.listFileVOList(condition));
    }

    /**
     * 上传文件
     *
     * @param file 文件
     * @return {@link Result<>}
     */
    @OptLogger(value = UPLOAD)
    @SaCheckPermission("system:file:upload")
    @PostMapping("/admin/file/upload")
    public Result<?> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("path") String path) {
        fileService.uploadFile(file, path);
        return Result.success();
    }

    /**
     * 创建目录
     *
     * @param folder 目录信息
     * @return {@link Result<>}
     */
    @OptLogger(value = ADD)
    @SaCheckPermission("system:file:createFolder")
    @PostMapping("/admin/file/createFolder")
    public Result<?> createFolder(@Validated @RequestBody FolderDTO folder) {
        fileService.createFolder(folder);
        return Result.success();
    }

    /**
     * 删除文件
     *
     * @param fileIdList 文件id列表
     * @return {@link Result<>}
     */
    @OptLogger(value = DELETE)
    @SaCheckPermission("system:file:delete")
    @DeleteMapping("/admin/file/delete")
    public Result<?> deleteFile(@RequestBody List<Integer> fileIdList) {
        fileService.deleteFile(fileIdList);
        return Result.success();
    }

    /**
     * 下载文件
     *
     * @param fileId 文件id
     * @return {@link Result<>}
     */
    @GetMapping("/file/download/{fileId}")
    public Result<?> downloadFile(@PathVariable("fileId") Integer fileId) {
        fileService.downloadFile(fileId);
        return Result.success();
    }

}