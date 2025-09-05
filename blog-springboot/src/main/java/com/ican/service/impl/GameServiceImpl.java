package com.ican.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ican.mapper.GameMapper;
import com.ican.entity.Game;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.vo.GameVO;
import com.ican.service.GameService;
import com.ican.utils.BeanCopyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GameServiceImpl extends ServiceImpl<GameMapper, Game>  implements GameService {

    @Autowired
    private GameMapper gameMapper;

    /**
     * 获取游戏库列表
     */
    @Override
    public Map<String, Object> getGameList(ConditionDTO condition) {
        List<GameVO> gameList = new ArrayList<>();
        List<Game> games = gameMapper.getGameList(
                (condition.getCurrent() - 1) * condition.getSize(),
                condition.getSize(),
                condition
        );
        for (Game game : games) {
            if (game.getTags() != null) {
                GameVO gameVO = BeanCopyUtils.copyBean(game, GameVO.class);
                gameVO.setTags(Arrays.asList(game.getTags().split(",")));
                gameList.add(gameVO);
            }
        }
        int count = gameMapper.getTotalCount();
        Map<String, Object> result = new HashMap<>();
        result.put("recordList", gameList);
        result.put("count", count);
        return result;
    }
}
