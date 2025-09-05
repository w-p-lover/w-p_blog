package com.ican.controller;


import com.ican.model.dto.ConditionDTO;
import com.ican.model.vo.Result;
import com.ican.service.GameService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/games")
public class GameController {

    @Autowired
    private GameService gameService;

    /**
     * 查看游戏库列表
     *
     * @param condition 条件
     * @return {@link Result<Map<String, Object>>} 游戏列表
     */
    @ApiOperation(value = "查看游戏库列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> getGameList(ConditionDTO condition) {

        // 获取游戏列表
        Map<String, Object> data = gameService.getGameList(condition);
        return Result.success(data);
    }
}
