package com.ican.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ican.entity.Game;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.vo.GameVO;

import java.util.Map;


public interface GameService extends IService<Game> {

    /**
     * 获取游戏列表
     *
     * @param condition 条件
     * @return {@link Map<String, Object>} 游戏列表
     */
    Map<String, Object> getGameList(ConditionDTO condition);

}
