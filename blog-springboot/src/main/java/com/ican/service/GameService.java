package com.ican.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ican.entity.Game;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.dto.GameDTO;
import com.ican.model.vo.GameVO;

import java.util.List;
import java.util.Map;

public interface GameService extends IService<Game> {

    /**
     * 查看游戏库列表
     *
     * @param condition 条件
     * @return 游戏列表
     */
    Map<String, Object> getGameList(ConditionDTO condition);

    /**
     * 查看游戏库列表
     *
     * @param condition 条件
     * @return 游戏列表
     */
    Map<String, Object> getAdminGameList(ConditionDTO condition);

    /**
     * 获取游戏详情
     *
     * @param id 游戏id
     * @return 游戏详情
     */
    GameVO getGameById(Long id);

    /**
     * 新增游戏
     *
     * @param gameDTO 游戏信息
     */
    void addGame(GameDTO gameDTO);

    /**
     * 更新游戏
     *
     * @param gameDTO 游戏信息
     */
    void updateGame(GameDTO gameDTO);

    /**
     * 删除游戏
     *
     * @param ids 游戏id列表
     */
    void deleteGameBatch(List<Long> ids);

}
