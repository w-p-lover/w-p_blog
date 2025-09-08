package com.ican.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ican.mapper.GameMapper;
import com.ican.entity.Game;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.dto.GameDTO;
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
            GameVO gameVO = BeanCopyUtils.copyBean(game, GameVO.class);
            if (game.getTags() != null) {
                gameVO.setTags(Arrays.asList(game.getTags().split(",")));
            }
            if (game.getScreenshotUrl() != null){
                gameVO.setScreenshotUrl(Arrays.asList(game.getScreenshotUrl().split(",")));
            }
            gameList.add(gameVO);
        }
        int count = gameMapper.getTotalCount();
        Map<String, Object> result = new HashMap<>();
        result.put("recordList", gameList);
        result.put("count", count);
        return result;
    }

    @Override
    public Map<String, Object> getAdminGameList(ConditionDTO condition) {
        List<GameVO> gameList = new ArrayList<>();
        List<Game> games = gameMapper.getAdminGameList(
                (condition.getCurrent() - 1) * condition.getSize(),
                condition.getSize(),
                condition
        );
        for (Game game : games) {
            GameVO gameVO = BeanCopyUtils.copyBean(game, GameVO.class);
            if (game.getTags() != null) {
                gameVO.setTags(Arrays.asList(game.getTags().split(",")));
            }
            if (game.getScreenshotUrl() != null){
                gameVO.setScreenshotUrl(Arrays.asList(game.getScreenshotUrl().split(",")));
            }
            gameList.add(gameVO);
        }
        int count = gameMapper.getTotalCount();
        Map<String, Object> result = new HashMap<>();
        result.put("recordList", gameList);
        result.put("count", count);
        return result;
    }

    @Override
    public GameVO getGameById(Long id) {
        Game game = gameMapper.selectById(id);
        GameVO gameVO = BeanCopyUtils.copyBean(game, GameVO.class);
        if (game.getTags() != null) {
            gameVO.setTags(Arrays.asList(game.getTags().split(",")));
        }
        if (game.getScreenshotUrl() != null){
            gameVO.setScreenshotUrl(Arrays.asList(game.getScreenshotUrl().split(",")));
        }
        return gameVO;
    }

    @Override
    public void addGame(GameDTO gameDTO) {
        Game game = BeanCopyUtils.copyBean(gameDTO, Game.class);
        if (gameDTO.getTags() != null) {
            game.setTags(String.join(",", gameDTO.getTags()));
        }
        if (gameDTO.getScreenshotUrl() != null) {
            game.setTags(String.join(",", gameDTO.getScreenshotUrl()));
        }

        gameMapper.insert(game);
    }

    @Override
    public void updateGame(GameDTO gameDTO) {
        Game game = BeanCopyUtils.copyBean(gameDTO, Game.class);
        if (gameDTO.getTags() != null) {
            game.setTags(String.join(",", gameDTO.getTags()));
        }
        if (gameDTO.getScreenshotUrl() != null) {
            game.setTags(String.join(",", gameDTO.getScreenshotUrl()));
        }
        gameMapper.updateById(game);
    }

    @Override
    public void deleteGameBatch(List<Long> ids) {
        gameMapper.deleteBatchIds(ids);
    }
}
