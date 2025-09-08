package com.ican.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ican.entity.BlogFile;
import com.ican.enums.FilePathEnum;
import com.ican.mapper.BlogFileMapper;
import com.ican.mapper.GameMapper;
import com.ican.entity.Game;
import com.ican.model.dto.ConditionDTO;
import com.ican.model.dto.GameDTO;
import com.ican.model.vo.GameVO;
import com.ican.service.GameService;
import com.ican.strategy.context.UploadStrategyContext;
import com.ican.utils.BeanCopyUtils;
import com.ican.utils.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static com.ican.constant.CommonConstant.FALSE;

@Service
public class GameServiceImpl extends ServiceImpl<GameMapper, Game>  implements GameService {

    @Autowired
    private GameMapper gameMapper;
    @Autowired
    private BlogFileMapper blogFileMapper;
    @Autowired
    private UploadStrategyContext uploadStrategyContext;

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

    /**
     * 删除游戏
     */
    @Override
    public void deleteGameBatch(List<Long> ids) {
        gameMapper.deleteBatchIds(ids);
    }

    /**
     * 上传说说图片
     */
    @Override
    public String uploadTalkCover(MultipartFile file) {
        // 上传文件
        String url = uploadStrategyContext.executeUploadStrategy(file, FilePathEnum.TALK.getPath());
        try {
            // 获取文件md5值
            String md5 = FileUtils.getMd5(file.getInputStream());
            // 获取文件扩展名
            String extName = FileUtils.getExtension(file);
            BlogFile existFile = blogFileMapper.selectOne(new LambdaQueryWrapper<BlogFile>()
                    .select(BlogFile::getId)
                    .eq(BlogFile::getFileName, md5)
                    .eq(BlogFile::getFilePath, FilePathEnum.TALK.getFilePath()));
            if (Objects.isNull(existFile)) {
                // 保存文件信息
                BlogFile newFile = BlogFile.builder()
                        .fileUrl(url)
                        .fileName(md5)
                        .filePath(FilePathEnum.TALK.getFilePath())
                        .extendName(extName)
                        .fileSize((int) file.getSize())
                        .isDir(FALSE)
                        .build();
                blogFileMapper.insert(newFile);
            }
        } catch (IOException e) {
            log.error("文件上传持久化错误: {}" + e.getMessage());
        }
        return url;
    }
}
