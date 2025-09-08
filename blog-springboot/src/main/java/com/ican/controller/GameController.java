package com.ican.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ican.annotation.OptLogger;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.dto.GameDTO;
import com.ican.model.vo.GameVO;
import com.ican.model.vo.Result;
import com.ican.service.GameService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static com.ican.constant.OptTypeConstant.UPLOAD;

@RestController
@RequestMapping("/games")
public class GameController {

    @Autowired
    private GameService gameService;

    /**
     * 获取游戏列表（分页）
     */
    @ApiOperation("查看游戏库列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> getGameList(ConditionDTO condition) {
        return Result.success(gameService.getGameList(condition));
    }

    /**
     * 上传说说图片
     *
     * @param file 文件
     * @return {@link Result<String>}
     */
    @OptLogger(value = UPLOAD)
    @ApiOperation(value = "上传截图图片")
    @ApiImplicitParam(name = "file", value = "截图图片", required = true, dataType = "MultipartFile")
    @SaCheckPermission("game:upload")
    @PostMapping("/admin/upload")
    public Result<String> uploadTalkCover(@RequestParam("file") MultipartFile file) {
        return Result.success(gameService.uploadTalkCover(file));
    }
    /**
     * 获取游戏列表（分页）
     */
    @ApiOperation("查看游戏库列表")
    @GetMapping("/admin/list")
    public Result<Map<String, Object>> getAdminGameList(ConditionDTO condition) {
        return Result.success(gameService.getAdminGameList(condition));
    }

/*    *//**
     * 获取单个游戏详情
     *//*
    @ApiOperation("获取游戏详情")
    @GetMapping("/admin/{id}")
    public Result<GameVO> getGame(@PathVariable("id") Long id) {
        return Result.success(gameService.getGameById(id));
    }*/

    /**
     * 新增游戏
     */
    @ApiOperation("新增游戏")
    @PostMapping("/admin/add")
    public Result<Void> addGame(@RequestBody GameDTO gameDTO) {
        gameService.addGame(gameDTO);
        return Result.success();
    }

    /**
     * 更新游戏
     */
    @ApiOperation("更新游戏")
    @PutMapping("/admin/update")
    public Result<Void> updateGame(@RequestBody GameDTO gameDTO) {
        gameService.updateGame(gameDTO);
        return Result.success();
    }

    /**
     * 删除游戏（支持批量）
     */
    @ApiOperation("删除游戏")
    @DeleteMapping("/admin/delete")
    public Result<Void> deleteGame(@RequestBody List<Long> ids) {
        gameService.deleteGameBatch(ids);
        return Result.success();
    }
}
